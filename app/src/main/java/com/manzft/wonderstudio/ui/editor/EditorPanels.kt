package com.manzft.wonderstudio.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.manzft.wonderstudio.data.AssetRepository
import com.manzft.wonderstudio.model.Animation
import com.manzft.wonderstudio.model.Component
import com.manzft.wonderstudio.model.Defaults
import com.manzft.wonderstudio.model.Setting
import com.manzft.wonderstudio.model.int
import com.manzft.wonderstudio.model.string
import com.manzft.wonderstudio.ui.EditorViewModel
import com.manzft.wonderstudio.ui.RootSelection
import com.manzft.wonderstudio.ui.common.AssetBrowser
import com.manzft.wonderstudio.ui.common.ConfirmDialog
import com.manzft.wonderstudio.ui.common.FieldsDialog
import com.manzft.wonderstudio.ui.common.OptionPickerDialog
import com.manzft.wonderstudio.ui.common.PropertyEditor
import com.manzft.wonderstudio.ui.inspector.PropertySpecs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
// Jerarquía de componentes
// ---------------------------------------------------------------------------

@Composable
fun HierarchyPanel(vm: EditorViewModel, modifier: Modifier = Modifier) {
	var showAdd by remember { mutableStateOf(false) }
	var renameTarget by remember { mutableStateOf<Component?>(null) }
	var removeTarget by remember { mutableStateOf<Component?>(null) }
	@Suppress("UNUSED_VARIABLE")
	val revision = vm.revision
	val element = vm.currentElement
	val selected = vm.selectedComponent
	val index = selected?.let { element?.components?.indexOf(it) } ?: -1

	Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
		if (element == null) {
			Text("Open a theme, character or object first.")
			return@Column
		}

		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.fillMaxWidth()
				.background(
					if (vm.selectedRoot == RootSelection.ELEMENT) MaterialTheme.colorScheme.surfaceVariant
					else MaterialTheme.colorScheme.surface,
					RoundedCornerShape(6.dp),
				)
				.clickable { vm.selectElementRoot() }
				.padding(10.dp),
		) {
			Text("[${vm.currentElementType?.singular ?: ""}] ", color = MaterialTheme.colorScheme.tertiary)
			Text(element.name, style = MaterialTheme.typography.titleSmall)
		}

		element.components.forEachIndexed { i, component ->
			val isSelected = component === selected
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.background(
						if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
						else MaterialTheme.colorScheme.surface,
						RoundedCornerShape(6.dp),
					)
					.clickable { vm.selectComponent(component.name) }
					.padding(horizontal = 10.dp, vertical = 8.dp),
			) {
				Text("${i + 1}. ", style = MaterialTheme.typography.labelSmall)
				Text(component.name, modifier = Modifier.weight(1f))
				Text(component.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
		}

		Row(
			horizontalArrangement = Arrangement.spacedBy(4.dp),
			modifier = Modifier
				.fillMaxWidth()
				.horizontalScroll(rememberScrollState())
				.padding(top = 10.dp),
		) {
			Button(onClick = { showAdd = true }) { Text("Add") }
			Button(enabled = selected != null, onClick = { selected?.let(vm::duplicateComponent) }) { Text("Dup") }
			Button(enabled = selected != null, onClick = { renameTarget = selected }) { Text("Rename") }
			Button(enabled = selected != null, onClick = { removeTarget = selected }) { Text("Remove") }
			Button(enabled = index > 0, onClick = { vm.moveComponent(index, -1) }) { Text("Up") }
			Button(enabled = index >= 0 && index < element.components.size - 1, onClick = { vm.moveComponent(index, 1) }) { Text("Down") }
		}
	}

	if (showAdd) {
		OptionPickerDialog(
			title = "Add component",
			message = "Choose a component type",
			options = Defaults.COMPONENT_TYPES,
			onDismiss = { showAdd = false },
			onPick = { type ->
				vm.addComponent(type)
				showAdd = false
			},
		)
	}

	renameTarget?.let { target ->
		FieldsDialog(
			title = "Rename component",
			message = "Enter the new name",
			labels = listOf("Component name"),
			initial = listOf(target.name),
			onDismiss = { renameTarget = null },
			onConfirm = { values ->
				vm.renameComponent(target, values.firstOrNull().orEmpty())
				renameTarget = null
			},
		)
	}

	removeTarget?.let { target ->
		ConfirmDialog(
			title = "Remove component",
			message = "Remove \"${target.name}\"? This can't be undone.",
			onDismiss = { removeTarget = null },
			onConfirm = {
				vm.removeComponent(target)
				removeTarget = null
			},
		)
	}
}

