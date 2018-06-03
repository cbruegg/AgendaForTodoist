package com.cbruegg.agendafortodoist

import com.cbruegg.agendafortodoist.shared.auth.AuthServiceApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepo
import dagger.Component
import javax.inject.Singleton

@Component(modules = [(NetModule::class)])
@Singleton
interface NetComponent {
    fun authService(): AuthServiceApi
    fun todoistRepo(): TodoistRepo
}