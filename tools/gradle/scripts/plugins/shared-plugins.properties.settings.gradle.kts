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

// In some runtime contexts, gradle.startParameter.projectProperties is immutable.
val sharedProperties = mapOf(
		"GRADLE_VERSION" to "4.10-rc-3",
		"TARGET_JVM_VERSION" to "1.8",
		"KOTLIN_VERSION" to "1.2.60",
		"ACORNUI_GROUP" to "com.polyforest",
		// From plugins project directories.
		"ACORNUI_PLUGINS_REPO" to "../repository",
		"DEFAULT_ACORNUI_PLUGIN_VERSION" to "1.0.0",
		"ACORNUI_BUILD_UTIL_PACKAGE" to "com.acornui.build.util",
		"ACORNUI_PLUGINS_AVAILABLE" to "acornui-project,acornui-texturepack"
)

fun StartParameter.setProjectPropertiesIfAbsent(projectProperties: Map<String, String>) {
	val previousProperties: Map<String, String> = this.projectProperties
	val propertiesToAdd: Map<String, String> = projectProperties.filterNot {
		previousProperties.contains(it.key)
	}
	// Use setter for safety.  gradle.startParameter.projectProperties is immutable or mutable depending
	// on the runtime context (composite vs non-composite build).
	this.setProjectProperties(previousProperties + propertiesToAdd)
}

gradle.startParameter.setProjectPropertiesIfAbsent(sharedProperties)
// Todo: Make this merge regular properties in
