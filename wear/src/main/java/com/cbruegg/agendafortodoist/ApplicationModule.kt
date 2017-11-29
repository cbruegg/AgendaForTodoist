package com.cbruegg.agendafortodoist

import android.content.Context
import com.google.android.gms.common.api.GoogleApiClient
import dagger.Module
import dagger.Provides

@Module
class ApplicationModule(
        private val context: Context,
        private val googleApiClient: GoogleApiClient?
) {
    @Provides
    fun provideAppContext() = context.applicationContext

    @Provides
    fun provideGoogleApiClient() = googleApiClient
}