package com.example.xtreamplayer.player

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.util.StateSet
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible
import androidx.core.graphics.toColorInt
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import com.example.xtreamplayer.R
import com.example.xtreamplayer.observability.AppDiagnostics
import com.example.xtreamplayer.settings.SubtitleAppearanceSettings
import com.example.xtreamplayer.settings.applySubtitleAppearanceSettings
import java.util.Locale
import kotlin.math.roundToInt
import timber.log.Timber
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
    private var positionTimeView: TextView? = null
    private var durationTimeView: TextView? = null
    private var nowPlayingTickerLayoutListener: View.OnLayoutChangeListener? = null
    private var nowPlayingTickerAnimator: ValueAnimator? = null
    private var focusAuditLoggedOnce = false
    private val controllerProgressListener = object : PlayerControlView.ProgressUpdateListener {
        override fun onProgressUpdate(position: Long, bufferedPosition: Long) {
            updateBottomTimeLabels(position)
        }
    }
    var focusAccentColor: Int = "#4F8CFF".toColorInt()
        set(value) {
            if (field == value) return
            field = value
            applyFocusChrome()
        }
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
    var subtitleAppearanceSettings: SubtitleAppearanceSettings = SubtitleAppearanceSettings()
        set(value) {
            field = value
            applySubtitleAppearance()
        }
    var isLiveContent: Boolean = false
        set(value) {
            if (field == value) return
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
    private val nowPlayingTickerSeparator = "     |     "
    private val nowPlayingTickerSpeedDpPerSecond = 36f
    private val nowPlayingTickerMinDurationMs = 4000L
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
        val downHandled = controller.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        val upHandled = controller.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        return downHandled || upHandled
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
        bindBottomTimeViews()
        applySubtitleAppearance()
        updateControlsForContentType()
        updateFocusOrder()
        applyFocusChrome()
        viewTreeObserver.addOnPreDrawListener(topBarSyncListener)
    }

    override fun onDetachedFromWindow() {
        stopSeekRepeat()
        hideSeekHud()
        clearNowPlayingTickerLayoutListener()
        stopNowPlayingTickerAnimation()
        (findViewById<View>(Media3UiR.id.exo_controller) as? PlayerControlView)
            ?.setProgressUpdateListener(null)
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

    private fun applySubtitleAppearance() {
        val subtitleView = findViewById<androidx.media3.ui.SubtitleView>(Media3UiR.id.exo_subtitles)
            ?: return
        val activeSettings =
            if (
                subtitleAppearanceSettings.customStyleEnabled &&
                    !subtitleAppearanceSettings.overrideEmbeddedStyles &&
                    !hasExternalSubtitleConfiguration()
            ) {
                // Keep embedded subtitle author styling when override is disabled.
                subtitleAppearanceSettings.copy(customStyleEnabled = false)
            } else {
                subtitleAppearanceSettings
            }
        subtitleView.applySubtitleAppearanceSettings(activeSettings)
    }

    private fun hasExternalSubtitleConfiguration(): Boolean {
        return player?.currentMediaItem
            ?.localConfiguration
            ?.subtitleConfigurations
            ?.isNotEmpty() == true
    }

    private fun isControllerVisibleForNavigation(): Boolean {
        if (isControllerFullyVisible) return true
        val controller = findViewById<View>(Media3UiR.id.exo_controller) ?: return false
        if (controller.visibility != View.VISIBLE || !controller.isShown) return false
        val background = findViewById<View>(Media3UiR.id.exo_controls_background)
        val backgroundVisible =
            background != null &&
                background.visibility == View.VISIBLE &&
                background.alpha > 0.06f
        val bottomBar = findViewById<View>(Media3UiR.id.exo_bottom_bar)
        val bottomBarVisible =
            bottomBar != null &&
                bottomBar.visibility == View.VISIBLE &&
                bottomBar.alpha > 0.06f &&
                (bottomBar.height <= 1 || bottomBar.translationY < bottomBar.height * 0.9f)
        return backgroundVisible || bottomBarVisible
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
            val controlsFullyVisible = isControllerFullyVisible
            val controlsShowing = isControllerVisibleForNavigation()
            if (controlsShowing &&
                event.repeatCount == 0 &&
                (event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            ) {
                updateFocusOrder()
                if (handleExplicitControllerNavigation(event.keyCode)) {
                    return true
                }
            }
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
        val progress = findViewById<View>(Media3UiR.id.exo_progress)
        if (progress != null) {
            if (isLive) {
                progress.visibility = View.GONE
                progress.isEnabled = false
                progress.isFocusable = false
            } else {
                progress.visibility = View.VISIBLE
                progress.isEnabled = true
                progress.isFocusable = true
            }
        }
        setViewVisible(Media3UiR.id.exo_rew_with_amount, !isLive)
        setViewVisible(Media3UiR.id.exo_ffwd_with_amount, !isLive)
        setViewVisible(Media3UiR.id.exo_shuffle, false)
        setViewVisible(Media3UiR.id.exo_repeat_toggle, false)
        setViewVisible(Media3UiR.id.exo_subtitle, false)
        setViewVisible(R.id.exo_subtitle_timing, !isLive)
        setViewVisible(R.id.exo_subtitle_download, !isLive)
        findViewById<View>(R.id.exo_live_progress_line)?.visibility =
            if (isLive) View.VISIBLE else View.GONE
        bindPrevNextView()
        updateBottomTimeLabels()
        updateFocusOrder()
        applyFocusChrome()
    }

    private fun setViewVisible(id: Int, visible: Boolean) {
        val view = findViewById<View>(id) ?: return
        view.visibility = if (visible) View.VISIBLE else View.GONE
        view.isFocusable = visible
    }

    private fun updateFocusOrder() {
        val transportControls = navigableControls(transportControlOrderIds())
        val rightControls = navigableControls(rightControlOrderIds())
        wireHorizontalLane(transportControls + rightControls)
        wireVerticalFocusTargets(transportControls, rightControls)
        runFocusMapAudit(transportControls, rightControls)
    }

    private fun runFocusMapAudit(transportControls: List<View>, rightControls: List<View>) {
        if (!isDebuggableBuild()) return
        val lane = transportControls + rightControls
        val issues =
            auditLinearFocusLane(
                lane.map { view ->
                    FocusLinkSnapshot(
                        id = view.id,
                        nextFocusLeftId = view.nextFocusLeftId,
                        nextFocusRightId = view.nextFocusRightId
                    )
                }
            )
        if (issues.isEmpty()) {
            if (!focusAuditLoggedOnce) {
                focusAuditLoggedOnce = true
                Timber.d("FocusAudit: lane validated (${lane.size} controls)")
            }
            return
        }
        issues.take(8).forEach { issue ->
            Timber.w("FocusAudit: $issue")
            AppDiagnostics.recordWarning(
                event = "focus_map_issue",
                fields = mapOf("detail" to issue)
            )
        }
    }

    private fun isDebuggableBuild(): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun navigableControls(ids: List<Int>): List<View> {
        return ids.mapNotNull { findViewById<View>(it) }
            .filter { isControlNavigable(it) }
            .onEach { ensureControlNavigable(it) }
    }

    private fun ensureControlNavigable(view: View) {
        // Keep DPAD traversal reliable without overriding player command availability.
        if (!view.isFocusable) {
            view.isFocusable = true
        }
        if (!view.isFocusableInTouchMode) {
            view.isFocusableInTouchMode = true
        }
    }

    private fun isControlNavigable(view: View): Boolean {
        if (!view.isVisible || !view.isShown) return false
        if (!view.isEnabled) return false
        return true
    }

    private fun wireHorizontalLane(views: List<View>) {
        if (views.isEmpty()) return
        views.forEachIndexed { index, view ->
            val left = views.getOrNull(index - 1)
            val right = views.getOrNull(index + 1)
            view.nextFocusLeftId = left?.id ?: View.NO_ID
            view.nextFocusRightId = right?.id ?: View.NO_ID
        }
    }

    private fun wireVerticalFocusTargets(transportControls: List<View>, rightControls: List<View>) {
        val progress = findViewById<View>(Media3UiR.id.exo_progress) ?: return
        val back = backButtonView ?: findViewById<View>(R.id.exo_back)
        val progressVisible = isProgressNavigationEnabled(progress)
        val preferredTransport =
            transportControls.firstOrNull { it.id == Media3UiR.id.exo_play_pause }
                ?: transportControls.firstOrNull()
                ?: rightControls.firstOrNull()
        if (progressVisible) {
            back?.nextFocusDownId = progress.id
            progress.nextFocusUpId = back?.id ?: View.NO_ID
            progress.nextFocusDownId = preferredTransport?.id ?: View.NO_ID
            (transportControls + rightControls).forEach { control ->
                control.nextFocusUpId = progress.id
            }
        } else {
            back?.nextFocusDownId = preferredTransport?.id ?: View.NO_ID
            preferredTransport?.nextFocusUpId = back?.id ?: View.NO_ID
            (transportControls + rightControls).forEach { control ->
                control.nextFocusUpId = back?.id ?: View.NO_ID
            }
        }
    }

    private fun isProgressNavigationEnabled(progress: View?): Boolean {
        if (isLiveContent) return false
        if (progress == null) return false
        if (progress.visibility != View.VISIBLE || !progress.isShown) return false
        // Skip the seek bar in DPAD routing when Media3 marks it disabled (e.g., failed/non-seekable playback).
        if (!progress.isEnabled) return false
        if (!progress.isFocusable && !progress.hasFocusable()) return false
        if (progress.alpha <= 0.01f) return false
        if (progress.width <= 1 || progress.height <= 1) return false
        return true
    }

    private fun transportControlOrderIds(): List<Int> {
        return listOf(
            Media3UiR.id.exo_prev,
            Media3UiR.id.exo_rew_with_amount,
            Media3UiR.id.exo_play_pause,
            Media3UiR.id.exo_ffwd_with_amount,
            Media3UiR.id.exo_next
        )
    }

    private fun rightControlOrderIds(): List<Int> {
        return listOf(
            R.id.exo_subtitle_timing,
            Media3UiR.id.exo_subtitle,
            R.id.exo_subtitle_download,
            R.id.exo_audio_track,
            R.id.exo_audio_boost,
            R.id.exo_resize_mode,
            Media3UiR.id.exo_settings,
            Media3UiR.id.exo_fullscreen,
            Media3UiR.id.exo_overflow_show
        )
    }

    private fun moveFocusTo(target: View?): Boolean {
        if (target == null || !isControlNavigable(target)) {
            return false
        }
        ensureControlNavigable(target)
        val moved = target.requestFocus()
        if (moved && isControllerVisibleForNavigation()) {
            setControllerShowTimeoutMs(defaultControllerTimeoutMs)
            showController()
        }
        return moved
    }

    private fun applyFocusChrome() {
        val pillBackground = createGlassPillBackgroundDrawable(focusAccentColor)
        listOf(Media3UiR.id.exo_center_controls, Media3UiR.id.exo_basic_controls).forEach { id ->
            findViewById<View>(id)?.background = pillBackground.constantState?.newDrawable()?.mutate()
        }
        val background = createFocusBackgroundDrawable(focusAccentColor)
        focusChromeControlIds().forEach { id ->
            findViewById<View>(id)?.background = background.constantState?.newDrawable()?.mutate()
        }
        applyProgressFocusChrome()
    }

    private fun createGlassPillBackgroundDrawable(accent: Int): Drawable {
        // Lighter overall glass body with a subtle accent shadow dropping from the top.
        val topSheenShadow = applyAlpha(accent, 0.34f)
        val upperGlass = applyAlpha(Color.WHITE, 0.24f)
        val lowerGlass = applyAlpha(Color.WHITE, 0.15f)
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topSheenShadow, upperGlass, lowerGlass)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setStroke(1, applyAlpha(Color.WHITE, 0.42f))
        }
    }

    private fun applyProgressFocusChrome() {
        val progress = findViewById<DefaultTimeBar>(Media3UiR.id.exo_progress) ?: return
        updateProgressScrubberStyle(progress, progress.hasFocus())
        progress.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            updateProgressScrubberStyle(progress, hasFocus)
        }
    }

    private fun updateProgressScrubberStyle(progress: DefaultTimeBar, focused: Boolean) {
        val focusedScrubber = applyAlpha(focusAccentColor, 0.95f)
        val unfocusedScrubber = Color.WHITE
        progress.setScrubberColor(if (focused) focusedScrubber else unfocusedScrubber)
        progress.invalidate()
    }

    private fun focusChromeControlIds(): List<Int> {
        return listOf(
            R.id.exo_back,
            Media3UiR.id.exo_prev,
            Media3UiR.id.exo_rew_with_amount,
            Media3UiR.id.exo_play_pause,
            Media3UiR.id.exo_ffwd_with_amount,
            Media3UiR.id.exo_next,
            R.id.exo_subtitle_timing,
            Media3UiR.id.exo_subtitle,
            R.id.exo_subtitle_download,
            R.id.exo_audio_track,
            R.id.exo_audio_boost,
            R.id.exo_resize_mode,
            Media3UiR.id.exo_settings,
            Media3UiR.id.exo_fullscreen,
            Media3UiR.id.exo_overflow_show
        )
    }

    private fun createFocusBackgroundDrawable(accent: Int): Drawable {
        fun shape(fill: Int, stroke: Int): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 999f
                setColor(fill)
                setStroke(2, stroke)
            }
        }
        val focused = shape(applyAlpha(accent, 0.42f), applyAlpha(accent, 0.95f))
        val pressed = shape(applyAlpha(accent, 0.56f), applyAlpha(accent, 1f))
        val normal = shape(Color.TRANSPARENT, Color.TRANSPARENT)
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(android.R.attr.state_selected), focused)
            addState(StateSet.WILD_CARD, normal)
        }
    }

    private fun applyAlpha(color: Int, alphaFraction: Float): Int {
        val alpha = (alphaFraction.coerceIn(0f, 1f) * 255f).toInt()
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun handleExplicitControllerNavigation(keyCode: Int): Boolean {
        val focusedId = findFocus()?.id ?: return false
        val laneOrderIds = transportControlOrderIds() + rightControlOrderIds()
        val laneOrderIndexById = laneOrderIds.withIndex().associate { (index, id) -> id to index }
        val transportControls = navigableControls(transportControlOrderIds())
        val rightControls = navigableControls(rightControlOrderIds())
        val bottomLane = transportControls + rightControls
        val focusedLaneIndex = bottomLane.indexOfFirst { it.id == focusedId }
        val focusedLaneOrderIndex = laneOrderIndexById[focusedId]
        val progress = findViewById<View>(Media3UiR.id.exo_progress)
        val progressVisible = isProgressNavigationEnabled(progress)
        val preferredTransport =
            transportControls.firstOrNull { it.id == Media3UiR.id.exo_play_pause }
                ?: transportControls.firstOrNull()
                ?: rightControls.firstOrNull()
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (focusedLaneIndex >= 0) {
                    moveFocusTo(bottomLane.getOrNull(focusedLaneIndex + 1))
                } else if (focusedLaneOrderIndex != null) {
                    moveFocusTo(
                        bottomLane.firstOrNull { view ->
                            val index = laneOrderIndexById[view.id] ?: Int.MAX_VALUE
                            index > focusedLaneOrderIndex
                        }
                    )
                } else {
                    false
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (focusedLaneIndex >= 0) {
                    moveFocusTo(bottomLane.getOrNull(focusedLaneIndex - 1))
                } else if (focusedLaneOrderIndex != null) {
                    moveFocusTo(
                        bottomLane.lastOrNull { view ->
                            val index = laneOrderIndexById[view.id] ?: Int.MIN_VALUE
                            index < focusedLaneOrderIndex
                        }
                    )
                } else {
                    false
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (focusedId == Media3UiR.id.exo_progress && progressVisible) {
                    moveFocusTo(preferredTransport)
                } else if (focusedId == (backButtonView?.id ?: R.id.exo_back)) {
                    if (progressVisible) {
                        moveFocusTo(progress)
                    } else {
                        moveFocusTo(preferredTransport)
                    }
                } else {
                    false
                }
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (focusedLaneIndex >= 0 || focusedLaneOrderIndex != null) {
                    if (progressVisible) {
                        moveFocusTo(progress)
                    } else {
                        moveFocusTo(backButtonView ?: findViewById(R.id.exo_back))
                    }
                } else if (focusedId == Media3UiR.id.exo_progress && progressVisible) {
                    moveFocusTo(backButtonView ?: findViewById(R.id.exo_back))
                } else {
                    false
                }
            }
            else -> false
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
                clearNowPlayingTickerLayoutListener()
                stopNowPlayingTickerAnimation()
                view.scrollTo(0, 0)
            } else {
                view.ellipsize = null
                view.isSingleLine = true
                view.setHorizontallyScrolling(true)
                startNowPlayingTickerWhenReady(view, text)
            }
        }
    }

    private fun bindBottomTimeViews() {
        positionTimeView = positionTimeView
            ?: findViewById<TextView>(Media3UiR.id.exo_position)
        durationTimeView = durationTimeView
            ?: findViewById<TextView>(Media3UiR.id.exo_duration)
        (findViewById<View>(Media3UiR.id.exo_controller) as? PlayerControlView)
            ?.setProgressUpdateListener(controllerProgressListener)
        updateBottomTimeLabels()
    }

    private fun updateBottomTimeLabels(positionMs: Long? = null) {
        if (isLiveContent) return
        val posView = positionTimeView ?: return
        val durView = durationTimeView ?: return
        val currentPlayer = player ?: return
        val durationMs = currentPlayer.duration
        if (durationMs <= 0L) return
        val resolvedPositionMs = (positionMs ?: currentPlayer.currentPosition).coerceIn(0L, durationMs)
        posView.text = formatTimeMsAlwaysHms(resolvedPositionMs)
        durView.text = formatTimeMsAlwaysHms(durationMs)
    }

    private fun clearNowPlayingTickerLayoutListener() {
        val view = nowPlayingInfoView ?: return
        val listener = nowPlayingTickerLayoutListener ?: return
        view.removeOnLayoutChangeListener(listener)
        nowPlayingTickerLayoutListener = null
    }

    private fun stopNowPlayingTickerAnimation() {
        nowPlayingTickerAnimator?.cancel()
        nowPlayingTickerAnimator = null
    }

    private fun startNowPlayingTickerWhenReady(view: TextView, rawText: String) {
        clearNowPlayingTickerLayoutListener()
        stopNowPlayingTickerAnimation()
        view.scrollTo(0, 0)

        fun startWhenSized(): Boolean {
            if (view.visibility != View.VISIBLE || !view.isShown) return false
            if (view.width <= 1 || view.height <= 1) return false
            val availableWidth = (view.width - view.paddingLeft - view.paddingRight).coerceAtLeast(0).toFloat()
            if (availableWidth <= 1f) return false
            val baseWidth = view.paint.measureText(rawText)
            if (baseWidth <= availableWidth) {
                view.text = rawText
                view.scrollTo(0, 0)
                return true
            }

            val repeatedText = rawText + nowPlayingTickerSeparator + rawText
            view.text = repeatedText
            val cycleDistance = view.paint.measureText(rawText + nowPlayingTickerSeparator)
            if (cycleDistance <= 1f) {
                view.scrollTo(0, 0)
                return true
            }

            val pixelsPerSecond =
                view.resources.displayMetrics.density * nowPlayingTickerSpeedDpPerSecond
            val durationMs =
                ((cycleDistance / pixelsPerSecond) * 1000f).toLong()
                    .coerceAtLeast(nowPlayingTickerMinDurationMs)

            nowPlayingTickerAnimator =
                ValueAnimator.ofFloat(0f, cycleDistance).apply {
                    interpolator = LinearInterpolator()
                    duration = durationMs
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener { animation ->
                        val scrollX = (animation.animatedValue as Float).roundToInt()
                        view.scrollTo(scrollX, 0)
                    }
                    start()
                }
            return true
        }

        if (startWhenSized()) return

        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (startWhenSized()) {
                clearNowPlayingTickerLayoutListener()
            }
        }
        nowPlayingTickerLayoutListener = listener
        view.addOnLayoutChangeListener(listener)
        view.post {
            if (startWhenSized()) {
                clearNowPlayingTickerLayoutListener()
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

private fun formatTimeMsAlwaysHms(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
}
