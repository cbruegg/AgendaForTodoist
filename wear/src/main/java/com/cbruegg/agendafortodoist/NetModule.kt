package com.cbruegg.agendafortodoist

import com.cbruegg.agendafortodoist.shared.auth.AuthServiceApi
import com.cbruegg.agendafortodoist.shared.todoist.api.TodoistApi
import dagger.Module
import dagger.Provides

@Module
class NetModule(
    private val todoist: TodoistApi,
    private val authService: AuthServiceApi
) {
    @Provides
    fun provideTodoist() = todoist

    @Provides
    fun provideAuthService() = authService
}