package com.cbruegg.agendafortodoist

import com.cbruegg.agendafortodoist.shared.auth.AuthServiceApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.CachingTodoistRepo
import dagger.Component
import javax.inject.Singleton

@Component(modules = [(NetModule::class)])
@Singleton
interface NetComponent {
    fun authService(): AuthServiceApi
    fun todoistRepo(): CachingTodoistRepo
}