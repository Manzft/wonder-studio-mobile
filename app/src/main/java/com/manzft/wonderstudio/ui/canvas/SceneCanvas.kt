package com.manzft.wonderstudio.ui.canvas

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.manzft.wonderstudio.data.AssetRepository
import com.manzft.wonderstudio.model.Component
import com.manzft.wonderstudio.model.Element
import com.manzft.wonderstudio.model.bool
import com.manzft.wonderstudio.model.number
import com.manzft.wonderstudio.model.string
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val MIN_SCALE = 0.1f
private const val MAX_SCALE = 8f

enum class CanvasMode { PAN, MOVE }

// Previsualización del elemento actual sobre el lienzo: sprites, animaciones,
// colliders/areas y raycasts, con pan/zoom y arrastre del componente elegido.
@Composable
fun SceneCanvas(
	element: Element?,
	selectedComponent: Component?,
	revision: Int,
	assets: AssetRepository,
	onSelect: (String?) -> Unit,
	onMove: (Component, Float, Float) -> Unit,
	modifier: Modifier = Modifier,
) {
	var scale by remember { mutableFloatStateOf(1f) }
	var offset by remember { mutableStateOf(Offset.Zero) }
	var mode by remember { mutableStateOf(CanvasMode.PAN) }
	var canvasSize by remember { mutableStateOf(IntSize.Zero) }

	// Decodifica (con caché) los bitmaps usados por los componentes del elemento
	val bitmaps by produceState<Map<String, Bitmap>>(initialValue = emptyMap(), element, revision) {
		value = withContext(Dispatchers.IO) {
			val result = mutableMapOf<String, Bitmap>()
			element?.components?.forEach { component ->
				val paths = mutableListOf(component.settings.string("texture"))
				component.animations.forEach { paths.add(it.settings.string("texture")) }
				paths.filter { it.isNotBlank() }.forEach { path ->
					assets.bitmap(path)?.let { result[path] = it }
				}
			}
			result
		}
	}

	val renderables = remember(element, bitmaps, revision) {
		element?.components?.mapNotNull { buildRenderable(it, bitmaps) } ?: emptyList()
	}

	Box(
		modifier = modifier
			.background(Color(0xFF101018))
			.onSizeChanged { canvasSize = it }
			.pointerInput(element, bitmaps, revision, mode, scale, selectedComponent?.name) {
				awaitEachGesture {
					val down = awaitFirstDown(requireUnconsumed = false)
					var moved = false
					while (true) {
						val event = awaitPointerEvent()
						val pressed = event.changes.filter { it.pressed }
						if (pressed.isEmpty()) break
						if (pressed.size >= 2) {
							val zoom = event.calculateZoom()
							val pan = event.calculatePan()
							if (zoom != 1f) scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
							if (pan != Offset.Zero) offset += pan
							pressed.forEach { it.consume() }
							moved = true
						} else {
							val change = pressed.first()
							val delta = change.positionChange()
							if (delta.getDistance() > 0.5f) {
								moved = true
								val target = selectedComponent
								if (mode == CanvasMode.MOVE && target != null) {
									onMove(target, delta.x / scale, delta.y / scale)
								} else {
									offset += delta
								}
								change.consume()
							}
						}
					}
					if (!moved) {
						val design = screenToDesign(down.position, canvasSize, offset, scale)
						onSelect(hitTest(renderables, design)?.component?.name)
					}
				}
			},
	) {
		Canvas(Modifier.fillMaxSize()) {
			val center = Offset(size.width / 2f, size.height / 2f)
			val origin = center + offset

			// Guía de grid (80px por celda) y bounds del grid_size
			val gridColor = Color(0x22FFFFFF)
			for (i in -20..20) {
				val x = origin.x + i * 80f * scale
				drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
				val y = origin.y + i * 80f * scale
				drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
			}
			drawCircle(Color(0x66FFD93B), radius = 3f * scale, center = origin)

			renderables.forEach { item ->
				if (!item.visible) return@forEach
				val itemCenter = origin + Offset(item.position.x * scale, item.position.y * scale)
				when (item.kind) {
					"collider", "area" -> {
						val w = item.size.x * scale
						val h = item.size.y * scale
						val color = if (item.kind == "collider") Color(0xAA4ADE80) else Color(0xAA60A5FA)
						val fill = if (item.deactivated) Color(0x224ADE80) else Color(0x33444444)
						drawRect(
							color = fill,
							topLeft = Offset(itemCenter.x - w / 2f, itemCenter.y - h / 2f),
							size = Size(w, h),
						)
						drawRect(
							color = color,
							topLeft = Offset(itemCenter.x - w / 2f, itemCenter.y - h / 2f),
							size = Size(w, h),
							style = Stroke(width = 2f),
						)
					}

					"raycast" -> {
						val dirLength = kotlin.math.sqrt(item.dir.x * item.dir.x + item.dir.y * item.dir.y)
						if (dirLength > 0.0001f) {
							val angle = atan2(item.dir.y, item.dir.x)
							val end = Offset(
								itemCenter.x + cos(angle) * item.length * scale,
								itemCenter.y + sin(angle) * item.length * scale,
							)
							drawLine(Color(0xAAFB923C), itemCenter, end, strokeWidth = 3f)
							drawCircle(Color(0xFFFB923C), radius = 6f, center = end)
						} else {
							drawCircle(Color(0xFFFB923C), radius = 5f, center = itemCenter)
						}
					}

					else -> {
						val bitmap = item.bitmap ?: return@forEach
						val image = bitmap.asImageBitmap()
						val src = item.src ?: IntRect(0, 0, bitmap.width, bitmap.height)
						// flip resolviendo el rect de origen
						val srcX = if (item.flipX) src.right - src.width else src.left
						val srcY = if (item.flipY) src.bottom - src.height else src.top
						val srcConst = IntRect(srcX, srcY, srcX + src.width, srcY + src.height)
						val drawW = (src.width * item.drawScale.x * scale).toInt().coerceAtLeast(1)
						val drawH = (src.height * item.drawScale.y * scale).toInt().coerceAtLeast(1)
						drawImage(
							image = image,
							srcOffset = IntOffset(srcConst.left, srcConst.top),
							srcSize = IntSize(src.width, src.height),
							dstOffset = IntOffset(
								(itemCenter.x - drawW / 2f).toInt(),
								(itemCenter.y - drawH / 2f).toInt(),
							),
							dstSize = IntSize(drawW, drawH),
							alpha = item.alpha,
							colorFilter = item.colorFilter,
						)
					}
				}

				if (item.component === selectedComponent) {
					val bounds = itemBounds(item, scale)
					drawRect(
						color = Color(0xFFFFD93B),
						topLeft = origin + bounds.first,
						size = bounds.second,
						style = Stroke(width = 2f),
					)
				}
			}
		}

		Column(
			modifier = Modifier
				.align(Alignment.BottomEnd)
				.padding(10.dp),
			verticalArrangement = Arrangement.spacedBy(6.dp),
			horizontalAlignment = Alignment.End,
		) {
			Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
				Button(onClick = { mode = if (mode == CanvasMode.MOVE) CanvasMode.PAN else CanvasMode.MOVE }) {
					Text(if (mode == CanvasMode.MOVE) "Move" else "Pan")
				}
				Button(onClick = { scale = (scale * 1.25f).coerceAtMost(MAX_SCALE) }) { Text("+") }
				Button(onClick = { scale = (scale / 1.25f).coerceAtLeast(MIN_SCALE) }) { Text("-") }
				Button(onClick = { scale = 1f; offset = Offset.Zero }) { Text("Reset") }
			}
		}
	}
}

