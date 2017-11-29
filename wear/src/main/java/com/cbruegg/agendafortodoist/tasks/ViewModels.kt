package com.cbruegg.agendafortodoist.tasks

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.Settings
import com.cbruegg.agendafortodoist.shared.queuing.TaskCompletedStateChangeEvent
import com.cbruegg.agendafortodoist.shared.queuing.toPutDataRequest
import com.cbruegg.agendafortodoist.shared.todoist.TaskDto
import com.cbruegg.agendafortodoist.shared.todoist.TodoistApi
import com.cbruegg.agendafortodoist.tasks.TaskCompletionMarker.FailureType.AuthError
import com.cbruegg.agendafortodoist.tasks.TaskCompletionMarker.FailureType.HttpError
import com.cbruegg.agendafortodoist.tasks.TaskCompletionMarker.FailureType.IOError
import com.cbruegg.agendafortodoist.util.LiveData
import com.cbruegg.agendafortodoist.util.MutableLiveData
import com.cbruegg.agendafortodoist.util.UniqueRequestIdGenerator
import com.cbruegg.agendafortodoist.util.retry
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Result
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import kotlinx.coroutines.experimental.sync.Mutex
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.await
import ru.gildor.coroutines.retrofit.awaitResponse
import java.io.IOException

class TasksViewModel(
        val projectId: Long,
        private val todoist: TodoistApi,
        private val requestIdGenerator: UniqueRequestIdGenerator,
        private val settings: Settings
) : ViewModel() {

    private val _taskViewModels = MutableLiveData(emptyList<TaskViewModel>())
    val taskViewModels: LiveData<List<TaskViewModel>> = _taskViewModels

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _bigMessageId = MutableLiveData<Int?>(null)
    val bigMessageId: LiveData<Int?> = _bigMessageId

    private val _showList = MutableLiveData(false)
    val showList: LiveData<Boolean> = _showList

    var onAuthError: () -> Unit = {}
    var alert: (Int) -> Unit = {}

    fun notifyCompletedStateChanged(taskId: Long, isCompleted: Boolean) {
        _taskViewModels.data.firstOrNull { it.id == taskId }?.notifyCompletedStateChangedExternally(isCompleted)
    }

    private fun reload() {
        if (!settings.showedCompleteTaskIntro) {
            alert(R.string.double_tap_to_complete_task)
            settings.showedCompleteTaskIntro = true
        }

        launch(UI) {
            _isLoading.data = true
            _showList.data = false
            try {
                val tasks = todoist.tasks(projectId).await()
                _taskViewModels.data = tasks.map { TaskViewModel(it, requestIdGenerator, todoist, onAuthError) }
                _bigMessageId.data = if (tasks.isEmpty()) R.string.no_tasks else null
            } catch (e: HttpException) {
                _taskViewModels.data = emptyList()
                _bigMessageId.data = R.string.network_error
            }
            _isLoading.data = false
            _showList.data = true
        }
    }

    fun onCreate() {
        reload()
    }
}

class TaskViewModel(
        val content: String,
        val id: Long,
        isCompleted: Boolean,
        private val onAuthError: () -> Unit,
        private val taskCompletionMarker: TaskCompletionMarker
) : ViewModel() {
    private val _strikethrough = MutableLiveData(isCompleted)
    val strikethrough: LiveData<Boolean> = _strikethrough

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    var toast: (Int) -> Unit = {}

    @Volatile
    var isCompleted = isCompleted
        private set

    private val lock = Mutex()

    fun onDoubleTab() {
        toggleCompletionState()
    }

    fun onSwipe() {
        toggleCompletionState()
    }

    fun notifyCompletedStateChangedExternally(isCompleted: Boolean) {
        this.isCompleted = isCompleted
        _strikethrough.data = isCompleted
    }

    private fun toggleCompletionState() {
        launch {
            if (lock.tryLock()) {
                if (isCompleted) {
                    markUncompleted().join()
                } else {
                    markCompleted().join()
                }
                isCompleted = !isCompleted
                lock.unlock()
            }
        }
    }

    private fun markCompleted() = launch(UI) {
        _isLoading.data = true
        val failureType = taskCompletionMarker.setCompletionState(id, true)
        when (failureType) {
            AuthError -> onAuthError()
            HttpError -> toast(R.string.http_error)
            IOError -> toast(R.string.network_error)
            null -> _strikethrough.data = true
        }
        _isLoading.data = false
    }

    private fun markUncompleted() = launch(UI) {
        _isLoading.data = true
        val failureType = taskCompletionMarker.setCompletionState(id, false)
        when (failureType) {
            AuthError -> onAuthError()
            HttpError -> toast(R.string.http_error)
            IOError -> toast(R.string.network_error)
            null -> _strikethrough.data = false
        }
        _isLoading.data = false
    }
}

interface TaskCompletionMarker {
    enum class FailureType { AuthError, HttpError, IOError }

    suspend fun setCompletionState(taskId: Long, isCompleted: Boolean): FailureType?
}

class CompanionAppCompletionMarker(private val googleApiClient: GoogleApiClient) : TaskCompletionMarker {
    suspend override fun setCompletionState(taskId: Long, isCompleted: Boolean): TaskCompletionMarker.FailureType? {
        val event = TaskCompletedStateChangeEvent(taskId, isCompleted)
        val result = Wearable.DataApi.putDataItem(googleApiClient, event.toPutDataRequest()).awaitAsync()
        return null
    }
}

suspend fun <T : Result> PendingResult<T>.awaitAsync(): T = suspendCancellableCoroutine { continuation ->
    setResultCallback { result ->
        if (!continuation.isCancelled) {
            continuation.resume(result)
        }
    }

    continuation.invokeOnCompletion {
        if (continuation.isCancelled) {
            try {
                cancel()
            } catch (ignored: Throwable) {
            }
        }
    }
}

class ApiTaskCompletionMarker(private val todoist: TodoistApi, private val requestIdGenerator: UniqueRequestIdGenerator) : TaskCompletionMarker {
    suspend override fun setCompletionState(taskId: Long, isCompleted: Boolean): TaskCompletionMarker.FailureType? {
        try {
            val requestId = requestIdGenerator.nextRequestId()
            retry(HttpException::class, IOException::class) {
                if (isCompleted) {
                    todoist.closeTask(taskId, requestId).awaitResponse()
                } else {
                    todoist.reopenTask(taskId, requestId).awaitResponse()
                }
            }
            return null
        } catch (e: HttpException) {
            e.printStackTrace()
            return if (e.response().code() == 401) {
                AuthError
            } else {
                HttpError
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return IOError
        }
    }

}

fun TaskViewModel(
        taskDto: TaskDto,
        requestIdGenerator: UniqueRequestIdGenerator,
        todoist: TodoistApi,
        onAuthError: () -> Unit
) = TaskViewModel(taskDto.content, taskDto.id, taskDto.isCompleted, todoist, onAuthError)