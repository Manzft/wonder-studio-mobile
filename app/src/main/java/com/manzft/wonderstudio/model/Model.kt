package com.manzft.wonderstudio.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

// ---------------------------------------------------------------------------
// Modelo del proyecto Wonder Studio. Replica el formato JSON del Studio de PC:
//   project.json + sourcecode/styles/<estilo>/{style.json, themes, characters, objects}
// Cada setting se guarda como { "type": "...", "value": ... }.
// ---------------------------------------------------------------------------

object WonderJson {
	val instance: Json = Json {
		ignoreUnknownKeys = true
		explicitNulls = false
		encodeDefaults = false
		prettyPrint = true
		prettyPrintIndent = "\t"
		isLenient = true
	}
}

@Serializable
data class Setting(
	var type: String = "text",
	var value: JsonElement = JsonPrimitive(""),
) {
	val asFloat: Float get() = (value as? JsonPrimitive)?.doubleOrNull?.toFloat() ?: 0f
	val asInt: Int get() = (value as? JsonPrimitive)?.intOrNull ?: 0
	val asBool: Boolean get() = (value as? JsonPrimitive)?.booleanOrNull ?: false
	val asString: String get() = (value as? JsonPrimitive)?.contentOrNull ?: ""

	companion object {
		fun number(value: Number, integer: Boolean = false): Setting =
			Setting("number", JsonPrimitive(if (integer) value.toInt() else value.toFloat()))

		fun switch(value: Boolean): Setting = Setting("switch", JsonPrimitive(value))

		fun text(value: String): Setting = Setting("text", JsonPrimitive(value))

		fun path(value: String): Setting = Setting("path", JsonPrimitive(value))
	}
}

typealias Settings = MutableMap<String, Setting>

fun Settings.number(key: String, def: Float = 0f): Float = this[key]?.asFloat ?: def

fun Settings.int(key: String, def: Int = 0): Int = this[key]?.asInt ?: def

fun Settings.bool(key: String, def: Boolean = false): Boolean = this[key]?.asBool ?: def

fun Settings.string(key: String, def: String = ""): String = this[key]?.asString ?: def

fun Settings.putNumber(key: String, value: Float, integer: Boolean = false) {
	this[key] = Setting.number(value, integer)
}

fun Settings.putSwitch(key: String, value: Boolean) {
	this[key] = Setting.switch(value)
}

fun Settings.putPath(key: String, value: String) {
	this[key] = Setting.path(value)
}

fun Settings.putText(key: String, value: String) {
	this[key] = Setting.text(value)
}

@Serializable
data class Animation(
	var name: String = "",
	var settings: Settings = mutableMapOf(),
) {
	fun setting(key: String, def: Float): Float = settings.number(key, def)

	companion object {
		fun create(name: String): Animation = Animation(
			name = name,
			settings = mutableMapOf(
				"texture" to Setting.path(""),
				"hframes" to Setting.number(1, true),
				"vframes" to Setting.number(1, true),
				"frame" to Setting.number(0, true),
				"fps" to Setting.number(60, true),
				"start_frame" to Setting.number(0, true),
				"end_frame" to Setting.number(0, true),
				"offset_x" to Setting.number(0, false),
				"offset_y" to Setting.number(0, false),
				"repeat" to Setting.switch(true),
			),
		)
	}
}

@Serializable
data class Component(
	var name: String = "",
	var type: String = "",
	var settings: Settings = mutableMapOf(),
	var animations: MutableList<Animation> = mutableListOf(),
	var current_animation: String? = null,
) {
	val isVisual: Boolean get() = type == "sprite" || type == "sprite_parallax" || type == "animated_sprite"

	fun animation(name: String?): Animation? = animations.firstOrNull { it.name == name }

	val currentAnimation: Animation? get() = animation(current_animation) ?: animations.firstOrNull()
}

@Serializable
data class Element(
	var order: Int = 0,
	var name: String = "",
	var components: MutableList<Component> = mutableListOf(),
	var script: String? = null,
	var settings: Settings = mutableMapOf(),
) {
	val isTheme: Boolean get() = script == null
}

@Serializable
data class Style(
	var order: Int = 0,
	var name: String = "",
	var themes: MutableList<Element> = mutableListOf(),
	var characters: MutableList<Element> = mutableListOf(),
	var objects: MutableList<Element> = mutableListOf(),
	var settings: Settings = mutableMapOf(),
)

@Serializable
data class Project(
	var project_name: String = "",
	var author_name: String = "",
	var project_version: String = "1.0",
	var version: String = "",
	var styles: MutableList<Style> = mutableListOf(),
)

// ---------------------------------------------------------------------------
// Valores por defecto (espejo de hierarchy.gd / inspector.gd / characters.gd / objects.gd)
// ---------------------------------------------------------------------------

object Defaults {
	const val ENGINE_VERSION = "1.0"

	const val DEFAULT_SCRIPT = "create()\n\nend\n\nstart()\n\nend\n\nframe()\n\nend\n"

	val COMPONENT_TYPES = listOf("sprite", "animated_sprite", "sprite_parallax", "collider", "area", "raycast", "sound")

	val CATEGORIES = listOf("terrain", "items", "enemies", "gizmos")

	private fun colorSettings(): Settings = mutableMapOf(
		"color_r" to Setting.number(255, true),
		"color_g" to Setting.number(255, true),
		"color_b" to Setting.number(255, true),
		"color_a" to Setting.number(255, true),
	)

