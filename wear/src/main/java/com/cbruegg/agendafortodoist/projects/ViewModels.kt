package com.cbruegg.agendafortodoist.projects

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.shared.ProjectDto
import com.cbruegg.agendafortodoist.shared.TodoistApi
import com.cbruegg.agendafortodoist.util.MutableLiveData
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.await

class ProjectsViewModel(private val todoist: TodoistApi) : ViewModel() {

    private val _projectViewModels = MutableLiveData(emptyList<ProjectViewModel>())
    val projectViewModels: LiveData<List<ProjectViewModel>> = _projectViewModels

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _bigMessageId = MutableLiveData<Int?>(null)
    val bigMessageId: com.cbruegg.agendafortodoist.util.LiveData<Int?> = _bigMessageId

    fun onCreate() {
        launch(UI) {
            _isLoading.data = true
            try {
                val projects = todoist.projects().await()
                val projectVms = projects.map { ProjectViewModel(it) }
                _projectViewModels.data = projectVms
                _bigMessageId.data = null
            } catch (e: HttpException) {
                _projectViewModels.data = emptyList()
                _bigMessageId.data = R.string.network_error
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
fun ProjectViewModel(projectDto: ProjectDto) = ProjectViewModel(projectDto.name, projectDto.id, indentLevelSymbols[projectDto.indent - 1])