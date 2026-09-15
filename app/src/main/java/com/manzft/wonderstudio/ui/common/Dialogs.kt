package com.manzft.wonderstudio.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manzft.wonderstudio.data.AssetRepository
import com.manzft.wonderstudio.data.ProjectRepository

// Diálogo de formulario: uno o varios campos de texto.
@Composable
fun FieldsDialog(
	title: String,
	message: String,
	labels: List<String>,
	initial: List<String> = emptyList(),
	onDismiss: () -> Unit,
	onConfirm: (List<String>) -> Unit,
) {
	val values = remember {
		labels.mapIndexed { index, _ -> initial.getOrElse(index) { "" } }.toMutableStateList()
	}
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				if (message.isNotEmpty()) Text(message)
				labels.forEachIndexed { index, label ->
					OutlinedTextField(
						value = values[index],
						onValueChange = { values[index] = it },
						label = { Text(label) },
						singleLine = true,
						modifier = Modifier.fillMaxWidth(),
					)
				}
			}
		},
		confirmButton = {
			TextButton(onClick = { onConfirm(values.toList()) }) { Text("OK") }
		},
		dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
	)
}

@Composable
fun ConfirmDialog(
	title: String,
	message: String,
	onDismiss: () -> Unit,
	onConfirm: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = { Text(message) },
		confirmButton = { TextButton(onClick = onConfirm) { Text("OK") } },
		dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
	)
}

@Composable
fun OptionPickerDialog(
	title: String,
	message: String,
	options: List<String>,
	onDismiss: () -> Unit,
	onPick: (String) -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = {
			Column(Modifier.verticalScroll(rememberScrollState())) {
				if (message.isNotEmpty()) {
					Text(message, modifier = Modifier.padding(bottom = 8.dp))
				}
				options.forEach { option ->
					TextButton(onClick = { onPick(option) }, modifier = Modifier.fillMaxWidth()) {
						Text(option.replaceFirstChar { it.uppercase() })
					}
				}
			}
		},
		confirmButton = {},
		dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
	)
}

// Selector de un asset del proyecto (imágenes, sonidos, música).
@Composable
fun AssetPickerDialog(
	repository: ProjectRepository,
	assets: AssetRepository,
	onPlaySound: (String) -> Unit,
	onDismiss: () -> Unit,
	onPick: (String) -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Choose an asset") },
		text = {
			AssetBrowser(
				repository = repository,
				assets = assets,
				onPlaySound = onPlaySound,
				onPick = { path ->
					onPick(path)
					onDismiss()
				},
				modifier = Modifier.fillMaxWidth(),
			)
		},
		confirmButton = {},
		dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
	)
}
