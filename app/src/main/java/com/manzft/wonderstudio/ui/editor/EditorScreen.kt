package com.manzft.wonderstudio.ui.editor

import com.manzft.wonderstudio.ui.icons.WonderIcons
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.manzft.wonderstudio.model.Element
import com.manzft.wonderstudio.model.ElementType
import com.manzft.wonderstudio.model.elements
import com.manzft.wonderstudio.ui.EditorViewModel
import com.manzft.wonderstudio.ui.canvas.SceneCanvas
import com.manzft.wonderstudio.ui.common.ConfirmDialog
import com.manzft.wonderstudio.ui.common.ItemMenu
import com.manzft.wonderstudio.ui.common.FieldsDialog
import kotlinx.coroutines.launch

private enum class EditorTab(val label: String, val icon: ImageVector) {
	SCENE("Scene", WonderIcons.Image),
	TREE("Tree", WonderIcons.AccountTree),
	INSPECTOR("Inspector", WonderIcons.Tune),
	ANIMS("Anims", WonderIcons.Animation),
	CODE("Code", WonderIcons.Code),
	FILES("Files", WonderIcons.Folder),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel, modifier: Modifier = Modifier) {
	val scope = rememberCoroutineScope()
	val snackbar = remember { SnackbarHostState() }
	val context = LocalContext.current
	var tab by remember { mutableStateOf(EditorTab.SCENE) }
	var showExport by remember { mutableStateOf(false) }
	var showLogs by remember { mutableStateOf(false) }
	var menuOpen by remember { mutableStateOf(false) }
	@Suppress("UNUSED_VARIABLE")
	val revision = vm.revision
	val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)

	val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
		if (uri != null) {
			try {
				context.contentResolver.takePersistableUriPermission(
					uri,
					android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
				)
			} catch (_: Exception) {
			}
			vm.exportMod(uri, vm.exportEverything)
		}
	}

	LaunchedEffect(vm.statusMessage) {
		vm.statusMessage?.let {
			snackbar.showSnackbar(it)
			vm.consumeStatus()
		}
	}

	ModalNavigationDrawer(
		drawerState = drawerState,
		drawerContent = {
			ModalDrawerSheet {
				ProjectDrawer(vm) { scope.launch { drawerState.close() } }
			}
		},
		modifier = modifier,
	) {
		Scaffold(
			topBar = {
				TopAppBar(
					title = { Text(vm.project?.project_name ?: "Wonder Studio") },
					navigationIcon = {
						IconButton(onClick = { scope.launch { drawerState.open() } }) {
							Icon(Icons.Default.Menu, contentDescription = "Project panel")
						}
					},
					actions = {
						IconButton(onClick = { vm.save() }) {
							Icon(WonderIcons.Save, contentDescription = "Save")
						}
						IconButton(onClick = { if (vm.running) vm.stopTest() else vm.launchTest() }) {
							Icon(
								if (vm.running) WonderIcons.Stop else Icons.Default.PlayArrow,
								contentDescription = if (vm.running) "Stop" else "Run",
								tint = if (vm.running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
							)
						}
						IconButton(onClick = { menuOpen = true }) {
							Icon(Icons.Default.MoreVert, contentDescription = "More")
						}
						DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
							DropdownMenuItem(
								text = { Text("Export mod") },
								leadingIcon = { Icon(WonderIcons.Upload, contentDescription = null) },
								onClick = { menuOpen = false; showExport = true },
							)
							DropdownMenuItem(
								text = { Text("Grant Wonder Maker file access") },
								leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
								onClick = { menuOpen = false; vm.grantWonderMakerFileAccess() },
							)
							DropdownMenuItem(
								text = { Text("Output logs") },
								leadingIcon = { Icon(WonderIcons.Terminal, contentDescription = null) },
								onClick = { menuOpen = false; showLogs = true },
							)
							DropdownMenuItem(
								text = { Text("Close project") },
								leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
								onClick = { menuOpen = false; vm.closeProject() },
							)
						}
					},
				)
			},
			snackbarHost = { SnackbarHost(snackbar) },
			bottomBar = {
				NavigationBar {
					EditorTab.entries.forEach { item ->
						NavigationBarItem(
							selected = tab == item,
							onClick = { tab = item },
							icon = { Icon(item.icon, contentDescription = item.label) },
							label = { Text(item.label) },
						)
					}
				}
			},
		) { padding ->
			Column(Modifier.fillMaxSize().padding(padding)) {
				when (tab) {
					EditorTab.SCENE -> {
						if (vm.currentElement == null) {
							EmptyHint("Open a theme, character or object from the project panel (top-left).")
						} else {
							SceneCanvas(
								element = vm.currentElement,
								selectedComponent = vm.selectedComponent,
								revision = vm.revision,
								assets = vm.assets,
								onSelect = { vm.selectComponent(it) },
								onMove = { component, dx, dy -> vm.moveComponentPosition(component, dx, dy) },
								modifier = Modifier.fillMaxSize(),
							)
						}
					}

					EditorTab.TREE -> HierarchyPanel(vm, Modifier.fillMaxSize())
					EditorTab.INSPECTOR -> InspectorPanel(vm, Modifier.fillMaxSize())
					EditorTab.ANIMS -> AnimationsPanel(vm, Modifier.fillMaxSize())
					EditorTab.CODE -> {
						val element = vm.currentElement
						if (element?.script == null) {
							EmptyHint("Themes have no script. Open a character or object.")
						} else {
							WonderScriptEditor(
								initialText = element.script ?: "",
								resetKey = element,
								onTextChange = { vm.updateScript(it) },
								modifier = Modifier.fillMaxSize().padding(10.dp),
							)
						}
					}

					EditorTab.FILES -> FilesPanel(vm, Modifier.fillMaxSize())
				}
			}
		}
	}

	if (showExport) {
		ExportDialog(vm, onDismiss = { showExport = false }, onExport = {
			exportPicker.launch(null)
			showExport = false
		})
	}

	if (showLogs) {
		LogsDialog(vm, onDismiss = { showLogs = false })
	}
}

