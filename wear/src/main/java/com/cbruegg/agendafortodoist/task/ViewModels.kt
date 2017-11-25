package com.cbruegg.agendafortodoist.task

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.shared.todoist.TodoistApi
import com.cbruegg.agendafortodoist.util.LiveData
import com.cbruegg.agendafortodoist.util.MutableLiveData
import com.cbruegg.agendafortodoist.util.UniqueRequestIdGenerator
import com.cbruegg.agendafortodoist.util.retry
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.awaitResponse
import java.io.IOException

class TaskViewModel(
        val taskContent: String,
        val taskId: Long,
        isCompleted: Boolean,
        private val todoist: TodoistApi,
        private val requestIdGenerator: UniqueRequestIdGenerator
) : ViewModel() {

    private val _completionButtonStringId = MutableLiveData(if (isCompleted) R.string.uncomplete else R.string.complete)
    val completionButtonStringId: MutableLiveData<Int> = _completionButtonStringId

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _strikethrough = MutableLiveData(isCompleted)
    val strikethrough: LiveData<Boolean> = _strikethrough

    private val _isCompleted = MutableLiveData(isCompleted)
    val isCompleted: LiveData<Boolean> = _isCompleted

    var toast: (Int) -> Unit = {}

    private var completionButtonAction: () -> Job = if (isCompleted) this::uncomplete else this::complete

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
            retry(HttpException::class, IOException::class) {
                todoist.closeTask(taskId, requestId).awaitResponse()
                _completionButtonStringId.data = R.string.uncomplete
                _strikethrough.data = true
                _isCompleted.data = true
                completionButtonAction = this@TaskViewModel::uncomplete
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


    private fun uncomplete(): Job = launch(UI) {
        val requestId = requestIdGenerator.nextRequestId()
        _isLoading.data = true
        try {
            retry(HttpException::class, IOException::class) {
                todoist.reopenTask(taskId, requestId).awaitResponse()
                _completionButtonStringId.data = R.string.complete
                _strikethrough.data = false
                _isCompleted.data = false
                completionButtonAction = this@TaskViewModel::complete
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