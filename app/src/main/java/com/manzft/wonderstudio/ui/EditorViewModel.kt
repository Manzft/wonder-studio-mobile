package com.manzft.wonderstudio.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.manzft.wonderstudio.audio.AudioPreview
import com.manzft.wonderstudio.data.AppConfig
import com.manzft.wonderstudio.data.AssetRepository
import com.manzft.wonderstudio.data.ProjectRepository
import com.manzft.wonderstudio.data.RecentProject
import com.manzft.wonderstudio.data.RunLauncher
import com.manzft.wonderstudio.model.Animation
import com.manzft.wonderstudio.model.Component
import com.manzft.wonderstudio.model.Defaults
import com.manzft.wonderstudio.model.Element
import com.manzft.wonderstudio.model.ElementType
import com.manzft.wonderstudio.model.Project
import com.manzft.wonderstudio.model.Setting
import com.manzft.wonderstudio.model.Style
import com.manzft.wonderstudio.model.element
import com.manzft.wonderstudio.model.elements
import com.manzft.wonderstudio.model.int
import com.manzft.wonderstudio.model.string
import com.manzft.wonderstudio.net.UdpLogReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RootSelection { NONE, STYLE, ELEMENT }

class EditorViewModel(application: Application) : AndroidViewModel(application) {

	val config = AppConfig(application)
	val repository = ProjectRepository(application)
	val assets = AssetRepository(application, repository)
	private val audio = AudioPreview(application)
	private val udpReceiver = UdpLogReceiver { line ->
		viewModelScope.launch { logs.add(line) }
	}

	// Revisión general: cualquier mutación del proyecto la incrementa para que
	// Compose vuelva a leer el modelo (que es mutable por diseño).
	var revision by mutableIntStateOf(0)
		private set

	var project: Project? by mutableStateOf(null)
		private set

	var currentStyleName: String? by mutableStateOf(null)
		private set

	var currentElementType: ElementType? by mutableStateOf(null)
		private set

	var currentElementName: String? by mutableStateOf(null)
		private set

	var selectedRoot: RootSelection by mutableStateOf(RootSelection.NONE)
		private set

	var selectedComponentName: String? by mutableStateOf(null)
		private set

	var statusMessage: String? by mutableStateOf(null)
		private set

	var running: Boolean by mutableStateOf(false)
		private set

	val logs = mutableStateListOf<String>()

	var exportSelection: Map<String, ProjectRepository.StyleSelection> by mutableStateOf(emptyMap())

	var exportEverything: Boolean by mutableStateOf(false)

	fun touch() {
		revision++
	}

	// ------------------------------------------------------------- proyectos

	val recentProjects: List<RecentProject> get() = config.recentProjects

	val modLoaded: Boolean get() = repository.modLoaded

	fun openProject(uri: Uri) {
		viewModelScope.launch {
			val root = DocumentFile.fromTreeUri(getApplication(), uri)
			val loaded = root?.let { withContext(Dispatchers.IO) { repository.openProject(it) } }
			if (loaded == null) {
				statusMessage = "No se pudo abrir el proyecto (falta project.json)"
				return@launch
			}
			project = loaded
			config.addRecent(loaded.project_name, uri.toString())
			config.lastProjectUri = uri.toString()
			afterProjectOpened()
		}
	}

	fun createProject(uri: Uri, name: String, author: String) {
		viewModelScope.launch {
			val root = DocumentFile.fromTreeUri(getApplication(), uri)
			val created = root?.let { withContext(Dispatchers.IO) { repository.createProject(it, name, author) } }
			if (created == null) {
				statusMessage = "No se pudo crear el proyecto"
				return@launch
			}
			project = created
			config.addRecent(created.project_name, uri.toString())
			config.lastProjectUri = uri.toString()
			afterProjectOpened()
		}
	}

	private fun afterProjectOpened() {
		val styles = project?.styles.orEmpty()
		currentStyleName = styles.firstOrNull()?.name
		currentElementType = null
		currentElementName = null
		selectedRoot = RootSelection.NONE
		selectedComponentName = null
		logs.clear()
		touch()
	}

	fun closeProject() {
		repository.close()
		project = null
		currentStyleName = null
		currentElementType = null
		currentElementName = null
		selectedRoot = RootSelection.NONE
		selectedComponentName = null
		stopTest()
		touch()
	}

