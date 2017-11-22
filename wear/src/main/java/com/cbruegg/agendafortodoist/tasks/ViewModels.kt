package com.cbruegg.agendafortodoist.tasks

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.shared.TaskDto
import com.cbruegg.agendafortodoist.shared.TodoistApi
import com.cbruegg.agendafortodoist.util.LiveData
import com.cbruegg.agendafortodoist.util.MutableLiveData
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.await

class TasksViewModel(val projectId: Long, private val todoist: TodoistApi) : ViewModel() {

    private val _taskViewModels = MutableLiveData(emptyList<TaskViewModel>())
    val taskViewModels: LiveData<List<TaskViewModel>> = _taskViewModels

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _noTasks = MutableLiveData(false)
    val noTasks: LiveData<Boolean> = _noTasks

    fun onCreate() {
        launch(UI) {
            _isLoading.data = true
            val tasks = todoist.tasks(projectId).await()
            val taskVms = tasks.map { TaskViewModel(it) }
            _isLoading.data = false
            _taskViewModels.data = taskVms
            _noTasks.data = taskVms.isEmpty()
        }
    }
}

data class TaskViewModel(
        val content: String,
        val id: Long
) : ViewModel()

fun TaskViewModel(taskDto: TaskDto) = TaskViewModel(taskDto.content, taskDto.id)