package com.cbruegg.agendafortodoist.task

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.shared.todoist.repo.Task
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistNetworkException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepo
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepoException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistServiceException
import com.cbruegg.agendafortodoist.shared.util.UniqueRequestIdGenerator
import com.cbruegg.agendafortodoist.util.LiveData
import com.cbruegg.agendafortodoist.util.MutableLiveData
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class TaskViewModel(
    val task: Task,
    private val todoist: TodoistRepo,
    private val requestIdGenerator: UniqueRequestIdGenerator
) : ViewModel() {

    private val _completionButtonStringId = MutableLiveData(if (task.isCompleted) R.string.uncomplete else R.string.complete)
    val completionButtonStringId: MutableLiveData<Int> = _completionButtonStringId

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _strikethrough = MutableLiveData(task.isCompleted)
    val strikethrough: LiveData<Boolean> = _strikethrough

    private val _isCompleted = MutableLiveData(task.isCompleted)
    val isCompleted: LiveData<Boolean> = _isCompleted

    var toast: (Int) -> Unit = {}

    private var completionButtonAction: () -> Job = if (task.isCompleted) this::uncomplete else this::complete

    var onAuthError: () -> Unit = {}

    fun onCreate() {

    }

    fun onCompletionButtonClick() {
        completionButtonAction()
    }

    private fun complete(): Job = launch(UI) {
        val requestId = requestIdGenerator.nextRequestId()
        _isLoading.data = true
        try {
            todoist.closeTask(task, requestId).await()
            _completionButtonStringId.data = R.string.uncomplete
            _strikethrough.data = true
            _isCompleted.data = true
            completionButtonAction = this@TaskViewModel::uncomplete
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


    private fun uncomplete(): Job = launch(UI) {
        val requestId = requestIdGenerator.nextRequestId()
        _isLoading.data = true
        try {
            todoist.reopenTask(task, requestId).await()
            _completionButtonStringId.data = R.string.complete
            _strikethrough.data = false
            _isCompleted.data = false
            completionButtonAction = this@TaskViewModel::complete
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