	fun save() {
		viewModelScope.launch {
			val ok = withContext(Dispatchers.IO) { repository.save() }
			statusMessage = if (repository.modLoaded) {
				"No se guarda mientras hay un mod importado"
			} else if (ok) {
				"Proyecto guardado"
			} else {
				"No se pudo guardar"
			}
		}
	}

	fun projectUri(): Uri? = repository.projectRoot?.uri

	// ---------------------------------------------------------------- estilos

	val currentStyle: Style? get() = project?.styles?.firstOrNull { it.name == currentStyleName }

	fun selectStyle(name: String) {
		currentStyleName = name
		currentElementType = null
		currentElementName = null
		selectedRoot = RootSelection.NONE
		selectedComponentName = null
		touch()
	}

	fun addStyle(name: String) {
		project?.let {
			it.styles.add(Defaults.newStyle(name))
			touch()
		}
	}

	fun renameStyle(style: Style, newName: String) {
		val wasCurrent = style.name == currentStyleName
		style.name = newName
		if (wasCurrent) currentStyleName = newName
		touch()
	}

	fun duplicateStyle(style: Style) {
		val project = project ?: return
		val copy = repository.deepCopy(Project(styles = mutableListOf(style))).styles.first()
		copy.name = uniqueName(project.styles.map { it.name }, style.name)
		project.styles.add(copy)
		touch()
	}

	fun removeStyle(style: Style) {
		val project = project ?: return
		project.styles.remove(style)
		if (currentStyleName == style.name) {
			currentStyleName = project.styles.firstOrNull()?.name
			currentElementType = null
			currentElementName = null
			selectedRoot = RootSelection.NONE
			selectedComponentName = null
		}
		touch()
	}

	fun moveStyle(index: Int, offset: Int) {
		val project = project ?: return
		val target = index + offset
		if (target < 0 || target >= project.styles.size) return
		val current = project.styles[index]
		project.styles[index] = project.styles[target]
		project.styles[target] = current
		touch()
	}

	// ----------------------------------------------------------- elementos

	val currentElements: List<Element> get() = currentStyle?.let { style ->
		currentElementType?.let { style.elements(it) }
	}.orEmpty()

	val currentElement: Element? get() = currentStyle?.let { style ->
		currentElementType?.let { style.element(it, currentElementName) }
	}

	fun selectElementType(type: ElementType?) {
		currentElementType = type
		currentElementName = null
		selectedRoot = RootSelection.NONE
		selectedComponentName = null
		touch()
	}

	fun openElement(type: ElementType, name: String) {
		currentElementType = type
		currentElementName = name
		selectedRoot = RootSelection.NONE
		selectedComponentName = null
		touch()
	}

	fun addElement(type: ElementType, name: String) {
		val style = currentStyle ?: return
		val element = when (type) {
			ElementType.THEME -> Defaults.newTheme(name)
			ElementType.CHARACTER -> Defaults.newCharacter(name)
			ElementType.OBJECT -> Defaults.newObject(name)
		}
		style.elements(type).add(element)
		touch()
	}

	fun renameElement(type: ElementType, element: Element, newName: String) {
		element.name = newName
		if (currentElement === element) currentElementName = newName
		touch()
	}

	fun duplicateElement(type: ElementType, element: Element) {
		val style = currentStyle ?: return
		val cloned = deepCopyElement(element)
		cloned.name = uniqueName(style.elements(type).map { it.name }, element.name)
		style.elements(type).add(cloned)
		touch()
	}

	private fun deepCopyElement(element: Element): Element {
		val temp = Style(themes = mutableListOf(element))
		val encoded = com.manzft.wonderstudio.model.WonderJson.instance
		val json = encoded.encodeToString(Style.serializer(), temp)
		return encoded.decodeFromString(Style.serializer(), json).themes.first()
	}

	fun removeElement(type: ElementType, element: Element) {
		val style = currentStyle ?: return
		style.elements(type).remove(element)
		if (currentElement === element) {
			currentElementName = null
			selectedRoot = RootSelection.NONE
			selectedComponentName = null
		}
		touch()
	}