	private fun spriteCommon(): Settings = mutableMapOf(
		"position_x" to Setting.number(0, false),
		"position_y" to Setting.number(0, false),
		"scale_x" to Setting.number(1, false),
		"scale_y" to Setting.number(1, false),
		"visible" to Setting.switch(true),
		"blur" to Setting.switch(true),
		"flipx" to Setting.switch(false),
		"flipy" to Setting.switch(false),
	)

	fun componentSettings(type: String): Settings = when (type) {
		"sprite" -> spriteCommon().apply {
			put("texture", Setting.path(""))
			put("hframes", Setting.number(1, true))
			put("vframes", Setting.number(1, true))
			put("frame", Setting.number(0, true))
			putAll(colorSettings())
		}

		"sprite_parallax" -> spriteCommon().apply {
			put("texture", Setting.path(""))
			put("hframes", Setting.number(1, true))
			put("vframes", Setting.number(1, true))
			put("frame", Setting.number(0, true))
			putAll(colorSettings())
			put("scroll_scale_x", Setting.number(1, false))
			put("scroll_scale_y", Setting.number(1, false))
			put("repeat_size_x", Setting.number(0, false))
			put("repeat_size_y", Setting.number(0, false))
		}

		"animated_sprite" -> spriteCommon().apply {
			putAll(colorSettings())
		}

		"collider" -> mutableMapOf(
			"position_x" to Setting.number(0, false),
			"position_y" to Setting.number(0, false),
			"size_x" to Setting.number(40, false),
			"size_y" to Setting.number(40, false),
			"deactivated" to Setting.switch(false),
		)

		"area" -> mutableMapOf(
			"position_x" to Setting.number(0, false),
			"position_y" to Setting.number(0, false),
			"size_x" to Setting.number(40, false),
			"size_y" to Setting.number(40, false),
		)

		"raycast" -> mutableMapOf(
			"position_x" to Setting.number(0, false),
			"position_y" to Setting.number(0, false),
			"dirx" to Setting.number(0, false),
			"diry" to Setting.number(-1, false),
			"length" to Setting.number(80, false),
		)

		"sound" -> mutableMapOf(
			"path" to Setting.path(""),
			"db" to Setting.number(0, false),
			"pitch" to Setting.number(0, false),
			"loop" to Setting.switch(false),
		)

		else -> mutableMapOf()
	}

	fun styleSettings(): Settings = mutableMapOf(
		"card" to Setting.path(""),
	)

	fun themeSettings(): Settings = mutableMapOf(
		"card" to Setting.path(""),
		"square_card" to Setting.path(""),
		"shadows" to Setting.switch(true),
		"editor_music" to Setting.path(""),
		"editor_music_db" to Setting.number(0, false),
		"gameplay_music" to Setting.path(""),
		"gameplay_music_db" to Setting.number(0, false),
	)

	private fun collisionSettings(): Settings = mutableMapOf(
		"collision_scan_layer_1" to Setting.switch(true),
		"collision_scan_layer_2" to Setting.switch(false),
		"collision_scan_layer_3" to Setting.switch(false),
		"collision_scan_layer_4" to Setting.switch(false),
		"collision_scan_layer_5" to Setting.switch(false),
		"collision_scan_layer_6" to Setting.switch(false),
		"collision_id_layer_1" to Setting.switch(true),
		"collision_id_layer_2" to Setting.switch(false),
		"collision_id_layer_3" to Setting.switch(false),
		"collision_id_layer_4" to Setting.switch(false),
		"collision_id_layer_5" to Setting.switch(false),
		"collision_id_layer_6" to Setting.switch(false),
	)

	fun characterSettings(): Settings = mutableMapOf(
		"gravity" to Setting.number(1.0, false),
		"friction" to Setting.number(1.0, false),
		"grid_size_x" to Setting.number(1, true),
		"grid_size_y" to Setting.number(1, true),
		"layer" to Setting.number(0, true),
	).apply { putAll(collisionSettings()) }

	fun objectSettings(): Settings = mutableMapOf(
		"gravity" to Setting.number(1.0, false),
		"friction" to Setting.number(1.0, false),
		"icon" to Setting.path(""),
		"category" to Setting.text(""),
		"solid" to Setting.switch(false),
		"is_tile" to Setting.switch(false),
		"is_floor" to Setting.switch(false),
		"is_solid_entity" to Setting.switch(false),
		"can_receive_drags" to Setting.switch(false),
		"grid_size_x" to Setting.number(1, true),
		"grid_size_y" to Setting.number(1, true),
		"layer" to Setting.number(0, true),
	).apply { putAll(collisionSettings()) }

	fun newStyle(name: String) = Style(name = name, settings = styleSettings())

	fun newTheme(name: String) = Element(name = name, settings = themeSettings())

	fun newCharacter(name: String) = Element(name = name, script = DEFAULT_SCRIPT, settings = characterSettings())

	fun newObject(name: String) = Element(name = name, script = DEFAULT_SCRIPT, settings = objectSettings())

	fun newProject(name: String, author: String): Project = Project(
		project_name = name,
		author_name = author,
		project_version = "1.0",
		version = ENGINE_VERSION,
		styles = mutableListOf(),
	)
}

// ---------------------------------------------------------------------------
// Helpers de búsqueda
// ---------------------------------------------------------------------------

fun Project.style(name: String?): Style? = styles.firstOrNull { it.name == name }

fun Style.elements(type: ElementType): MutableList<Element> = when (type) {
	ElementType.THEME -> themes
	ElementType.CHARACTER -> characters
	ElementType.OBJECT -> objects
}

fun Style.element(type: ElementType, name: String?): Element? =
	elements(type).firstOrNull { it.name == name }

enum class ElementType(val key: String, val singular: String) {
	THEME("themes", "theme"),
	CHARACTER("characters", "character"),
	OBJECT("objects", "object"),
}
