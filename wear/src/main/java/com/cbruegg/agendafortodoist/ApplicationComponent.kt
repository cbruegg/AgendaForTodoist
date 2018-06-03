package com.cbruegg.agendafortodoist

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {
    fun settings(): Settings

    fun accessTokenGetter(): SettingsAccessTokenGetter
}