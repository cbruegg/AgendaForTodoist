package com.cbruegg.agendafortodoist.projects

import android.arch.lifecycle.ViewModel
import com.cbruegg.agendafortodoist.util.MutableLiveData
import com.cbruegg.agendafortodoist.shared.ProjectDto

data class ProjectsViewModel(
        val projectViewModels: MutableLiveData<List<ProjectViewModel>>
) : ViewModel()

data class ProjectViewModel(
        val name: String,
        val id: Long,
        val indentPrefix: String
) : ViewModel()


private val indentLevelSymbols = arrayOf("", "•\t", "‣\t", "◦\t")
fun ProjectViewModel(projectDto: ProjectDto) = ProjectViewModel(projectDto.name, projectDto.id, indentLevelSymbols[projectDto.indent - 1])