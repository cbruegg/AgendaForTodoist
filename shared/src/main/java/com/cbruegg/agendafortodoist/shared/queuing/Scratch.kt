package com.cbruegg.agendafortodoist.shared.queuing

import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest

private const val IS_COMPLETED_KEY = "is_completed"

data class TaskCompletedStateChangeEvent(val taskId: Long, val isCompleted: Boolean)

private fun taskPath(taskId: Long) = "/task/$taskId"

fun TaskCompletedStateChangeEvent.toPutDataRequest(): PutDataRequest =
        PutDataMapRequest.create(taskPath(taskId)).apply {
            dataMap.putBoolean(IS_COMPLETED_KEY, isCompleted)
        }.asPutDataRequest()