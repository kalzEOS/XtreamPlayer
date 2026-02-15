package com.example.xtreamplayer.player

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import com.example.xtreamplayer.R
import java.util.Locale
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
    private var subtitleTimingView: View? = null
    private var audioTrackView: View? = null
    private var audioBoostView: View? = null
    private var settingsView: View? = null
    private var prevButtonView: View? = null
    private var nextButtonView: View? = null
    private var titleView: TextView? = null
    private var nowPlayingInfoView: TextView? = null
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
    var onSubtitleTimingClick: (() -> Unit)? = null
        set(value) {
            field = value
            bindSubtitleTimingView()
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
        set(value) {
            field = value
            bindPrevNextView()
        }
    var onChannelDown: (() -> Boolean)? = null
        set(value) {
            field = value
            bindPrevNextView()
        }
    var onOpenLiveGuide: (() -> Boolean)? = null
    var onLiveGuideMove: ((Int) -> Boolean)? = null
    var onLiveGuideSelect: (() -> Boolean)? = null
    var onLiveGuideBack: (() -> Boolean)? = null
    var onLiveGuideSearchKey: ((KeyEvent) -> Boolean)? = null
    var onLiveGuideDismiss: (() -> Boolean)? = null
    var isLiveGuideOpen: Boolean = false
    var onToggleControls: (() -> Boolean)? = null
    var fastSeekEnabled: Boolean = true
    var defaultControllerTimeoutMs: Int = 3000
    var titleText: String? = null
        set(value) {
            field = value
            bindTitleView()
        }
    var nowPlayingInfoText: String? = null
        set(value) {
            field = value
            bindNowPlayingInfoView()
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
    private var suppressSeekKeyUp = false
    private var suppressHideControllerBackKeyUp = false
    private var suppressHiddenSeekKeyUp = false
    private var suppressHiddenSelectKeyUp = false
    private var isHiddenSeek = false
    private var suppressLiveGuideRightKeyUp = false
    private var seekHudLayout: LinearLayout? = null
    private var seekHudText: TextView? = null
    private var seekHudProgress: ProgressBar? = null
    private var seekHudHideRunnable: Runnable? = null
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
        bindSubtitleTimingView()
        bindAudioTrackView()
        bindAudioBoostView()
        bindSettingsView()
        bindTitleView()
        bindNowPlayingInfoView()
        updateControlsForContentType()
        updateFocusOrder()
        viewTreeObserver.addOnPreDrawListener(topBarSyncListener)
    }

    override fun onDetachedFromWindow() {
        stopSeekRepeat()
        hideSeekHud()
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

    private fun isControllerVisibleForNavigation(): Boolean {
        if (isControllerFullyVisible) return true
        val background = findViewById<View>(Media3UiR.id.exo_controls_background)
        return background?.visibility == View.VISIBLE && background.alpha > 0.01f
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (isLiveGuideOpen) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_BACK,
                    KeyEvent.KEYCODE_ESCAPE -> {
                        return onLiveGuideDismiss?.invoke() ?: true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        return onLiveGuideMove?.invoke(-1) ?: true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        return onLiveGuideMove?.invoke(1) ?: true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        return onLiveGuideBack?.invoke() ?: true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        return onLiveGuideSelect?.invoke() ?: true
                    }
                    else -> {
                        if (onLiveGuideSearchKey?.invoke(event) == true) {
                            return true
                        }
                    }
                }
            }
            if (event.keyCode == KeyEvent.KEYCODE_MENU ||
                event.keyCode == KeyEvent.KEYCODE_INFO) {
                val handled = onToggleControls?.invoke() ?: false
                if (handled) return true
            }
            if (isLiveContent &&
                event.repeatCount > 0 &&
                (event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
            ) {
                // Ignore held repeats to prevent rapid control focus run-through / accidental zapping.
                return true
            }
            val controlsShowing = isControllerVisibleForNavigation()
            val controlsFullyVisible = isControllerFullyVisible
            if (controlsShowing &&
                event.repeatCount > 0 &&
                (event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            ) {
                val focusedId = findFocus()?.id
                val isProgressSeekRepeat =
                    fastSeekEnabled &&
                        !isLiveContent &&
                        (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                            event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) &&
                        controlsFullyVisible &&
                        focusedId == Media3UiR.id.exo_progress
                if (!isProgressSeekRepeat) {
                    // Block hold-repeat run-through on controller navigation.
                    return true
                }
            }
            if (controlsShowing &&
                (event.keyCode == KeyEvent.KEYCODE_BACK ||
                    event.keyCode == KeyEvent.KEYCODE_ESCAPE)
            ) {
                hideController()
                suppressHideControllerBackKeyUp = true
                return true
            }
            if (controlsShowing && event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                val focused = findFocus()
                val backId = backButtonView?.id ?: R.id.exo_back
                if (focused?.id == backId) {
                    return true
                }
            }
            if (fastSeekEnabled &&
                !isLiveContent &&
                (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            ) {
                if (player == null) {
                    return super.dispatchKeyEvent(event)
                }
                // Once a seek cycle is armed, consume repeated key-down events even if
                // controller visibility/focus changes mid-hold.
                if (pendingSeekKey == event.keyCode) {
                    return true
                }
                val focused = findFocus()
                val canHandleVisibleSeek =
                    controlsFullyVisible && focused?.id == Media3UiR.id.exo_progress
                val canHandleHiddenSeek = !controlsFullyVisible
                val canHandleSeek = canHandleVisibleSeek || canHandleHiddenSeek
                if (canHandleSeek) {
                    // Clear stale guard from any previous hidden-seek cycle.
                    suppressHiddenSeekKeyUp = false
                    if (pendingSeekKey == null) {
                        val hidden = canHandleHiddenSeek
                        isHiddenSeek = hidden
                        pendingSeekKey = event.keyCode
                        isLongSeekActive = false
                        longPressRunnable = Runnable {
                            isLongSeekActive = true
                            if (!hidden) {
                                setControllerShowTimeoutMs(0)
                                showController()
                            }
                            longSeekStartMs = SystemClock.uptimeMillis()
                            val direction = pendingSeekKey
                            repeatSeekRunnable = object : Runnable {
                                override fun run() {
                                    val currentPlayer = player
                                    if (currentPlayer == null) {
                                        stopSeekRepeat()
                                        return
                                    }
                                    if (pendingSeekKey == direction) {
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
                                        if (hidden) {
                                            updateSeekHud(direction ?: event.keyCode)
                                        }
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

            if (isSelect && event.repeatCount > 0) {
                // Prevent hold-repeat from toggling play/pause continuously.
                return true
            }

            if (isSelect && !controlsFullyVisible) {
                if (!isLiveContent) {
                    player?.let { currentPlayer ->
                        if (currentPlayer.isPlaying) {
                            currentPlayer.pause()
                        } else {
                            currentPlayer.play()
                        }
                        suppressHiddenSelectKeyUp = true
                        return true
                    }
                }
                showController()
                return true
            }

            if (!controlsShowing) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (isLiveContent) {
                            if (onChannelUp?.invoke() == true) {
                                showController()
                            }
                            return true
                        }
                        showController()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (isLiveContent) {
                            if (onChannelDown?.invoke() == true) {
                                showController()
                            }
                            return true
                        }
                        showController()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (isLiveContent) {
                            val opened = onOpenLiveGuide?.invoke() ?: false
                            if (opened) {
                                suppressLiveGuideRightKeyUp = true
                            }
                            return opened
                        }
                    }
                }
            }

            if (controlsShowing) {
                return super.dispatchKeyEvent(event)
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (isLiveGuideOpen) {
                return when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                    KeyEvent.KEYCODE_BACK,
                    KeyEvent.KEYCODE_ESCAPE -> true
                    else -> false
                }
            }
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && suppressLiveGuideRightKeyUp) {
                suppressLiveGuideRightKeyUp = false
                return true
            }
            if (suppressHideControllerBackKeyUp &&
                (event.keyCode == KeyEvent.KEYCODE_BACK ||
                    event.keyCode == KeyEvent.KEYCODE_ESCAPE)
            ) {
                suppressHideControllerBackKeyUp = false
                return true
            }
            if (suppressHiddenSelectKeyUp &&
                (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    event.keyCode == KeyEvent.KEYCODE_ENTER ||
                    event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
            ) {
                suppressHiddenSelectKeyUp = false
                return true
            }
            // Media3 PlayerView.dispatchKeyEvent does not check event action;
            // a D-pad ACTION_UP reaching super will show the controller.
            if (isLiveContent && !isControllerVisibleForNavigation() &&
                (event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
            ) {
                return true
            }
            if (handleSeekKeyUp(event.keyCode)) {
                suppressSeekKeyUp = true
                return true
            }
            // Robust guard: consume hidden-seek Left/Right key-ups only when
            // no pending seek was handled above.
            if (suppressHiddenSeekKeyUp &&
                (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            ) {
                suppressHiddenSeekKeyUp = false
                return true
            }
        } else if (event.action == android.view.MotionEvent.ACTION_CANCEL) {
            if (pendingSeekKey != null) {
                stopSeekRepeat()
                return true
            }
            suppressLiveGuideRightKeyUp = false
            suppressHideControllerBackKeyUp = false
            suppressHiddenSelectKeyUp = false
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isLiveGuideOpen) {
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_ESCAPE -> true
                else -> super.onKeyUp(keyCode, event)
            }
        }
        if (isLiveContent &&
            !isControllerVisibleForNavigation() &&
            (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
        ) {
            return true
        }
        if (suppressSeekKeyUp) {
            suppressSeekKeyUp = false
            return super.onKeyUp(keyCode, event)
        }
        if (suppressHideControllerBackKeyUp &&
            (keyCode == KeyEvent.KEYCODE_BACK ||
                keyCode == KeyEvent.KEYCODE_ESCAPE)
        ) {
            suppressHideControllerBackKeyUp = false
            return true
        }
        if (suppressHiddenSelectKeyUp &&
            (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
        ) {
            suppressHiddenSelectKeyUp = false
            return true
        }
        if (handleSeekKeyUp(keyCode)) {
            return true
        }
        if (suppressHiddenSeekKeyUp &&
            (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
        ) {
            suppressHiddenSeekKeyUp = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun stopSeekRepeat() {
        longPressRunnable?.let { keyHandler.removeCallbacks(it) }
        repeatSeekRunnable?.let { keyHandler.removeCallbacks(it) }
        longPressRunnable = null
        repeatSeekRunnable = null
        val wasHidden = isHiddenSeek
        isLongSeekActive = false
        isHiddenSeek = false
        pendingSeekKey = null
        longSeekStartMs = 0L
        if (wasHidden) {
            suppressHiddenSeekKeyUp = true
        } else {
            setControllerShowTimeoutMs(defaultControllerTimeoutMs)
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
        val wasHidden = isHiddenSeek
        stopSeekRepeat()
        if (!wasLongSeek) {
            val currentPlayer = player
            if (currentPlayer == null) {
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                currentPlayer.seekBack()
            } else {
                currentPlayer.seekForward()
            }
        }
        if (wasHidden) {
            updateSeekHud(keyCode)
            scheduleSeekHudHide()
        }
        return true
    }

    private fun updateControlsForContentType() {
        val isLive = isLiveContent
        setViewVisible(Media3UiR.id.exo_prev, true)
        setViewVisible(Media3UiR.id.exo_next, true)
        setViewVisible(Media3UiR.id.exo_rew_with_amount, !isLive)
        setViewVisible(Media3UiR.id.exo_ffwd_with_amount, !isLive)
        setViewVisible(Media3UiR.id.exo_shuffle, false)
        setViewVisible(Media3UiR.id.exo_repeat_toggle, false)
        setViewVisible(Media3UiR.id.exo_subtitle, false)
        setViewVisible(R.id.exo_subtitle_timing, !isLive)
        setViewVisible(R.id.exo_subtitle_download, !isLive)
        bindPrevNextView()
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
                R.id.exo_subtitle_timing,
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
            .filter { it.isVisible && it.isFocusable }
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

    private fun bindSubtitleTimingView() {
        val view = subtitleTimingView ?: findViewById<View>(R.id.exo_subtitle_timing).also {
            subtitleTimingView = it
        }
        view?.setOnClickListener { onSubtitleTimingClick?.invoke() }
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
        if (view != null) {
            view.nextFocusUpId = view.id
            view.setOnClickListener { onBackClick?.invoke() }
        }
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

    private fun bindPrevNextView() {
        val prev = prevButtonView ?: findViewById<View>(Media3UiR.id.exo_prev).also {
            prevButtonView = it
        }
        val next = nextButtonView ?: findViewById<View>(Media3UiR.id.exo_next).also {
            nextButtonView = it
        }
        prev?.setOnClickListener {
            if (isLiveContent) {
                onChannelDown?.invoke()
            } else {
                player?.seekToPrevious()
            }
        }
        next?.setOnClickListener {
            if (isLiveContent) {
                onChannelUp?.invoke()
            } else {
                player?.seekToNext()
            }
        }
    }

    private fun bindTitleView() {
        val view = titleView ?: findViewById<TextView>(R.id.exo_title).also {
            titleView = it
        }
        view?.text = titleText.orEmpty()
    }

    private fun bindNowPlayingInfoView() {
        val view = nowPlayingInfoView ?: findViewById<TextView>(R.id.exo_now_playing_info).also {
            nowPlayingInfoView = it
        }
        if (view != null) {
            val text = nowPlayingInfoText.orEmpty().trim()
            view.text = text
            view.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
            if (text.isBlank()) {
                view.isSelected = false
            } else {
                view.ellipsize = TextUtils.TruncateAt.MARQUEE
                view.marqueeRepeatLimit = -1
                view.isSingleLine = true
                view.setHorizontallyScrolling(true)
                // Marquee only runs when selected/focused.
                view.isSelected = true
            }
        }
    }

    // ---- Seek HUD ----

    private fun ensureSeekHud(): LinearLayout {
        seekHudLayout?.let { return it }
        val dp = resources.displayMetrics.density
        val bg = GradientDrawable().apply {
            setColor(Color.argb(190, 0, 0, 0))
            cornerRadius = 12f * dp
        }
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding((20 * dp).toInt(), (8 * dp).toInt(), (20 * dp).toInt(), (8 * dp).toInt())
            background = bg
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            visibility = View.GONE
        }
        val text = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isFocusable = false
            isFocusableInTouchMode = false
        }
        layout.addView(
            text,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = 0
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            progressTintList = ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = ColorStateList.valueOf(Color.argb(80, 255, 255, 255))
        }
        layout.addView(
            progress,
            LinearLayout.LayoutParams(
                (200 * dp).toInt(),
                (3 * dp).toInt()
            ).apply {
                topMargin = (6 * dp).toInt()
            }
        )
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (100 * dp).toInt()
        }
        addView(layout, params)
        seekHudLayout = layout
        seekHudText = text
        seekHudProgress = progress
        return layout
    }

    private fun updateSeekHud(directionKeyCode: Int) {
        val currentPlayer = player ?: return
        val layout = ensureSeekHud()
        val posMs = currentPlayer.currentPosition.coerceAtLeast(0L)
        val durMs = currentPlayer.duration
        val arrow = if (directionKeyCode == KeyEvent.KEYCODE_DPAD_LEFT) "\u00AB " else " \u00BB"
        if (durMs > 0) {
            val posStr = formatTimeMs(posMs)
            val durStr = formatTimeMs(durMs)
            seekHudText?.text = if (directionKeyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                "$arrow$posStr / $durStr"
            } else {
                "$posStr / $durStr$arrow"
            }
            seekHudProgress?.progress = (posMs * 1000 / durMs).toInt().coerceIn(0, 1000)
            seekHudProgress?.isIndeterminate = false
        } else {
            seekHudText?.text = if (directionKeyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                "${arrow}${formatTimeMs(posMs)}"
            } else {
                "${formatTimeMs(posMs)}$arrow"
            }
            seekHudProgress?.isIndeterminate = true
        }
        layout.visibility = View.VISIBLE
        cancelSeekHudHide()
    }

    private fun scheduleSeekHudHide() {
        cancelSeekHudHide()
        val runnable = Runnable { seekHudLayout?.visibility = View.GONE }
        seekHudHideRunnable = runnable
        keyHandler.postDelayed(runnable, SEEK_HUD_HIDE_DELAY_MS)
    }

    private fun cancelSeekHudHide() {
        seekHudHideRunnable?.let { keyHandler.removeCallbacks(it) }
        seekHudHideRunnable = null
    }

    private fun hideSeekHud() {
        cancelSeekHudHide()
        seekHudLayout?.visibility = View.GONE
    }

}

private fun resolveLongSeekStepMs(elapsedMs: Long): Long {
    return when {
        elapsedMs < 1_500L -> 10_000L
        elapsedMs < 3_500L -> 20_000L
        elapsedMs < 6_500L -> 30_000L
        elapsedMs < 10_000L -> 45_000L
        elapsedMs < 14_000L -> 60_000L
        else -> 90_000L
    }
}

private const val LONG_SEEK_DELAY_MS = 350L
private const val LONG_SEEK_REPEAT_MS = 65L
private const val SEEK_HUD_HIDE_DELAY_MS = 1000L

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
