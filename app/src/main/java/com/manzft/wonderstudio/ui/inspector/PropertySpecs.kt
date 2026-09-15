package com.manzft.wonderstudio.ui.inspector

import com.manzft.wonderstudio.model.ElementType

// Definición declarativa de los campos del inspector, en el mismo orden que el
// inspector del Studio de PC (inspector.gd).
sealed interface PropertyField

data class GroupField(val group: String) : PropertyField
data class SkipField(val marker: Int = 0) : PropertyField
data class LabelField(val text: String, val expand: Boolean = false) : PropertyField
data class NumberField(
	val name: String,
	val integer: Boolean = false,
	val min: Float? = null,
	val max: Float? = null,
) : PropertyField

data class SwitchField(val name: String) : PropertyField
data class PathField(val name: String) : PropertyField
data class TextField(val name: String) : PropertyField
data class OptionField(
	val name: String,
	val options: List<String>,
	val title: String,
	val desc: String,
) : PropertyField

object PropertySpecs {

	val style: List<PropertyField> = listOf(
		GroupField("settings"),
		SkipField(),
		LabelField("Card"),
		PathField("card"),
	)

	val themeTree: List<PropertyField> = listOf(
		GroupField("settings"),
		SkipField(),
		LabelField("Card"),
		PathField("card"),
		SkipField(),
		LabelField("Square card"),
		PathField("square_card"),
		SkipField(),
		LabelField("Shadows", expand = true),
		SwitchField("shadows"),
		SkipField(),
		LabelField("Editor music"),
		PathField("editor_music"),
		SkipField(),
		LabelField("Editor music dB"),
		NumberField("editor_music_db"),
		SkipField(),
		LabelField("Gameplay music"),
		PathField("gameplay_music"),
		SkipField(),
		LabelField("Gameplay music dB"),
		NumberField("gameplay_music_db"),
	)

	private fun collisionFields(): List<PropertyField> {
		val fields = mutableListOf<PropertyField>(
			SkipField(),
			GroupField("collisions"),
			SkipField(),
		)
		for (i in 1..6) {
			fields.add(LabelField("Scan $i", expand = true))
			fields.add(SwitchField("collision_scan_layer_$i"))
			fields.add(LabelField("Id $i", expand = true))
			fields.add(SwitchField("collision_id_layer_$i"))
			fields.add(SkipField())
		}
		return fields
	}

	val characterTree: List<PropertyField> = listOf(
		GroupField("settings"),
		SkipField(),
		LabelField("Gravity"),
		NumberField("gravity"),
		SkipField(),
		LabelField("Friction"),
		NumberField("friction"),
		SkipField(),
		LabelField("Grid size X"),
		NumberField("grid_size_x", integer = true),
		SkipField(),
		LabelField("Grid size Y"),
		NumberField("grid_size_y", integer = true),
		SkipField(),
		LabelField("Layer"),
		NumberField("layer", integer = true),
	) + collisionFields()

	val objectTree: List<PropertyField> = listOf(
		GroupField("settings"),
		SkipField(),
		LabelField("Gravity"),
		NumberField("gravity"),
		SkipField(),
		LabelField("Friction"),
		NumberField("friction"),
		SkipField(),
		LabelField("Grid size X"),
		NumberField("grid_size_x", integer = true),
		SkipField(),
		LabelField("Grid size Y"),
		NumberField("grid_size_y", integer = true),
		SkipField(),
		LabelField("Layer"),
		NumberField("layer", integer = true),
		SkipField(),
		LabelField("Icon"),
		PathField("icon"),
		SkipField(),
		LabelField("Category"),
		OptionField(
			name = "category",
			options = listOf("terrain", "items", "enemies", "gizmos"),
			title = "Choose an option",
			desc = "Choose a category for this object.",
		),
		SkipField(),
		LabelField("Solid", expand = true),
		SwitchField("solid"),
		SkipField(),
		LabelField("Is floor", expand = true),
		SwitchField("is_floor"),
		SkipField(),
		LabelField("Is Solid Entity", expand = true),
		SwitchField("is_solid_entity"),
		SkipField(),
		LabelField("Can receive drags", expand = true),
		SwitchField("can_receive_drags"),
	) + collisionFields()

	private fun transformFields(): List<PropertyField> = listOf(
		GroupField("position"),
		SkipField(),
		LabelField("x"),
		NumberField("position_x"),
		LabelField("y"),
		NumberField("position_y"),
		SkipField(),
		GroupField("scale"),
		SkipField(),
		LabelField("x"),
		NumberField("scale_x"),
		LabelField("y"),
		NumberField("scale_y"),
	)

	private fun colorFields(): List<PropertyField> = listOf(
		GroupField("color"),
		SkipField(),
		LabelField("R"),
		NumberField("color_r", integer = true, min = 0f, max = 255f),
		SkipField(),
		LabelField("G"),
		NumberField("color_g", integer = true, min = 0f, max = 255f),
		SkipField(),
		LabelField("B"),
		NumberField("color_b", integer = true, min = 0f, max = 255f),
		SkipField(),
		LabelField("A"),
		NumberField("color_a", integer = true, min = 0f, max = 255f),
	)

	private val spriteExtra: List<PropertyField> = listOf(
		SkipField(),
		LabelField("Texture"),
		PathField("texture"),
		SkipField(),
		LabelField("Blur", expand = true),
		SwitchField("blur"),
		SkipField(),
		LabelField("Flip X", expand = true),
		SwitchField("flipx"),
		SkipField(),
		LabelField("Flip Y", expand = true),
		SwitchField("flipy"),
		SkipField(),
		LabelField("Hframes"),
		NumberField("hframes", integer = true),
		SkipField(),
		LabelField("Vframes"),
		NumberField("vframes", integer = true),
		SkipField(),
		LabelField("Frame"),
		NumberField("frame", integer = true),
	)

