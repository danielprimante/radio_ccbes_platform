package com.radio.ccbes.data.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.radio.ccbes.MainActivity
import com.radio.ccbes.R
import com.radio.ccbes.data.repository.ConfigRepository
import com.radio.ccbes.data.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RadioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settingsManager: SettingsManager
    private var isNotificationEnabled = true
    private lateinit var player: ExoPlayer

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)

        serviceScope.launch {
            settingsManager.mediaNotificationEnabled.collect { enabled ->
                isNotificationEnabled = enabled
                if (!enabled) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // true = handle audio focus automatically
            .setHandleAudioBecomingNoisy(true) // handle headphone disconnection
            .build()

        val metadata = MediaMetadata.Builder()
            .setTitle(getString(R.string.radio_ccbes))
            .setArtist(getString(R.string.radio_tagline))
            .setDisplayTitle(getString(R.string.radio_live))
            .build()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()

        serviceScope.launch {
            val configRepository = ConfigRepository()
            val streamUrl = configRepository.getStreamUrl()

            val mediaItem = MediaItem.Builder()
                .setMediaId("radio_id")
                .setUri(streamUrl)
                .setMediaMetadata(metadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()

            if (settingsManager.autoPlayRadio.first()) {
                player.play()
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.playWhenReady == false) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    @OptIn(UnstableApi::class)
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (isNotificationEnabled) {
            super.onUpdateNotification(session, startInForegroundRequired)
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }
}
