package com.cbruegg.agendafortodoist

import android.support.v7.app.AppCompatActivity
import android.support.wear.ambient.AmbientModeSupport

abstract class WearableActivity : AppCompatActivity(), AmbientModeSupport.AmbientCallbackProvider {

    protected var ambientCallbackDelegate = object : AmbientModeSupport.AmbientCallback() {}

    protected lateinit var ambientController: AmbientModeSupport.AmbientController
        private set

    override fun getAmbientCallback() = ambientCallbackDelegate

    protected fun setAmbientEnabled() {
        ambientController = AmbientModeSupport.attach(this)
    }
}