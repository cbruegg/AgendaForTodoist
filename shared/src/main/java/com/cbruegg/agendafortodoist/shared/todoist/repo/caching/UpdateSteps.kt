package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import android.util.Log
import com.cbruegg.agendafortodoist.shared.todoist.api.TodoistApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.NewTask
import com.cbruegg.agendafortodoist.shared.todoist.repo.Task
import com.cbruegg.agendafortodoist.shared.util.UniqueRequestIdGenerator
import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.list
import retrofit2.HttpException
import retrofit2.Response
import ru.gildor.coroutines.retrofit.awaitResponse

internal data class SendResult(val virtualToRealIds: Map<Long, Long> = emptyMap())

@Serializable
internal sealed class UpdateStep {
    abstract suspend fun sendTo(todoist: TodoistApi): SendResult
    abstract suspend fun sendTo(cache: Cache)
    abstract fun applyTo(tasks: List<Task>): List<Task>
    abstract fun getRequestId(): Int? // This has to be a function, otherwise the compiler produces incorrect bytecode
}

internal fun List<UpdateStep>.toUpdateSteps() = fold(UpdateSteps.initial) { acc, updateStep -> acc + updateStep }

@Serializable
internal class UpdateSteps private constructor(private val steps: List<UpdateStep> = emptyList()) : UpdateStep() {

    @Serializer(forClass = UpdateSteps::class)
    companion object : KSerializer<UpdateSteps> {

        val initial = UpdateSteps()

        override val serialClassDesc = SerialClassDescImpl("com.cbruegg.agendafortodoist.shared.todoist.repo.caching.UpdateSteps")

        override fun load(input: KInput): UpdateSteps {
            return UpdateSteps(PolymorphicSerializer.list.load(input).map { it as UpdateStep })
        }

        override fun save(output: KOutput, obj: UpdateSteps) {
            PolymorphicSerializer.list.save(output, obj.steps)
        }

    }

    override fun getRequestId(): Int? = null

    override suspend fun sendTo(todoist: TodoistApi): SendResult {
        val virtualToRealIds = steps
            .map { it -> it.sendTo(todoist) }
            .fold(emptyMap<Long, Long>()) { acc, sendResult -> acc + sendResult.virtualToRealIds }
        return SendResult(virtualToRealIds)
    }

    override suspend fun sendTo(cache: Cache) {
        steps.forEach { it.sendTo(cache) }
    }

    override fun applyTo(tasks: List<Task>) = steps.fold(tasks) { acc, updateStep -> updateStep.applyTo(acc) }

    operator fun plus(updateStep: UpdateStep): UpdateSteps = UpdateSteps(
        when (updateStep) {
            is CloseTaskUpdateStep -> {
                if (updateStep.task.isVirtual) {
                    steps.filter {
                        // Simply remove the action that adds this non-submitted task
                        it !is AddTaskUpdateStep || it.newTask.virtualId != updateStep.task.id
                    }
                } else {
                    // We can discard all actions opening this task before
                    steps.filter { it !is ReopenTaskUpdateStep || it.task.id != updateStep.task.id } + updateStep
                }
            }
            is ReopenTaskUpdateStep -> {
                if (updateStep.task.isVirtual) {
                    if (steps.none { it is AddTaskUpdateStep && it.newTask.virtualId == updateStep.task.id }) {
                        // We have removed the AddTaskUpdateStep before, so we need to recreate it
                        val requestId = steps.asSequence().mapNotNull { it.getRequestId() }.max() ?: UniqueRequestIdGenerator.nextRequestId()
                        steps + AddTaskUpdateStep(NewTask(updateStep.task.content, updateStep.task.projectId, updateStep.task.id), requestId)
                    } else {
                        steps
                    }
                } else {
                    // Since we're reopening this task anyway, we don't need to submit any
                    // steps closing it before
                    steps.filter { it !is CloseTaskUpdateStep || it.task.id != updateStep.task.id } + updateStep
                }
            }
            is AddTaskUpdateStep -> {
                steps + updateStep
            }
            is UpdateSteps -> {
                updateStep.steps.fold(this) { acc, step -> acc + step }.steps
            }
        }
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpdateSteps

        if (steps != other.steps) return false

        return true
    }

    override fun hashCode(): Int {
        return steps.hashCode()
    }

    override fun toString(): String {
        return "UpdateSteps(steps=$steps)"
    }

}

@Serializable
internal data class CloseTaskUpdateStep(val task: Task, val requestId: Int) : UpdateStep() {
    override suspend fun sendTo(todoist: TodoistApi): SendResult {
        val resp = todoist.closeTask(task.id, requestId).awaitResponse()
        return resp.handle { SendResult() }
    }

    override suspend fun sendTo(cache: Cache) {
        cache.retrieveTasks(task.projectId)
            ?.map { if (it.id == task.id) it.copy(isCompleted = true) else it }
            ?.let { cache.cacheTasks(task.projectId, it) }
    }

    override fun applyTo(tasks: List<Task>) = tasks.map {
        if (it.id == task.id) it.copy(isCompleted = true) else it
    }

    override fun getRequestId() = requestId
}

@Serializable
internal data class ReopenTaskUpdateStep(val task: Task, val requestId: Int) : UpdateStep() {
    override suspend fun sendTo(todoist: TodoistApi): SendResult {
        val resp = todoist.reopenTask(task.id, requestId).awaitResponse()
        return resp.handle { SendResult() }
    }

    override suspend fun sendTo(cache: Cache) {
        cache.retrieveTasks(task.projectId)
            ?.map { if (it.id == task.id) it.copy(isCompleted = false) else it }
            ?.let { cache.cacheTasks(task.projectId, it) }
    }

    override fun applyTo(tasks: List<Task>) = tasks.map {
        if (it.id == task.id) it.copy(isCompleted = false) else it
    }

    override fun getRequestId() = requestId
}

@Serializable
internal data class AddTaskUpdateStep(val newTask: NewTask, val requestId: Int) : UpdateStep() {
    override suspend fun sendTo(todoist: TodoistApi): SendResult {
        val resp = todoist.addTask(requestId, newTask.toNewTaskDto()).awaitResponse()
        return resp.handle { SendResult(mapOf(newTask.virtualId to it.id)) }
    }

    override suspend fun sendTo(cache: Cache) {
        cache.retrieveTasks(newTask.projectId)
            ?.plus(newTask.toTask())
            ?.let { cache.cacheTasks(newTask.projectId, it) }
    }

    override fun applyTo(tasks: List<Task>) = tasks + newTask.toTask()

    override fun getRequestId() = requestId
}

private inline fun <T> Response<T>.handle(resultHandler: (T) -> SendResult): SendResult =
    if (code() !in 200..299) {
        Log.e("HttpResponse", "Received HTTP error with code ${code()}: ${errorBody()?.string()}")
        if (code() in 400..499) {
            SendResult()
        } else {
            throw HttpException(this)
        }
    } else {
        resultHandler(body()!!)
    }