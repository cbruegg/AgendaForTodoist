package com.cbruegg.agendafortodoist.projects

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.shared.todoist.repo.Project
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistNetworkException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepo
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepoException
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistServiceException
import com.cbruegg.agendafortodoist.util.LiveData
import com.cbruegg.agendafortodoist.util.MutableLiveData
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class ProjectsViewModel(private val todoist: TodoistRepo) : ViewModel() {

    private val _projectViewModels = MutableLiveData(emptyList<ProjectViewModel>())
    val projectViewModels: LiveData<List<ProjectViewModel>> = _projectViewModels

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _bigMessageId = MutableLiveData<Int?>(null)
    val bigMessageId: LiveData<Int?> = _bigMessageId

    var onAuthError: () -> Unit = {}

    fun onCreate() {
        launch(UI) {
            _isLoading.data = true
            try {
                val projects = todoist.projects().await()
                val projectVms = projects.map { ProjectViewModel(it) }
                _projectViewModels.data = projectVms
                _bigMessageId.data = null
            } catch (e: TodoistRepoException) {
                _projectViewModels.data = emptyList()
                when (e) {
                    is TodoistNetworkException -> {
                        _bigMessageId.data = R.string.network_error
                    }
                    is TodoistServiceException.General -> {
                        _bigMessageId.data = R.string.http_error
                    }
                    is TodoistServiceException.Auth -> {
                        onAuthError()
                        _bigMessageId.data = R.string.http_error
                    }
                }
            }
            _isLoading.data = false
        }
    }
}

data class ProjectViewModel(
    val name: String,
    val id: Long,
    val indentPrefix: String
) : ViewModel()


private val indentLevelSymbols = arrayOf("", "•\t", "‣\t", "◦\t")
fun ProjectViewModel(project: Project) = ProjectViewModel(project.name, project.id, indentLevelSymbols[project.indent - 1])