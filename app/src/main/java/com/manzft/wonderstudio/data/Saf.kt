package com.manzft.wonderstudio.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

// Utilidades sobre SAF (DocumentFile): el usuario elige carpetas con el selector
// del sistema y leemos/escribimos ahí.
object Saf {

	fun child(parent: DocumentFile, name: String): DocumentFile? =
		parent.listFiles().firstOrNull { it.name == name }

	fun ensureDir(parent: DocumentFile, name: String): DocumentFile? {
		child(parent, name)?.let { if (it.isDirectory) return it }
		return parent.createDirectory(name)
	}

	fun ensureDirs(parent: DocumentFile, path: String): DocumentFile? {
		var dir: DocumentFile = parent
		for (part in path.split('/').filter { it.isNotEmpty() }) {
			dir = ensureDir(dir, part) ?: return null
		}
		return dir
	}

	fun writeText(context: Context, file: DocumentFile, text: String) {
		context.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(text.toByteArray()) }
	}

	fun readText(context: Context, file: DocumentFile): String? =
		context.contentResolver.openInputStream(file.uri)?.use { it.readBytes().decodeToString() }

	fun createOrReplace(context: Context, dir: DocumentFile, name: String, text: String): DocumentFile? {
		child(dir, name)?.delete()
		val file = dir.createFile("application/json", name) ?: return null
		writeText(context, file, text)
		return file
	}

	fun createOrReplaceBinary(context: Context, dir: DocumentFile, name: String, mime: String, source: Uri): DocumentFile? {
		child(dir, name)?.delete()
		val file = dir.createFile(mime, name) ?: return null
		context.contentResolver.openInputStream(source)?.use { input ->
			context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
				input.copyTo(output)
			}
		}
		return file
	}

	fun deleteRecursive(file: DocumentFile) {
		if (file.isDirectory) {
			for (child in file.listFiles()) deleteRecursive(child)
		}
		file.delete()
	}

	fun ensureEmptyDir(context: Context, root: DocumentFile, name: String): DocumentFile? {
		child(root, name)?.let { deleteRecursive(it) }
		return root.createDirectory(name)
	}

	fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
		"png" -> "image/png"
		"jpg", "jpeg" -> "image/jpeg"
		"webp" -> "image/webp"
		"svg" -> "image/svg+xml"
		"wav" -> "audio/wav"
		"mp3" -> "audio/mpeg"
		"ogg" -> "audio/ogg"
		"json" -> "application/json"
		else -> "application/octet-stream"
	}
}