	private val spriteHeader: List<PropertyField> = listOf(
		SkipField(),
		GroupField("settings"),
		SkipField(),
		LabelField("Visible", expand = true),
		SwitchField("visible"),
		SkipField(),
		LabelField("Rotation"),
		NumberField("rotation", min = 0f, max = 360f),
	)

	val sprite: List<PropertyField> = transformFields() + spriteHeader + spriteExtra + colorFields()

	val animatedSprite: List<PropertyField> = transformFields() + listOf(
		SkipField(),
		GroupField("settings"),
		SkipField(),
		LabelField("Visible", expand = true),
		SwitchField("visible"),
		SkipField(),
		LabelField("Rotation"),
		NumberField("rotation", min = 0f, max = 360f),
		SkipField(),
		LabelField("Blur", expand = true),
		SwitchField("blur"),
		SkipField(),
		LabelField("Flip X", expand = true),
		SwitchField("flipx"),
		SkipField(),
		LabelField("Flip Y", expand = true),
		SwitchField("flipy"),
	) + colorFields()

	val spriteParallax: List<PropertyField> = transformFields() + spriteHeader + spriteExtra + colorFields() + listOf(
		SkipField(),
		GroupField("scroll_scale"),
		SkipField(),
		LabelField("x"),
		NumberField("scroll_scale_x"),
		LabelField("y"),
		NumberField("scroll_scale_y"),
		SkipField(),
		GroupField("repeat_size"),
		SkipField(),
		LabelField("x"),
		NumberField("repeat_size_x"),
		LabelField("y"),
		NumberField("repeat_size_y"),
	)

	val collider: List<PropertyField> = listOf(
		GroupField("position"),
		SkipField(),
		LabelField("x"),
		NumberField("position_x"),
		LabelField("y"),
		NumberField("position_y"),
		SkipField(),
		GroupField("size"),
		SkipField(),
		LabelField("x"),
		NumberField("size_x", min = 40f),
		LabelField("y"),
		NumberField("size_y", min = 40f),
		SkipField(),
		GroupField("settings"),
		SkipField(),
		LabelField("Deactivated", expand = true),
		SwitchField("deactivated"),
	)

	val area: List<PropertyField> = listOf(
		GroupField("position"),
		SkipField(),
		LabelField("x"),
		NumberField("position_x"),
		LabelField("y"),
		NumberField("position_y"),
		SkipField(),
		GroupField("size"),
		SkipField(),
		LabelField("x"),
		NumberField("size_x", min = 40f),
		LabelField("y"),
		NumberField("size_y", min = 40f),
	)

	val raycast: List<PropertyField> = listOf(
		GroupField("position"),
		SkipField(),
		LabelField("x"),
		NumberField("position_x"),
		LabelField("y"),
		NumberField("position_y"),
		SkipField(),
		GroupField("direction"),
		SkipField(),
		LabelField("x"),
		NumberField("dirx", min = -1f, max = 1f),
		LabelField("y"),
		NumberField("diry", min = -1f, max = 1f),
		SkipField(),
		LabelField("Length"),
		NumberField("length", min = 1f),
	)

	val sound: List<PropertyField> = listOf(
		GroupField("settings"),
		SkipField(),
		LabelField("Path"),
		PathField("path"),
		SkipField(),
		LabelField("dB"),
		NumberField("db"),
		SkipField(),
		LabelField("Pitch"),
		NumberField("pitch"),
		SkipField(),
		LabelField("Loop", expand = true),
		SwitchField("loop"),
	)

	// Campos de una animación de animated_sprite
	val animation: List<PropertyField> = listOf(
		SkipField(),
		LabelField("Texture"),
		PathField("texture"),
		SkipField(),
		LabelField("Hframes"),
		NumberField("hframes", integer = true, min = 1f),
		SkipField(),
		LabelField("Vframes"),
		NumberField("vframes", integer = true, min = 1f),
		SkipField(),
		LabelField("Frame"),
		NumberField("frame", integer = true, min = 0f),
		SkipField(),
		LabelField("FPS"),
		NumberField("fps", integer = true, min = 1f),
		SkipField(),
		LabelField("Start frame"),
		NumberField("start_frame", integer = true, min = 0f),
		SkipField(),
		LabelField("End frame"),
		NumberField("end_frame", integer = true, min = 0f),
		SkipField(),
		LabelField("Offset X"),
		NumberField("offset_x"),
		SkipField(),
		LabelField("Offset Y"),
		NumberField("offset_y"),
		SkipField(),
		LabelField("Repeat", expand = true),
		SwitchField("repeat"),
	)

	fun forComponent(type: String): List<PropertyField> = when (type) {
		"sprite" -> sprite
		"animated_sprite" -> animatedSprite
		"sprite_parallax" -> spriteParallax
		"collider" -> collider
		"area" -> area
		"raycast" -> raycast
		"sound" -> sound
		else -> emptyList()
	}

	fun forElementRoot(type: ElementType): List<PropertyField> = when (type) {
		ElementType.THEME -> themeTree
		ElementType.CHARACTER -> characterTree
		ElementType.OBJECT -> objectTree
	}

	// Grupo de settings que se muestra como encabezado animado
	val GROUPS = setOf("position", "scale", "settings", "color", "size", "direction", "collisions", "scroll_scale", "repeat_size")
}
