package com.cbruegg.agendafortodoist

import android.content.Context
import com.cbruegg.agendafortodoist.auth.Auth
import com.cbruegg.agendafortodoist.shared.todoist.api.AccessTokenGetter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Settings @Inject constructor(context: Context) {
    private val file = "app_prefs"
    private val keyAuthType = "auth_type"
    private val keyAccessToken = "access_token"
    private val keyShowedCompleteTaskIntro = "showed_complete_task_intro"

    private val prefs = context.getSharedPreferences(file, Context.MODE_PRIVATE)

    fun containsAuth() = prefs.contains(keyAuthType) && prefs.contains(keyAccessToken)

    var auth: Auth
        get() = Auth(prefs.getString(keyAuthType, null)!!,
                prefs.getString(keyAccessToken, null)!!)
        set(value) = prefs.edit()
                .putString(keyAuthType, value.tokenType)
                .putString(keyAccessToken, value.accessToken)
                .apply()

    var showedCompleteTaskIntro: Boolean
        get() = prefs.getBoolean(keyShowedCompleteTaskIntro, false)
        set(value) = prefs.edit().putBoolean(keyShowedCompleteTaskIntro, value).apply()
}

class SettingsAccessTokenGetter @Inject constructor(private val settings: Settings) : AccessTokenGetter {
    override val accessToken: String
        get() = settings.auth.accessToken
}