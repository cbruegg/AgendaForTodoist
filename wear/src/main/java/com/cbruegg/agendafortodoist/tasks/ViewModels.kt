package com.cbruegg.agendafortodoist.tasks

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.shared.TaskDto
import com.cbruegg.agendafortodoist.shared.TodoistApi
import com.cbruegg.agendafortodoist.shared.todoist
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

class TasksViewModel(
        val projectId: Long,
        private val todoist: TodoistApi,
        private val requestIdGenerator: UniqueRequestIdGenerator
) : ViewModel() {

    private val _taskViewModels = MutableLiveData(emptyList<TaskViewModel>())
    val taskViewModels: LiveData<List<TaskViewModel>> = _taskViewModels

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _bigMessageId = MutableLiveData<Int?>(null)
    val bigMessageId: LiveData<Int?> = _bigMessageId

    fun onCreate() {
        launch(UI) {
            _isLoading.data = true
            try {
                val tasks = todoist.tasks(projectId).await()
                _taskViewModels.data = tasks.map { TaskViewModel(it, requestIdGenerator) }
                _bigMessageId.data = if (tasks.isEmpty()) R.string.no_tasks else null
            } catch (e: HttpException) {
                _taskViewModels.data = emptyList()
                _bigMessageId.data = R.string.network_error
            }
            _isLoading.data = false
        }
    }
}

class TaskViewModel(
        val content: String,
        val id: Long,
        @Volatile private var isCompleted: Boolean,
        private val requestIdGenerator: UniqueRequestIdGenerator
) : ViewModel() {
    private val _strikethrough = MutableLiveData(isCompleted)
    val strikethrough: LiveData<Boolean> = _strikethrough

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val lock = Mutex()

    fun onDoubleTab() {
        toggleCompletionState()
    }

    fun onSwipe() {
        toggleCompletionState()
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
        val requestId = requestIdGenerator.nextRequestId()
        _isLoading.data = true
        retry(HttpException::class.java) {
            todoist.closeTask(id, requestId).awaitResponse()
            _strikethrough.data = true
        }
        _isLoading.data = false
    }

    private fun markUncompleted() = launch(UI) {
        val requestId = requestIdGenerator.nextRequestId()
        _isLoading.data = true
        retry {
            todoist.reopenTask(id, requestId).awaitResponse()
            _strikethrough.data = false
        }
        _isLoading.data = false
    }
}

fun TaskViewModel(taskDto: TaskDto, requestIdGenerator: UniqueRequestIdGenerator) = TaskViewModel(taskDto.content, taskDto.id, taskDto.isCompleted, requestIdGenerator)