@Composable
private fun EmptyHint(text: String) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		modifier = Modifier.fillMaxWidth().padding(24.dp),
	) {
		Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
		Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
	}
}

// ---------------------------------------------------------------------------
// Project panel (drawer)
// ---------------------------------------------------------------------------

@Composable
private fun ProjectDrawer(vm: EditorViewModel, onAction: () -> Unit) {
	@Suppress("UNUSED_VARIABLE")
	val revision = vm.revision
	val project = vm.project
	var showStyleAdd by remember { mutableStateOf(false) }
	var styleRename by remember { mutableStateOf<String?>(null) }
	var styleRemove by remember { mutableStateOf<String?>(null) }
	var showDownload by remember { mutableStateOf(false) }
	var showSettings by remember { mutableStateOf(false) }
	val selectedStyle = vm.currentStyleName
	val style = vm.currentStyle

	Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Text(
				project?.project_name ?: "",
				style = MaterialTheme.typography.titleLarge,
				modifier = Modifier.weight(1f),
			)
			IconButton(onClick = { showSettings = true }) {
				Icon(Icons.Default.Edit, contentDescription = "Project settings")
			}
		}
		Text(
			"by ${project?.author_name.orEmpty()} · v${project?.project_version.orEmpty()}",
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		Text(
			"Wonder Studio ${com.manzft.wonderstudio.model.Defaults.ENGINE_VERSION}",
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.tertiary,
		)
		if (vm.modLoaded) {
			Text("Mod imported (saving disabled)", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
		}

		SectionHeader("Styles")
		project?.styles?.forEachIndexed { index, current ->
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.background(
						if (current.name == selectedStyle) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
						else MaterialTheme.colorScheme.surface,
						RoundedCornerShape(8.dp),
					)
					.clickable { vm.selectStyle(current.name); onAction() }
					.padding(start = 12.dp),
			) {
				Text(current.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
				ItemMenu(
					canMoveUp = index > 0,
					canMoveDown = index < project.styles.size - 1,
					onRename = { styleRename = current.name },
					onDuplicate = { vm.duplicateStyle(current) },
					onMoveUp = { vm.moveStyle(index, -1) },
					onMoveDown = { vm.moveStyle(index, 1) },
					onRemove = { styleRemove = current.name },
				)
			}
		}
		TextButton(onClick = { showStyleAdd = true }, modifier = Modifier.padding(top = 4.dp)) {
			Icon(Icons.Default.Add, contentDescription = null)
			Text("Add style", modifier = Modifier.padding(start = 6.dp))
		}

		if (style != null) {
			SectionHeader("Elements")
			Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
				ElementType.entries.forEach { type ->
					FilterChip(
						selected = vm.currentElementType == type,
						onClick = { vm.selectElementType(type) },
						label = { Text(type.key.replaceFirstChar { it.uppercase() }) },
					)
				}
			}
			vm.currentElementType?.let { type -> ElementList(vm, type, onAction) }
		}

		SectionHeader("Repository")
		Button(
			onClick = { showDownload = true },
			enabled = !vm.syncing,
			modifier = Modifier.fillMaxWidth(),
		) {
			Icon(WonderIcons.Download, contentDescription = null)
			Text(if (vm.syncing) "Downloading…" else "Download main branch", modifier = Modifier.padding(start = 8.dp))
		}
		if (vm.syncing) {
			DownloadProgress(vm)
		}
	}

	if (showStyleAdd) {
		FieldsDialog(
			title = "Create style",
			message = "Enter the style info",
			labels = listOf("Name"),
			onDismiss = { showStyleAdd = false },
			onConfirm = { values ->
				val name = values.firstOrNull().orEmpty()
				if (name.isNotBlank()) vm.addStyle(name)
				showStyleAdd = false
			},
		)
	}
	styleRename?.let { name ->
		val target = vm.project?.styles?.firstOrNull { it.name == name }
		if (target != null) {
			FieldsDialog(
				title = "Rename style",
				message = "Fill the style info",
				labels = listOf("Style name"),
				initial = listOf(target.name),
				onDismiss = { styleRename = null },
				onConfirm = { values ->
					vm.renameStyle(target, values.firstOrNull().orEmpty())
					styleRename = null
				},
			)
		}
	}
	styleRemove?.let { name ->
		val target = vm.project?.styles?.firstOrNull { it.name == name }
		if (target != null) {
			ConfirmDialog(
				title = "Remove style",
				message = "Remove \"${target.name}\"? This can't be undone.",
				onDismiss = { styleRemove = null },
				onConfirm = {
					vm.removeStyle(target)
					styleRemove = null
				},
			)
		}
	}
	if (showDownload) {
		ConfirmDialog(
			title = "Download main branch",
			message = "This DELETES everything in the current project and replaces it with the main branch of wonder-maker-fangame. Continue?",
			onDismiss = { showDownload = false },
			onConfirm = {
				showDownload = false
				vm.downloadMainBranch()
			},
		)
	}
	if (showSettings) {
		val current = project
		if (current != null) {
			FieldsDialog(
				title = "Project settings",
				message = "Edit the project info",
				labels = listOf("Project name", "Author", "Version"),
				initial = listOf(current.project_name, current.author_name, current.project_version),
				onDismiss = { showSettings = false },
				onConfirm = { values ->
					vm.updateProjectSettings(
						values.getOrNull(0).orEmpty(),
						values.getOrNull(1).orEmpty(),
						values.getOrNull(2).orEmpty(),
					)
					showSettings = false
				},
			)
		}
	}
}

