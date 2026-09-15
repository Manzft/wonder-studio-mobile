package com.manzft.wonderstudio.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

// Lanza la app de Wonder Maker (Android). El proyecto se prueba poniéndolo en la
// carpeta "mods" del juego (~/.../Wonder Maker/mods en PC, /Wonder Maker/mods en
// Android), que Wonder Maker carga automáticamente al abrir.
object RunLauncher {

	fun buildIntent(packageName: String): Intent? {
		return Intent(Intent.ACTION_MAIN).apply {
			addCategory(Intent.CATEGORY_LAUNCHER)
			setPackage(packageName)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
	}

	fun isInstalled(context: Context, packageName: String): Boolean {
		return try {
			context.packageManager.getPackageInfo(packageName, 0)
			true
		} catch (_: Exception) {
			false
		}
	}

	fun launch(context: Context, packageName: String): Boolean {
		val intent = buildIntent(packageName) ?: return false
		return try {
			context.startActivity(intent)
			true
		} catch (_: Exception) {
			false
		}
	}

	// Abre la pantalla de "All files access" para Wonder Maker (así puede leer
	// la carpeta compartida /Wonder Maker/mods en Android).
	fun openAllFilesAccessSettings(context: Context, packageName: String): Boolean {
		return try {
			val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
			intent.data = Uri.parse("package:$packageName")
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			context.startActivity(intent)
			true
		} catch (_: Exception) {
			try {
				val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				context.startActivity(intent)
				true
			} catch (_: Exception) {
				false
			}
		}
	}
}
