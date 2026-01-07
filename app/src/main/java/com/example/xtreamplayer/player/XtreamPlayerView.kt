package com.example.xtreamplayer.player

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
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
    var onResizeModeClick: (() -> Unit)? = null
        set(value) {
            field = value
            bindResizeModeView()
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
}
