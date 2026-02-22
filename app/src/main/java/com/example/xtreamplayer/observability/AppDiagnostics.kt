package com.example.xtreamplayer.observability

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object AppDiagnostics {
    private const val LOG_DIR = "diagnostics"
    private const val LOG_FILE = "events.log"
    private const val MAX_LOG_BYTES = 512 * 1024L
    private const val ANR_TIMEOUT_MS = 7_000L

    private val initialized = AtomicBoolean(false)
    private val mainTick = AtomicLong(0L)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    @Volatile private var appContext: Context? = null
    @Volatile private var previousExceptionHandler: Thread.UncaughtExceptionHandler? = null

    fun initialize(context: Context, enableAnrWatchdog: Boolean) {
        if (!initialized.compareAndSet(false, true)) return
        appContext = context.applicationContext
        installUncaughtExceptionHandler()
        if (enableAnrWatchdog) {
            startAnrWatchdog()
        }
        record(
            level = "INFO",
            event = "diagnostics_initialized",
            fields = mapOf("anrWatchdogEnabled" to enableAnrWatchdog.toString())
        )
    }

    fun recordInfo(event: String, fields: Map<String, String> = emptyMap()) {
        record(level = "INFO", event = event, fields = fields)
    }

    fun recordWarning(event: String, fields: Map<String, String> = emptyMap()) {
        record(level = "WARN", event = event, fields = fields)
    }

    fun recordError(
        event: String,
        throwable: Throwable? = null,
        fields: Map<String, String> = emptyMap()
    ) {
        val mergedFields =
            if (throwable == null) {
                fields
            } else {
                fields + mapOf(
                    "errorType" to throwable::class.java.simpleName,
                    "errorMessage" to (throwable.message ?: "unknown")
                )
            }
        record(level = "ERROR", event = event, fields = mergedFields)
    }

    private fun installUncaughtExceptionHandler() {
        previousExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            recordError(
                event = "uncaught_exception",
                throwable = throwable,
                fields = mapOf("thread" to thread.name)
            )
            previousExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun startAnrWatchdog() {
        val mainHandler = Handler(Looper.getMainLooper())
        Thread(
            {
                var lastTick = -1L
                var lastReportElapsedMs = 0L
                while (true) {
                    mainHandler.post { mainTick.incrementAndGet() }
                    Thread.sleep(ANR_TIMEOUT_MS)
                    val currentTick = mainTick.get()
                    if (currentTick == lastTick) {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastReportElapsedMs >= ANR_TIMEOUT_MS) {
                            lastReportElapsedMs = now
                            val mainThread = Looper.getMainLooper().thread
                            val topFrame = mainThread.stackTrace.firstOrNull()?.toString().orEmpty()
                            recordWarning(
                                event = "possible_anr",
                                fields = mapOf(
                                    "mainThread" to mainThread.name,
                                    "topFrame" to topFrame
                                )
                            )
                        }
                    }
                    lastTick = currentTick
                }
            },
            "xp-anr-watchdog"
        ).apply {
            isDaemon = true
            start()
        }
    }

    private fun record(level: String, event: String, fields: Map<String, String>) {
        val context = appContext ?: return
        val logFile = ensureLogFile(context) ?: return
        synchronized(this) {
            val logLine = buildLogLine(level = level, event = event, fields = fields)
            rotateIfNeeded(logFile)
            logFile.appendText(logLine)
        }
    }

    private fun ensureLogFile(context: Context): File? {
        val dir = File(context.filesDir, LOG_DIR)
        if (!dir.exists() && !dir.mkdirs()) return null
        return File(dir, LOG_FILE)
    }

    private fun rotateIfNeeded(file: File) {
        if (!file.exists()) return
        if (file.length() <= MAX_LOG_BYTES) return
        val rotated = File(file.parentFile, "$LOG_FILE.1")
        if (rotated.exists()) {
            rotated.delete()
        }
        file.copyTo(rotated, overwrite = true)
        file.writeText("")
    }

    private fun buildLogLine(level: String, event: String, fields: Map<String, String>): String {
        val ts = timestampFormat.format(Date())
        val fieldsText =
            if (fields.isEmpty()) {
                ""
            } else {
                fields.entries.joinToString(prefix = " ", separator = " ") { (key, value) ->
                    "$key=${sanitize(value)}"
                }
            }
        return "$ts level=$level event=$event$fieldsText\n"
    }

    private fun sanitize(value: String): String {
        return value
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(300)
    }
}
