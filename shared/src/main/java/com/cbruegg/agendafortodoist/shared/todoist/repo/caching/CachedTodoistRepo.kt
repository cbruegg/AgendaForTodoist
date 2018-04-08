package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import com.cbruegg.agendafortodoist.shared.todoist.api.TodoistApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.NewTask
import com.cbruegg.agendafortodoist.shared.todoist.repo.Project
import com.cbruegg.agendafortodoist.shared.todoist.repo.Task
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistNetworkException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepo
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistServiceException
import com.cbruegg.agendafortodoist.shared.util.retry
import kotlinx.coroutines.experimental.async
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.await
import ru.gildor.coroutines.retrofit.awaitResponse
import java.io.IOException
import javax.inject.Inject

class CachedTodoistRepo @Inject constructor(
    private val todoist: TodoistApi
) : TodoistRepo {

    // TODO Make this caching

    override fun projects() = async {
        errorHandled {
            todoist.projects().await().map { Project(it) }
        }
    }

    override fun tasks(projectId: Long?, labelId: Long?) = async {
        errorHandled {
            todoist.tasks(projectId, labelId).await().map { Task(it) }
        }
    }

    override fun closeTask(taskId: Long, requestId: Int) = async {
        errorHandled {
            val resp = todoist.closeTask(taskId, requestId).awaitResponse()
            if (resp.code() !in 200..299) throw HttpException(resp)
        }
        Unit
    }

    override fun reopenTask(taskId: Long, requestId: Int) = async {
        errorHandled {
            val resp = todoist.reopenTask(taskId, requestId).awaitResponse()
            if (resp.code() !in 200..299) throw HttpException(resp)
        }
        Unit
    }

    override fun addTask(requestId: Int, task: NewTask) = async {
        errorHandled {
            val resp = todoist.addTask(requestId, task.toNewTaskDto()).awaitResponse()
            if (resp.code() !in 200..299) throw HttpException(resp)
        }
        Unit
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