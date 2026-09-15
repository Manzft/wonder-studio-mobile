package com.manzft.wonderstudio.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.manzft.wonderstudio.model.Component
import com.manzft.wonderstudio.model.Defaults
import com.manzft.wonderstudio.model.Element
import com.manzft.wonderstudio.model.ElementType
import com.manzft.wonderstudio.model.Project
import com.manzft.wonderstudio.model.Setting
import com.manzft.wonderstudio.model.Settings
import com.manzft.wonderstudio.model.Style
import com.manzft.wonderstudio.model.WonderJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Persistencia del proyecto en una carpeta SAF elegida por el usuario. Réplica
// del formato del Studio de PC.
class ProjectRepository(private val context: Context) {

	var project: Project? = null
		private set
	var projectRoot: DocumentFile? = null
		private set
	var modRoot: DocumentFile? = null
		private set
	var modLoaded: Boolean = false
		private set
	private var baseProject: Project? = null

	val isOpen: Boolean get() = project != null

	@Serializable
	private data class StyleFile(val order: Int, val settings: Settings)

	@Serializable
	private data class ElementFile(
		val order: Int,
		val components: List<Component> = emptyList(),
		val script: String? = null,
		val settings: Settings = mutableMapOf(),
	)

	// ------------------------------------------------------------------ open

	fun createProject(root: DocumentFile, name: String, author: String): Project {
		val created = Defaults.newProject(name, author)
		project = created
		projectRoot = root
		modRoot = null
		modLoaded = false
		baseProject = null
		Saf.ensureDir(root, "assets")
		Saf.ensureDirs(root, "sourcecode/styles")
		save()
		return created
	}

	fun openProject(root: DocumentFile): Project? {
		val loaded = readProject(root) ?: return null
		project = loaded
		projectRoot = root
		modRoot = null
		modLoaded = false
		baseProject = deepCopy(loaded)
		return loaded
	}

	fun close() {
		project = null
		projectRoot = null
		modRoot = null
		modLoaded = false
		baseProject = null
	}

	fun readProject(root: DocumentFile): Project? {
		val projectJson = Saf.child(root, "project.json") ?: return null
		val text = Saf.readText(context, projectJson) ?: return null
		val loaded = runCatching {
			WonderJson.instance.decodeFromString(Project.serializer(), text)
		}.getOrNull() ?: return null

		val styles = mutableListOf<Style>()
		val stylesDir = Saf.child(root, "sourcecode")?.let { Saf.child(it, "styles") }
		if (stylesDir != null) {
			for (dir in stylesDir.listFiles().filter { it.isDirectory }) {
				val styleName = dir.name ?: continue
				val style = Style(name = styleName)
				Saf.child(dir, "style.json")?.let { file ->
					Saf.readText(context, file)?.let { content ->
						runCatching {
							WonderJson.instance.decodeFromString(StyleFile.serializer(), content)
						}.getOrNull()?.let { parsed ->
							style.order = parsed.order
							style.settings = parsed.settings
						}
					}
				}
				style.themes = readElements(dir, "themes")
				style.characters = readElements(dir, "characters")
				style.objects = readElements(dir, "objects")
				style.themes.sortBy { it.order }
				style.characters.sortBy { it.order }
				style.objects.sortBy { it.order }
				styles.add(style)
			}
		}
		styles.sortBy { it.order }
		loaded.styles = styles
		return loaded
	}

	private fun readElements(styleDir: DocumentFile, folder: String): MutableList<Element> {
		val result = mutableListOf<Element>()
		val dir = Saf.child(styleDir, folder) ?: return result
		for (file in dir.listFiles()) {
			val name = file.name ?: continue
			if (!file.isFile || !name.endsWith(".json")) continue
			val content = Saf.readText(context, file) ?: continue
			val element = runCatching {
				WonderJson.instance.decodeFromString(Element.serializer(), content)
			}.getOrNull() ?: continue
			element.name = name.removeSuffix(".json")
			result.add(element)
		}
		return result
	}

	// ------------------------------------------------------------------ save

