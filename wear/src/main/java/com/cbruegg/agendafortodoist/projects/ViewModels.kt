package com.cbruegg.agendafortodoist.projects

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.shared.ProjectDto
import com.cbruegg.agendafortodoist.shared.TodoistApi
import com.cbruegg.agendafortodoist.util.MutableLiveData
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.await

class ProjectsViewModel(private val todoist: TodoistApi) : ViewModel() {

    private val _projectViewModels = MutableLiveData(emptyList<ProjectViewModel>())
    val projectViewModels: LiveData<List<ProjectViewModel>> = _projectViewModels

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun onCreate() {
        launch(UI) {
            _isLoading.data = true

            val projects = todoist.projects().await()
            val projectVms = projects.map { ProjectViewModel(it) }
            _projectViewModels.data = projectVms

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
fun ProjectViewModel(projectDto: ProjectDto) = ProjectViewModel(projectDto.name, projectDto.id, indentLevelSymbols[projectDto.indent - 1])