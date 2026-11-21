package com.freemusic.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("freemusic", Context.MODE_PRIVATE)

    var clientId by mutableStateOf(prefs.getString("client_id", "") ?: "")
        private set
    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var currentId by mutableStateOf<String?>(null)
        private set
    var nowPlaying by mutableStateOf<MediaMetadata?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var position by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(0L)
        private set

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync()
        }

        override fun onPlayerError(e: PlaybackException) {
            error = "Playback error: ${e.message ?: e.errorCodeName}"
        }
    }

    init {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                sync()
            } catch (e: Exception) {
                error = "Could not start the player: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun saveClientId(id: String) {
        val trimmed = id.trim()
        prefs.edit().putString("client_id", trimmed).apply()
        clientId = trimmed
        tracks = emptyList()
        error = null
    }

    fun clearClientId() = saveClientId("")

    fun search(query: String) {
        if (clientId.isBlank()) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { JamendoApi.fetchTracks(clientId, query) }
                tracks = result
                if (result.isEmpty()) error = "No playable tracks found"
            } catch (e: Exception) {
                error = "Could not load music: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    fun play(index: Int) {
        val c = controller
        if (c == null) {
            error = "Player is still starting, try again in a moment"
            return
        }
        c.setMediaItems(tracks.map { it.toMediaItem() }, index, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
    }

    /** Called about twice a second from the UI to move the progress slider. */
    fun tick() {
        val c = controller ?: return
        position = c.currentPosition.coerceAtLeast(0L)
        duration = c.duration.coerceAtLeast(0L)
    }

    private fun sync() {
        val c = controller ?: return
        isPlaying = c.isPlaying
        currentId = c.currentMediaItem?.mediaId
        nowPlaying = c.currentMediaItem?.mediaMetadata
    }

    override fun onCleared() {
        controller?.let {
            it.removeListener(listener)
            it.release()
        }
        controller = null
        super.onCleared()
    }

    private fun Track.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(if (image.isBlank()) null else Uri.parse(image))
                    .build()
            )
            .build()
}