@Composable
private fun ElementList(vm: EditorViewModel, type: ElementType, onAction: () -> Unit) {
	var showAdd by remember { mutableStateOf(false) }
	var renameTarget by remember { mutableStateOf<Element?>(null) }
	var removeTarget by remember { mutableStateOf<Element?>(null) }
	val elements = vm.currentElements

	Column {
		elements.forEachIndexed { index, element ->
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.background(
						if (vm.currentElement === element) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
						else MaterialTheme.colorScheme.surface,
						RoundedCornerShape(8.dp),
					)
					.clickable { vm.openElement(type, element.name); onAction() }
					.padding(start = 12.dp),
			) {
				Text(element.name, modifier = Modifier.weight(1f))
				ItemMenu(
					canMoveUp = index > 0,
					canMoveDown = index < elements.size - 1,
					onRename = { renameTarget = element },
					onDuplicate = { vm.duplicateElement(type, element) },
					onMoveUp = { vm.moveElement(type, index, -1) },
					onMoveDown = { vm.moveElement(type, index, 1) },
					onRemove = { removeTarget = element },
				)
			}
		}
		TextButton(onClick = { showAdd = true }, modifier = Modifier.padding(top = 4.dp)) {
			Icon(Icons.Default.Add, contentDescription = null)
			Text("Add ${type.singular}", modifier = Modifier.padding(start = 6.dp))
		}
	}

	if (showAdd) {
		FieldsDialog(
			title = "Create ${type.singular}",
			message = "Enter the ${type.singular} info",
			labels = listOf("Name"),
			onDismiss = { showAdd = false },
			onConfirm = { values ->
				val name = values.firstOrNull().orEmpty()
				if (name.isNotBlank()) vm.addElement(type, name)
				showAdd = false
			},
		)
	}
	renameTarget?.let { element ->
		FieldsDialog(
			title = "Rename ${type.singular}",
			message = "Fill the ${type.singular} info",
			labels = listOf("Name"),
			initial = listOf(element.name),
			onDismiss = { renameTarget = null },
			onConfirm = { values ->
				vm.renameElement(type, element, values.firstOrNull().orEmpty())
				renameTarget = null
			},
		)
	}
	removeTarget?.let { element ->
		ConfirmDialog(
			title = "Remove ${type.singular}",
			message = "Remove \"${element.name}\"? This can't be undone.",
			onDismiss = { removeTarget = null },
			onConfirm = {
				vm.removeElement(type, element)
				removeTarget = null
			},
		)
	}
}