	fun save(): Boolean {
		val current = project ?: return false
		val root = projectRoot ?: return false
		if (modLoaded) return false

		Saf.ensureDir(root, "assets")
		Saf.child(root, "sourcecode")?.let { Saf.deleteRecursive(it) }
		val sourcecode = Saf.ensureDir(root, "sourcecode") ?: return false
		val stylesRoot = Saf.ensureDir(sourcecode, "styles") ?: return false

		current.version = Defaults.ENGINE_VERSION
		writeProjectJson(root, current)

		current.styles.forEachIndexed { index, style ->
			style.order = index
			val styleDir = Saf.ensureDir(stylesRoot, style.name) ?: return@forEachIndexed
			Saf.ensureDir(styleDir, "themes")
			Saf.ensureDir(styleDir, "characters")
			Saf.ensureDir(styleDir, "objects")

			val styleJson = WonderJson.instance.encodeToString(
				StyleFile.serializer(),
				StyleFile(style.order, style.settings),
			)
			Saf.createOrReplace(context, styleDir, "style.json", styleJson)

			writeElements(styleDir, "themes", style.themes, withScript = false)
			writeElements(styleDir, "characters", style.characters, withScript = true)
			writeElements(styleDir, "objects", style.objects, withScript = true)
		}
		return true
	}

	private fun writeElements(styleDir: DocumentFile, folder: String, elements: List<Element>, withScript: Boolean) {
		val dir = Saf.child(styleDir, folder) ?: return
		elements.forEachIndexed { index, element ->
			element.order = index
			val file = ElementFile(
				order = index,
				components = element.components,
				script = if (withScript) element.script else null,
				settings = element.settings,
			)
			val text = WonderJson.instance.encodeToString(ElementFile.serializer(), file)
			Saf.createOrReplace(context, dir, "${element.name}.json", text)
		}
	}

	private fun writeProjectJson(root: DocumentFile, project: Project) {
		val obj = buildJsonObject {
			put("project_name", project.project_name)
			put("author_name", project.author_name)
			put("project_version", project.project_version)
			put("version", project.version.ifEmpty { Defaults.ENGINE_VERSION })
		}
		val text = WonderJson.instance.encodeToString(JsonObject.serializer(), obj)
		Saf.createOrReplace(context, root, "project.json", text)
	}

	fun isValidProject(root: DocumentFile): Boolean {
		val file = Saf.child(root, "project.json") ?: return false
		val text = Saf.readText(context, file) ?: return false
		return text.contains("project_name")
	}

	// ------------------------------------------------------------------- mods

	fun importMod(modDir: DocumentFile): Boolean {
		val current = project ?: return false
		val modProject = readProject(modDir) ?: return false
		if (modLoaded) {
			baseProject?.let { project = deepCopy(it) }
		}
		modLoaded = true
		modRoot = modDir
		val target = project ?: return false
		for (modStyle in modProject.styles) mergeStyle(target, modStyle)
		return true
	}

	fun clearMod() {
		if (!modLoaded) return
		baseProject?.let { project = deepCopy(it) }
		modLoaded = false
		modRoot = null
	}

	private fun mergeStyle(target: Project, incoming: Style) {
		val existing = target.styles.firstOrNull { it.name == incoming.name }
		if (existing == null) {
			target.styles.add(incoming)
			return
		}
		for ((key, value) in incoming.settings) existing.settings[key] = value
		mergeElements(existing.themes, incoming.themes)
		mergeElements(existing.characters, incoming.characters)
		mergeElements(existing.objects, incoming.objects)
	}

	private fun mergeElements(base: MutableList<Element>, incoming: List<Element>) {
		for (element in incoming) {
			val index = base.indexOfFirst { it.name == element.name }
			if (index >= 0) base[index] = element else base.add(element)
		}
	}

	fun deepCopy(project: Project): Project {
		val text = WonderJson.instance.encodeToString(Project.serializer(), project)
		return WonderJson.instance.decodeFromString(Project.serializer(), text)
	}

	// ---------------------------------------------------------------- assets

	fun projectAssetsRoot(): DocumentFile? = projectRoot?.let { Saf.child(it, "assets") }

	fun findAsset(relativePath: String): Uri? {
		if (relativePath.isBlank()) return null
		modRoot?.let { resolveAsset(it, relativePath)?.let { uri -> return uri } }
		projectRoot?.let { resolveAsset(it, relativePath)?.let { uri -> return uri } }
		return null
	}

	private fun resolveAsset(root: DocumentFile, relativePath: String): Uri? {
		var dir = Saf.child(root, "assets") ?: return null
		val parts = relativePath.trim('/').split('/').filter { it.isNotEmpty() }
		for ((index, part) in parts.withIndex()) {
			val node = Saf.child(dir, part) ?: return null
			if (index == parts.lastIndex) return if (node.isFile) node.uri else null
			dir = node
		}
		return null
	}

	// ---------------------------------------------------------------- export

	data class StyleSelection(
		val themes: Set<String> = emptySet(),
		val characters: Set<String> = emptySet(),
		val objects: Set<String> = emptySet(),
	) {
		val isEmpty: Boolean get() = themes.isEmpty() && characters.isEmpty() && objects.isEmpty()
	}