// ---------------------------------------------------------------------------
// Inspector de propiedades
// ---------------------------------------------------------------------------

@Composable
fun InspectorPanel(vm: EditorViewModel, modifier: Modifier = Modifier) {
	@Suppress("UNUSED_VARIABLE")
	val revision = vm.revision
	val style = vm.currentStyle
	val element = vm.currentElement
	val component = vm.selectedComponent
	val elementType = vm.currentElementType

	Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
		when {
			vm.selectedRoot == RootSelection.STYLE && style != null -> {
				Text("Style: ${style.name}", style = MaterialTheme.typography.titleSmall)
				PropertyEditor(
					fields = PropertySpecs.style,
					settings = style.settings,
					assets = vm.assets,
					repository = vm.repository,
					onChanged = { key, value -> vm.updateStyleSetting(key, value) },
					onPlaySound = { vm.playSound(it) },
				)
			}

			vm.selectedRoot == RootSelection.ELEMENT && element != null && elementType != null -> {
				Text("${elementType.singular.replaceFirstChar { it.uppercase() }}: ${element.name}", style = MaterialTheme.typography.titleSmall)
				PropertyEditor(
					fields = PropertySpecs.forElementRoot(elementType),
					settings = element.settings,
					assets = vm.assets,
					repository = vm.repository,
					onChanged = { key, value -> vm.updateElementSetting(key, value) },
					onPlaySound = { vm.playSound(it) },
				)
			}

			component != null -> {
				Text("${component.name} (${component.type})", style = MaterialTheme.typography.titleSmall)
				PropertyEditor(
					fields = PropertySpecs.forComponent(component.type),
					settings = component.settings,
					assets = vm.assets,
					repository = vm.repository,
					onChanged = { key, value -> vm.updateComponentSetting(component, key, value) },
					onPlaySound = { vm.playSound(it) },
				)
			}

			else -> Text("Select a component or the root to edit its properties.")
		}
	}
}

// ---------------------------------------------------------------------------
// Animaciones
// ---------------------------------------------------------------------------

@Composable
fun AnimationsPanel(vm: EditorViewModel, modifier: Modifier = Modifier) {
	@Suppress("UNUSED_VARIABLE")
	val revision = vm.revision
	val component = vm.selectedComponent
	Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
		if (component == null || component.type != "animated_sprite") {
			Text("Select an animated_sprite component to manage its animations.")
			return@Column
		}

		var showAdd by remember { mutableStateOf(false) }
		var renameTarget by remember { mutableStateOf<Animation?>(null) }
		var removeTarget by remember { mutableStateOf<Animation?>(null) }
		val animation = component.animation(component.current_animation) ?: component.animations.firstOrNull()
		val index = animation?.let { component.animations.indexOf(it) } ?: -1

		Row(
			horizontalArrangement = Arrangement.spacedBy(6.dp),
			modifier = Modifier
				.fillMaxWidth()
				.horizontalScroll(rememberScrollState()),
		) {
			component.animations.forEach { item ->
				FilterChip(
					selected = animation === item,
					onClick = { vm.setCurrentAnimation(component, item.name) },
					label = { Text(item.name) },
				)
			}
		}

		Row(
			horizontalArrangement = Arrangement.spacedBy(4.dp),
			modifier = Modifier
				.fillMaxWidth()
				.horizontalScroll(rememberScrollState())
				.padding(vertical = 8.dp),
		) {
			Button(onClick = { showAdd = true }) { Text("Add") }
			Button(enabled = animation != null, onClick = { animation?.let { vm.duplicateAnimation(component, it) } }) { Text("Dup") }
			Button(enabled = animation != null, onClick = { renameTarget = animation }) { Text("Rename") }
			Button(enabled = animation != null, onClick = { removeTarget = animation }) { Text("Remove") }
			Button(enabled = index > 0, onClick = { vm.moveAnimation(component, index, -1) }) { Text("Up") }
			Button(enabled = index >= 0 && index < component.animations.size - 1, onClick = { vm.moveAnimation(component, index, 1) }) { Text("Down") }
		}

		if (animation == null) {
			Text("This component has no animations yet.")
		} else {
			AnimationPreview(vm, animation)
			PropertyEditor(
				fields = PropertySpecs.animation,
				settings = animation.settings,
				assets = vm.assets,
				repository = vm.repository,
				onChanged = { key, value -> vm.updateAnimationSetting(component, animation, key, value) },
				onPlaySound = { vm.playSound(it) },
			)
		}

		if (showAdd) {
			FieldsDialog(
				title = "Create animation",
				message = "Enter the animation info",
				labels = listOf("Name"),
				onDismiss = { showAdd = false },
				onConfirm = { values ->
					val name = values.firstOrNull().orEmpty()
					if (name.isNotBlank()) vm.addAnimation(component, name)
					showAdd = false
				},
			)
		}

		renameTarget?.let { target ->
			FieldsDialog(
				title = "Rename animation",
				message = "Enter the new name",
				labels = listOf("Animation name"),
				initial = listOf(target.name),
				onDismiss = { renameTarget = null },
				onConfirm = { values ->
					vm.renameAnimation(component, target, values.firstOrNull().orEmpty())
					renameTarget = null
				},
			)
		}

		removeTarget?.let { target ->
			ConfirmDialog(
				title = "Remove animation",
				message = "Remove \"${target.name}\"? This can't be undone.",
				onDismiss = { removeTarget = null },
				onConfirm = {
					vm.removeAnimation(component, target)
					removeTarget = null
				},
			)
		}
	}
}

