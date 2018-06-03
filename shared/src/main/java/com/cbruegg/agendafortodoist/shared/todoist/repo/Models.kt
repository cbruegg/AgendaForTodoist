package com.cbruegg.agendafortodoist.shared.todoist.repo

import android.os.Parcelable
import com.cbruegg.agendafortodoist.shared.todoist.api.NewTaskDto
import com.cbruegg.agendafortodoist.shared.todoist.api.ProjectDto
import com.cbruegg.agendafortodoist.shared.todoist.api.TaskDto
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Parcelize
@Serializable
data class Project(
    val id: Long,
    val name: String,
    val indent: Int
) : Parcelable {
    internal constructor(dto: ProjectDto) : this(
        id = dto.id,
        name = dto.name,
        indent = dto.indent
    )
}

@Parcelize
@Serializable
data class Task(
    val id: Long,
    val content: String,
    val isCompleted: Boolean,
    val projectId: Long
) : Parcelable {
    internal constructor(dto: TaskDto) : this(
        id = dto.id,
        content = dto.content,
        isCompleted = dto.isCompleted,
        projectId = dto.projectId
    )

    /**
     * True iff this task has not been submitted to the API
     */
    @Transient
    inline val isVirtual
        get() = id < 0
}

@Parcelize
@Serializable
data class NewTask(
    val content: String,
    val projectId: Long,
    val virtualId: Long = generateVirtualId()
) : Parcelable {
    companion object {
        private fun generateVirtualId() = (Math.random() * Long.MIN_VALUE).toLong()
    }

    init {
        require(virtualId < 0)
    }

    internal fun toNewTaskDto() = NewTaskDto(
        content = content,
        projectId = projectId
    )

    fun toTask() = Task(
        id = virtualId,
        content = content,
        isCompleted = false,
        projectId = projectId
    )
}