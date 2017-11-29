package com.cbruegg.agendafortodoist

import com.google.android.gms.common.api.GoogleApiClient
import dagger.Component

@Component(modules = arrayOf(ApplicationModule::class))
interface ApplicationComponent {
    fun settings(): Settings
    fun accessTokenGetter(): SettingsAccessTokenGetter
    fun googleApiClient(): GoogleApiClient?
}