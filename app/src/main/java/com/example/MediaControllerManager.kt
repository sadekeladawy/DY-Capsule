package com.example

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*

object MediaControllerManager {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var tickerJob: Job? = null
    
    private val scope = CoroutineScope(Dispatchers.Main)

    private val activeSessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaInfo()
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaInfo()
        }
    }

    fun init(context: Context) {
        mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            val component = ComponentName(context, CapsuleNotificationListener::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(
                activeSessionsChangedListener,
                component
            )
            updateActiveController(mediaSessionManager?.getActiveSessions(component))
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        activeController?.unregisterCallback(callback)
        mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        stopTicker()
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(callback)
        activeController = controllers?.firstOrNull { 
            it.playbackState?.state == PlaybackState.STATE_PLAYING 
        } ?: controllers?.firstOrNull()
        
        activeController?.registerCallback(callback)
        updateMediaInfo()
    }

    private fun updateMediaInfo() {
        val controller = activeController
        if (controller == null) {
            CapsuleStateManager.updateMediaInfo(MediaInfo(isPlaying = false))
            stopTicker()
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown"
        val bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState?.position ?: 0L

        if (bitmap != null) {
            Palette.from(bitmap).generate { palette ->
                val color = palette?.dominantSwatch?.rgb ?: palette?.vibrantSwatch?.rgb
                updateState(title, artist, bitmap, isPlaying, duration, position, color)
            }
        } else {
            updateState(title, artist, null, isPlaying, duration, position, null)
        }
        
        if (isPlaying) {
            startTicker()
        } else {
            stopTicker()
        }
    }

    private fun updateState(
        title: String, artist: String, bitmap: Bitmap?,
        isPlaying: Boolean, duration: Long, position: Long,
        color: Int?
    ) {
        CapsuleStateManager.updateMediaInfo(
            MediaInfo(
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                albumArt = bitmap,
                dominantColor = color,
                duration = duration,
                currentPosition = position
            )
        )
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                val controller = activeController
                val position = controller?.playbackState?.position ?: 0L
                val currentInfo = CapsuleStateManager.mediaInfo.value
                CapsuleStateManager.updateMediaInfo(currentInfo.copy(currentPosition = position))
                delay(1000)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun playPause() {
        val controller = activeController ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun next() {
        activeController?.transportControls?.skipToNext()
    }
    fun previous() {
        activeController?.transportControls?.skipToPrevious()
    }
    fun seekTo(position: Long) {
        activeController?.transportControls?.seekTo(position)
    }
}