	fun moveElement(type: ElementType, index: Int, offset: Int) {
		val list = currentStyle?.elements(type) ?: return
		val target = index + offset
		if (target < 0 || target >= list.size) return
		val current = list[index]
		list[index] = list[target]
		list[target] = current
		touch()
	}

	// --------------------------------------------------------- componentes

	val currentComponents: MutableList<Component> get() = currentElement?.components ?: mutableListOf()

	val selectedComponent: Component? get() = selectedComponentName?.let { name ->
		currentElement?.components?.firstOrNull { it.name == name }
	}

	fun selectComponent(name: String?) {
		selectedComponentName = name
		selectedRoot = RootSelection.NONE
		touch()
	}

	fun selectStyleRoot() {
		selectedRoot = RootSelection.STYLE
		selectedComponentName = null
		touch()
	}

	fun selectElementRoot() {
		selectedRoot = RootSelection.ELEMENT
		selectedComponentName = null
		touch()
	}

	fun addComponent(type: String) {
		val element = currentElement ?: return
		val baseName = type.replaceFirstChar { it.uppercase() }
		val name = uniqueName(element.components.map { it.name }, baseName)
		element.components.add(
			Component(name = name, type = type, settings = Defaults.componentSettings(type)),
		)
		touch()
	}

	fun removeComponent(component: Component) {
		val element = currentElement ?: return
		element.components.remove(component)
		if (selectedComponentName == component.name) selectedComponentName = null
		touch()
	}

	fun duplicateComponent(component: Component) {
		val element = currentElement ?: return
		val cloned = deepCopyComponent(component)
		cloned.name = uniqueName(element.components.map { it.name }, component.name + " copy")
		element.components.add(cloned)
		touch()
	}

	private fun deepCopyComponent(component: Component): Component {
		val temp = Style(themes = mutableListOf(Element(components = mutableListOf(component))))
		val json = com.manzft.wonderstudio.model.WonderJson.instance.encodeToString(Style.serializer(), temp)
		return com.manzft.wonderstudio.model.WonderJson.instance.decodeFromString(Style.serializer(), json)
			.themes.first().components.first()
	}

	fun renameComponent(component: Component, newName: String) {
		val element = currentElement ?: return
		if (newName.isBlank() || element.components.any { it.name == newName && it !== component }) return
		val wasSelected = selectedComponentName == component.name
		component.name = newName
		if (wasSelected) selectedComponentName = newName
		touch()
	}

	fun moveComponent(index: Int, offset: Int) {
		val list = currentElement?.components ?: return
		val target = index + offset
		if (target < 0 || target >= list.size) return
		val current = list[index]
		list[index] = list[target]
		list[target] = current
		touch()
	}

	// -------------------------------------------------------------- settings

	fun updateStyleSetting(key: String, value: Setting) {
		currentStyle?.settings?.let { it[key] = value; touch() }
	}

	fun updateElementSetting(key: String, value: Setting) {
		currentElement?.settings?.let { it[key] = value; touch() }
	}

	fun updateComponentSetting(component: Component, key: String, value: Setting) {
		component.settings[key] = value
		touch()
	}

	fun moveComponentPosition(component: Component, deltaX: Float, deltaY: Float) {
		val x = component.settings["position_x"]?.asFloat ?: 0f
		val y = component.settings["position_y"]?.asFloat ?: 0f
		component.settings["position_x"] = Setting.number(x + deltaX)
		component.settings["position_y"] = Setting.number(y + deltaY)
		touch()
	}

	// ----------------------------------------------------------- animaciones

	fun setCurrentAnimation(component: Component, name: String) {
		component.current_animation = name
		touch()
	}

	fun addAnimation(component: Component, name: String) {
		component.animations.add(Animation.create(name))
		if (component.current_animation.isNullOrEmpty()) component.current_animation = name
		touch()
	}

	fun removeAnimation(component: Component, animation: Animation) {
		component.animations.remove(animation)
		if (component.current_animation == animation.name) {
			component.current_animation = component.animations.firstOrNull()?.name
		}
		touch()
	}

	fun duplicateAnimation(component: Component, animation: Animation) {
		val cloned = deepCopyAnimation(animation)
		cloned.name = uniqueName(component.animations.map { it.name }, animation.name + " copy")
		component.animations.add(cloned)
		touch()
	}

