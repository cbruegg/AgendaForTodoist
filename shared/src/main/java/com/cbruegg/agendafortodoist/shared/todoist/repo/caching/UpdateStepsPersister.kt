package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import android.content.Context
import com.cbruegg.agendafortodoist.shared.todoist.api.TodoistApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistNetworkException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepoException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistServiceException
import com.cbruegg.agendafortodoist.shared.util.retry
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
        get() = JSON.parse(prefs.getString(KEY_STEPS, defaultSerializedValue))
        set(value) {
            prefs.edit().putString(KEY_STEPS, JSON.stringify(value)).apply()
        }

    override val lockedProperty = LockProtectedVar({ updateSteps }, { updateSteps = it })

}

/**
 * @throws [TodoistRepoException]
 */
internal suspend fun UpdateStepsPersister.processQueue(todoist: TodoistApi, virtualIdToRealIdPersister: VirtualIdToRealIdPersister) {
    lockedProperty.inTransaction { updateSteps ->
        errorHandled {
            virtualIdToRealIdPersister.lockedProperty.inTransaction { virtualIdsToRealIds ->
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