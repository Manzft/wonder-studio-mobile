package com.manzft.wonderstudio.ui.common

import com.manzft.wonderstudio.ui.icons.WonderIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.manzft.wonderstudio.data.AssetRepository
import com.manzft.wonderstudio.data.ProjectRepository
import com.manzft.wonderstudio.model.Setting
import com.manzft.wonderstudio.model.Settings
import com.manzft.wonderstudio.ui.inspector.GroupField
import com.manzft.wonderstudio.ui.inspector.LabelField
import com.manzft.wonderstudio.ui.inspector.NumberField
import com.manzft.wonderstudio.ui.inspector.OptionField
import com.manzft.wonderstudio.ui.inspector.PathField
import com.manzft.wonderstudio.ui.inspector.PropertyField
import com.manzft.wonderstudio.ui.inspector.SkipField
import com.manzft.wonderstudio.ui.inspector.SwitchField
import com.manzft.wonderstudio.ui.inspector.TextField
import androidx.compose.material3.Switch

fun defaultSetting(field: PropertyField): Setting = when (field) {
	is NumberField -> Setting.number(
		field.min ?: if (field.name == "hframes" || field.name == "vframes") 1f else 0f,
		field.integer,
	)

	is SwitchField -> Setting.switch(
		field.name == "collision_scan_layer_1" || field.name == "collision_id_layer_1",
	)

	is PathField -> Setting.path("")
	is TextField -> Setting.text("")
	is OptionField -> Setting.text(field.options.firstOrNull() ?: "")
	else -> Setting.text("")
}

private fun currentSetting(settings: Settings, field: PropertyField): Setting {
	val name = nameOf(field) ?: return Setting.text("")
	return settings[name] ?: defaultSetting(field)
}

private fun nameOf(field: PropertyField): String? = when (field) {
	is NumberField -> field.name
	is SwitchField -> field.name
	is PathField -> field.name
	is TextField -> field.name
	is OptionField -> field.name
	else -> null
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun PropertyEditor(
	fields: List<PropertyField>,
	settings: Settings,
	// Cambia con cada mutación del proyecto: fuerza la recomposición del
	// editor (si no, Compose lo saltea con "strong skipping" porque el mapa de
	// settings es la misma instancia y no lee ningún estado).
	revision: Int,
	assets: AssetRepository,
	repository: ProjectRepository,
	onChanged: (String, Setting) -> Unit,
	onPlaySound: (String) -> Unit,
	modifier: Modifier = Modifier,
) {
	var assetTarget by remember { mutableStateOf<PathField?>(null) }
	var optionTarget by remember { mutableStateOf<OptionField?>(null) }

	Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
		fields.forEach { field ->
			when (field) {
				is GroupField -> Text(
					text = field.group.replaceFirstChar { it.uppercase() },
					style = MaterialTheme.typography.titleSmall,
					color = MaterialTheme.colorScheme.tertiary,
					modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
				)

				is SkipField -> Spacer(Modifier.height(2.dp))
				is LabelField -> Text(
					text = field.text,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.padding(top = 6.dp),
				)

				is NumberField -> NumberRow(field, currentSetting(settings, field)) { value ->
					onChanged(field.name, Setting.number(value, field.integer))
				}

				is SwitchField -> Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.fillMaxWidth(),
				) {
					Text("Value", modifier = Modifier.width(72.dp))
					Switch(
						checked = currentSetting(settings, field).asBool,
						onCheckedChange = { onChanged(field.name, Setting.switch(it)) },
					)
				}

				is PathField -> PathRow(
					value = currentSetting(settings, field).asString,
					onChoose = { assetTarget = field },
					onPlay = { onPlaySound(currentSetting(settings, field).asString) },
				)

				is TextField -> {
					var text by remember(field.name) { mutableStateOf(currentSetting(settings, field).asString) }
					OutlinedTextField(
						value = text,
						onValueChange = {
							text = it
							onChanged(field.name, Setting.text(it))
						},
						singleLine = true,
						modifier = Modifier.fillMaxWidth(),
					)
				}

				is OptionField -> Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.fillMaxWidth(),
				) {
					Text(
						text = currentSetting(settings, field).asString.ifEmpty { field.options.firstOrNull() ?: "" }
							.replaceFirstChar { it.uppercase() },
						modifier = Modifier.weight(1f),
					)
					TextButton(onClick = { optionTarget = field }) {
						Icon(Icons.Default.List, contentDescription = null)
						Text("Choose", modifier = Modifier.padding(start = 6.dp))
					}
				}
			}
		}
	}

	assetTarget?.let { target ->
		AssetPickerDialog(
			repository = repository,
			assets = assets,
			onPlaySound = onPlaySound,
			onDismiss = { assetTarget = null },
			onPick = { path -> onChanged(target.name, Setting.path(path)) },
		)
	}

	optionTarget?.let { target ->
		OptionPickerDialog(
			title = target.title,
			message = target.desc,
			options = target.options,
			onDismiss = { optionTarget = null },
			onPick = { option ->
				onChanged(target.name, Setting.text(option))
				optionTarget = null
			},
		)
	}
}

@Composable
private fun NumberRow(field: NumberField, setting: Setting, onChanged: (Float) -> Unit) {
	val value = setting.asFloat
	var text by remember(field.name) { mutableStateOf(formatNumber(value, field.integer)) }

	LaunchedEffect(value) {
		val parsed = text.toFloatOrNull()
		if (parsed == null || kotlin.math.abs(parsed - value) > 0.0001f) {
			text = formatNumber(value, field.integer)
		}
	}

	OutlinedTextField(
		value = text,
		onValueChange = { raw ->
			text = raw
			val parsed = raw.toFloatOrNull() ?: return@OutlinedTextField
			var clamped = parsed
			field.min?.let { if (clamped < it) clamped = it }
			field.max?.let { if (clamped > it) clamped = it }
			onChanged(clamped)
		},
		singleLine = true,
		keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
		modifier = Modifier.fillMaxWidth(),
	)
}

private fun formatNumber(value: Float, integer: Boolean): String {
	if (integer) return value.toInt().toString()
	return if (value % 1f == 0f) value.toInt().toString() else value.toString()
}

@Composable
private fun PathRow(value: String, onChoose: () -> Unit, onPlay: () -> Unit) {
	Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
		Text(
			text = value.ifEmpty { "(none)" },
			modifier = Modifier.weight(1f),
			style = MaterialTheme.typography.bodySmall,
		)
		val isAudio = value.endsWith(".wav") || value.endsWith(".mp3") || value.endsWith(".ogg")
		if (isAudio) {
			IconButton(onClick = onPlay) {
				Icon(Icons.Default.PlayArrow, contentDescription = "Play")
			}
		}
		TextButton(onClick = onChoose) {
			Icon(WonderIcons.FolderOpen, contentDescription = null)
			Text("Choose", modifier = Modifier.padding(start = 6.dp))
		}
	}
}
