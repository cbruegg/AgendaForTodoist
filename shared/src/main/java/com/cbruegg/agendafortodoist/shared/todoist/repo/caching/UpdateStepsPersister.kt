package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import android.content.Context
import com.cbruegg.agendafortodoist.shared.todoist.api.TodoistApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistNetworkException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepoException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistServiceException
import com.cbruegg.agendafortodoist.shared.util.retry
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import kotlinx.serialization.json.JSON
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal typealias UpdateStepsPersister = Persister<UpdateSteps>

private const val FILE = "update_steps"
private const val KEY_STEPS = "steps"

@Singleton
internal class SharedPreferencesUpdateStepsPersister
@Inject constructor(context: Context) : UpdateStepsPersister {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val defaultSerializedValue = JSON.stringify(UpdateSteps.initial)

    private var updateSteps: UpdateSteps
        get() {
            check(accessLock.isLocked) { "Must lock before using this property!" }
            return JSON.parse(prefs.getString(KEY_STEPS, defaultSerializedValue))
        }
        set(value) {
            check(accessLock.isLocked) { "Must lock before using this property!" }
            prefs.edit().putString(KEY_STEPS, JSON.stringify(value)).apply()
        }

    private val accessLock: Mutex = Mutex()

    override suspend fun <R> inTransactionWithReturn(f: suspend (UpdateSteps) -> Pair<UpdateSteps, R>): R {
        return accessLock.withLock {
            val (newUpdateSteps, r) = f(updateSteps)
            updateSteps = newUpdateSteps
            r
        }
    }
}

/**
 * @throws [TodoistRepoException]
 */
internal suspend fun UpdateStepsPersister.processQueue(todoist: TodoistApi, virtualIdToRealIdPersister: VirtualIdToRealIdPersister) {
    inTransaction { updateSteps ->
        errorHandled {
            virtualIdToRealIdPersister.inTransaction { virtualIdsToRealIds ->
                virtualIdsToRealIds + updateSteps.sendTo(todoist).virtualToRealIds
            }
        }
        UpdateSteps.initial
    }
}

private suspend fun <T> errorHandled(f: suspend () -> T): T {
    try {
        return retry(HttpException::class, IOException::class) {
            f()
        }
    } catch (e: HttpException) {
        if (e.response().code() == 401) {
            throw TodoistServiceException.Auth(e)
        } else {
            throw TodoistServiceException.General(e)
        }
    } catch (e: IOException) {
        throw TodoistNetworkException(e)
    }
}