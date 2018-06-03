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
    private val virtualIdToReadIdPersister: VirtualIdToRealIdPersister,
    private val cache: Cache
) : TodoistRepo {

    private suspend fun Task.insertRealIdIfNeeded(): Task {
        return virtualIdToReadIdPersister.lockedProperty.readOnly { virtualIdsToRealIds ->
            val realId = virtualIdsToRealIds[id]
            if (realId != null) copy(id = realId) else this
        }
    }

    private suspend fun processQueue() {
        try {
            updateStepsPersister.lockedProperty.readOnly { it.sendTo(cache) }
            updateStepsPersister.processQueue(todoist, virtualIdToReadIdPersister)
        } catch (e: TodoistRepoException) {
            scheduleSendJob()
        }
    }

    override fun projects() = async {
        try {
            errorHandled {
                todoist
                    .projects()
                    .await()
                    .map { Project(it) }
                    .also { cache.cacheProjects(it) }
            }
        } catch (e: TodoistRepoException) {
            cache.retrieveProjects() ?: throw e
        }
    }

    override fun tasks(projectId: Long?) = async {
        val tasks = try {
            errorHandled {
                todoist
                    .tasks(projectId)
                    .await()
                    .map { Task(it) }
                    .also {
                        if (projectId != null) {
                            cache.cacheTasks(projectId, it)
                        }
                    }
            }
        } catch (e: TodoistRepoException) {
            projectId?.let { cache.retrieveTasks(it) } ?: throw e
        }
        updateStepsPersister.lockedProperty.readOnly { it.applyTo(tasks) }
    }

    override fun closeTask(task: Task, requestId: Int) = async {
        updateStepsPersister.lockedProperty.inTransaction {
            it + CloseTaskUpdateStep(task.insertRealIdIfNeeded(), requestId)
        }
        processQueue()
    }

    override fun reopenTask(task: Task, requestId: Int) = async {
        updateStepsPersister.lockedProperty.inTransaction {
            it + ReopenTaskUpdateStep(task.insertRealIdIfNeeded(), requestId)
        }
        processQueue()
    }

    override fun addTask(requestId: Int, task: NewTask) = async {
        updateStepsPersister.lockedProperty.inTransaction {
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