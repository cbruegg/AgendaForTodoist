package com.cbruegg.agendafortodoist.shared.todoist.repo

import kotlinx.coroutines.experimental.Deferred
import retrofit2.HttpException
import java.io.IOException

interface TodoistRepo {
    fun projects(): Deferred<List<Project>>

    fun tasks(projectId: Long? = null, labelId: Long? = null): Deferred<List<Task>>

    fun closeTask(task: Task, requestId: Int): Deferred<Unit>

    fun reopenTask(task: Task, requestId: Int): Deferred<Unit>

    fun addTask(requestId: Int, task: NewTask): Deferred<Unit>
}

sealed class TodoistRepoException(open val e: Exception) : RuntimeException(e)

class TodoistNetworkException(override val e: IOException) : TodoistRepoException(e)

sealed class TodoistServiceException(override val e: HttpException) : TodoistRepoException(e) {
    class General(e: HttpException) : TodoistServiceException(e)
    class Auth(e: HttpException) : TodoistServiceException(e)
}