package com.cbruegg.agendafortodoist.tasks

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.Settings
import com.cbruegg.agendafortodoist.shared.todoist.repo.Task
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistNetworkException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepo
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepoException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistServiceException
import com.cbruegg.agendafortodoist.util.LiveData
import com.cbruegg.agendafortodoist.util.MutableLiveData
import com.cbruegg.agendafortodoist.util.UniqueRequestIdGenerator
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex

class TasksViewModel(
    val projectId: Long,
    private val todoist: TodoistRepo,
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
            } catch (e: TodoistRepoException) {
                _taskViewModels.data = emptyList()
                when (e) {
                    is TodoistNetworkException -> {
                        _bigMessageId.data = R.string.network_error
                    }
                    is TodoistServiceException -> {
                        _bigMessageId.data = R.string.http_error
                    }
                }
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
    private val todoist: TodoistRepo,
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
            todoist.closeTask(id, requestId).await()
            _strikethrough.data = true
        } catch (e: TodoistRepoException) {
            e.printStackTrace()
            when (e) {
                is TodoistNetworkException -> {
                    toast(R.string.network_error)
                }
                is TodoistServiceException.General -> {
                    toast(R.string.http_error)
                }
                is TodoistServiceException.Auth -> {
                    onAuthError()
                    toast(R.string.http_error)
                }
            }
        }
        _isLoading.data = false
    }

    private fun markUncompleted() = launch(UI) {
        val requestId = requestIdGenerator.nextRequestId()
        _isLoading.data = true
        try {
            todoist.reopenTask(id, requestId).await()
            _strikethrough.data = false
        } catch (e: TodoistRepoException) {
            e.printStackTrace()
            when (e) {
                is TodoistNetworkException -> {
                    toast(R.string.network_error)
                }
                is TodoistServiceException.General -> {
                    toast(R.string.http_error)
                }
                is TodoistServiceException.Auth -> {
                    onAuthError()
                    toast(R.string.http_error)
                }
            }
        }
        _isLoading.data = false
    }
}

fun TaskViewModel(
    task: Task,
    requestIdGenerator: UniqueRequestIdGenerator,
    todoist: TodoistRepo,
    onAuthError: () -> Unit
) = TaskViewModel(task.content, task.id, task.isCompleted, requestIdGenerator, todoist, onAuthError)