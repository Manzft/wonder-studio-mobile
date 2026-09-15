package com.manzft.wonderstudio.data

import android.content.Context
import android.content.Intent
import android.net.Uri

// Lanza la app de Wonder Maker (Android) pasándole el proyecto del estudio y,
// si hay, el mod importado. El juego los recibe como URIs de SAF.
object RunLauncher {

	const val EXTRA_PROJECT_URI = "studio_project_uri"
	const val EXTRA_MOD_URI = "studio_mod_uri"

	fun buildIntent(packageName: String, projectUri: Uri?, modUri: Uri?): Intent? {
		if (projectUri == null) return null
		val intent = Intent(Intent.ACTION_VIEW).apply {
			setPackage(packageName)
			setDataAndType(projectUri, "resource/folder")
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			putExtra(EXTRA_PROJECT_URI, projectUri.toString())
			if (modUri != null) {
				putExtra(EXTRA_MOD_URI, modUri.toString())
			}
		}
		// ClipData propaga el permiso de lectura de las URIs
		val clip = android.content.ClipData.newRawUri("project", projectUri)
		if (modUri != null) clip.addItem(android.content.ClipData.Item(modUri))
		intent.clipData = clip
		return intent
	}

	fun isInstalled(context: Context, packageName: String): Boolean {
		return try {
			context.packageManager.getPackageInfo(packageName, 0)
			true
		} catch (_: Exception) {
			false
		}
	}

	fun launch(context: Context, packageName: String, projectUri: Uri?, modUri: Uri?): Boolean {
		val intent = buildIntent(packageName, projectUri, modUri) ?: return false
		return try {
			context.startActivity(intent)
			true
		} catch (_: Exception) {
			false
		}
	}
}
