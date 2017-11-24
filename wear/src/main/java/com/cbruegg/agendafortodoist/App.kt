package com.cbruegg.agendafortodoist

import android.app.Application
import android.content.Context
import android.support.v4.app.Fragment
import com.cbruegg.agendafortodoist.shared.auth.authService
import com.cbruegg.agendafortodoist.shared.todoist.todoist

class App : Application() {

    val applicationComponent: ApplicationComponent by lazy {
        DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()
    }

    val netComponent: NetComponent by lazy {
        DaggerNetComponent.builder()
                .netModule(NetModule(todoist(applicationComponent.accessTokenGetter()), authService))
                .build()
    }

}

val Context.app: App
    get() = applicationContext as App

val Fragment.app: App
    get() = context!!.app