package com.example.xtreamplayer.player

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewTreeObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import com.example.xtreamplayer.R
import androidx.media3.ui.R as Media3UiR

@UnstableApi
class XtreamPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    private var lastVideoAspectRatio = 0f
    private var resizeModeView: TextView? = null
    private var resizeModeLabel: String = "Fit"
    private var backButtonView: View? = null
    private var subtitleDownloadView: View? = null
    private var subtitleToggleView: View? = null
    private var audioTrackView: View? = null
    private var audioBoostView: View? = null
    private var settingsView: View? = null
    private var titleView: TextView? = null
    private var topBarView: View? = null
    var onResizeModeClick: (() -> Unit)? = null
        set(value) {
            field = value
            bindResizeModeView()
        }
    var onBackClick: (() -> Unit)? = null
        set(value) {
            field = value
            bindBackButtonView()
        }
    var onSubtitleDownloadClick: (() -> Unit)? = null
        set(value) {
            field = value
            bindSubtitleDownloadView()
        }
    var onSubtitleToggleClick: (() -> Unit)? = null
        set(value) {
            field = value
            bindSubtitleToggleView()
        }
    var onAudioTrackClick: (() -> Unit)? = null
        set(value) {
            field = value
            bindAudioTrackView()
        }
    var onAudioBoostClick: (() -> Unit)? = null
        set(value) {
            field = value
            bindAudioBoostView()
        }
    var onSettingsClick: (() -> Unit)? = null
        set(value) {
            field = value
            bindSettingsView()
        }
    var onChannelUp: (() -> Boolean)? = null
    var onChannelDown: (() -> Boolean)? = null
    var onToggleControls: (() -> Boolean)? = null
    var onSeekPreviewChanged: ((Boolean) -> Unit)? = null
    var fastSeekEnabled: Boolean = true
    var defaultControllerTimeoutMs: Int = 3000
    var titleText: String? = null
        set(value) {
            field = value
            bindTitleView()
        }
    var isLiveContent: Boolean = false
        set(value) {
            field = value
            updateControlsForContentType()
        }
    var forcedAspectRatio: Float? = null
        set(value) {
            field = value
            applyAspectRatio()
        }

    private val keyHandler = Handler(Looper.getMainLooper())
    private var pendingSeekKey: Int? = null
    private var isLongSeekActive = false
    private var longPressRunnable: Runnable? = null
    private var repeatSeekRunnable: Runnable? = null
    private var longSeekStartMs: Long = 0L
    private var seekPreviewActive = false
    private var pendingSeekFromHidden = false
    private var suppressSeekKeyUp = false
    private val topBarSyncListener = ViewTreeObserver.OnPreDrawListener {
        syncTopBarWithControlsBackground()
        true
    }

    fun setResizeModeLabel(label: String) {
        resizeModeLabel = label
        bindResizeModeView()
    }

    fun resetControllerFocus() {
        val controller = findViewById<View>(Media3UiR.id.exo_controller)
        controller?.clearFocus()
        requestFocus()
    }

    fun focusPlayPause() {
        findViewById<View>(Media3UiR.id.exo_play_pause)?.requestFocus()
    }

    fun dismissSettingsWindowIfShowing(): Boolean {
        val controller = findViewById<View>(Media3UiR.id.exo_controller) as? PlayerControlView
            ?: return false
        return runCatching {
            val field = PlayerControlView::class.java.getDeclaredField("settingsWindow")
            field.isAccessible = true
            val popup = field.get(controller) as? PopupWindow ?: return false
            if (popup.isShowing) {
                popup.dismiss()
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    override fun onContentAspectRatioChanged(
        contentFrame: AspectRatioFrameLayout?,
        aspectRatio: Float
    ) {
        lastVideoAspectRatio = aspectRatio
        val resolvedRatio = forcedAspectRatio ?: lastVideoAspectRatio
        super.onContentAspectRatioChanged(contentFrame, resolvedRatio)
    }

    private fun applyAspectRatio() {
        val frame = findViewById<AspectRatioFrameLayout>(Media3UiR.id.exo_content_frame)
        val ratio = forcedAspectRatio ?: lastVideoAspectRatio
        if (frame != null && ratio > 0f) {
            frame.setAspectRatio(ratio)
            frame.requestLayout()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bindResizeModeView()
        bindBackButtonView()
        bindSubtitleDownloadView()
        bindSubtitleToggleView()
        bindAudioTrackView()
        bindAudioBoostView()
        bindSettingsView()
        bindTitleView()
        updateControlsForContentType()
        updateFocusOrder()
        viewTreeObserver.addOnPreDrawListener(topBarSyncListener)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnPreDrawListener(topBarSyncListener)
        super.onDetachedFromWindow()
    }

    private fun syncTopBarWithControlsBackground() {
        val background = findViewById<View>(Media3UiR.id.exo_controls_background) ?: return
        val topBar = topBarView ?: findViewById<View>(R.id.exo_top_bar)?.also {
            topBarView = it
        } ?: return

        topBar.visibility = background.visibility
        topBar.alpha = background.alpha
    }

    private fun areControlsShowing(): Boolean {
        val background = findViewById<View>(Media3UiR.id.exo_controls_background)
        return background?.visibility == View.VISIBLE && background.alpha > 0.01f
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_MENU ||
                event.keyCode == KeyEvent.KEYCODE_INFO) {
                val handled = onToggleControls?.invoke() ?: false
                if (handled) return true
            }
            val controlsShowing = areControlsShowing()
            if (fastSeekEnabled &&
                !isLiveContent &&
                (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            ) {
                val focused = findFocus()
                val canHandleSeek = if (controlsShowing) {
                    focused?.id == Media3UiR.id.exo_progress
                } else {
                    true
                }
                if (canHandleSeek) {
                    if (pendingSeekKey == null) {
                        pendingSeekKey = event.keyCode
                        isLongSeekActive = false
                        pendingSeekFromHidden = !controlsShowing
                        if (pendingSeekFromHidden) {
                            setSeekPreviewVisible(true)
                        }
                        longPressRunnable = Runnable {
                            isLongSeekActive = true
                            if (!pendingSeekFromHidden) {
                                setControllerShowTimeoutMs(0)
                                showController()
                            }
                            longSeekStartMs = SystemClock.uptimeMillis()
                            val direction = pendingSeekKey
                            repeatSeekRunnable = object : Runnable {
                                override fun run() {
                                    val currentPlayer = player
                                    if (currentPlayer != null && pendingSeekKey == direction) {
                                        val elapsed = SystemClock.uptimeMillis() - longSeekStartMs
                                        val stepMs = resolveLongSeekStepMs(elapsed)
                                        val delta = if (direction == KeyEvent.KEYCODE_DPAD_LEFT) {
                                            -stepMs
                                        } else {
                                            stepMs
                                        }
                                        val duration = currentPlayer.duration
                                        val target = (currentPlayer.currentPosition + delta).let { next ->
                                            if (duration > 0) {
                                                next.coerceIn(0L, duration)
                                            } else {
                                                next.coerceAtLeast(0L)
                                            }
                                        }
                                        currentPlayer.seekTo(target)
                                        keyHandler.postDelayed(this, LONG_SEEK_REPEAT_MS)
                                    }
                                }
                            }
                            repeatSeekRunnable?.let { keyHandler.post(it) }
                        }
                        keyHandler.postDelayed(longPressRunnable!!, LONG_SEEK_DELAY_MS)
                    }
                    return true
                }
            }
            val isSelect =
                event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    event.keyCode == KeyEvent.KEYCODE_ENTER ||
                    event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

            if (!controlsShowing) {
                if (isSelect) {
                    showController()
                    return true
                }
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (isLiveContent) {
                            return onChannelUp?.invoke() ?: false
                        }
                        showController()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (isLiveContent) {
                            return onChannelDown?.invoke() ?: false
                        }
                        showController()
                        return true
                    }
                }
            }

            if (controlsShowing) {
                return super.dispatchKeyEvent(event)
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (handleSeekKeyUp(event.keyCode)) {
                suppressSeekKeyUp = true
                return true
            }
        } else if (event.action == android.view.MotionEvent.ACTION_CANCEL) {
            if (pendingSeekKey != null) {
                stopSeekRepeat()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (suppressSeekKeyUp) {
            suppressSeekKeyUp = false
            return super.onKeyUp(keyCode, event)
        }
        if (handleSeekKeyUp(keyCode)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun setSeekPreviewVisible(visible: Boolean) {
        if (seekPreviewActive == visible) return
        seekPreviewActive = visible
        onSeekPreviewChanged?.invoke(visible)
    }

    private fun stopSeekRepeat() {
        longPressRunnable?.let { keyHandler.removeCallbacks(it) }
        repeatSeekRunnable?.let { keyHandler.removeCallbacks(it) }
        longPressRunnable = null
        repeatSeekRunnable = null
        isLongSeekActive = false
        pendingSeekKey = null
        longSeekStartMs = 0L
        if (!pendingSeekFromHidden) {
            setControllerShowTimeoutMs(defaultControllerTimeoutMs)
        } else {
            setSeekPreviewVisible(false)
            pendingSeekFromHidden = false
        }
    }

    private fun handleSeekKeyUp(keyCode: Int): Boolean {
        if (!fastSeekEnabled ||
            isLiveContent ||
            (keyCode != KeyEvent.KEYCODE_DPAD_LEFT &&
                keyCode != KeyEvent.KEYCODE_DPAD_RIGHT)
        ) {
            return false
        }
        if (pendingSeekKey == null) return false
        val wasLongSeek = isLongSeekActive
        stopSeekRepeat()
        if (!wasLongSeek) {
            val currentPlayer = player ?: return true
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                currentPlayer.seekBack()
            } else {
                currentPlayer.seekForward()
            }
        }
        return true
    }

    private fun updateControlsForContentType() {
        val isLive = isLiveContent
        setViewVisible(Media3UiR.id.exo_prev, !isLive)
        setViewVisible(Media3UiR.id.exo_next, !isLive)
        setViewVisible(Media3UiR.id.exo_rew_with_amount, !isLive)
        setViewVisible(Media3UiR.id.exo_ffwd_with_amount, !isLive)
        updateFocusOrder()
    }

    private fun setViewVisible(id: Int, visible: Boolean) {
        val view = findViewById<View>(id) ?: return
        view.visibility = if (visible) View.VISIBLE else View.GONE
        view.isFocusable = visible
    }

    private fun updateFocusOrder() {
        updateRowFocusOrder(
            listOf(
                Media3UiR.id.exo_vr,
                Media3UiR.id.exo_shuffle,
                Media3UiR.id.exo_repeat_toggle,
                Media3UiR.id.exo_subtitle,
                R.id.exo_subtitle_download,
                R.id.exo_audio_track,
                R.id.exo_audio_boost,
                R.id.exo_resize_mode,
                Media3UiR.id.exo_settings,
                Media3UiR.id.exo_fullscreen,
                Media3UiR.id.exo_overflow_show
            )
        )
        updateRowFocusOrder(
            listOf(
                Media3UiR.id.exo_prev,
                Media3UiR.id.exo_rew_with_amount,
                Media3UiR.id.exo_play_pause,
                Media3UiR.id.exo_ffwd_with_amount,
                Media3UiR.id.exo_next
            )
        )
    }

    private fun updateRowFocusOrder(ids: List<Int>) {
        val views = ids.mapNotNull { findViewById<View>(it) }
            .filter { it.visibility == View.VISIBLE && it.isFocusable }
        if (views.isEmpty()) return
        views.forEachIndexed { index, view ->
            val left = views.getOrNull(index - 1)
            val right = views.getOrNull(index + 1)
            view.nextFocusLeftId = left?.id ?: View.NO_ID
            view.nextFocusRightId = right?.id ?: View.NO_ID
        }
    }

    private fun bindSubtitleDownloadView() {
        val view = subtitleDownloadView ?: findViewById<View>(R.id.exo_subtitle_download).also {
            subtitleDownloadView = it
        }
        view?.setOnClickListener { onSubtitleDownloadClick?.invoke() }
    }

    private fun bindSubtitleToggleView() {
        val view = subtitleToggleView
            ?: findViewById<View>(Media3UiR.id.exo_subtitle).also {
                subtitleToggleView = it
            }
        view?.setOnClickListener { onSubtitleToggleClick?.invoke() }
    }

    private fun bindResizeModeView() {
        val view = resizeModeView ?: findViewById<TextView>(R.id.exo_resize_mode).also {
            resizeModeView = it
        }
        if (view != null) {
            view.text = resizeModeLabel
            view.setOnClickListener { onResizeModeClick?.invoke() }
        }
    }

    private fun bindBackButtonView() {
        val view = backButtonView ?: findViewById<View>(R.id.exo_back).also {
            backButtonView = it
        }
        view?.setOnClickListener { onBackClick?.invoke() }
    }

    private fun bindAudioTrackView() {
        val view = audioTrackView ?: findViewById<View>(R.id.exo_audio_track).also {
            audioTrackView = it
        }
        view?.setOnClickListener { onAudioTrackClick?.invoke() }
    }

    private fun bindAudioBoostView() {
        val view = audioBoostView ?: findViewById<View>(R.id.exo_audio_boost).also {
            audioBoostView = it
        }
        view?.setOnClickListener { onAudioBoostClick?.invoke() }
    }

    private fun bindSettingsView() {
        val view = settingsView ?: findViewById<View>(Media3UiR.id.exo_settings).also {
            settingsView = it
        }
        view?.setOnClickListener { onSettingsClick?.invoke() }
    }

    private fun bindTitleView() {
        val view = titleView ?: findViewById<TextView>(R.id.exo_title).also {
            titleView = it
        }
        view?.text = titleText.orEmpty()
    }

}

private fun resolveLongSeekStepMs(elapsedMs: Long): Long {
    return when {
        elapsedMs < 2_000L -> 1_000L
        elapsedMs < 4_000L -> 2_000L
        elapsedMs < 7_000L -> 5_000L
        elapsedMs < 11_000L -> 10_000L
        elapsedMs < 15_000L -> 20_000L
        else -> 30_000L
    }
}

private const val LONG_SEEK_DELAY_MS = 350L
private const val LONG_SEEK_REPEAT_MS = 80L
