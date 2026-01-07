package com.example.xtreamplayer.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.xtreamplayer.settings.AudioLanguage
import com.example.xtreamplayer.settings.PlaybackQuality
import com.example.xtreamplayer.settings.SettingsState

class Media3PlaybackEngine(context: Context) : PlaybackEngine {
    private val appContext = context.applicationContext
    val player: ExoPlayer = ExoPlayer.Builder(appContext).build()
    private var currentMedia: Uri? = null

    fun setMedia(uri: Uri) {
        if (currentMedia == uri) return
        currentMedia = uri
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    override fun applySettings(settings: SettingsState) {
        player.repeatMode = if (settings.autoPlayNext) {
            Player.REPEAT_MODE_ALL
        } else {
            Player.REPEAT_MODE_OFF
        }
        player.playWhenReady = settings.autoPlayNext

        val builder = player.trackSelectionParameters.buildUpon()
            .setPreferredAudioLanguage(languageCode(settings.audioLanguage))
            .setPreferredTextLanguage(
                if (settings.subtitlesEnabled) languageCode(settings.audioLanguage) else null
            )
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !settings.subtitlesEnabled)

        val (maxWidth, maxHeight) = maxVideoSize(settings.playbackQuality)
        builder.setMaxVideoSize(maxWidth, maxHeight)

        val maxBitrate = if (settings.dataSaverEnabled) 2_000_000 else Int.MAX_VALUE
        builder.setMaxVideoBitrate(maxBitrate)

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
