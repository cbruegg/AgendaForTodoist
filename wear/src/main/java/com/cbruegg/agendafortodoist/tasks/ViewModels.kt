package com.cbruegg.agendafortodoist.tasks

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.shared.TaskDto
import com.cbruegg.agendafortodoist.util.MutableLiveData

data class TasksViewModel(
        val taskViewModels: MutableLiveData<List<TaskViewModel>>
) : ViewModel()

data class TaskViewModel(
        val content: String,
        val id: Long
) : ViewModel()

fun TaskViewModel(taskDto: TaskDto) = TaskViewModel(taskDto.content, taskDto.id)