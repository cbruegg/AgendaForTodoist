package com.cbruegg.agendafortodoist

import com.cbruegg.agendafortodoist.shared.auth.AuthServiceApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.caching.CachedTodoistRepo
import dagger.Component

@Component(modules = [(NetModule::class)])
interface NetComponent {
    fun authService(): AuthServiceApi
    fun todoistRepo(): CachedTodoistRepo
}