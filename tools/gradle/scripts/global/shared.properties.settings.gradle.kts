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

import java.util.Properties
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

open class GlobalConfig(val ACORNUI_HOME: String) {

	val GRADLE_VERSION = "5.0-rc-3"
	val TARGET_JVM_VERSION = "1.8"
	val KOTLIN_VERSION = "1.3.0"
	val TARGET_ECMA_VERSION = "v5"
	val DOKKA_VERSION = "0.9.17"
	val ACORNUI_GROUP = "com.polyforest"
	val MANIFEST_VERSION = "1.0"

	val DEFAULT_ACORNUI_PROJECT_VERSION = "0.0.1"
	val DEFAULT_ACORNUI_PLUGIN_VERSION = "1.0.0"

	private val ACORNUI_GRADLE_HELPER_ROOT = "tools/gradle"
	val ACORNUI_PLUGINS_PATH = "$ACORNUI_HOME/$ACORNUI_GRADLE_HELPER_ROOT/plugins"
	val ACORNUI_SCRIPTS_PLUGINS_PATH = "$ACORNUI_HOME/$ACORNUI_GRADLE_HELPER_ROOT/scripts"
	val ACORNUI_PLUGINS_REPO = "$ACORNUI_PLUGINS_PATH/repository"
	val ACORNUI_PLUGINS_AVAILABLE = "acornui-project,acornui-texturepack"

	val ACORNUI_SKINS_PATH = "$ACORNUI_HOME/skins"
	val ACORNUI_DEFAULT_SKIN = "basic"

}

fun existing(relativePath: String): File =
		file(relativePath).canonicalFile

fun loadGradleProperties() =
		loadPropertiesFrom(gradlePropertiesFile)

val gradlePropertiesFile by lazy { existing("$rootDir/gradle.properties") }

fun loadPropertiesFrom(file: File) =
	file.takeIf { it.isFile }?.inputStream()?.use { Properties().apply { load(it) } } ?: Properties()

fun Properties.toStringMap(): Map<String, String> {
	val propMap = mutableMapOf<String, String>()
	var v: String?
	this.stringPropertyNames().forEach {
		v = this.getProperty(it)
		if (v != null) {
			propMap[it] = v!!
			v = null
		}
	}
	return propMap.toMap()
}

inline fun <reified T : Any> memberPropertiesStringMap(instance: T): Map<String, String> {
	return T::class.memberProperties.associate { Pair(it.name, it.get(instance) as String) }
}

fun StartParameter.mergeIntoProjectPropertiesIfAbsent(downstreamProperties: Map<String, String>) {
	val existingProperties: Map<String, String> = this.projectProperties
	val propertiesToAdd: Map<String, String> = downstreamProperties.filterNot {
		existingProperties.contains(it.key)
	}
	// Use setter for safety.  gradle.startParameter.projectProperties is immutable or mutable depending
	// on the runtime context (composite vs non-composite build).
	this.setProjectProperties(existingProperties + propertiesToAdd)
}

fun loadAcornProperties() {
	val ACORNUI_HOME: String by gradle.startParameter.projectProperties
	with(gradle.startParameter) {
		mergeIntoProjectPropertiesIfAbsent(memberPropertiesStringMap(object : GlobalConfig(ACORNUI_HOME) {}))
		mergeIntoProjectPropertiesIfAbsent(loadGradleProperties().toStringMap())
	}
}
loadAcornProperties()
