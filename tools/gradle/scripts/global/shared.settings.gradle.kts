import java.net.URL

/*
 * Copyright 2018 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

fun loadStartParamProp(propName: String, default: String? = null, isFilePath: Boolean = false): String? {
	val startParams: MutableMap<String, String> = gradle.startParameter.projectProperties
	val startParamProp = startParams[propName]
	val extraProp = if (extra.has(propName)) extra[propName] else null

	val value= (listOf(extraProp, startParamProp, default).firstOrNull {
		!(it as? String?).isNullOrBlank()
	} as String?).let {
		if (isFilePath && it != null) file(it).canonicalPath else it
	}

	startParams[propName] = value ?: ""
	extra[propName] = value
	return value
}

val acornUiHomePropName = "ACORNUI_HOME"
val ACORNUI_HOME: String = gradle.startParameter.projectProperties[acornUiHomePropName]!!
val acornUiHome = file(ACORNUI_HOME)
val ACORNUI_SHARED_PROPS_PATH: String by gradle.startParameter.projectProperties
apply(from = "$ACORNUI_HOME/$ACORNUI_SHARED_PROPS_PATH")

val acornConfig: MutableMap<String, String> = gradle.startParameter.projectProperties

val ACORNUI_PLUGINS_REPO by acornConfig
val KOTLIN_VERSION by acornConfig
val DOKKA_VERSION by acornConfig
val NODE_PLUGIN_VERSION by acornConfig
val ACORNUI_GROUP by acornConfig
val DEFAULT_ACORNUI_PLUGIN_VERSION by acornConfig
val DEFAULT_ACORNUI_PROJECT_VERSION by acornConfig

val separator: String = File.separator
val separatorCharacter = File.separatorChar

val isCompositeRoot = gradle.parent == null
val PluginId.isPolyForest
	get() = id.startsWith(ACORNUI_GROUP)
settings.pluginManagement {
	repositories {
		maven {
			name = "Local AcornUi Plugin Repository"
			url = uri(ACORNUI_PLUGINS_REPO)
		}

		gradlePluginPortal()
	}

	resolutionStrategy {
		eachPlugin {
			if (requested.id.id.startsWith("org.jetbrains.kotlin"))
				useVersion(KOTLIN_VERSION)
			if (rootDir.canonicalPath == acornUiHome.canonicalPath && requested.id.id.startsWith("org.jetbrains.dokka"))
				useVersion(DOKKA_VERSION)
			if (requested.id.id == "com.liferay.node")
				useVersion(NODE_PLUGIN_VERSION)
			if (requested.id.isPolyForest) {
				val version = requested.version ?: DEFAULT_ACORNUI_PLUGIN_VERSION
				useVersion(version)
			}
		}
	}
}

fun File.isValid() = this.exists() and this.isDirectory
val validSkins by lazy {
	listOf(acornConfig["ACORNUI_SKINS_PATH"], acornConfig["APP_SKINS_PATH"]).mapNotNull { skinsLocation ->
		// Use skin path directories that exist.
		skinsLocation?.let { path: String ->
			File(path).takeIf { it.isValid() }
		}
	}.flatMap { skinLocation ->
		// For all directories in the given skin directory, grab the directory names of those with resource directories.
		skinLocation.listFiles().mapNotNull { skinDirCandidate ->
			skinDirCandidate.takeIf { file ->
				file.isValid() && File(file, "resources").isValid()
			}
		}
	}.associateBy { skinDir ->
		skinDir.name
	}
}

val isValidSkin = { skin: String? ->
	skin?.let { validSkins.contains(it) } ?: false
}

enum class PropType(val validOption: String) {
	ABSOLUTE_PATH("an absolute path"),
	RELATIVE_PATH("a relative path"),
	SKIN("an available skin")
}

val standardPathSeparatorMessage =
	"Please use '/' or '\\\\' as a path separator for Windows platforms or '/' for non-Windows platforms."

val missingRequiredProperty = { name: String, type: PropType ->
	"""MISSING OR INVALID PROPERTY:  ${if (type != PropType.SKIN) {
		"""$name is missing from command-line (-D$name=<val>), $rootDir${separator}gradle.properties ($name=<val>) for this machine only, or $rootDir${separator}gradle.settings.kts for all build consumers (gradle.startParameter.projectProperties[\"$name\"] = <val>
			|${"\t".repeat(2)}NOTE: <val> must be ${type.validOption}.
			|$standardPathSeparatorMessage""".trimMargin().split(",").joinToString(",\n")
	}
	else {
		"""$name is undefined
			|VALID SKINS (name, location) -> ${validSkins.toList()}
		""".trimMargin()
	}}""".trimMargin()
}

val incorrectPathSeparator = { name: String ->
	"""
		INCORRECT SEPARATOR: $name value is using a path separator that escapes out characters ('\').
		$standardPathSeparatorMessage
	""".trimIndent()
}

val badDirectory = { name: String, path: String ->
	"INCORRECT $name:  $path either doesn't exist or isn't a directory.\n$standardPathSeparatorMessage"
}

val APP_DIR = loadStartParamProp("APP_DIR", "app")
val appHomeDefault: String? = if (isCompositeRoot && rootDir.canonicalPath != acornUiHome.canonicalPath) {
	rootDir.canonicalPath + File.separator + APP_DIR
}
else {
	null
}

val APP_HOME = loadStartParamProp("APP_HOME", appHomeDefault, true)
// Setup default app-side skins path
val APP_SKINS_PATH = loadStartParamProp("APP_SKINS_PATH", APP_HOME + separator + "skins", true)

val isAppRoot = rootDir.canonicalPath == APP_HOME
val appSkinPropName = "APP_SKIN"
val APP_SKIN = acornConfig[appSkinPropName]
val ACORNUI_DEFAULT_SKIN by acornConfig
// Validate the app skin.
if (isAppRoot) {
	acornConfig[appSkinPropName] = APP_SKIN?.takeIf { isValidSkin(it) } ?: run {

		logger.info(missingRequiredProperty(appSkinPropName, PropType.SKIN))
		logger.info("...attempting to fall back to the default skin: $ACORNUI_DEFAULT_SKIN")

		ACORNUI_DEFAULT_SKIN.takeIf { isValidSkin(it) }

	} ?: throw Exception(
		missingRequiredProperty("ACORNUI_DEFAULT_SKIN", PropType.SKIN) + "\n\n\tPlease file a bug with the " +
				"AcornUi project: https://github.com/polyforest/acornui/issues"
	)

	// Make the app skin directory available to build scripts.
	acornConfig["APP_SKIN_DIR"] = validSkins[acornConfig[appSkinPropName]]!!.canonicalPath
}

fun String?.asList(): List<String> = if (this.isNullOrBlank()) emptyList() else listOf(this)
val listProperty: (String?) -> List<String> = {
	if (it.isNullOrBlank())
		emptyList()
	else
		it.split(",")
}
if (isCompositeRoot) {
	fun includeNonPlugins() {
		val INCLUDED_BUILDS: String? by settings
		val requestedIncludedBuilds = mutableSetOf<String>()
		listOf(INCLUDED_BUILDS, acornConfig["INCLUDED_BUILDS"]).forEach {
			if (it != null)
				requestedIncludedBuilds.addAll(listProperty(it.toLowerCase()))
		}

		val allIncludedBuilds =
			mutableSetOf(acornUiHome.canonicalPath, APP_HOME) + requestedIncludedBuilds

		@Suppress("UNCHECKED_CAST")
		val childrenBuilds = allIncludedBuilds.filter {
			val excludeAppWhenAcornComposite = { path: String ->
				if (rootDir.canonicalPath == acornUiHome.canonicalPath)
					path != APP_HOME
				else
					true
			}

			it != null && it != rootDir.canonicalPath && excludeAppWhenAcornComposite(it)
		} as List<String>

		// Make available globally
		acornConfig["CHILDREN_BUILDS"] = childrenBuilds.joinToString(",")

		childrenBuilds.forEach { settings.includeBuild(it) }
	}

	fun includePlugins() {
		val ACORNUI_PLUGINS_PATH by acornConfig
		val ACORNUI_PLUGINS_AVAILABLE by acornConfig
		val acornUiPlugins = ACORNUI_PLUGINS_AVAILABLE.split(",")
		acornUiPlugins.forEach {
			if (it != rootProject.name)
				settings.includeBuild("$ACORNUI_PLUGINS_PATH$separator$it")
		}
	}

	// Not using plugins yet.
	//	includePlugins()
	includeNonPlugins()
}

val MULTI_MODULES: String? by settings
// There is a bug in gradle composite builds which causes properties to be non-null even if they are null
// in includedBuilds if the included build property name shadows the composite build when using settings
// delegate.
val multiModulesList = run {
	val a = listProperty(MULTI_MODULES)
	// Composite root builds pass down gradle.startParam.projectProperties to downstream builds.
	if (a.isEmpty() || !isCompositeRoot)
		listProperty(acornConfig["MULTI_MODULES"])
	else
		a
}

val MODULES: String? by settings
// There is a bug in gradle composite builds which causes properties to be non-null even if they are null
// in includedBuilds if the included build property name shadows the composite build when using settings
// delegate.
val modulesList = run {
	val a = listProperty(MODULES)
	// Composite root builds pass down gradle.startParam.projectProperties to downstream builds.
	if (a.isEmpty() || !isCompositeRoot)
		listProperty(acornConfig["MODULES"])
	else
		a
}

if (multiModulesList.isNotEmpty() || modulesList.isNotEmpty()) {
	fun File.isValidModule() = this.isValid() && (this.listFiles { _, name ->
		name.startsWith("build.gradle")
	}?.isNotEmpty() ?: false)

	fun includeModule(name: String, dir: String, includeContainerProjects: Boolean = true) {
		val projectDir = File("${rootDir.canonicalPath}/$dir")

		if (includeContainerProjects) {
			var path = dir.replace(separatorCharacter, ':').replaceAfterLast(':', name)
			if (dir == path)
				path = name

			include(path)
			project(":$path").projectDir = projectDir
		} else {
			include(name)
			project(":$name").projectDir = projectDir
		}
	}

	fun multiModule(dir: String, name: String = "") {
		val fileDir = if (dir == ".")
			File(rootDir.canonicalPath)
		else
			File("${rootDir.canonicalPath}$separator$dir")

		if (fileDir.isValid()) {
			fileDir.listFiles().forEach { file ->
				if (file.isValidModule()) {
					// Dependency resolution in multiproject projects uses group:name (com.polyforest:js) instead of the
					// full subproject's path (i.e. :acorn-core:js).  Once the group is set, multiplatform
					// subprojects end up sharing the same coordinate, causing a problem at the class level.

					// Therefore, leaf project names (not just archives names) must be unique.
					val subProjectName = when {
						fileDir.canonicalPath == rootDir.canonicalPath && name.isEmpty() ->
							"${rootProject.name}-${file.name}"
						name.isEmpty()                                                   -> "${fileDir.name}-${file.name}"
						else                                                             -> "$name-${file.name}"
					}

					includeModule(subProjectName, file.toRelativeString(rootDir))
				}
			}
		}
	}

	fun module(dir: String, name: String = "") {
		var moduleName = name
		val fileDir = file("${rootDir.canonicalPath}$separator$dir")

		if (moduleName.isEmpty())
			moduleName = fileDir.name

		includeModule(moduleName, fileDir.toRelativeString(rootDir))
	}

	multiModulesList.forEach { if (it.isNotBlank()) multiModule(it) }
	modulesList.forEach { if (it.isNotBlank()) module(it) }
}
