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

val acornUiHomePropName = "ACORNUI_HOME"
val ACORNUI_HOME: String = gradle.startParameter.projectProperties[acornUiHomePropName]!!
val acornUiHome = file(ACORNUI_HOME)
val ACORNUI_SHARED_PROPS_PATH: String by gradle.startParameter.projectProperties
apply(from = "$ACORNUI_HOME/$ACORNUI_SHARED_PROPS_PATH")

val acornConfig: MutableMap<String, String> = gradle.startParameter.projectProperties

val ACORNUI_PLUGINS_REPO by acornConfig
val KOTLIN_VERSION by acornConfig
val DOKKA_VERSION by acornConfig
val ACORNUI_GROUP by acornConfig
val DEFAULT_ACORNUI_PLUGIN_VERSION by acornConfig
val DEFAULT_ACORNUI_PROJECT_VERSION by acornConfig

// TODO - MP: Pull version back into project properties.
val GRETTY_VERSION = "2.2.0"
val separator = File.separator
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
			if (requested.id.id.startsWith("org.gretty"))
				useVersion(GRETTY_VERSION)
			if (rootDir.canonicalPath == acornUiHome.canonicalPath && requested.id.id.startsWith("org.jetbrains.dokka"))
				useVersion(DOKKA_VERSION)
			if (requested.id.isPolyForest) {
				val version = requested.version ?: DEFAULT_ACORNUI_PLUGIN_VERSION
				useVersion(version)
			}
		}
	}
}

enum class PropType(val validOptions: String) {
	ABSOLUTE_PATH("an absolute path"),
	RELATIVE_PATH("a relative path"),
	SKIN("an available skin")
}

fun missingOrInvalidProperty(name: String, type: PropType): String {
	return """MISSING OR INVALID PROPERTY:  $name must be set on the command-line (-D$name=<val>), in $rootDir${separator}gradle.properties ($name=<val>), or $rootDir/gradle.settings.kts (gradle.startParameter.projectProperties["$name"] = <val>)
		|${"\t".repeat(2)}NOTE: <val> must be ${type.validOptions}""".trimMargin().split(",").joinToString(",\n") +
			// If this is a skin property, display the valid skins.
			(type.takeIf { it == PropType.SKIN }?.let { " -> ${validSkins.toList()}" } ?: "")
}

fun File.isValid() = this.exists() and this.isDirectory
val validSkins by lazy {
	listOf(acornConfig["ACORNUI_SKINS_PATH"], acornConfig["APP_SKINS_PATH"]).mapNotNull {
		// Use skin path directories that exist.
		it?.let { path: String -> File(path).takeIf { it.isValid() } }
	}.flatMap {
		// For all directories in the given skin directory, grab the directory names of those with resource directories.
		it.listFiles().mapNotNull {
			it.takeIf {
				it.isValid() && File(it, "resources").let { it.isValid() }
			}
		}
	}.associateBy {
		it.name
	}
}

val isValidSkin = { skin: String? ->
	skin?.let { validSkins.contains(it) } ?: false
}

val APP_HOME = acornConfig["APP_HOME"]
val isAppRoot = APP_HOME?.let { rootDir.canonicalPath == APP_HOME } ?: false
val appSkinPropName = "APP_SKIN"
val APP_SKIN = acornConfig[appSkinPropName]
val ACORNUI_DEFAULT_SKIN by acornConfig
// Validate the app skin.
if (isAppRoot) {
	acornConfig[appSkinPropName] = APP_SKIN?.takeIf { isValidSkin(it) } ?: run {

		logger.warn(missingOrInvalidProperty(appSkinPropName, PropType.SKIN))
		logger.warn("...falling back to the default skin: $ACORNUI_DEFAULT_SKIN")

		ACORNUI_DEFAULT_SKIN.takeIf { isValidSkin(it) }

	} ?: throw Exception(missingOrInvalidProperty("ACORNUI_DEFAULT_SKIN", PropType.SKIN) + "\n\n\tPlease file a " +
			"bug with the AcornUi project.")

	// Make the app skin directory available to build scripts.
	acornConfig["APP_SKIN_DIR"] = validSkins[acornConfig[appSkinPropName]]!!.canonicalPath
}

fun String?.asList(): List<String> = if (this.isNullOrBlank()) emptyList() else listOf(this!!)
val listProperty: (String?) -> List<String> = {
	if (it.isNullOrBlank())
		emptyList()
	else
		it!!.split(",")
}
if (isCompositeRoot) {
	fun includeNonPlugins() {
		val INCLUDED_BUILDS: String? by settings
		val includedBuildsList = run {
			val a = listProperty(INCLUDED_BUILDS)
			if (a.isEmpty())
				listProperty(acornConfig["INCLUDED_BUILDS"])
			else
				a
		}
		val allIncludedBuilds = acornUiHome.canonicalPath.asList() + acornConfig["APP_DIR"].asList() + includedBuildsList

		// Make available globally
		acornConfig["ALL_INCLUDED_BUILDS"] = allIncludedBuilds.joinToString(",")

		// Include all builds for this composite build
		allIncludedBuilds.forEach {
			if (it != rootDir.canonicalPath)
				settings.includeBuild(it)
		}
	}

	fun includePlugins() {
		val ACORNUI_PLUGINS_PATH by acornConfig
		val ACORNUI_PLUGINS_AVAILABLE by acornConfig
		val acornUiPlugins = ACORNUI_PLUGINS_AVAILABLE.split(",")
		acornUiPlugins.forEach {
			if (it != rootProject.name)
				settings.includeBuild("$ACORNUI_PLUGINS_PATH${separator}$it")
		}
	}

	includePlugins()
	includeNonPlugins()
}

val MULTI_MODULES: String? by settings
// There is a bug in gradle composite builds which causes properties to be non-null even if they are null
// in includedBuilds if the included build property name shadows the composite build when using settings
// delegate.
//val multiModulesList = (MULTI_MODULES ?: acornConfig.get("MULTI_MODULES"))?.split(",")
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
//val modulesList = (MODULES ?: acornConfig.get("MODULES"))?.split(",")
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
						name.isEmpty() -> "${fileDir.name}-${file.name}"
						else -> "$name-${file.name}"
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
