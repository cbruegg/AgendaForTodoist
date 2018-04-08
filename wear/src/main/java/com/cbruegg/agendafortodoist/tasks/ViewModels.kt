package com.cbruegg.agendafortodoist.tasks

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.Settings
import com.cbruegg.agendafortodoist.shared.todoist.TaskDto
import com.cbruegg.agendafortodoist.shared.todoist.TodoistApi
import com.cbruegg.agendafortodoist.util.LiveData
import com.cbruegg.agendafortodoist.util.MutableLiveData
import com.cbruegg.agendafortodoist.util.UniqueRequestIdGenerator
import com.cbruegg.agendafortodoist.util.retry
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
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
            } catch (e: IOException) {
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
    private val requestIdGenerator: UniqueRequestIdGenerator,
    private val todoist: TodoistApi,
    private val onAuthError: () -> Unit
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
                try {
                    if (isCompleted) {
                        markUncompleted().join()
                    } else {
                        markCompleted().join()
                    }
                    isCompleted = !isCompleted
                } finally {
                    lock.unlock()
                }
            }
        }
    }

    private fun markCompleted() = launch(UI) {
        val requestId = requestIdGenerator.nextRequestId()
        _isLoading.data = true
        try {
            retry(HttpException::class, IOException::class) {
                todoist.closeTask(id, requestId).awaitResponse()
                _strikethrough.data = true
            }
        } catch (e: HttpException) {
            if (e.response().code() == 401) {
                onAuthError()
            }
            e.printStackTrace()
            toast(R.string.http_error)
        } catch (e: IOException) {
            e.printStackTrace()
            toast(R.string.network_error)
        }
        _isLoading.data = false
    }

    private fun markUncompleted() = launch(UI) {
        val requestId = requestIdGenerator.nextRequestId()
        _isLoading.data = true
        try {
            retry(HttpException::class, IOException::class) {
                todoist.reopenTask(id, requestId).awaitResponse()
                _strikethrough.data = false
            }
        } catch (e: HttpException) {
            if (e.response().code() == 401) {
                onAuthError()
            }
            e.printStackTrace()
            toast(R.string.http_error)
        } catch (e: IOException) {
            e.printStackTrace()
            toast(R.string.network_error)
        }
        _isLoading.data = false
    }
}

fun TaskViewModel(
    taskDto: TaskDto,
    requestIdGenerator: UniqueRequestIdGenerator,
    todoist: TodoistApi,
    onAuthError: () -> Unit
) = TaskViewModel(taskDto.content, taskDto.id, taskDto.isCompleted, requestIdGenerator, todoist, onAuthError)