package com.manzft.wonderstudio.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Wonder Script: cabecera obligatoria "wscript", resaltado y autocompletado.
object WonderScript {
	const val HEADER = "wscript"

	val BUILTINS = listOf(
		"Log", "CheckInput", "MoveX", "MoveY", "AddSpeedX", "AddSpeedY",
		"GetComponent", "Timer", "TimerOnce", "Random", "RandomInt", "GetTheme",
		"IsOnFloor", "GetObject", "StopLevel", "GetSpeedX", "GetSpeedY",
		"SetSpeedX", "SetSpeedY", "GetFriction", "SetFriction", "GetGravity",
		"SetGravity", "Absolute", "GetPositionX", "GetPositionY", "SetPositionX",
		"SetPositionY", "Instantiate", "Lerp", "SetGridSizeX", "SetGridSizeY",
		"GetGridSizeX", "GetGridSizeY", "GetLayer", "SetLayer",
		"GetDraggedObject", "GetDraggedObjectName", "EraseFromLevel", "TweenProperty",
	)

	val EVENTS = listOf(
		"create", "start", "frame", "themeChange", "animationFinished",
		"levelStopped", "drag", "drop", "draggedObject",
	)

	val KEYWORDS = listOf("if", "elif", "else", "end", "true", "false", "and", "or", "not")

	val INPUT_CODES = listOf("left", "right", "up", "down", "jump", "crouch", "spin", "run")

	val COMPONENT_PROPERTIES = listOf(
		"position_x", "position_y", "scale_x", "scale_y", "visible", "rotation",
		"hframes", "vframes", "frame", "color_r", "color_g", "color_b", "color_a",
		"size_x", "size_y", "dirx", "diry", "length", "deactivated",
	)

	val COMPONENT_METHODS = listOf(
		"Play", "Stop", "ChangeFPS", "SetFrame", "GetAnimation", "GetLastAnimation",
		"SetFlipX", "SetFlipY", "GetBody", "GetBodyName",
		"SetColorR", "SetColorG", "SetColorB", "SetColorA",
		"GetColorR", "GetColorG", "GetColorB", "GetColorA",
		"Hide", "Show", "SetRotation", "GetRotation", "Deactivate", "Activate",
	)
}

private data class CompletionCategory(val name: String, val tokens: List<String>, val withParen: Boolean = false, val eventStyle: Boolean = false)

private val CATEGORIES = listOf(
	CompletionCategory("Builtins", WonderScript.BUILTINS, withParen = true),
	CompletionCategory("Events", WonderScript.EVENTS, eventStyle = true),
	CompletionCategory("Keywords", WonderScript.KEYWORDS),
	CompletionCategory("Input", WonderScript.INPUT_CODES),
	CompletionCategory("Props", WonderScript.COMPONENT_PROPERTIES),
	CompletionCategory("Methods", WonderScript.COMPONENT_METHODS, withParen = true),
)

@Composable
fun WonderScriptEditor(
	initialText: String,
	resetKey: Any?,
	onTextChange: (String) -> Unit,
	modifier: Modifier = Modifier,
) {
	var field by remember(resetKey) { mutableStateOf(TextFieldValue(enforceHeader(initialText))) }
	var categoryIndex by remember { mutableStateOf(0) }

	Column(modifier = modifier) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(6.dp),
			modifier = Modifier
				.fillMaxWidth()
				.horizontalScroll(rememberScrollState()),
		) {
			CATEGORIES.forEachIndexed { index, category ->
				FilterChip(
					selected = categoryIndex == index,
					onClick = { categoryIndex = index },
					label = { Text(category.name) },
				)
			}
		}

		Row(
			horizontalArrangement = Arrangement.spacedBy(4.dp),
			modifier = Modifier
				.fillMaxWidth()
				.horizontalScroll(rememberScrollState())
				.padding(vertical = 6.dp),
		) {
			val category = CATEGORIES[categoryIndex]
			category.tokens.forEach { token ->
				TextButton(onClick = {
					field = insertToken(field, token, category)
					onTextChange(field.text)
				}) {
					Text(token, style = MaterialTheme.typography.labelMedium)
				}
			}
		}

		BasicTextField(
			value = field,
			onValueChange = { updated ->
				val enforced = enforceHeader(updated.text)
				field = if (enforced != updated.text) {
					TextFieldValue(enforced, selection = androidx.compose.ui.text.TextRange(enforced.length))
				} else {
					updated
				}
				onTextChange(field.text)
			},
			textStyle = TextStyle(
				fontFamily = FontFamily.Monospace,
				fontSize = 14.sp,
				color = Color(0xFFEDEDF5),
			),
			visualTransformation = WonderScriptHighlight(),
			modifier = Modifier
				.fillMaxWidth()
				.heightIn(min = 260.dp)
				.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
				.padding(10.dp),
		)
	}
}

private fun enforceHeader(text: String): String {
	val cleaned = text.replace("\r\n", "\n")
	val firstLine = cleaned.substringBefore('\n')
	return if (firstLine.trim() == WonderScript.HEADER) {
		cleaned
	} else {
		WonderScript.HEADER + "\n" + cleaned
	}
}

private fun insertToken(field: TextFieldValue, token: String, category: CompletionCategory): TextFieldValue {
	val insertion = when {
		category.eventStyle -> "$token()\n\t"
		category.withParen -> "$token("
		else -> token
	}
	val start = field.selection.min
	val end = field.selection.max
	val text = field.text.substring(0, start) + insertion + field.text.substring(end)
	val cursor = start + insertion.length
	return TextFieldValue(text, androidx.compose.ui.text.TextRange(cursor))
}

private class WonderScriptHighlight : VisualTransformation {
	override fun filter(text: AnnotatedString): TransformedText {
		val source = text.text
		val annotated = buildAnnotatedString {
			append(source)
			applyHighlighting(source)
		}
		return TransformedText(annotated, OffsetMapping.Identity)
	}

	private fun androidx.compose.ui.text.AnnotatedString.Builder.applyHighlighting(source: String) {
		var index = 0
		while (index < source.length) {
			val rest = source.substring(index)
			when {
				rest.startsWith("//") -> {
					val end = source.indexOf('\n', index).let { if (it < 0) source.length else it }
					addStyle(SpanStyle(color = Color(0xFF7A8090), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), index, end)
					index = end
				}

				source[index] == '"' -> {
					val end = source.indexOf('"', index + 1).let { if (it < 0) source.length else it + 1 }
					addStyle(SpanStyle(color = Color(0xFF7EE787)), index, end)
					index = end
				}

				source[index].isDigit() -> {
					var end = index
					while (end < source.length && (source[end].isDigit() || source[end] == '.')) end++
					addStyle(SpanStyle(color = Color(0xFFFFA657)), index, end)
					index = end
				}

				source[index].isLetter() || source[index] == '_' -> {
					var end = index
					while (end < source.length && (source[end].isLetterOrDigit() || source[end] == '_')) end++
					val word = source.substring(index, end)
					val color = when {
						word in WonderScript.KEYWORDS -> Color(0xFFD2A8FF)
						word in WonderScript.BUILTINS -> Color(0xFF79C0FF)
						word in WonderScript.EVENTS -> Color(0xFFFFD93B)
						else -> null
					}
					if (color != null) addStyle(SpanStyle(color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), index, end)
					index = end
				}

				else -> index++
			}
		}
	}
}
