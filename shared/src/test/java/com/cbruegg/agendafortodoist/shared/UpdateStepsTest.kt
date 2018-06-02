package com.cbruegg.agendafortodoist.shared

import com.cbruegg.agendafortodoist.shared.Scenario.taskToAdd
import com.cbruegg.agendafortodoist.shared.Scenario.tasks
import com.cbruegg.agendafortodoist.shared.todoist.repo.NewTask
import com.cbruegg.agendafortodoist.shared.todoist.repo.Task
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.AddTaskUpdateStep
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.CloseTaskUpdateStep
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.ReopenTaskUpdateStep
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.UpdateSteps
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.toUpdateSteps
import kotlinx.serialization.json.JSON
import org.junit.Assert
import org.junit.Test

private object Scenario {
    val tasks = listOf(
        Task(id = 0, content = "0", isCompleted = false, projectId = 0),
        Task(id = 1, content = "1", isCompleted = true, projectId = 0),
        Task(id = 2, content = "2", isCompleted = false, projectId = 0)
    )
    val taskToAdd = NewTask(content = "3", projectId = 0)
}

class UpdateStepsTest {
    @Test
    fun serializationTest() {
        val closeTask0Step = CloseTaskUpdateStep(tasks[0])
        val closeTask2Step = CloseTaskUpdateStep(tasks[2])
        val reopenTask0Step = ReopenTaskUpdateStep(tasks[0])
        val addStep = AddTaskUpdateStep(taskToAdd)
        val steps = listOf(closeTask0Step, closeTask2Step, reopenTask0Step, addStep).toUpdateSteps()

        val json = JSON.stringify(steps)
        Assert.assertEquals(steps, JSON.parse<UpdateSteps>(json))
    }

    @Test
    fun updateStepsTest() {
        val closeTask0Step = CloseTaskUpdateStep(tasks[0])
        val closeTask2Step = CloseTaskUpdateStep(tasks[2])
        val reopenTask0Step = ReopenTaskUpdateStep(tasks[0])
        val addStep = AddTaskUpdateStep(taskToAdd)
        val steps = listOf(closeTask0Step, closeTask2Step, reopenTask0Step, addStep).toUpdateSteps()

        val result = steps.applyTo(tasks)

        Assert.assertEquals(
            listOf(
                tasks[0],
                tasks[1],
                tasks[2].copy(isCompleted = true),
                Task(taskToAdd.virtualId, taskToAdd.content, isCompleted = false, projectId = taskToAdd.projectId)
            ),
            result
        )
    }

    @Test
    fun updateStepsAddRemoveTest() {
        val steps = listOf(
            AddTaskUpdateStep(taskToAdd),
            CloseTaskUpdateStep(
                Task(taskToAdd.virtualId, taskToAdd.content, isCompleted = false, projectId = taskToAdd.projectId)
            )
        ).toUpdateSteps()

        Assert.assertEquals(tasks, steps.applyTo(tasks))
    }

    @Test
    fun testCloseTaskUpdateStep() {
        val closeTask0Step = CloseTaskUpdateStep(tasks[0])
        val expected = tasks.map { if (it.id == 0L) it.copy(isCompleted = true) else it }
        Assert.assertEquals(expected, closeTask0Step.applyTo(tasks))
    }

    @Test
    fun testReopenTaskUpdateStep() {
        val reopenTask1Step = ReopenTaskUpdateStep(tasks[1])
        val expected = tasks.map { if (it.id == 1L) it.copy(isCompleted = false) else it }
        Assert.assertEquals(expected, reopenTask1Step.applyTo(tasks))
    }

    @Test
    fun testAddStep() {
        val addStep = AddTaskUpdateStep(taskToAdd)
        val actual = addStep.applyTo(tasks)
        Assert.assertEquals(taskToAdd.content, actual.getOrNull(3)?.content)
        Assert.assertEquals(taskToAdd.projectId, actual.getOrNull(3)?.projectId)
        Assert.assertEquals(false, actual.getOrNull(3)?.isCompleted)
        Assert.assertEquals(true, actual.getOrNull(3)?.isVirtual)
    }
}