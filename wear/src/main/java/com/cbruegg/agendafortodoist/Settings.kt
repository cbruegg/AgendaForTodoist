package com.cbruegg.agendafortodoist

import android.content.Context
import com.cbruegg.agendafortodoist.auth.Auth
import com.cbruegg.agendafortodoist.shared.todoist.AccessTokenGetter
import javax.inject.Inject

class Settings @Inject constructor(context: Context) {
    private val file = "app_prefs"
    private val keyAuthType = "auth_type"
    private val keyAccessToken = "access_token"

    private val prefs = context.getSharedPreferences(file, Context.MODE_PRIVATE)

    fun containsAuth() = prefs.contains(keyAuthType) && prefs.contains(keyAccessToken)

    fun storeAuth(auth: Auth) {
        prefs.edit()
                .putString(keyAuthType, auth.tokenType)
                .putString(keyAccessToken, auth.accessToken)
                .apply()
    }

    fun retrieveAuth() = Auth(prefs.getString(keyAuthType, null)!!,
            prefs.getString(keyAccessToken, null)!!)
}

class SettingsAccessTokenGetter @Inject constructor(private val settings: Settings) : AccessTokenGetter {
    override val accessToken: String
        get() = settings.retrieveAuth().accessToken
}