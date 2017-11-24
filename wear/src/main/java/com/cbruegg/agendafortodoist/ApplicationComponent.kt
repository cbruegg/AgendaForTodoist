package com.cbruegg.agendafortodoist

import dagger.Component

@Component(modules = arrayOf(ApplicationModule::class))
interface ApplicationComponent {
    fun settings(): Settings
    fun accessTokenGetter(): SettingsAccessTokenGetter
}