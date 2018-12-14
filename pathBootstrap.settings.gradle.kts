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

// Prevents projects from needing to know about acornui project structure by bootstrapping paths needed
// to apply shared properties which hold the rest.
// Note:  To be used by *.settings.gradle.kts files (settings scripts)
val scriptPluginsRoot = "tools/gradle/scripts"
val pluginsRoot = "tools/gradle/plugins"
val namespacePrefix = "ACORNUI"

// Paths with script plugins root prefixed.
val rootedProperties = mapOf(
		"SHARED_PROPS_PATH" to "global/shared.properties.settings.gradle.kts",
		"SHARED_BUILD_PATH" to "global/shared.build.gradle.kts",
		"SHARED_SETTINGS_PATH" to "global/shared.settings.gradle.kts"
).mapValues { "$scriptPluginsRoot/${it.value}" }
// Paths without root prefixed.
val nonRootedProperties = mapOf(
		"BUILDUTILS_PATH" to "tools/acornui-buildutils"
)

val paths = (rootedProperties + nonRootedProperties)
		.mapKeys { "${namespacePrefix}_${it.key}".toUpperCase() }

fun StartParameter.setProjectPropertiesIfAbsent(projectProperties: Map<String, String>) {
	val previousProperties: Map<String, String> = this.projectProperties
	val propertiesToAdd: Map<String, String> = projectProperties.filterNot {
		previousProperties.contains(it.key)
	}
	// Use setter for safety.  gradle.startParameter.projectProperties is immutable or mutable depending
	// on the runtime context (composite vs non-composite build).
	this.setProjectProperties(previousProperties + propertiesToAdd)
}

gradle.startParameter.setProjectPropertiesIfAbsent(paths)
