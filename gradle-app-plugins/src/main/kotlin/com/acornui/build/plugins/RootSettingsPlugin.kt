/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.build.plugins

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.provideDelegate
import java.io.File

@Suppress("unused")
class RootSettingsPlugin : Plugin<Settings> {

	override fun apply(target: Settings) {
		with(target) {
			enableFeaturePreview("GRADLE_METADATA")

			// Acorn composite project as sub-projects as a workaround to https://youtrack.jetbrains.com/issue/KT-30285
			val acornUiHome: String? by settings
			if (acornUiHome != null && File(acornUiHome!!).exists()) {
				listOf("utils", "core", "game", "test-utils").forEach { acornModule ->
					val name = ":acornui-$acornModule"
					include(name)
					project(name).projectDir = File("$acornUiHome/acornui-$acornModule")
				}
				listOf("lwjgl", "webgl").forEach { backend ->
					val name = ":acornui-$backend-backend"
					include(name)
					project(name).projectDir = File("$acornUiHome/backends/acornui-$backend-backend")
				}
			}
		}
	}
}