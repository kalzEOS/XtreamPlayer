package com.example.xtreamplayer.player

import android.content.Context
import android.net.Uri
import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
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
import com.example.xtreamplayer.settings.SettingsState
import timber.log.Timber

@OptIn(UnstableApi::class)
class Media3PlaybackEngine(context: Context) : PlaybackEngine {
    private val appContext = context.applicationContext
    private val renderersFactory = DefaultRenderersFactory(appContext)
        .setEnableDecoderFallback(true)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
    private var bufferProfile: BufferProfile = BufferProfile.VOD
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
    private val playerListener =
        object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                updateLoudnessEnhancer(audioSessionId)
            }
        }
    var player: ExoPlayer = buildPlayer(buildLoadControl(bufferProfile))
    private var currentMedia: Uri? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var loudnessSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var boostDb: Float = 0f
    private var lastSettings: SettingsState = SettingsState()
    private var subtitleTrackSelectionListener: Player.Listener? = null

    init {
        updateLoudnessEnhancer(player.audioSessionId)
    }

    fun setMedia(uri: Uri) {
        if (currentMedia == uri) return
        currentMedia = uri
        val mediaItem =
            MediaItem.Builder()
                .setUri(uri)
                .setMimeType(guessMimeType(uri))
                .build()
        player.setMediaItem(mediaItem)
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
                .setMimeType(guessMimeType(item.uri))
                .build()
        }
        val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex)
        player.setMediaItems(mediaItems, safeIndex, 0L)
        player.prepare()
    }

    @OptIn(UnstableApi::class)
    override fun applySettings(settings: SettingsState) {
        lastSettings = settings
        // Auto-play is handled manually in UI for series episodes only
        player.repeatMode = Player.REPEAT_MODE_OFF

        val builder = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !settings.subtitlesEnabled)
            .setAudioOffloadPreferences(
                TrackSelectionParameters.AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(
                        TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                    )
                    .build()
            )

        player.trackSelectionParameters = builder.build()
    }

    fun release() {
        clearSubtitleTrackSelectionListener(player)
        player.removeListener(playerListener)
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        player.release()
    }

    fun reset() {
        val oldPlayer = player
        clearSubtitleTrackSelectionListener(oldPlayer)
        oldPlayer.removeListener(playerListener)
        oldPlayer.release()
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        loudnessSessionId = C.AUDIO_SESSION_ID_UNSET
        currentMedia = null
        player = buildPlayer(buildLoadControl(bufferProfile))
        updateLoudnessEnhancer(player.audioSessionId)
        applySettings(lastSettings)
        setAudioBoostDb(boostDb)
    }

    fun setBufferProfile(profile: BufferProfile) {
        if (bufferProfile == profile) return
        bufferProfile = profile
        rebuildPlayerPreservingState()
    }

    fun getAudioBoostDb(): Float = boostDb

    fun setAudioBoostDb(db: Float) {
        boostDb = db.coerceIn(0f, MAX_BOOST_DB)
        val targetGainMb = (boostDb * 100).toInt()
        val enhancer = loudnessEnhancer
        if (enhancer != null) {
            val applyResult = runCatching {
                enhancer.setTargetGain(targetGainMb)
                enhancer.enabled = boostDb > 0f
            }
            if (applyResult.isFailure) {
                Timber.w(
                    applyResult.exceptionOrNull(),
                    "Failed to apply loudness boost; resetting enhancer"
                )
                runCatching { enhancer.release() }
                loudnessEnhancer = null
                loudnessSessionId = C.AUDIO_SESSION_ID_UNSET
            }
        }
    }

    private fun updateLoudnessEnhancer(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == loudnessSessionId) {
            return
        }
        loudnessSessionId = sessionId
        loudnessEnhancer?.release()
        loudnessEnhancer =
            runCatching {
                LoudnessEnhancer(sessionId).apply {
                    enabled = boostDb > 0f
                    setTargetGain((boostDb * 100).toInt())
                }
            }.getOrNull()
    }

    companion object {
        private const val MAX_BOOST_DB = 12f
        private const val MAX_TRACK_CHANGES_WITHOUT_SUBTITLE_SELECTION = 6
    }

    private fun clearSubtitleTrackSelectionListener(targetPlayer: Player) {
        subtitleTrackSelectionListener?.let { listener ->
            runCatching { targetPlayer.removeListener(listener) }
            subtitleTrackSelectionListener = null
        }
    }

    private fun buildPlayer(loadControl: DefaultLoadControl): ExoPlayer {
        return ExoPlayer.Builder(appContext, renderersFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(SEEK_BACK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_FORWARD_INCREMENT_MS)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                setHandleAudioBecomingNoisy(true)
                volume = 1f
                addListener(playerListener)
            }
    }

    private fun rebuildPlayerPreservingState() {
        val oldPlayer = player
        val mediaItems =
            if (oldPlayer.mediaItemCount > 0) {
                (0 until oldPlayer.mediaItemCount).map { index ->
                    oldPlayer.getMediaItemAt(index)
                }
            } else {
                emptyList()
            }
        val currentIndex = oldPlayer.currentMediaItemIndex
        val currentPosition = oldPlayer.currentPosition
        val playWhenReady = oldPlayer.playWhenReady
        val trackParameters = oldPlayer.trackSelectionParameters

        clearSubtitleTrackSelectionListener(oldPlayer)
        oldPlayer.removeListener(playerListener)
        oldPlayer.release()
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        loudnessSessionId = C.AUDIO_SESSION_ID_UNSET

        player = buildPlayer(buildLoadControl(bufferProfile))
        player.trackSelectionParameters = trackParameters
        updateLoudnessEnhancer(player.audioSessionId)
        setAudioBoostDb(boostDb)

        if (mediaItems.isNotEmpty()) {
            val safeIndex = currentIndex.coerceAtLeast(0)
            player.setMediaItems(mediaItems, safeIndex, currentPosition)
            player.prepare()
            player.playWhenReady = playWhenReady
        }
    }

    private fun guessMimeType(uri: Uri): String? {
        val candidate = uri.toString().lowercase()
        return if (candidate.contains(".m3u8")) {
            MimeTypes.APPLICATION_M3U8
        } else {
            null
        }
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
                    val label = buildAudioTrackLabel(format, languageName)
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

    fun isVideoOverrideActive(): Boolean {
        val overrides = player.trackSelectionParameters.overrides
        if (overrides.isEmpty()) return false
        return player.currentTracks.groups.any { group ->
            group.type == C.TRACK_TYPE_VIDEO && overrides.containsKey(group.mediaTrackGroup)
        }
    }

    @OptIn(UnstableApi::class)
    fun getAvailableSubtitleTracks(): List<SubtitleTrackInfo> {
        val tracks = mutableListOf<SubtitleTrackInfo>()
        val currentTracks = player.currentTracks

        currentTracks.groups.forEachIndexed { groupIndex, trackGroup ->
            if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val languageCode = format.language ?: "und"
                    val languageName = getLanguageName(languageCode)
                    val isForced =
                        (format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0 ||
                            format.label?.contains("forced", ignoreCase = true) == true
                    val label = buildSubtitleTrackLabel(format, languageName, isForced)
                    val isSelected = trackGroup.isTrackSelected(i)
                    val isSupported = trackGroup.isTrackSupported(i)
                    tracks.add(
                        SubtitleTrackInfo(
                            groupIndex = groupIndex,
                            trackIndex = i,
                            label = label,
                            language = languageName,
                            isSelected = isSelected,
                            isSupported = isSupported,
                            isForced = isForced
                        )
                    )
                }
            }
        }
        return tracks
    }

    @OptIn(UnstableApi::class)
    fun getAvailableVideoTracks(): List<VideoTrackInfo> {
        val tracks = mutableListOf<VideoTrackInfo>()
        val currentTracks = player.currentTracks

        currentTracks.groups.forEachIndexed { groupIndex, trackGroup ->
            if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val label = buildVideoTrackLabel(format)
                    val isSelected = trackGroup.isTrackSelected(i)
                    val isSupported = trackGroup.isTrackSupported(i)
                    tracks.add(
                        VideoTrackInfo(
                            groupIndex,
                            i,
                            label,
                            format.width,
                            format.height,
                            format.bitrate,
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
    fun selectVideoTrack(groupIndex: Int, trackIndex: Int) {
        val currentTracks = player.currentTracks
        if (groupIndex >= currentTracks.groups.size) return

        val trackGroup = currentTracks.groups[groupIndex]
        if (trackGroup.type != C.TRACK_TYPE_VIDEO) return
        if (trackIndex < 0 || trackIndex >= trackGroup.length) return
        if (!trackGroup.isTrackSupported(trackIndex)) return

        val wasPlaying = player.isPlaying
        val builder = player.trackSelectionParameters.buildUpon()
        builder.setOverrideForType(
            TrackSelectionOverride(
                trackGroup.mediaTrackGroup,
                listOf(trackIndex)
            )
        )
        player.trackSelectionParameters = builder.build()

        if (wasPlaying && !player.isPlaying) {
            player.play()
        }
    }

    @OptIn(UnstableApi::class)
    fun selectSubtitleTrack(groupIndex: Int, trackIndex: Int) {
        val currentTracks = player.currentTracks
        if (groupIndex >= currentTracks.groups.size) return

        val trackGroup = currentTracks.groups[groupIndex]
        if (trackGroup.type != C.TRACK_TYPE_TEXT) return
        if (trackIndex < 0 || trackIndex >= trackGroup.length) return
        if (!trackGroup.isTrackSupported(trackIndex)) return

        val wasPlaying = player.isPlaying
        val builder = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(
                TrackSelectionOverride(
                    trackGroup.mediaTrackGroup,
                    listOf(trackIndex)
                )
            )
        player.trackSelectionParameters = builder.build()

        if (wasPlaying && !player.isPlaying) {
            player.play()
        }
    }

    @OptIn(UnstableApi::class)
    fun addSubtitle(
        subtitleUri: Uri,
        language: String = "en",
        label: String = "Downloaded",
        mimeType: String = MimeTypes.APPLICATION_SUBRIP
    ) {
        val currentItem = player.currentMediaItem ?: return
        clearSubtitleTrackSelectionListener(player)

        Timber.d("Adding subtitle: uri=$subtitleUri, language=$language, mimeType=$mimeType")

        // Verify file exists if it's a file URI
        if (subtitleUri.scheme == "file") {
            val file = java.io.File(subtitleUri.path ?: "")
            if (file.exists()) {
                Timber.d("Subtitle file exists: ${file.absolutePath}, size=${file.length()} bytes")
            } else {
                Timber.e("Subtitle file does not exist: ${file.absolutePath}")
                return
            }
        }

        val subtitleConfig = SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(mimeType)
            .setLanguage(language)
            .setLabel(label)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val newItem = currentItem.buildUpon()
            .setSubtitleConfigurations(listOf(subtitleConfig))
            .build()

        // Verify subtitle was added to MediaItem
        val subtitleConfigs = newItem.localConfiguration?.subtitleConfigurations ?: emptyList()
        Timber.d("New MediaItem subtitle configs: ${subtitleConfigs.size}")
        subtitleConfigs.forEach { config ->
            Timber.d("  Subtitle config: uri=${config.uri}, mimeType=${config.mimeType}, lang=${config.language}")
        }

        // Enable text tracks BEFORE replacing media item
        val builder = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setPreferredTextLanguage(language)
        player.trackSelectionParameters = builder.build()

        fun selectTextTrack(tracks: androidx.media3.common.Tracks): Boolean {
            Timber.d("Checking text tracks (groupCount=${tracks.groups.size})")
            data class Candidate(
                val groupIndex: Int,
                val trackIndex: Int,
                val trackGroup: androidx.media3.common.Tracks.Group,
                val format: Format,
                val score: Int
            )

            val desiredLanguage = language.trim().lowercase()
            val desiredLanguageBase = desiredLanguage.substringBefore('-')
            val desiredLabel = label.trim().lowercase()
            var fallback: Candidate? = null
            var best: Candidate? = null

            tracks.groups.forEachIndexed { groupIndex, trackGroup ->
                if (trackGroup.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
                Timber.d(
                    "Found text track group: groupIndex=$groupIndex, length=${trackGroup.length}"
                )
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val supported = trackGroup.isTrackSupported(i)
                    val selected = trackGroup.isTrackSelected(i)
                    val trackLabel = format.label?.trim().orEmpty()
                    val trackLabelLower = trackLabel.lowercase()
                    val trackLanguage = format.language?.trim().orEmpty().lowercase()
                    val trackLanguageBase = trackLanguage.substringBefore('-')
                    val isForced =
                        (format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0 ||
                            trackLabelLower.contains("forced")
                    Timber.d(
                        "  Track $i: supported=$supported, selected=$selected, label=${format.label}, lang=${format.language}, mimeType=${format.sampleMimeType}, forced=$isForced"
                    )
                    if (!supported) continue

                    var score = 0
                    if (desiredLabel.isNotBlank()) {
                        if (trackLabelLower == desiredLabel) {
                            score += 1000
                        } else if (trackLabelLower.contains(desiredLabel)) {
                            score += 800
                        }
                    }
                    if (desiredLanguage.isNotBlank()) {
                        if (trackLanguage == desiredLanguage) {
                            score += 500
                        } else if (
                            desiredLanguageBase.isNotBlank() &&
                                trackLanguageBase == desiredLanguageBase
                        ) {
                            score += 350
                        }
                    }
                    if (isForced) {
                        score -= 500
                    } else {
                        score += 120
                    }

                    val candidate = Candidate(groupIndex, i, trackGroup, format, score)
                    if (fallback == null) {
                        fallback = candidate
                    }
                    if (best == null || candidate.score > best!!.score) {
                        best = candidate
                    }
                }
            }

            val selectedCandidate = best ?: fallback
            if (selectedCandidate == null) {
                Timber.d("No supported text tracks found in ${tracks.groups.size} total groups")
                return false
            }

            Timber.d(
                "Selecting text track: groupIndex=${selectedCandidate.groupIndex}, trackIndex=${selectedCandidate.trackIndex}, score=${selectedCandidate.score}, label=${selectedCandidate.format.label}, lang=${selectedCandidate.format.language}, mimeType=${selectedCandidate.format.sampleMimeType}"
            )
            val newBuilder = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(language)
                .setOverrideForType(
                    TrackSelectionOverride(
                        selectedCandidate.trackGroup.mediaTrackGroup,
                        listOf(selectedCandidate.trackIndex)
                    )
                )
            player.trackSelectionParameters = newBuilder.build()
            Timber.d("Text track selected successfully")
            return true
        }

        // Register listener before replacing to avoid missing an early tracks update.
        var trackChangesWithoutSelection = 0
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                Timber.d("onTracksChanged: ${tracks.groups.size} groups")
                if (selectTextTrack(tracks)) {
                    // Verify selection
                    val currentTracks = player.currentTracks
                    val textSelected = currentTracks.groups.any { group ->
                        group.type == C.TRACK_TYPE_TEXT && (0 until group.length).any { group.isTrackSelected(it) }
                    }
                    Timber.d("After selection - text track selected: $textSelected")
                    clearSubtitleTrackSelectionListener(player)
                    return
                }
                trackChangesWithoutSelection++
                if (trackChangesWithoutSelection >= MAX_TRACK_CHANGES_WITHOUT_SUBTITLE_SELECTION) {
                    Timber.w(
                        "Subtitle track selection timed out after $trackChangesWithoutSelection track updates"
                    )
                    clearSubtitleTrackSelectionListener(player)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Timber.e(error, "Player error during subtitle loading")
                clearSubtitleTrackSelectionListener(player)
            }
        }
        subtitleTrackSelectionListener = listener
        player.addListener(listener)

        replaceMediaItemPreservingState(newItem)

        // Verify the replacement
        val currentMediaItem = player.currentMediaItem
        val currentSubConfigs = currentMediaItem?.localConfiguration?.subtitleConfigurations ?: emptyList()
        Timber.d("After replacement - current MediaItem subtitle configs: ${currentSubConfigs.size}")
        if (currentSubConfigs.isEmpty()) {
            Timber.e("WARNING: Subtitle configuration was NOT preserved after replacement!")
        }

        // Try immediately in case tracks are already available.
        if (selectTextTrack(player.currentTracks)) {
            clearSubtitleTrackSelectionListener(player)
        }
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
        player.setMediaItem(item, currentPosition.coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = wasPlaying
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

    private fun buildAudioTrackLabel(format: Format, languageName: String): String {
        val parts = mutableListOf<String>()
        val baseLabel =
            format.label?.takeIf { it.isNotBlank() }
                ?: languageName.takeIf { it != "Unknown" }
                ?: "Audio"
        parts.add(baseLabel)

        val channels = formatChannelCount(format.channelCount)
        if (channels != null) {
            parts.add(channels)
        }

        val codec = formatAudioCodec(format)
        if (codec != null) {
            parts.add(codec)
        }

        return parts.joinToString(" \u2022 ")
    }

    private fun buildSubtitleTrackLabel(
        format: Format,
        languageName: String,
        isForced: Boolean
    ): String {
        val parts = mutableListOf<String>()
        val baseLabel =
            format.label?.takeIf { it.isNotBlank() }
                ?: languageName.takeIf { it != "Unknown" }
                ?: "Subtitle"
        parts.add(baseLabel)

        if (isForced) {
            parts.add("Forced")
        } else if ((format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0) {
            parts.add("Default")
        }

        return parts.joinToString(" \u2022 ")
    }

    private fun formatChannelCount(channelCount: Int): String? {
        if (channelCount <= 0) return null
        return when (channelCount) {
            1 -> "1.0"
            2 -> "2.0"
            3 -> "2.1"
            4 -> "4.0"
            5 -> "5.0"
            6 -> "5.1"
            7 -> "6.1"
            8 -> "7.1"
            else -> "${channelCount}.0"
        }
    }

    private fun formatAudioCodec(format: Format): String? {
        val codec = format.codecs ?: format.sampleMimeType ?: return null
        val normalized = codec.lowercase()
        return when {
            normalized.contains("ec-3") || normalized.contains("eac3") -> "E-AC3"
            normalized.contains("ac-3") || normalized.contains("ac3") -> "AC-3"
            normalized.contains("truehd") -> "TrueHD"
            normalized.contains("dts") -> "DTS"
            normalized.contains("flac") -> "FLAC"
            normalized.contains("opus") -> "Opus"
            normalized.contains("mp4a") || normalized.contains("aac") -> "AAC"
            normalized.contains("mp3") -> "MP3"
            normalized.contains("vorbis") -> "Vorbis"
            else -> format.sampleMimeType?.uppercase()
        }
    }

    private fun buildVideoTrackLabel(format: Format): String {
        val parts = mutableListOf<String>()
        val height = format.height
        val width = format.width
        val resolution =
            when {
                height > 0 -> "${height}p"
                width > 0 -> "${width}w"
                else -> "Unknown"
            }
        parts.add(resolution)
        if (width > 0 && height > 0) {
            parts.add("${width}x${height}")
        }
        if (format.bitrate > 0) {
            parts.add("${format.bitrate / 1000} kbps")
        }
        return parts.joinToString(" \u2022 ")
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

data class VideoTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val isSelected: Boolean,
    val isSupported: Boolean
)

data class SubtitleTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean,
    val isSupported: Boolean,
    val isForced: Boolean
)

enum class BufferProfile(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int
) {
    LIVE(
        minBufferMs = 5_000,
        maxBufferMs = 25_000,
        bufferForPlaybackMs = 1_000,
        bufferForPlaybackAfterRebufferMs = 2_000
    ),
    VOD(
        minBufferMs = 25_000,
        maxBufferMs = 120_000,
        bufferForPlaybackMs = 2_500,
        bufferForPlaybackAfterRebufferMs = 5_000
    )
}

private const val SEEK_BACK_INCREMENT_MS = 15_000L
private const val SEEK_FORWARD_INCREMENT_MS = 30_000L

private fun buildLoadControl(profile: BufferProfile): DefaultLoadControl {
    return DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            profile.minBufferMs,
            profile.maxBufferMs,
            profile.bufferForPlaybackMs,
            profile.bufferForPlaybackAfterRebufferMs
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()
}