@Composable
private fun SectionHeader(text: String) {
	Text(
		text,
		style = MaterialTheme.typography.titleMedium,
		color = MaterialTheme.colorScheme.tertiary,
		modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
	)
}

// ---------------------------------------------------------------------------
// Export
// ---------------------------------------------------------------------------

@Composable
private fun ExportDialog(vm: EditorViewModel, onDismiss: () -> Unit, onExport: () -> Unit) {
	val project = vm.project
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Export mod") },
		text = {
			Column(Modifier.height(420.dp).verticalScroll(rememberScrollState())) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Switch(checked = vm.exportEverything, onCheckedChange = { vm.exportEverything = it })
					Text("Export everything", modifier = Modifier.padding(start = 8.dp))
				}
				if (!vm.exportEverything) {
					project?.styles?.forEach { style ->
						Text(style.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
						ElementType.entries.forEach { type ->
							style.elements(type).forEach { element ->
								val selected = vm.exportSelection[style.name]?.let { sel ->
									when (type) {
										ElementType.THEME -> element.name in sel.themes
										ElementType.CHARACTER -> element.name in sel.characters
										ElementType.OBJECT -> element.name in sel.objects
									}
								} ?: false
								Row(verticalAlignment = Alignment.CenterVertically) {
									Checkbox(
										checked = selected,
										onCheckedChange = { vm.toggleExportSelection(style.name, type, element.name) },
									)
									Text("${element.name} (${type.singular})")
								}
							}
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onExport) {
				Icon(WonderIcons.Upload, contentDescription = null)
				Text("Export", modifier = Modifier.padding(start = 6.dp))
			}
		},
		dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
	)
}

@Composable
private fun LogsDialog(vm: EditorViewModel, onDismiss: () -> Unit) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Output") },
		text = {
			Column(Modifier.height(360.dp).verticalScroll(rememberScrollState())) {
				if (vm.logs.isEmpty()) {
					Text("No logs yet. Run the test with the play button.")
				} else {
					vm.logs.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
				}
			}
		},
		confirmButton = {
			TextButton(onClick = { vm.clearLogs() }) {
				Icon(Icons.Default.Delete, contentDescription = null)
				Text("Clear", modifier = Modifier.padding(start = 6.dp))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Icon(Icons.Default.Check, contentDescription = null)
				Text("Close", modifier = Modifier.padding(start = 6.dp))
			}
		},
	)
}

// ---------------------------------------------------------------------------
// Descarga de la rama principal
// ---------------------------------------------------------------------------

@Composable
private fun DownloadProgress(vm: EditorViewModel) {
	Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
		if (vm.downloadProgress >= 0f) {
			LinearProgressIndicator(progress = { vm.downloadProgress }, modifier = Modifier.fillMaxWidth())
		} else {
			LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
		}
		val percent = if (vm.downloadProgress >= 0f) "${(vm.downloadProgress * 100).toInt()}%" else "…"
		val sizes = if (vm.downloadTotal > 0) {
			"${formatBytes(vm.downloadBytes)} / ${formatBytes(vm.downloadTotal)}"
		} else {
			formatBytes(vm.downloadBytes)
		}
		val speed = if (vm.downloadSpeed > 0f) " · ${formatBytes(vm.downloadSpeed.toLong())}/s" else ""
		val eta = if (vm.downloadEta >= 0) " · ETA ${formatEta(vm.downloadEta)}" else ""
		Text(
			"$percent · $sizes$speed$eta",
			style = MaterialTheme.typography.labelMedium,
			modifier = Modifier.padding(top = 8.dp),
		)
		if (vm.downloadFile.isNotEmpty()) {
			Text(
				vm.downloadFile,
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}
	}
}

private fun formatBytes(bytes: Long): String {
	val kb = 1024.0
	val mb = kb * 1024.0
	val gb = mb * 1024.0
	return when {
		bytes >= gb -> String.format(java.util.Locale.US, "%.2f GB", bytes / gb)
		bytes >= mb -> String.format(java.util.Locale.US, "%.1f MB", bytes / mb)
		bytes >= kb -> String.format(java.util.Locale.US, "%.0f KB", bytes / kb)
		else -> "$bytes B"
	}
}

private fun formatEta(seconds: Long): String =
	if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
