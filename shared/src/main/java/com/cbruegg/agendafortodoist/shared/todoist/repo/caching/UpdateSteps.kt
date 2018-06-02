package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import com.cbruegg.agendafortodoist.shared.todoist.api.TodoistApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.NewTask
import com.cbruegg.agendafortodoist.shared.todoist.repo.Task
import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.list
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.awaitResponse

@Serializable
sealed class UpdateStep {
    abstract suspend fun sendTo(todoist: TodoistApi, requestId: Int)
    abstract fun applyTo(tasks: List<Task>): List<Task>
}

fun List<UpdateStep>.toUpdateSteps() = fold(UpdateSteps.initial) { acc, updateStep -> acc + updateStep }

@Serializable
class UpdateSteps private constructor(private val steps: List<UpdateStep> = emptyList()) : UpdateStep() {

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

    val hasSteps get() = steps.isNotEmpty()

    override suspend fun sendTo(todoist: TodoistApi, requestId: Int) {
        steps.forEachIndexed { index, updateStep -> updateStep.sendTo(todoist, requestId + index) }
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
                        steps + AddTaskUpdateStep(NewTask(updateStep.task.content, updateStep.task.projectId, updateStep.task.id))
                    } else {
                        steps
                    }
                } else {
                    // Since we're reopening this task anyway, we don't need to submit any
                    // steps closing it before
                    steps.filter { it !is CloseTaskUpdateStep || it.task.id != updateStep.task.id }
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
data class CloseTaskUpdateStep(val task: Task) : UpdateStep() {
    override suspend fun sendTo(todoist: TodoistApi, requestId: Int) {
        val resp = todoist.closeTask(task.id, requestId).awaitResponse()
        if (resp.code() !in 200..299) throw HttpException(resp)
    }

    override fun applyTo(tasks: List<Task>) = tasks.map {
        if (it.id == task.id) it.copy(isCompleted = true) else it
    }
}

@Serializable
data class ReopenTaskUpdateStep(val task: Task) : UpdateStep() {
    override suspend fun sendTo(todoist: TodoistApi, requestId: Int) {
        val resp = todoist.reopenTask(task.id, requestId).awaitResponse()
        if (resp.code() !in 200..299) throw HttpException(resp)
    }

    override fun applyTo(tasks: List<Task>) = tasks.map {
        if (it.id == task.id) it.copy(isCompleted = false) else it
    }
}

@Serializable
data class AddTaskUpdateStep(val newTask: NewTask) : UpdateStep() {
    override suspend fun sendTo(todoist: TodoistApi, requestId: Int) {
        val resp = todoist.addTask(requestId, newTask.toNewTaskDto()).awaitResponse()
        if (resp.code() !in 200..299) throw HttpException(resp)
    }

    override fun applyTo(tasks: List<Task>) = tasks + Task(
        id = newTask.virtualId,
        content = newTask.content,
        isCompleted = false,
        projectId = newTask.projectId
    )
}