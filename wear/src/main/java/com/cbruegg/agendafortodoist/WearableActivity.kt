package com.cbruegg.agendafortodoist

import android.support.v7.app.AppCompatActivity
import android.support.wear.ambient.AmbientMode

abstract class WearableActivity : AppCompatActivity(), AmbientMode.AmbientCallbackProvider {

    protected var ambientCallbackDelegate = object : AmbientMode.AmbientCallback() {}

    protected lateinit var ambientController: AmbientMode.AmbientController
        private set

    override fun getAmbientCallback() = ambientCallbackDelegate

    protected fun setAmbientEnabled() {
        ambientController = AmbientMode.attachAmbientSupport(this)
    }
}