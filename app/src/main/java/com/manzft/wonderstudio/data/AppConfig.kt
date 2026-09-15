package com.manzft.wonderstudio.data

import android.content.Context
import com.manzft.wonderstudio.model.WonderJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class RecentProject(val name: String, val uri: String)

// Preferencias de la app: proyectos recientes y configuración del editor.
class AppConfig(context: Context) {

	private val prefs = context.getSharedPreferences("wonder_studio", Context.MODE_PRIVATE)

	var recentProjects: List<RecentProject>
		get() {
			val raw = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
			return runCatching { WonderJson.instance.decodeFromString<List<RecentProject>>(raw) }.getOrDefault(emptyList())
		}
		set(value) {
			prefs.edit().putString(KEY_RECENTS, WonderJson.instance.encodeToString(value)).apply()
		}

	var lastProjectUri: String?
		get() = prefs.getString(KEY_LAST_PROJECT, null)
		set(value) {
			prefs.edit().putString(KEY_LAST_PROJECT, value).apply()
		}

	// Paquete de la app de Wonder Maker para lanzar el test por Intent
	var testPackage: String
		get() = prefs.getString(KEY_TEST_PACKAGE, DEFAULT_TEST_PACKAGE) ?: DEFAULT_TEST_PACKAGE
		set(value) {
			prefs.edit().putString(KEY_TEST_PACKAGE, value).apply()
		}

	fun addRecent(name: String, uri: String) {
		val current = recentProjects.filterNot { it.uri == uri }.toMutableList()
		current.add(0, RecentProject(name, uri))
		recentProjects = current.take(20)
	}

	fun removeRecent(uri: String) {
		recentProjects = recentProjects.filterNot { it.uri == uri }
	}

	companion object {
		private const val KEY_RECENTS = "recent_projects"
		private const val KEY_LAST_PROJECT = "last_project"
		private const val KEY_TEST_PACKAGE = "test_package"
		const val DEFAULT_TEST_PACKAGE = "com.Manzft.wondermaker"
	}
}
