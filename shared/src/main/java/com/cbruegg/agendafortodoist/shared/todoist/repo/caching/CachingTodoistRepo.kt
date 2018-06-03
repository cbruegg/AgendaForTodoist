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
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CachingTodoistRepo @Inject constructor(
    private val todoist: TodoistApi,
    private val updateStepsPersister: UpdateStepsPersister,
    private val virtualIdToReadIdPersister: VirtualIdToRealIdPersister
) : TodoistRepo {

    // TODO Make this caching

    private suspend fun Task.insertRealIdIfNeeded(): Task {
        return virtualIdToReadIdPersister.readOnly { virtualIdsToRealIds ->
            val realId = virtualIdsToRealIds[id]
            if (realId != null) copy(id = realId) else this
        }
    }

    private suspend fun processQueue() {
        try {
            updateStepsPersister.processQueue(todoist, virtualIdToReadIdPersister)
        } catch (e: TodoistRepoException) {
            scheduleSendJob()
        }
    }

    override fun projects() = async {
        errorHandled {
            todoist.projects().await().map { Project(it) }
            // TODO Catch exception and return from cache
        }
    }

    override fun tasks(projectId: Long?, labelId: Long?) = async {
        val tasks = errorHandled {
            // TODO Cached?
            todoist.tasks(projectId, labelId).await().map { Task(it) }
        }
        // TODO Catch exception and return from cache
        updateStepsPersister.readOnly { it.applyTo(tasks) }
    }

    override fun closeTask(task: Task, requestId: Int) = async {
        updateStepsPersister.inTransaction {
            it + CloseTaskUpdateStep(task.insertRealIdIfNeeded(), requestId)
        }
        processQueue()
    }

    override fun reopenTask(task: Task, requestId: Int) = async {
        updateStepsPersister.inTransaction {
            it + ReopenTaskUpdateStep(task.insertRealIdIfNeeded(), requestId)
        }
        processQueue()
    }

    override fun addTask(requestId: Int, task: NewTask) = async {
        updateStepsPersister.inTransaction {
            it + AddTaskUpdateStep(task, requestId)
        }
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