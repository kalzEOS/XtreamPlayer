package com.example.xtreamplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class XtreamPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (always enabled for now)
        Timber.plant(Timber.DebugTree())
    }
}
