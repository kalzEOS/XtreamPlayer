package com.example.xtreamplayer

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.xtreamplayer.observability.AppDiagnostics
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import timber.log.Timber

@HiltAndroidApp
class XtreamPlayerApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        AppDiagnostics.initialize(this, enableAnrWatchdog = !isDebuggable)
        if (isDebuggable) {
            Timber.plant(Timber.DebugTree())
        }

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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(false)
            .respectCacheHeaders(false)
            .bitmapFactoryMaxParallelism(4)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizePercent(0.03)
                    .build()
            }
            .build()
    }
}
