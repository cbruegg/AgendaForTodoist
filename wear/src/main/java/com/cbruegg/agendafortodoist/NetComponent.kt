package com.cbruegg.agendafortodoist

import com.cbruegg.agendafortodoist.shared.auth.AuthServiceApi
import com.cbruegg.agendafortodoist.shared.todoist.TodoistApi
import dagger.Component

@Component(modules = arrayOf(NetModule::class))
interface NetComponent {
    fun todoist(): TodoistApi
    fun authService(): AuthServiceApi
}