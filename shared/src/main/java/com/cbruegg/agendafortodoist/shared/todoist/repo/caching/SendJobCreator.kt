package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import com.cbruegg.agendafortodoist.shared.todoist.api.TodoistApi
import com.evernote.android.job.JobCreator

internal class SendJobCreator(
    private val todoist: TodoistApi,
    private val updateStepsPersister: UpdateStepsPersister,
    private val virtualIdToRealIdPersister: VirtualIdToRealIdPersister
) : JobCreator {
    override fun create(tag: String) =
        if (tag == TAG_SEND_JOB) SendJob(updateStepsPersister, virtualIdToRealIdPersister, todoist) else null
}