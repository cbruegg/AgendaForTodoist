package com.cbruegg.agendafortodoist

import android.content.Context
import dagger.Module
import dagger.Provides

@Module
class ApplicationModule(
        private val context: Context
) {
    @Provides
    fun provideAppContext() = context.applicationContext
}