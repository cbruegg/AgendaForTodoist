package com.cbruegg.agendafortodoist.shared

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Wearable

open class GoogleApiClientApplication : Application(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    override fun onConnectionSuspended(p0: Int) {
        googleApiClient = null
        super.onCreate()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        googleApiClient = null
        super.onCreate()
    }

    override fun onConnected(p0: Bundle?) {
        super.onCreate()
    }

    protected var googleApiClient: GoogleApiClient? = null // TODO Handle that connection can fail anytime, so check operations exception
        private set

    @SuppressLint("MissingSuperCall")
    override fun onCreate() {
        googleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build()
                .also { it.connect() }
    }

}