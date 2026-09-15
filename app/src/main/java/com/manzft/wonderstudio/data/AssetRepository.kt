package com.manzft.wonderstudio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache

// Carga y cachea bitmaps de los assets del proyecto. Decodifica con downsample
// para no llenar memoria con texturas grandes y cachea por ruta relativa.
class AssetRepository(private val context: Context, private val repository: ProjectRepository) {

	private val cache = object : LruCache<String, Bitmap>(cacheSizeBytes()) {
		override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
	}

	private val missing: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

	fun bitmap(relativePath: String): Bitmap? {
		if (relativePath.isBlank()) return null
		cache.get(relativePath)?.let { return it }
		if (relativePath in missing) return null
		val uri = repository.findAsset(relativePath)
		if (uri == null) {
			missing.add(relativePath)
			return null
		}
		val bitmap = decode(uri)
		if (bitmap == null) {
			missing.add(relativePath)
			return null
		}
		cache.put(relativePath, bitmap)
		return bitmap
	}

	fun bitmap(uri: Uri?): Bitmap? {
		if (uri == null) return null
		val key = uri.toString()
		cache.get(key)?.let { return it }
		val bitmap = decode(uri) ?: return null
		cache.put(key, bitmap)
		return bitmap
	}

	private fun decode(uri: Uri): Bitmap? {
		return try {
			val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
			context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
			options.inSampleSize = sampleSize(options.outWidth, options.outHeight)
			options.inJustDecodeBounds = false
			options.inPreferredConfig = Bitmap.Config.ARGB_8888
			context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
		} catch (error: Exception) {
			null
		}
	}

	private fun sampleSize(width: Int, height: Int): Int {
		var sample = 1
		while (width / sample > MAX_TEXTURE_SIZE || height / sample > MAX_TEXTURE_SIZE) {
			sample *= 2
		}
		return sample
	}

	private fun cacheSizeBytes(): Int {
		val maxMemory = Runtime.getRuntime().maxMemory()
		return (maxMemory / 8).toInt()
	}

	companion object {
		private const val MAX_TEXTURE_SIZE = 2048
	}
}
