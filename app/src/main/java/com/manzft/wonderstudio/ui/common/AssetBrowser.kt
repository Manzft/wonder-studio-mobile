package com.manzft.wonderstudio.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.manzft.wonderstudio.data.AssetRepository
import com.manzft.wonderstudio.data.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "svg")
private val AUDIO_EXTENSIONS = setOf("wav", "mp3", "ogg")

private fun extensionOf(name: String): String = name.substringAfterLast('.', "").lowercase()

private fun isImage(name: String) = extensionOf(name) in IMAGE_EXTENSIONS
private fun isAudio(name: String) = extensionOf(name) in AUDIO_EXTENSIONS

private fun resolveDir(root: DocumentFile?, segments: List<String>): DocumentFile? {
	var dir = root ?: return null
	for (segment in segments) {
		dir = dir.listFiles().firstOrNull { it.isDirectory && it.name == segment } ?: return null
	}
	return dir
}

// Explorador de assets del proyecto: navega la carpeta `assets/` y permite
// elegir un archivo (para campos de tipo path).
@Composable
fun AssetBrowser(
	repository: ProjectRepository,
	assets: AssetRepository,
	onPlaySound: (String) -> Unit,
	onPick: (String) -> Unit,
	modifier: Modifier = Modifier,
) {
	var segments by remember { mutableStateOf(emptyList<String>()) }
	var refresh by remember { mutableStateOf(0) }
	val root = repository.projectAssetsRoot()
	val current = remember(root, segments, refresh) { resolveDir(root, segments) }
	val entries = remember(current, refresh) {
		current?.listFiles()?.sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() })
			?: emptyList()
	}

	Column(modifier) {
		Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
			IconButton(onClick = { segments = emptyList(); refresh++ }) {
				Icon(Icons.Default.Home, contentDescription = "Home")
			}
			IconButton(
				enabled = segments.isNotEmpty(),
				onClick = { if (segments.isNotEmpty()) segments = segments.dropLast(1); refresh++ },
			) {
				Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
			}
			Text(
				text = "/" + segments.joinToString("/"),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				style = MaterialTheme.typography.bodySmall,
				modifier = Modifier.padding(start = 8.dp),
			)
		}
		LazyVerticalGrid(
			columns = GridCells.Adaptive(minSize = 88.dp),
			horizontalArrangement = Arrangement.spacedBy(6.dp),
			verticalArrangement = Arrangement.spacedBy(6.dp),
			modifier = Modifier
				.fillMaxWidth()
				.height(320.dp),
		) {
			items(entries, key = { it.uri.toString() }) { file ->
				val name = file.name ?: return@items
				val relative = "/" + (segments + name).joinToString("/")
				AssetEntry(
					file = file,
					name = name,
					assets = assets,
					onClick = {
						if (file.isDirectory) {
							segments = segments + name
							refresh++
						} else {
							if (isAudio(name)) onPlaySound(relative)
							onPick(relative)
						}
					},
				)
			}
		}
	}
}

@Composable
private fun AssetEntry(
	file: DocumentFile,
	name: String,
	assets: AssetRepository,
	onClick: () -> Unit,
) {
	val bitmap by produceState<android.graphics.Bitmap?>(null, file.uri.toString()) {
		value = if (!file.isDirectory && isImage(name)) {
			withContext(Dispatchers.IO) { assets.bitmap(file.uri) }
		} else {
			null
		}
	}

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.clip(MaterialTheme.shapes.small)
			.background(MaterialTheme.colorScheme.surfaceVariant)
			.clickable(onClick = onClick)
			.padding(6.dp),
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(1f),
			contentAlignment = Alignment.Center,
		) {
			when {
				file.isDirectory -> Text("DIR", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
				bitmap != null -> Image(
					bitmap = bitmap!!.asImageBitmap(),
					contentDescription = null,
					modifier = Modifier.fillMaxSize(),
					contentScale = ContentScale.Fit,
				)
				isAudio(name) -> Icon(Icons.Default.PlayArrow, contentDescription = null)
				else -> Text("?", fontSize = 18.sp)
			}
		}
		Text(
			text = name,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			fontSize = 10.sp,
			modifier = Modifier.padding(top = 2.dp),
		)
	}
}
