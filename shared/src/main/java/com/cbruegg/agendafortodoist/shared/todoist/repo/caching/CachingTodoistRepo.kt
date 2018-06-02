package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import com.cbruegg.agendafortodoist.shared.todoist.api.TodoistApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.NewTask
import com.cbruegg.agendafortodoist.shared.todoist.repo.Project
import com.cbruegg.agendafortodoist.shared.todoist.repo.Task
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistNetworkException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepo
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepoException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistServiceException
import com.cbruegg.agendafortodoist.shared.util.UniqueRequestIdGenerator
import com.cbruegg.agendafortodoist.shared.util.retry
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newSingleThreadContext
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.await
import ru.gildor.coroutines.retrofit.awaitResponse
import java.io.IOException
import javax.inject.Inject

class CachingTodoistRepo @Inject constructor(
    private val todoist: TodoistApi,
    private val requestIdGenerator: UniqueRequestIdGenerator
) : TodoistRepo {

    private val coroutineContext = newSingleThreadContext(CachingTodoistRepo::class.simpleName ?: "")

    // TODO Save and restore this
    // TODO Schedule this to retry
    private var updateSteps = UpdateSteps()

    // TODO Make this caching

    // TODO On any action, if updateSteps non-empty, enqueue the action
    // TODO After processing queue, update UI

    fun processQueue() = async(coroutineContext) {
        try {
            errorHandled {
                updateSteps.sendTo(todoist, requestIdGenerator.nextRequestId())
            }
            updateSteps = UpdateSteps()
        } catch (e: TodoistRepoException) {
            e.printStackTrace()
        }
    }

    override fun projects() = async(coroutineContext) {
        errorHandled {
            todoist.projects().await().map { Project(it) }
            // TODO Catch exception and return from cache
        }
    }

    override fun tasks(projectId: Long?, labelId: Long?) = async(coroutineContext) {
        val tasks = errorHandled {
            todoist.tasks(projectId, labelId).await().map { Task(it) }
        }
        // TODO Catch exception and return from cache
        updateSteps.applyTo(tasks)
    }

    override fun closeTask(task: Task, requestId: Int) = async(coroutineContext) {
        if (updateSteps.hasSteps) {
            updateSteps += CloseTaskUpdateStep(task)
            return@async
        }

        try {
            errorHandled {
                task.requireNonVirtual()
                val resp = todoist.closeTask(task.id, requestId).awaitResponse()
                if (resp.code() !in 200..299) throw HttpException(resp)
            }
        } catch (e: TodoistRepoException) {
            e.printStackTrace()
            updateSteps += CloseTaskUpdateStep(task)
        }
        Unit
    }

    override fun reopenTask(task: Task, requestId: Int) = async(coroutineContext) {
        if (updateSteps.hasSteps) {
            updateSteps += ReopenTaskUpdateStep(task)
            return@async
        }

        try {
            errorHandled {
                task.requireNonVirtual()
                val resp = todoist.reopenTask(task.id, requestId).awaitResponse()
                if (resp.code() !in 200..299) throw HttpException(resp)
            }
        } catch (e: TodoistRepoException) {
            e.printStackTrace()
            updateSteps += ReopenTaskUpdateStep(task)
        }
        Unit
    }

    override fun addTask(requestId: Int, task: NewTask) = async(coroutineContext) {
        if (updateSteps.hasSteps) {
            updateSteps += AddTaskUpdateStep(task)
            return@async
        }

        try {
            errorHandled {
                val resp = todoist.addTask(requestId, task.toNewTaskDto()).awaitResponse()
                if (resp.code() !in 200..299) throw HttpException(resp)
            }
        } catch (e: TodoistRepoException) {
            e.printStackTrace()
            updateSteps += AddTaskUpdateStep(task)
        }
        Unit
    }

    private fun Task.requireNonVirtual() = require(!isVirtual) { "Task must not be virtual!" }

    private suspend fun <T> errorHandled(f: suspend () -> T): T {
        try {
            return retry(HttpException::class, IOException::class) {
                f()
            }
        } catch (e: HttpException) {
            if (e.response().code() == 401) {
                throw TodoistServiceException.Auth(e)
            } else {
                throw TodoistServiceException.General(e)
            }
        } catch (e: IOException) {
            throw TodoistNetworkException(e)
        }
    }

}