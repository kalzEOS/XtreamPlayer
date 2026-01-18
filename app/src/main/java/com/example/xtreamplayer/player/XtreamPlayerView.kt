package com.example.xtreamplayer.player

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
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
    var forcedAspectRatio: Float? = null
        set(value) {
            field = value
            applyAspectRatio()
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
}