@Composable
private fun AnimationPreview(vm: EditorViewModel, animation: Animation) {
	val path = animation.settings.string("texture")
	val bitmap by produceState<Bitmap?>(null, path) {
		value = withContext(Dispatchers.IO) { vm.assets.bitmap(path) }
	}
	val hframes = animation.settings.int("hframes", 1).coerceAtLeast(1)
	val vframes = animation.settings.int("vframes", 1).coerceAtLeast(1)
	val total = (hframes * vframes).coerceAtLeast(1)
	val start = animation.settings.int("start_frame", 0).coerceIn(0, total - 1)
	val endRaw = animation.settings.int("end_frame", 0)
	val end = if (endRaw <= 0) total - 1 else endRaw.coerceIn(start, total - 1)
	val fps = animation.settings.int("fps", 60).coerceAtLeast(1)
	var playing by remember(animation.name) { mutableStateOf(false) }
	var frame by remember(animation.name) { mutableIntStateOf(animation.settings.int("frame", 0).coerceIn(0, total - 1)) }

	LaunchedEffect(playing, animation.name) {
		if (!playing) return@LaunchedEffect
		while (true) {
			delay((1000L / fps).coerceAtLeast(16L))
			frame = if (frame >= end) start else frame + 1
		}
	}

	Column {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(160.dp)
				.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
			contentAlignment = Alignment.Center,
		) {
			if (bitmap == null) {
				Text("No texture")
			} else {
				val image = bitmap!!.asImageBitmap()
				val cellW = bitmap!!.width / hframes
				val cellH = bitmap!!.height / vframes
				val col = (frame % total) % hframes
				val row = (frame % total) / hframes
				Canvas(Modifier.fillMaxSize()) {
					val scale = minOf(
						size.width / cellW.coerceAtLeast(1),
						size.height / cellH.coerceAtLeast(1),
					).coerceAtMost(4f)
					val w = (cellW * scale).toInt()
					val h = (cellH * scale).toInt()
					drawImage(
						image = image,
						srcOffset = IntOffset(col * cellW, row * cellH),
						srcSize = IntSize(cellW, cellH),
						dstOffset = IntOffset(((size.width - w) / 2f).toInt(), ((size.height - h) / 2f).toInt()),
						dstSize = IntSize(w, h),
					)
				}
			}
		}

		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(6.dp),
			modifier = Modifier.padding(vertical = 8.dp),
		) {
			Button(onClick = {
				frame = start
				playing = true
			}) { Text("Play") }
			Button(onClick = { playing = false }) { Text("Pause") }
			Button(onClick = {
				playing = false
				frame = animation.settings.int("frame", 0).coerceIn(0, total - 1)
			}) { Text("Stop") }
			Text("Frame $frame / $end", style = MaterialTheme.typography.labelSmall)
		}
	}
}

// ---------------------------------------------------------------------------
// Assets
// ---------------------------------------------------------------------------

@Composable
fun FilesPanel(vm: EditorViewModel, modifier: Modifier = Modifier) {
	Column(modifier = modifier.padding(8.dp)) {
		AssetBrowser(
			repository = vm.repository,
			assets = vm.assets,
			onPlaySound = { vm.playSound(it) },
			onPick = { /* solo previsualización */ },
			modifier = Modifier.fillMaxWidth(),
		)
	}
}
