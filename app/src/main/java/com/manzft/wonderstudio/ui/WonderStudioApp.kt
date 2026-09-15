package com.manzft.wonderstudio.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manzft.wonderstudio.ui.editor.EditorScreen
import com.manzft.wonderstudio.ui.projects.ProjectsScreen

@Composable
fun WonderStudioApp() {
	val vm: EditorViewModel = viewModel()
	// La lectura de `project` suscribe la navegación al estado del proyecto
	val project = vm.project
	if (project == null) {
		ProjectsScreen(vm)
	} else {
		EditorScreen(vm)
	}
}
