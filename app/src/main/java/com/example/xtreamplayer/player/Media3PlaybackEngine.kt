package com.example.xtreamplayer.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.xtreamplayer.settings.AudioLanguage
import com.example.xtreamplayer.settings.PlaybackQuality
import com.example.xtreamplayer.settings.SettingsState

class Media3PlaybackEngine(context: Context) : PlaybackEngine {
    private val appContext = context.applicationContext
    private val renderersFactory = DefaultRenderersFactory(appContext)
        .setEnableDecoderFallback(true)
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            MIN_BUFFER_MS,
            MAX_BUFFER_MS,
            BUFFER_FOR_PLAYBACK_MS,
            BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()
    val player: ExoPlayer = ExoPlayer.Builder(appContext, renderersFactory)
        .setLoadControl(loadControl)
        .build()
    private var currentMedia: Uri? = null

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(audioAttributes, true)
        player.setHandleAudioBecomingNoisy(true)
        player.volume = 1f
    }

    fun setMedia(uri: Uri) {
        if (currentMedia == uri) return
        currentMedia = uri
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    @OptIn(UnstableApi::class)
    override fun applySettings(settings: SettingsState) {
        player.repeatMode = if (settings.autoPlayNext) {
            Player.REPEAT_MODE_ALL
        } else {
            Player.REPEAT_MODE_OFF
        }

        val builder = player.trackSelectionParameters.buildUpon()
            .setPreferredAudioLanguage(languageCode(settings.audioLanguage))
            .setPreferredTextLanguage(
                if (settings.subtitlesEnabled) languageCode(settings.audioLanguage) else null
            )
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !settings.subtitlesEnabled)
            .setAudioOffloadPreferences(
                TrackSelectionParameters.AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(
                        TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                    )
                    .build()
            )

        val (maxWidth, maxHeight) = maxVideoSize(settings.playbackQuality)
        builder.setMaxVideoSize(maxWidth, maxHeight)

        player.trackSelectionParameters = builder.build()
    }

    fun release() {
        player.release()
    }

    private fun languageCode(language: AudioLanguage): String {
        return when (language) {
            AudioLanguage.ENGLISH -> "en"
            AudioLanguage.SPANISH -> "es"
            AudioLanguage.FRENCH -> "fr"
        }
    }

    private fun maxVideoSize(quality: PlaybackQuality): Pair<Int, Int> {
        return when (quality) {
            PlaybackQuality.AUTO -> Int.MAX_VALUE to Int.MAX_VALUE
            PlaybackQuality.UHD_4K -> 3840 to 2160
            PlaybackQuality.FHD_1080 -> 1920 to 1080
            PlaybackQuality.HD_720 -> 1280 to 720
        }
    }
}

private const val MIN_BUFFER_MS = 15_000
private const val MAX_BUFFER_MS = 60_000
private const val BUFFER_FOR_PLAYBACK_MS = 2_000
private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 4_000
