package com.cbruegg.agendafortodoist.shared

import com.squareup.moshi.Json

data class ProjectDto(
        @Json(name = "id") val id: Long,
        @Json(name = "name") val name: String,
        @Json(name = "order") val order: Int,
        @Json(name = "indent") val indent: Int,
        @Json(name = "comment_count") val commentCount: Int
)

data class TaskDto(
        @Json(name = "id") val id: Long,
        @Json(name = "project_id") val projectId: Long,
        @Json(name = "content") val content: String,
        @Json(name = "completed") val isCompleted: Boolean,
        @Json(name = "label_ids") val labelIds: List<Int>?,
        @Json(name = "order") val order: Int,
        @Json(name = "indent") val indent: Int,
        @Json(name = "priority") val priority: Int,
        @Json(name = "url") val url: String,
        @Json(name = "comment_count") val commentCount: Int
)