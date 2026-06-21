package com.example.xtreamplayer.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.xtreamplayer.settings.SyncScheduleInterval
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val WORK_NAME = "library_sync_periodic"

    fun schedule(context: Context, interval: SyncScheduleInterval) {
        val workManager = WorkManager.getInstance(context)
        if (interval == SyncScheduleInterval.OFF) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request =
            PeriodicWorkRequestBuilder<LibrarySyncWorker>(interval.hours, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
