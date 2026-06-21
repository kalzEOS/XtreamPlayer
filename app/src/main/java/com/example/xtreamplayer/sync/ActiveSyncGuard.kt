package com.example.xtreamplayer.sync

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-scoped flag that lets LibrarySyncWorker know when the in-app
 * ProgressiveSyncCoordinator already has an active background sync running,
 * so the two never write to the content cache simultaneously.
 */
object ActiveSyncGuard {
    private val active = AtomicBoolean(false)

    fun markActive() { active.set(true) }
    fun markInactive() { active.set(false) }
    val isActive: Boolean get() = active.get()
}
