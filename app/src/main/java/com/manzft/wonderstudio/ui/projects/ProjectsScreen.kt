package com.manzft.wonderstudio.ui.projects

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.manzft.wonderstudio.data.RecentProject
import com.manzft.wonderstudio.model.Defaults
import com.manzft.wonderstudio.ui.EditorViewModel
import com.manzft.wonderstudio.ui.common.FieldsDialog

private enum class PendingAction { NEW, OPEN, IMPORT_MOD }

@Composable
fun ProjectsScreen(vm: EditorViewModel, modifier: Modifier = Modifier) {
	val context = LocalContext.current
	// rememberSaveable: el selector de carpetas puede recrear la Activity y hay
	// que conservar la acción pendiente para mostrar el diálogo al volver.
	var pendingAction by rememberSaveable { mutableStateOf<PendingAction?>(null) }
	var newProjectUri by rememberSaveable { mutableStateOf<Uri?>(null) }
	var dialogError by rememberSaveable { mutableStateOf<String?>(null) }

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

	Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(24.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				"Wonder Studio",
				style = MaterialTheme.typography.headlineMedium,
				color = MaterialTheme.colorScheme.primary,
				textAlign = TextAlign.Center,
			)
			Text(
				"Create Wonder Maker projects right from your phone.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.Center,
				modifier = Modifier.padding(top = 4.dp),
			)
			Text(
				"Wonder Studio ${Defaults.ENGINE_VERSION}",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.tertiary,
				modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
			)

			Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
				Button(
					onClick = { pendingAction = PendingAction.NEW; folderPicker.launch(null) },
					modifier = Modifier.weight(1f),
				) {
					Icon(Icons.Default.Add, contentDescription = null)
					Text("New", modifier = Modifier.padding(start = 6.dp), maxLines = 1)
				}
				Button(
					onClick = { pendingAction = PendingAction.OPEN; folderPicker.launch(null) },
					modifier = Modifier.weight(1f),
				) {
					Icon(Icons.Default.FolderOpen, contentDescription = null)
					Text("Open", modifier = Modifier.padding(start = 6.dp), maxLines = 1)
				}
			}
			OutlinedButton(
				onClick = { pendingAction = PendingAction.IMPORT_MOD; folderPicker.launch(null) },
				modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
			) {
				Icon(Icons.Default.Extension, contentDescription = null)
				Text("Import mod", modifier = Modifier.padding(start = 6.dp))
			}

			Text(
				"Recent projects",
				style = MaterialTheme.typography.titleMedium,
				modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
			)

			if (vm.recentProjects.isEmpty()) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
				) {
					Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
					Text("No projects yet. Create one with \"New\".", color = MaterialTheme.colorScheme.onSurfaceVariant)
				}
			} else {
				LazyColumn(
					verticalArrangement = Arrangement.spacedBy(6.dp),
					modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
				) {
					items(vm.recentProjects, key = { it.uri }) { recent ->
						RecentRow(
							recent = recent,
							onOpen = { vm.openProject(Uri.parse(recent.uri)) },
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
					dialogError = "The name can't be empty"
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
			.padding(vertical = 6.dp),
	) {
		Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
		Column(Modifier.weight(1f).padding(start = 12.dp)) {
			Text(recent.name, style = MaterialTheme.typography.bodyLarge)
			Text(recent.uri, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
		IconButton(onClick = onRemove) {
			Icon(Icons.Default.Delete, contentDescription = "Remove")
		}
	}
}
