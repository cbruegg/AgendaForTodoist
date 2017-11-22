package com.cbruegg.agendafortodoist.tasks

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.shared.TaskDto
import com.cbruegg.agendafortodoist.shared.TodoistApi
import com.cbruegg.agendafortodoist.util.LiveData
import com.cbruegg.agendafortodoist.util.MutableLiveData
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.await

class TasksViewModel(val projectId: Long, private val todoist: TodoistApi) : ViewModel() {

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
                _taskViewModels.data = tasks.map { TaskViewModel(it) }
                _bigMessageId.data = if (tasks.isEmpty()) R.string.no_tasks else null
            } catch (e: HttpException) {
                _taskViewModels.data = emptyList()
                _bigMessageId.data = R.string.network_error
            }
            _isLoading.data = false
        }
    }
}

data class TaskViewModel(
        val content: String,
        val id: Long
) : ViewModel()

fun TaskViewModel(taskDto: TaskDto) = TaskViewModel(taskDto.content, taskDto.id)