private data class Renderable(
	val component: Component,
	val kind: String,
	val position: Offset,
	val bitmap: Bitmap?,
	val src: IntRect?,
	val drawScale: Offset,
	val flipX: Boolean,
	val flipY: Boolean,
	val alpha: Float,
	val colorFilter: ColorFilter?,
	val visible: Boolean,
	val size: Offset,
	val dir: Offset,
	val length: Float,
	val deactivated: Boolean,
)

private fun buildRenderable(component: Component, bitmaps: Map<String, Bitmap>): Renderable? {
	val settings = component.settings
	val position = Offset(settings.number("position_x"), settings.number("position_y"))
	val visible = settings.bool("visible", true)
	val flipX = settings.bool("flipx")
	val flipY = settings.bool("flipy")

	fun tint(): Pair<Float, ColorFilter?> {
		val alpha = (settings.number("color_a", 255f) / 255f).coerceIn(0f, 1f)
		val r = (settings.number("color_r", 255f) / 255f).coerceIn(0f, 1f)
		val g = (settings.number("color_g", 255f) / 255f).coerceIn(0f, 1f)
		val b = (settings.number("color_b", 255f) / 255f).coerceIn(0f, 1f)
		val filter = if (r == 1f && g == 1f && b == 1f) null
		else ColorFilter.tint(Color(r, g, b, 1f), BlendMode.Modulate)
		return alpha to filter
	}

	return when (component.type) {
		"sprite", "sprite_parallax" -> {
			val path = settings.string("texture")
			val bitmap = bitmaps[path]
			val hframes = settings.number("hframes", 1f).toInt().coerceAtLeast(1)
			val vframes = settings.number("vframes", 1f).toInt().coerceAtLeast(1)
			val frame = settings.number("frame", 0f).toInt()
			val (alpha, filter) = tint()
			Renderable(
				component = component,
				kind = component.type,
				position = position,
				bitmap = bitmap,
				src = srcRect(bitmap, hframes, vframes, frame),
				drawScale = Offset(settings.number("scale_x", 1f), settings.number("scale_y", 1f)),
				flipX = flipX,
				flipY = flipY,
				alpha = alpha,
				colorFilter = filter,
				visible = visible,
				size = Offset.Zero,
				dir = Offset.Zero,
				length = 0f,
				deactivated = false,
			)
		}

		"animated_sprite" -> {
			val animation = component.currentAnimation ?: return null
			val path = animation.settings.string("texture")
			val bitmap = bitmaps[path]
			val hframes = animation.settings.number("hframes", 1f).toInt().coerceAtLeast(1)
			val vframes = animation.settings.number("vframes", 1f).toInt().coerceAtLeast(1)
			val frame = animation.settings.number("frame", 0f).toInt()
			val (alpha, filter) = tint()
			val offsetX = animation.settings.number("offset_x", 0f)
			val offsetY = animation.settings.number("offset_y", 0f)
			Renderable(
				component = component,
				kind = "animated_sprite",
				position = Offset(position.x + offsetX, position.y + offsetY),
				bitmap = bitmap,
				src = srcRect(bitmap, hframes, vframes, frame),
				drawScale = Offset(settings.number("scale_x", 1f), settings.number("scale_y", 1f)),
				flipX = flipX,
				flipY = flipY,
				alpha = alpha,
				colorFilter = filter,
				visible = visible,
				size = Offset.Zero,
				dir = Offset.Zero,
				length = 0f,
				deactivated = false,
			)
		}

		"collider", "area" -> Renderable(
			component = component,
			kind = component.type,
			position = position,
			bitmap = null,
			src = null,
			drawScale = Offset(1f, 1f),
			flipX = false,
			flipY = false,
			alpha = 1f,
			colorFilter = null,
			visible = true,
			size = Offset(settings.number("size_x", 40f), settings.number("size_y", 40f)),
			dir = Offset.Zero,
			length = 0f,
			deactivated = settings.bool("deactivated"),
		)

		"raycast" -> Renderable(
			component = component,
			kind = "raycast",
			position = position,
			bitmap = null,
			src = null,
			drawScale = Offset(1f, 1f),
			flipX = false,
			flipY = false,
			alpha = 1f,
			colorFilter = null,
			visible = true,
			size = Offset.Zero,
			dir = Offset(settings.number("dirx"), settings.number("diry", -1f)),
			length = settings.number("length", 80f),
			deactivated = false,
		)

		else -> null
	}
}

