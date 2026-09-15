package com.manzft.wonderstudio.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

// Lanza la app de Wonder Maker (Android) pasándole la ruta real del proyecto
// del estudio (y del mod, si hay). Wonder Maker necesita el permiso
// "All files access" para poder leer esas rutas.
object RunLauncher {

	const val EXTRA_PROJECT_PATH = "studio_project_path"
	const val EXTRA_MOD_PATH = "studio_mod_path"

	// Wonder Maker es una app Godot común (solo tiene MAIN/LAUNCHER), así que se
	// lanza su actividad launcher y se le pasan las rutas por extras.
	fun buildIntent(packageName: String, projectPath: String?, modPath: String?): Intent? {
		if (projectPath.isNullOrEmpty()) return null
		return Intent(Intent.ACTION_MAIN).apply {
			addCategory(Intent.CATEGORY_LAUNCHER)
			setPackage(packageName)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			putExtra(EXTRA_PROJECT_PATH, projectPath)
			if (!modPath.isNullOrEmpty()) putExtra(EXTRA_MOD_PATH, modPath)
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

	fun launch(context: Context, packageName: String, projectPath: String?, modPath: String?): Boolean {
		val intent = buildIntent(packageName, projectPath, modPath) ?: return false
		return try {
			context.startActivity(intent)
			true
		} catch (_: Exception) {
			false
		}
	}

	// Abre la pantalla de "All files access" para Wonder Maker (el juego la
	// necesita para leer la carpeta del proyecto desde el almacenamiento).
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
