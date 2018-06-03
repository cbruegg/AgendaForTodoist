package com.cbruegg.agendafortodoist

import com.cbruegg.agendafortodoist.shared.auth.AuthServiceApi
import com.cbruegg.agendafortodoist.shared.todoist.repo.TodoistRepo
import com.cbruegg.agendafortodoist.shared.util.UniqueRequestIdGenerator
import dagger.Module
import dagger.Provides

@Module
class NetModule(
    private val todoistRepo: TodoistRepo,
    private val authService: AuthServiceApi
) {
    @Provides
    fun provideTodoistRepo() = todoistRepo

    @Provides
    fun provideAuthService() = authService

    @Provides
    fun provideUniqueRequestIdGenerator() = UniqueRequestIdGenerator
}