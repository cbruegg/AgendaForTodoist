package com.cbruegg.agendafortodoist

import android.content.Context
import com.cbruegg.agendafortodoist.auth.Auth

class Settings(context: Context) {
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