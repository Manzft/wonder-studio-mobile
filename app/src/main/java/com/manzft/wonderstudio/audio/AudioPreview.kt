package com.manzft.wonderstudio.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

// Reproductor simple para previsualizar wav/mp3 de los assets del proyecto.
class AudioPreview(private val context: Context) {

	private var player: MediaPlayer? = null

	fun play(uri: Uri?, volume: Float = 1f) {
		stop()
		if (uri == null) return
		try {
			player = MediaPlayer().apply {
				setDataSource(context, uri)
				setVolume(volume, volume)
				setOnPreparedListener { it.start() }
				setOnCompletionListener { stop() }
				prepareAsync()
			}
		} catch (error: Exception) {
			stop()
		}
	}

	fun stop() {
		player?.let {
			try {
				it.stop()
			} catch (_: Exception) {
			}
			it.release()
		}
		player = null
	}

	fun release() = stop()
}