	data class ExportSelection(
		val everything: Boolean,
		val styles: Map<String, StyleSelection>,
	)

	fun export(target: DocumentFile, selection: ExportSelection): Boolean {
		val current = project ?: return false
		Saf.ensureDir(target, "assets")
		writeProjectJson(target, current)

		val referencedAssets = linkedSetOf<String>()

		current.styles.forEach { style ->
			val styleSelection = selection.styles[style.name]
			val includeAll = selection.everything
			if (!includeAll && (styleSelection == null || styleSelection.isEmpty)) return@forEach

			val selectedThemes = if (includeAll) style.themes else style.themes.filter { it.name in (styleSelection?.themes ?: emptySet()) }
			val selectedCharacters = if (includeAll) style.characters else style.characters.filter { it.name in (styleSelection?.characters ?: emptySet()) }
			val selectedObjects = if (includeAll) style.objects else style.objects.filter { it.name in (styleSelection?.objects ?: emptySet()) }

			val stylesRoot = Saf.ensureDirs(target, "sourcecode/styles") ?: return@forEach
			val styleDir = Saf.ensureDir(stylesRoot, style.name) ?: return@forEach
			Saf.ensureDir(styleDir, "themes")
			Saf.ensureDir(styleDir, "characters")
			Saf.ensureDir(styleDir, "objects")

			collectPaths(style.settings, referencedAssets)
			selectedThemes.forEach { collectElementPaths(it, referencedAssets) }
			selectedCharacters.forEach { collectElementPaths(it, referencedAssets) }
			selectedObjects.forEach { collectElementPaths(it, referencedAssets) }

			writeElements(styleDir, "themes", selectedThemes, withScript = false)
			writeElements(styleDir, "characters", selectedCharacters, withScript = true)
			writeElements(styleDir, "objects", selectedObjects, withScript = true)

			val styleJson = WonderJson.instance.encodeToString(
				StyleFile.serializer(),
				StyleFile(style.order, style.settings),
			)
			Saf.createOrReplace(context, styleDir, "style.json", styleJson)
		}

		for (relative in referencedAssets) copyAsset(target, relative)
		return true
	}

	private fun collectElementPaths(element: Element, out: MutableSet<String>) {
		collectPaths(element.settings, out)
		for (component in element.components) {
			collectPaths(component.settings, out)
			for (animation in component.animations) collectPaths(animation.settings, out)
		}
	}

	private fun collectPaths(settings: Settings, out: MutableSet<String>) {
		for (setting in settings.values) {
			if (setting.type == "path" && setting.asString.isNotBlank()) out.add(setting.asString)
		}
	}

	private fun copyAsset(target: DocumentFile, relativePath: String) {
		val source = findAsset(relativePath) ?: return
		val parts = relativePath.trim('/').split('/').filter { it.isNotEmpty() }
		if (parts.isEmpty()) return
		val assetsRoot = Saf.ensureDir(target, "assets") ?: return
		var dir = assetsRoot
		for (index in 0 until parts.size - 1) {
			dir = Saf.ensureDir(dir, parts[index]) ?: return
		}
		val fileName = parts.last()
		Saf.createOrReplaceBinary(context, dir, fileName, Saf.mimeFor(fileName), source)
	}

	// ------------------------------------------------------------------ sync

	// Borra TODO el contenido del proyecto y lo reemplaza por el de un .zip de
	// GitHub (archivo de una rama). El zip trae una carpeta raíz (p. ej.
	// "wonder-maker-fangame-main/") que se descarta al extraer.
	fun replaceAll(root: DocumentFile, input: java.io.InputStream, onProgress: (String) -> Unit = {}): Boolean {
		for (child in root.listFiles()) Saf.deleteRecursive(child)
		java.util.zip.ZipInputStream(input).use { zip ->
			var entry = zip.nextEntry
			while (entry != null) {
				val relative = entry.name.substringAfter('/', "")
				if (relative.isNotEmpty()) {
					if (entry.isDirectory) {
						Saf.ensureDirs(root, relative)
					} else {
						val dir = relative.substringBeforeLast('/', "")
						val fileName = relative.substringAfterLast('/')
						val parent = if (dir.isEmpty()) root else Saf.ensureDirs(root, dir)
						if (parent != null) {
							val file = Saf.child(parent, fileName) ?: parent.createFile(Saf.mimeFor(fileName), fileName)
							if (file != null) {
								context.contentResolver.openOutputStream(file.uri, "wt")?.use { out -> zip.copyTo(out) }
							}
						}
					}
					onProgress(relative)
				}
				zip.closeEntry()
				entry = zip.nextEntry
			}
		}
		return true
	}
}
