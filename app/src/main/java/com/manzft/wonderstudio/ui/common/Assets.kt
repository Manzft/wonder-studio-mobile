package com.manzft.wonderstudio.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.manzft.wonderstudio.data.AssetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Carga un asset de forma asíncrona y lo dibuja ocupando el contenedor.
@Composable
fun AssetImage(
	assets: AssetRepository,
	path: String,
	modifier: Modifier = Modifier,
	contentScale: ContentScale = ContentScale.Fit,
) {
	val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, path) {
		value = withContext(Dispatchers.IO) { assets.bitmap(path) }
	}
	bitmap?.let {
		Box(modifier) {
			Image(
				bitmap = it.asImageBitmap(),
				contentDescription = null,
				modifier = Modifier.fillMaxSize(),
				contentScale = contentScale,
			)
		}
	}
}
