package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import android.content.Context
import com.cbruegg.agendafortodoist.shared.todoist.repo.Project
import com.cbruegg.agendafortodoist.shared.todoist.repo.Task
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list

internal interface Cache {
    fun cacheProjects(projects: List<Project>)
    fun cacheTasks(projectId: Long, tasks: List<Task>)

    fun retrieveProjects(): List<Project>?
    fun retrieveTasks(projectId: Long): List<Task>?
}

private const val FILE = "cache"
private const val KEY_PROJECTS = "projects"

internal class SharedPreferencesCache(context: Context) : Cache {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun cacheProjects(projects: List<Project>) {
        prefs.edit().putString(KEY_PROJECTS, JSON.stringify(Project.serializer().list, projects)).apply()
    }

    override fun cacheTasks(projectId: Long, tasks: List<Task>) {
        prefs.edit().putString(projectId.toString(), JSON.stringify(Task.serializer().list, tasks)).apply()
    }

    override fun retrieveProjects() =
        prefs.getString(KEY_PROJECTS, null)?.let { JSON.parse(Project.serializer().list, it) }

    override fun retrieveTasks(projectId: Long) =
        prefs.getString(projectId.toString(), null)?.let { JSON.parse(Task.serializer().list, it) }
}