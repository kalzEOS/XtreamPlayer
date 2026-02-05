package com.example.xtreamplayer

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class XtreamPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (always enabled for now)
        Timber.plant(Timber.DebugTree())

        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectActivityLeaks()
                    .detectFileUriExposure()
                    .penaltyLog()
                    .build()
            )
        }
    }
}