private fun srcRect(bitmap: Bitmap?, hframes: Int, vframes: Int, frame: Int): IntRect? {
	if (bitmap == null) return null
	val cellWidth = bitmap.width / hframes.coerceAtLeast(1)
	val cellHeight = bitmap.height / vframes.coerceAtLeast(1)
	val total = hframes * vframes
	val index = ((frame % total) + total) % total
	val column = index % hframes
	val row = index / hframes
	return IntRect(column * cellWidth, row * cellHeight, (column + 1) * cellWidth, (row + 1) * cellHeight)
}

// Bounds en pantalla del componente seleccionado (para el outline)
private fun itemBounds(item: Renderable, scale: Float): Pair<Offset, Size> {
	val size: Size = when {
		item.bitmap != null && item.src != null ->
			Size(
				item.src.width * item.drawScale.x * scale,
				item.src.height * item.drawScale.y * scale,
			)
		item.size != Offset.Zero -> Size(item.size.x * scale, item.size.y * scale)
		item.length > 0f -> Size(item.length * scale, item.length * scale)
		else -> Size(40f * scale, 40f * scale)
	}
	val center = Offset(item.position.x * scale, item.position.y * scale)
	return Offset(center.x - size.width / 2f, center.y - size.height / 2f) to size
}

private fun screenToDesign(position: Offset, canvasSize: IntSize, offset: Offset, scale: Float): Offset {
	val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
	return (position - center - offset) / scale
}

private fun hitTest(renderables: List<Renderable>, design: Offset): Renderable? {
	// De atrás hacia adelante (el último dibujado gana)
	return renderables.asReversed().firstOrNull { item ->
		if (!item.visible) return@firstOrNull false
		val halfW: Float
		val halfH: Float
		if (item.bitmap != null && item.src != null) {
			halfW = item.src.width * item.drawScale.x / 2f
			halfH = item.src.height * item.drawScale.y / 2f
		} else if (item.size != Offset.Zero) {
			halfW = item.size.x / 2f
			halfH = item.size.y / 2f
		} else {
			halfW = item.length / 2f
			halfH = item.length / 2f
		}
		val dx = design.x - item.position.x
		val dy = design.y - item.position.y
		kotlin.math.abs(dx) <= halfW && kotlin.math.abs(dy) <= halfH
	}
}
