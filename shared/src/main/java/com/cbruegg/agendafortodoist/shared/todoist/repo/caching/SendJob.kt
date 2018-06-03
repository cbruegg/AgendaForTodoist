package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import com.cbruegg.agendafortodoist.shared.todoist.api.TodoistApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepoException
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import kotlinx.coroutines.experimental.runBlocking

internal const val TAG_SEND_JOB = "send_job"

internal class SendJob(
    private val updateStepsPersister: UpdateStepsPersister,
    private val virtualIdToRealIdPersister: VirtualIdToRealIdPersister,
    private val todoist: TodoistApi
) : Job() {

    override fun onRunJob(params: Params) = runBlocking {
        try {
            updateStepsPersister.processQueue(todoist, virtualIdToRealIdPersister)
            Result.SUCCESS
        } catch (e: TodoistRepoException) {
            Result.RESCHEDULE
        }
    }

}

internal fun scheduleSendJob() {
    JobRequest.Builder(TAG_SEND_JOB)
        .setExecutionWindow(30_000, 120_000)
        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
        .setRequirementsEnforced(true)
        .setUpdateCurrent(true)
        .build()
        .schedule()
}