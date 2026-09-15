package com.manzft.wonderstudio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta oscura del editor, inspirada en los colores de Wonder Studio
private val WonderColors = darkColorScheme(
	primary = Color(0xFF7C6CF0),
	onPrimary = Color.White,
	secondary = Color(0xFF5EC8B0),
	onSecondary = Color(0xFF10231E),
	tertiary = Color(0xFFFFD93B),
	onTertiary = Color(0xFF2A2200),
	background = Color(0xFF13121B),
	surface = Color(0xFF1D1C29),
	surfaceVariant = Color(0xFF2A2939),
	onBackground = Color(0xFFEDEDF5),
	onSurface = Color(0xFFEDEDF5),
	onSurfaceVariant = Color(0xFFC9C7DB),
	outline = Color(0xFF45435A),
	error = Color(0xFFFF5A6E),
)

@Composable
fun WonderStudioTheme(content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = WonderColors,
		content = content,
	)
}
