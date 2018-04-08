package com.cbruegg.agendafortodoist.shared.todoist.repo

import com.cbruegg.agendafortodoist.shared.todoist.api.NewTaskDto
import com.cbruegg.agendafortodoist.shared.todoist.api.ProjectDto
import com.cbruegg.agendafortodoist.shared.todoist.api.TaskDto

data class Project(
    val id: Long,
    val name: String,
    val order: Int,
    val indent: Int,
    val commentCount: Int
) {
    constructor(dto: ProjectDto) : this(
        id = dto.id,
        name = dto.name,
        order = dto.order,
        indent = dto.indent,
        commentCount = dto.commentCount
    )
}

data class Task(
    val id: Long,
    val projectId: Long,
    val content: String,
    val isCompleted: Boolean,
    val labelIds: List<Long>?,
    val order: Int,
    val indent: Int,
    val priority: Int,
    val url: String,
    val commentCount: Int
) {
    constructor(dto: TaskDto) : this(
        id = dto.id,
        projectId = dto.projectId,
        content = dto.content,
        isCompleted = dto.isCompleted,
        labelIds = dto.labelIds,
        order = dto.order,
        indent = dto.indent,
        priority = dto.priority,
        url = dto.url,
        commentCount = dto.commentCount
    )
}

data class NewTask(
    val content: String,
    val projectId: Long
) {
    fun toNewTaskDto() = NewTaskDto(
        content = content,
        projectId = projectId
    )
}