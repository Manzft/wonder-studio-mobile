package com.manzft.wonderstudio.ui.common

import com.manzft.wonderstudio.ui.icons.WonderIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Menú de acciones reutilizable (renombrar, duplicar, mover, eliminar) para
// filas de estilos, elementos, componentes y animaciones.
@Composable
fun ItemMenu(
	canMoveUp: Boolean,
	canMoveDown: Boolean,
	onRename: () -> Unit,
	onDuplicate: (() -> Unit)? = null,
	onMoveUp: () -> Unit,
	onMoveDown: () -> Unit,
	onRemove: () -> Unit,
) {
	var open by remember { mutableStateOf(false) }
	IconButton(onClick = { open = true }) {
		Icon(Icons.Default.MoreVert, contentDescription = "Actions")
	}
	DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
		DropdownMenuItem(
			text = { Text("Rename") },
			leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
			onClick = { open = false; onRename() },
		)
		if (onDuplicate != null) {
			DropdownMenuItem(
				text = { Text("Duplicate") },
				leadingIcon = { Icon(WonderIcons.ContentCopy, contentDescription = null) },
				onClick = { open = false; onDuplicate() },
			)
		}
		DropdownMenuItem(
			text = { Text("Move up") },
			enabled = canMoveUp,
			leadingIcon = { Icon(WonderIcons.ArrowUpward, contentDescription = null) },
			onClick = { open = false; onMoveUp() },
		)
		DropdownMenuItem(
			text = { Text("Move down") },
			enabled = canMoveDown,
			leadingIcon = { Icon(WonderIcons.ArrowDownward, contentDescription = null) },
			onClick = { open = false; onMoveDown() },
		)
		DropdownMenuItem(
			text = { Text("Remove") },
			leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
			onClick = { open = false; onRemove() },
		)
	}
}
