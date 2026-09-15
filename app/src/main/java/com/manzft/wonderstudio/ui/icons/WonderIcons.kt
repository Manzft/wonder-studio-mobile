package com.manzft.wonderstudio.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Iconos materiales vendorizados (subset de @material-design-icons/svg, Apache-2.0).
// Se generan acá para no depender de material-icons-extended (~35 MB) y acelerar el build/R8.
object WonderIcons {

	val AccountTree: ImageVector by lazy { build("AccountTree", listOf(VPath("M22 11V3h-7v3H9V3H2v8h7V8h2v10h4v3h7v-8h-7v3h-2V8h2v3z", false))) }
	val Animation: ImageVector by lazy { build("Animation", listOf(VPath("M15 2c-2.71 0-5.05 1.54-6.22 3.78a7.062 7.062 0 0 0-3 3A7.014 7.014 0 0 0 2 15c0 3.87 3.13 7 7 7 2.71 0 5.05-1.54 6.22-3.78a7.062 7.062 0 0 0 3-3A7.014 7.014 0 0 0 22 9c0-3.87-3.13-7-7-7zM9 20a5.002 5.002 0 0 1-4-8c0 3.87 3.13 7 7 7-.84.63-1.88 1-3 1zm3-3a5.002 5.002 0 0 1-4-8c0 3.86 3.13 6.99 7 7-.84.63-1.88 1-3 1zm4.7-3.3c-.53.19-1.1.3-1.7.3-2.76 0-5-2.24-5-5 0-.6.11-1.17.3-1.7.53-.19 1.1-.3 1.7-.3 2.76 0 5 2.24 5 5 0 .6-.11 1.17-.3 1.7zM19 12c0-3.86-3.13-6.99-7-7a5.002 5.002 0 0 1 7 7z", false))) }
	val ArrowDownward: ImageVector by lazy { build("ArrowDownward", listOf(VPath("m20 12-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z", false))) }
	val ArrowUpward: ImageVector by lazy { build("ArrowUpward", listOf(VPath("m4 12 1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z", false))) }
	val Code: ImageVector by lazy { build("Code", listOf(VPath("M9.4 16.6 4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0 4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z", false))) }
	val ContentCopy: ImageVector by lazy { build("ContentCopy", listOf(VPath("M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z", false))) }
	val Folder: ImageVector by lazy { build("Folder", listOf(VPath("M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z", false))) }
	val FolderOpen: ImageVector by lazy { build("FolderOpen", listOf(VPath("M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z", false))) }
	val Pause: ImageVector by lazy { build("Pause", listOf(VPath("M6 19h4V5H6v14zm8-14v14h4V5h-4z", false))) }
	val Save: ImageVector by lazy { build("Save", listOf(VPath("M17 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z", false))) }
	val Stop: ImageVector by lazy { build("Stop", listOf(VPath("M6 6h12v12H6z", false))) }
	val Terminal: ImageVector by lazy { build("Terminal", listOf(VPath("M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16c1.1 0 2-.9 2-2V6a2 2 0 0 0-2-2zm0 14H4V8h16v10zm-2-1h-6v-2h6v2zM7.5 17l-1.41-1.41L8.67 13l-2.59-2.59L7.5 9l4 4-4 4z", false))) }
	val Tune: ImageVector by lazy { build("Tune", listOf(VPath("M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z", false))) }
	val Upload: ImageVector by lazy { build("Upload", listOf(VPath("M5 20h14v-2H5v2zm0-10h4v6h6v-6h4l-7-7-7 7z", false))) }
	val Image: ImageVector by lazy { build("Image", listOf(VPath("M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z", false))) }
	val CenterFocusStrong: ImageVector by lazy { build("CenterFocusStrong", listOf(VPath("M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm-7 7H3v4c0 1.1.9 2 2 2h4v-2H5v-4zM5 5h4V3H5c-1.1 0-2 .9-2 2v4h2V5zm14-2h-4v2h4v4h2V5c0-1.1-.9-2-2-2zm0 16h-4v2h4c1.1 0 2-.9 2-2v-4h-2v4z", false))) }
	val OpenWith: ImageVector by lazy { build("OpenWith", listOf(VPath("M10 9h4V6h3l-5-5-5 5h3v3zm-1 1H6V7l-5 5 5 5v-3h3v-4zm14 2-5-5v3h-3v4h3v3l5-5zm-9 3h-4v3H7l5 5 5-5h-3v-3z", false))) }
	val PanTool: ImageVector by lazy { build("PanTool", listOf(VPath("M23 5.5V20c0 2.2-1.8 4-4 4h-7.3c-1.08 0-2.1-.43-2.85-1.19L1 14.83s1.26-1.23 1.3-1.25c.22-.19.49-.29.79-.29.22 0 .42.06.6.16.04.01 4.31 2.46 4.31 2.46V4c0-.83.67-1.5 1.5-1.5S11 3.17 11 4v7h1V1.5c0-.83.67-1.5 1.5-1.5S15 .67 15 1.5V11h1V2.5c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5V11h1V5.5c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5z", false))) }
	val ZoomIn: ImageVector by lazy { build("ZoomIn", listOf(VPath("M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z", false), VPath("M12 10h-2v2H9v-2H7V9h2V7h1v2h2v1z", false))) }
	val ZoomOut: ImageVector by lazy { build("ZoomOut", listOf(VPath("M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14zM7 9h5v1H7z", false))) }
	val Extension: ImageVector by lazy { build("Extension", listOf(VPath("M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5a2.5 2.5 0 0 0-5 0V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5a2.5 2.5 0 0 0 0-5z", false))) }
}

private data class VPath(val d: String, val evenOdd: Boolean)

private fun build(name: String, paths: List<VPath>): ImageVector {
	val builder = ImageVector.Builder(
		name = name,
		defaultWidth = 24.dp,
		defaultHeight = 24.dp,
		viewportWidth = 24f,
		viewportHeight = 24f,
	)
	for (p in paths) {
		builder.addPath(
			pathData = addPathNodes(p.d),
			pathFillType = if (p.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
			fill = SolidColor(Color.Black),
		)
	}
	return builder.build()
}
