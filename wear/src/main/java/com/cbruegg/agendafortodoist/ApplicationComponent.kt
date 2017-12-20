package com.cbruegg.agendafortodoist

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(ApplicationModule::class))
interface ApplicationComponent {
    fun settings(): Settings

    fun accessTokenGetter(): SettingsAccessTokenGetter
}