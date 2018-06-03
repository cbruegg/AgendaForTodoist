package com.cbruegg.agendafortodoist.shared

import android.app.Application
import com.cbruegg.agendafortodoist.shared.todoist.api.AccessTokenGetter
import com.cbruegg.agendafortodoist.shared.todoist.api.todoist
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepo
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.CachingTodoistRepo
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.SendJobCreator
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.SharedPreferencesCache
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.SharedPreferencesUpdateStepsPersister
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.SharedPreferencesVirtualIdToReadIdPersister
import com.evernote.android.job.JobManager
import com.squareup.moshi.Moshi

internal val moshi = Moshi.Builder().build()

data class SharedLibraryInstances(val todoistRepo: TodoistRepo)

fun initSharedLibrary(app: Application, accessTokenGetter: AccessTokenGetter): SharedLibraryInstances {
    val todoist = todoist(accessTokenGetter)
    val updateStepsPersister = SharedPreferencesUpdateStepsPersister(app)
    val virtualIdToReadIdPersister = SharedPreferencesVirtualIdToReadIdPersister(app)
    val cache = SharedPreferencesCache(app)
    val cachingTodoistRepo = CachingTodoistRepo(todoist, updateStepsPersister, virtualIdToReadIdPersister, cache)

    JobManager.create(app).addJobCreator(SendJobCreator(todoist, updateStepsPersister, virtualIdToReadIdPersister))

    return SharedLibraryInstances(cachingTodoistRepo)
}
