package com.manzft.wonderstudio.ui.projects

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.manzft.wonderstudio.data.RecentProject
import com.manzft.wonderstudio.ui.EditorViewModel
import com.manzft.wonderstudio.ui.common.FieldsDialog

private enum class PendingAction { NEW, OPEN, IMPORT_MOD }

@Composable
fun ProjectsScreen(vm: EditorViewModel, modifier: Modifier = Modifier) {
	val context = LocalContext.current
	var pendingAction by remember { mutableStateOf<PendingAction?>(null) }
	var newProjectUri by remember { mutableStateOf<android.net.Uri?>(null) }
	var dialogError by remember { mutableStateOf<String?>(null) }

	val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
		if (uri == null) {
			pendingAction = null
			return@rememberLauncherForActivityResult
		}
		try {
			context.contentResolver.takePersistableUriPermission(
				uri,
				Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
			)
		} catch (_: Exception) {
		}

		when (pendingAction) {
			PendingAction.NEW -> newProjectUri = uri
			PendingAction.OPEN -> vm.openProject(uri)
			PendingAction.IMPORT_MOD -> vm.importMod(uri)
			null -> Unit
		}
		pendingAction = null
	}

	Column(modifier = modifier.padding(20.dp)) {
		Text("Wonder Studio", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
		Text(
			"Crea proyectos para Wonder Maker desde el celular.",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(bottom = 16.dp),
		)

		Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
			Button(onClick = { pendingAction = PendingAction.NEW; folderPicker.launch(null) }) { Text("New") }
			Button(onClick = { pendingAction = PendingAction.OPEN; folderPicker.launch(null) }) { Text("Open") }
			OutlinedButton(onClick = { pendingAction = PendingAction.IMPORT_MOD; folderPicker.launch(null) }) { Text("Open mod") }
		}

		Text(
			"Recent projects",
			style = MaterialTheme.typography.titleMedium,
			modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
		)

		if (vm.recentProjects.isEmpty()) {
			Text("Todavía no hay proyectos. Creá uno con \"New\".", color = MaterialTheme.colorScheme.onSurfaceVariant)
		} else {
			LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
				items(vm.recentProjects, key = { it.uri }) { recent ->
					RecentRow(
						recent = recent,
						onOpen = { vm.openProject(android.net.Uri.parse(recent.uri)) },
						onRemove = {
							vm.config.removeRecent(recent.uri)
							vm.touch()
						},
					)
				}
			}
		}

		dialogError?.let { error ->
			Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
		}
	}

	newProjectUri?.let { uri ->
		FieldsDialog(
			title = "New project",
			message = "Enter the project info",
			labels = listOf("Project name", "Author"),
			initial = listOf("My Project", ""),
			onDismiss = { newProjectUri = null },
			onConfirm = { values ->
				val name = values.getOrNull(0).orEmpty()
				val author = values.getOrNull(1).orEmpty()
				if (name.isBlank()) {
					dialogError = "El nombre no puede estar vacío"
				} else {
					vm.createProject(uri, name, author)
				}
				newProjectUri = null
			},
		)
	}
}

@Composable
private fun RecentRow(recent: RecentProject, onOpen: () -> Unit, onRemove: () -> Unit) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onOpen)
			.padding(vertical = 10.dp),
	) {
		Column(Modifier.weight(1f)) {
			Text(recent.name, style = MaterialTheme.typography.bodyLarge)
			Text(recent.uri, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
		TextButton(onClick = onRemove) { Text("Remove") }
	}
}
