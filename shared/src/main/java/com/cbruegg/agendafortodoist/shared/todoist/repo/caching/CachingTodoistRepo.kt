package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import com.cbruegg.agendafortodoist.shared.todoist.api.TodoistApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.NewTask
import com.cbruegg.agendafortodoist.shared.todoist.repo.Project
import com.cbruegg.agendafortodoist.shared.todoist.repo.Task
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistNetworkException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepo
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepoException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistServiceException
import com.cbruegg.agendafortodoist.shared.util.retry
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newSingleThreadContext
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachingTodoistRepo @Inject constructor(
    private val todoist: TodoistApi
) : TodoistRepo {

    private val coroutineContext = newSingleThreadContext(CachingTodoistRepo::class.simpleName ?: "")

    // TODO Save and restore this
    // TODO Schedule this to retry
    private var updateSteps = UpdateSteps.initial

    // TODO Make this caching

    suspend fun processQueue() = async(coroutineContext) {
        try {
            errorHandled {
                updateSteps.sendTo(todoist)
            }
            updateSteps = UpdateSteps.initial
        } catch (e: TodoistRepoException) {
            e.printStackTrace()
        }
    }.await()

    override fun projects() = async(coroutineContext) {
        errorHandled {
            todoist.projects().await().map { Project(it) }
            // TODO Catch exception and return from cache
        }
    }

    override fun tasks(projectId: Long?, labelId: Long?) = async(coroutineContext) {
        val tasks = errorHandled {
            // TODO Cached?
            todoist.tasks(projectId, labelId).await().map { Task(it) }
        }
        // TODO Catch exception and return from cache
        updateSteps.applyTo(tasks)
    }

    override fun closeTask(task: Task, requestId: Int) = async(coroutineContext) {
        updateSteps += CloseTaskUpdateStep(task, requestId)
        processQueue()
    }

    override fun reopenTask(task: Task, requestId: Int) = async(coroutineContext) {
        updateSteps += ReopenTaskUpdateStep(task, requestId)
        processQueue()
    }

    override fun addTask(requestId: Int, task: NewTask) = async(coroutineContext) {
        updateSteps += AddTaskUpdateStep(task, requestId)
        processQueue()
    }

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