package com.example.xtreamplayer.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.xtreamplayer.auth.AuthRepository
import com.example.xtreamplayer.content.ContentRepository
import com.example.xtreamplayer.Section
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

class LibrarySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun authRepository(): AuthRepository
        fun contentRepository(): ContentRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        val authRepository = entryPoint.authRepository()
        val contentRepository = entryPoint.contentRepository()

        if (ActiveSyncGuard.isActive) {
            Timber.d("LibrarySyncWorker: in-app sync already active, skipping this run")
            return Result.success()
        }

        val config = authRepository.authConfig.firstOrNull() ?: run {
            Timber.d("LibrarySyncWorker: no auth config, skipping")
            return Result.success()
        }

        Timber.i("LibrarySyncWorker: starting scheduled sync")
        return try {
            contentRepository.syncBackgroundFull(
                authConfig = config,
                sectionsToSync = listOf(Section.MOVIES, Section.SERIES, Section.LIVE),
                useBulkFirst = true,
                skipCompleted = false,
                fullReindex = true
            )
            Timber.i("LibrarySyncWorker: sync complete")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "LibrarySyncWorker: sync failed")
            Result.retry()
        }
    }
}
