package com.manzft.wonderstudio.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

// Lanza la app de Wonder Maker (Android) pasándole el proyecto del estudio y,
// si hay, el mod importado.
object RunLauncher {

	const val EXTRA_PROJECT_URI = "studio_project_uri"
	const val EXTRA_MOD_URI = "studio_mod_uri"

	// Wonder Maker es una app Godot común (solo tiene MAIN/LAUNCHER), así que se
	// lanza su actividad launcher y se le pasan las URIs por extras + ClipData.
	fun buildIntent(packageName: String, projectUri: Uri?, modUri: Uri?): Intent? {
		if (projectUri == null) return null
		val intent = Intent(Intent.ACTION_MAIN).apply {
			addCategory(Intent.CATEGORY_LAUNCHER)
			setPackage(packageName)
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			putExtra(EXTRA_PROJECT_URI, projectUri.toString())
			if (modUri != null) {
				putExtra(EXTRA_MOD_URI, modUri.toString())
			}
		}
		val clip = ClipData.newRawUri("project", projectUri)
		if (modUri != null) clip.addItem(ClipData.Item(modUri))
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
			// Otorga lectura del árbol al juego (además del flag del Intent)
			try {
				context.grantUriPermission(packageName, projectUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
				if (modUri != null) {
					context.grantUriPermission(packageName, modUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
				}
			} catch (_: Exception) {
			}
			context.startActivity(intent)
			true
		} catch (_: Exception) {
			false
		}
	}
}