	private fun deepCopyAnimation(animation: Animation): Animation {
		val temp = Style(themes = mutableListOf(Element(components = mutableListOf(Component(animations = mutableListOf(animation))))))
		val json = com.manzft.wonderstudio.model.WonderJson.instance.encodeToString(Style.serializer(), temp)
		return com.manzft.wonderstudio.model.WonderJson.instance.decodeFromString(Style.serializer(), json)
			.themes.first().components.first().animations.first()
	}

	fun renameAnimation(component: Component, animation: Animation, newName: String) {
		if (newName.isBlank() || component.animations.any { it.name == newName && it !== animation }) return
		if (component.current_animation == animation.name) component.current_animation = newName
		animation.name = newName
		touch()
	}

	fun moveAnimation(component: Component, index: Int, offset: Int) {
		val target = index + offset
		if (target < 0 || target >= component.animations.size) return
		val current = component.animations[index]
		component.animations[index] = component.animations[target]
		component.animations[target] = current
		touch()
	}

	fun updateAnimationSetting(component: Component, animation: Animation, key: String, value: Setting) {
		animation.settings[key] = value
		touch()
	}

	// ---------------------------------------------------------------- script

	fun updateScript(text: String) {
		val element = currentElement ?: return
		if (element.script == null) return
		element.script = text
		touch()
	}

	// ------------------------------------------------------------- assets/audio

	fun playSound(relativePath: String) {
		audio.play(repository.findAsset(relativePath))
	}

	fun stopSound() = audio.stop()

	// ------------------------------------------------------------------ mods

	fun importMod(uri: Uri) {
		viewModelScope.launch {
			val root = DocumentFile.fromTreeUri(getApplication(), uri)
			val ok = root?.let { withContext(Dispatchers.IO) { repository.importMod(it) } } ?: false
			if (ok) {
				project = repository.project
				afterProjectOpened()
				statusMessage = "Mod importado (no se puede guardar hasta abrir otro proyecto)"
			} else {
				statusMessage = "El mod no es un proyecto válido"
			}
		}
	}

	fun clearMod() = repository.clearMod()

	// ---------------------------------------------------------------- export

	fun toggleExportSelection(style: String, type: ElementType, name: String) {
		val current = exportSelection[style] ?: ProjectRepository.StyleSelection()
		val set = when (type) {
			ElementType.THEME -> current.themes
			ElementType.CHARACTER -> current.characters
			ElementType.OBJECT -> current.objects
		}.toMutableSet()
		if (!set.add(name)) set.remove(name)
		val updated = when (type) {
			ElementType.THEME -> current.copy(themes = set)
			ElementType.CHARACTER -> current.copy(characters = set)
			ElementType.OBJECT -> current.copy(objects = set)
		}
		exportSelection = exportSelection + (style to updated)
	}

	fun exportMod(uri: Uri, everything: Boolean) {
		viewModelScope.launch {
			val root = DocumentFile.fromTreeUri(getApplication(), uri)
			val ok = root?.let {
				withContext(Dispatchers.IO) {
					repository.export(it, ProjectRepository.ExportSelection(everything, exportSelection))
				}
			} ?: false
			statusMessage = if (ok) "Mod exportado" else "No se pudo exportar"
		}
	}

	// ------------------------------------------------------------------ test

	fun launchTest() {
		val uri = projectUri()
		if (uri == null) {
			statusMessage = "Abrí un proyecto primero"
			return
		}
		val packageName = config.testPackage
		if (!RunLauncher.isInstalled(getApplication(), packageName)) {
			statusMessage = "Wonder Maker ($packageName) no está instalado"
			return
		}
		udpReceiver.start()
		val ok = RunLauncher.launch(getApplication(), packageName, uri, repository.modRoot?.uri)
		if (ok) {
			running = true
			logs.clear()
		} else {
			statusMessage = "No se pudo lanzar Wonder Maker"
		}
	}

	fun stopTest() {
		running = false
		udpReceiver.stop()
	}

	fun clearLogs() = logs.clear()

	fun consumeStatus() {
		statusMessage = null
	}

	private fun uniqueName(existing: List<String>, base: String): String {
		if (base !in existing) return base
		var index = 2
		while ("$base$index" in existing) index++
		return "$base$index"
	}

	override fun onCleared() {
		super.onCleared()
		udpReceiver.stop()
		audio.release()
	}
}
