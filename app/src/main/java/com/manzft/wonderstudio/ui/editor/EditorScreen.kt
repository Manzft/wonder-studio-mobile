package com.manzft.wonderstudio.ui.editor

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.manzft.wonderstudio.model.Element
import com.manzft.wonderstudio.model.ElementType
import com.manzft.wonderstudio.model.elements
import com.manzft.wonderstudio.ui.EditorViewModel
import com.manzft.wonderstudio.ui.RootSelection
import com.manzft.wonderstudio.ui.canvas.SceneCanvas
import com.manzft.wonderstudio.ui.common.ConfirmDialog
import com.manzft.wonderstudio.ui.common.FieldsDialog
import com.manzft.wonderstudio.ui.inspector.PropertySpecs
import kotlinx.coroutines.launch

private enum class EditorTab(val label: String) {
	SCENE("Scene"),
	TREE("Tree"),
	INSPECTOR("Inspector"),
	ANIMS("Anims"),
	CODE("Code"),
	FILES("Files"),
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
							Icon(Icons.Default.List, contentDescription = "Project")
						}
					},
					actions = {
						IconButton(onClick = { vm.save() }) {
							Icon(Icons.Default.Refresh, contentDescription = "Save")
						}
						IconButton(onClick = { if (vm.running) vm.stopTest() else vm.launchTest() }) {
							Icon(
								Icons.Default.PlayArrow,
								contentDescription = "Run",
								tint = if (vm.running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
							)
						}
						IconButton(onClick = { menuOpen = true }) {
							Icon(Icons.Default.MoreVert, contentDescription = "More")
						}
						DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
							DropdownMenuItem(text = { Text("Export mod") }, onClick = { menuOpen = false; showExport = true })
							DropdownMenuItem(text = { Text("Logs") }, onClick = { menuOpen = false; showLogs = true })
							DropdownMenuItem(text = { Text("Close project") }, onClick = { menuOpen = false; vm.closeProject() })
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
							icon = { Text(item.label.take(1)) },
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
							Text(
								"Abrí un tema, personaje u objeto desde el panel de proyecto (arriba a la izquierda).",
								modifier = Modifier.padding(16.dp),
							)
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
							Text("Los temas no tienen script. Abrí un personaje u objeto.", modifier = Modifier.padding(16.dp))
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

// ---------------------------------------------------------------------------
// Panel de proyecto (drawer)
// ---------------------------------------------------------------------------

@Composable
private fun ProjectDrawer(vm: EditorViewModel, onAction: () -> Unit) {
	@Suppress("UNUSED_VARIABLE")
	val revision = vm.revision
	val project = vm.project
	var showStyleAdd by remember { mutableStateOf(false) }
	var styleRename by remember { mutableStateOf<String?>(null) }
	var styleRemove by remember { mutableStateOf<String?>(null) }
	val selectedStyle = vm.currentStyleName
	val style = vm.currentStyle
	Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
		Text(project?.project_name ?: "", style = MaterialTheme.typography.titleLarge)
		Text(
			"by ${project?.author_name.orEmpty()} · v${project?.project_version.orEmpty()}",
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		if (vm.modLoaded) {
			Text("Mod importado (no se guarda)", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
		}

		SectionHeader("Styles")
		vm.project?.styles?.forEachIndexed { index, style ->
			val isCurrent = style.name == selectedStyle
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.background(
						if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
						RoundedCornerShape(6.dp),
					)
					.clickable { vm.selectStyle(style.name); onAction() }
					.padding(horizontal = 8.dp, vertical = 6.dp),
			) {
				Text(style.name, modifier = Modifier.weight(1f))
				TextButton(onClick = { vm.moveStyle(index, -1) }) { Text("↑") }
				TextButton(onClick = { vm.moveStyle(index, 1) }) { Text("↓") }
				IconButton(onClick = { vm.duplicateStyle(style) }) { Icon(Icons.Default.Add, contentDescription = "Duplicate") }
				IconButton(onClick = { styleRename = style.name }) { Icon(Icons.Default.Edit, contentDescription = "Rename") }
				IconButton(onClick = { styleRemove = style.name }) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
			}
		}
		TextButton(onClick = { showStyleAdd = true }) { Text("+ Add style") }

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
		val style = vm.project?.styles?.firstOrNull { it.name == name }
		if (style != null) {
			FieldsDialog(
				title = "Rename style",
				message = "Fill the style info",
				labels = listOf("Style name"),
				initial = listOf(style.name),
				onDismiss = { styleRename = null },
				onConfirm = { values ->
					vm.renameStyle(style, values.firstOrNull().orEmpty())
					styleRename = null
				},
			)
		}
	}
	styleRemove?.let { name ->
		val style = vm.project?.styles?.firstOrNull { it.name == name }
		if (style != null) {
			ConfirmDialog(
				title = "Remove a style",
				message = "Are you sure you want to remove the ${style.name} style? You can't undo this action.",
				onDismiss = { styleRemove = null },
				onConfirm = {
					vm.removeStyle(style)
					styleRemove = null
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
			val isCurrent = vm.currentElement === element
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.background(
						if (isCurrent) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
						RoundedCornerShape(6.dp),
					)
					.clickable { vm.openElement(type, element.name); onAction() }
					.padding(horizontal = 8.dp, vertical = 4.dp),
			) {
				Text(element.name, modifier = Modifier.weight(1f))
				TextButton(onClick = { vm.moveElement(type, index, -1) }) { Text("↑") }
				TextButton(onClick = { vm.moveElement(type, index, 1) }) { Text("↓") }
				IconButton(onClick = { vm.duplicateElement(type, element) }) { Icon(Icons.Default.Add, contentDescription = "Duplicate") }
				IconButton(onClick = { renameTarget = element }) { Icon(Icons.Default.Edit, contentDescription = "Rename") }
				IconButton(onClick = { removeTarget = element }) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
			}
		}
		TextButton(onClick = { showAdd = true }) { Text("+ Add ${type.singular}") }
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
			title = "Remove a ${type.singular}",
			message = "Are you sure you want to remove ${element.name}? You can't undo this action.",
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
		modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
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
					Text("Export all", modifier = Modifier.padding(start = 8.dp))
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
		confirmButton = { TextButton(onClick = onExport) { Text("Choose folder & export") } },
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
					Text("Sin logs todavía. Corré el test con el botón Play.")
				} else {
					vm.logs.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
				}
			}
		},
		confirmButton = { TextButton(onClick = { vm.clearLogs() }) { Text("Clear") } },
		dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
	)
}
