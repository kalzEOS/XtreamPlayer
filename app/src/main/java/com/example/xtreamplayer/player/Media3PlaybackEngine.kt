package com.example.xtreamplayer.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.xtreamplayer.PlaybackQueueItem
import com.example.xtreamplayer.settings.AudioLanguage
import com.example.xtreamplayer.settings.PlaybackQuality
import com.example.xtreamplayer.settings.SettingsState

class Media3PlaybackEngine(context: Context) : PlaybackEngine {
    private val appContext = context.applicationContext
    private val renderersFactory = DefaultRenderersFactory(appContext)
        .setEnableDecoderFallback(true)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
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

    fun setQueue(items: List<PlaybackQueueItem>, startIndex: Int) {
        if (items.isEmpty()) return
        val mediaItems = items.map { item ->
            MediaItem.Builder()
                .setUri(item.uri)
                .setMediaId(item.mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .build()
                )
                .build()
        }
        val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex)
        player.setMediaItems(mediaItems, safeIndex, 0L)
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

    @OptIn(UnstableApi::class)
    fun getAvailableAudioTracks(): List<AudioTrackInfo> {
        val tracks = mutableListOf<AudioTrackInfo>()
        val currentTracks = player.currentTracks

        currentTracks.groups.forEachIndexed { groupIndex, trackGroup ->
            if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val languageCode = format.language ?: "und"
                    val languageName = getLanguageName(languageCode)
                    val label = format.label ?: languageName
                    val isSelected = trackGroup.isTrackSelected(i)
                    val isSupported = trackGroup.isTrackSupported(i)
                    tracks.add(
                        AudioTrackInfo(
                            groupIndex,
                            i,
                            label,
                            languageName,
                            isSelected,
                            isSupported
                        )
                    )
                }
            }
        }
        return tracks
    }

    @OptIn(UnstableApi::class)
    fun selectAudioTrack(groupIndex: Int, trackIndex: Int) {
        val currentTracks = player.currentTracks
        if (groupIndex >= currentTracks.groups.size) return

        val trackGroup = currentTracks.groups[groupIndex]
        if (trackGroup.type != C.TRACK_TYPE_AUDIO) return
        if (trackIndex < 0 || trackIndex >= trackGroup.length) return
        if (!trackGroup.isTrackSupported(trackIndex)) return

        // Preserve playback state
        val wasPlaying = player.isPlaying

        val builder = player.trackSelectionParameters.buildUpon()
        builder.setOverrideForType(
            TrackSelectionOverride(
                trackGroup.mediaTrackGroup,
                listOf(trackIndex)
            )
        )
        player.trackSelectionParameters = builder.build()

        // Restore playback state
        if (wasPlaying && !player.isPlaying) {
            player.play()
        }
    }

    @OptIn(UnstableApi::class)
    fun addSubtitle(subtitleUri: Uri, language: String = "en", label: String = "Downloaded") {
        val currentItem = player.currentMediaItem ?: return

        val subtitleConfig = SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage(language)
            .setLabel(label)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val newItem = currentItem.buildUpon()
            .setSubtitleConfigurations(listOf(subtitleConfig))
            .build()

        replaceMediaItemPreservingState(newItem)

        val builder = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setPreferredTextLanguage(language)
        player.trackSelectionParameters = builder.build()
    }

    @OptIn(UnstableApi::class)
    fun setSubtitlesEnabled(enabled: Boolean) {
        val builder = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
        if (!enabled) {
            builder.setPreferredTextLanguage(null)
        }
        player.trackSelectionParameters = builder.build()
    }

    fun refreshMediaItem() {
        val currentItem = player.currentMediaItem ?: return
        replaceMediaItemPreservingState(currentItem)
    }

    @OptIn(UnstableApi::class)
    fun clearExternalSubtitles() {
        val currentItem = player.currentMediaItem ?: return
        val hasSubtitles = currentItem.localConfiguration
            ?.subtitleConfigurations
            ?.isNotEmpty() == true

        if (hasSubtitles) {
            val newItem = currentItem.buildUpon()
                .setSubtitleConfigurations(emptyList())
                .build()
            replaceMediaItemPreservingState(newItem)
        }

        setSubtitlesEnabled(false)
    }

    private fun replaceMediaItemPreservingState(item: MediaItem) {
        val currentPosition = player.currentPosition
        val wasPlaying = player.isPlaying
        player.setMediaItem(item)
        player.prepare()
        player.seekTo(currentPosition)
        if (wasPlaying) {
            player.play()
        }
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

    private fun getLanguageName(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "en", "eng" -> "English"
            "es", "spa" -> "Spanish"
            "fr", "fra", "fre" -> "French"
            "de", "deu", "ger" -> "German"
            "it", "ita" -> "Italian"
            "pt", "por" -> "Portuguese"
            "ru", "rus" -> "Russian"
            "ja", "jpn" -> "Japanese"
            "zh", "chi", "zho" -> "Chinese"
            "ko", "kor" -> "Korean"
            "ar", "ara" -> "Arabic"
            "hi", "hin" -> "Hindi"
            "tr", "tur" -> "Turkish"
            "nl", "nld", "dut" -> "Dutch"
            "pl", "pol" -> "Polish"
            "sv", "swe" -> "Swedish"
            "no", "nor" -> "Norwegian"
            "da", "dan" -> "Danish"
            "fi", "fin" -> "Finnish"
            "cs", "ces", "cze" -> "Czech"
            "el", "ell", "gre" -> "Greek"
            "he", "heb" -> "Hebrew"
            "th", "tha" -> "Thai"
            "vi", "vie" -> "Vietnamese"
            "id", "ind" -> "Indonesian"
            "ms", "msa", "may" -> "Malay"
            "ro", "ron", "rum" -> "Romanian"
            "hu", "hun" -> "Hungarian"
            "uk", "ukr" -> "Ukrainian"
            "bg", "bul" -> "Bulgarian"
            "hr", "hrv" -> "Croatian"
            "sr", "srp" -> "Serbian"
            "sk", "slk", "slo" -> "Slovak"
            "ca", "cat" -> "Catalan"
            "fa", "fas", "per" -> "Persian"
            "und" -> "Unknown"
            else -> languageCode.uppercase()
        }
    }
}

data class AudioTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean,
    val isSupported: Boolean
)

private const val MIN_BUFFER_MS = 15_000
private const val MAX_BUFFER_MS = 60_000
private const val BUFFER_FOR_PLAYBACK_MS = 2_000
private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 4_000
