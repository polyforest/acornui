/*
 * Copyright 2019 Poly Forest, LLC
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

rootProject.name = "acornui"

pluginManagement {
	val kotlinVersion: String by extra
	val dokkaVersion: String by extra
	repositories {
		mavenLocal()
		gradlePluginPortal()
		maven {
			url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
		}
	}
	resolutionStrategy {
		eachPlugin {
			val id = requested.id
			when {
				id.namespace == "org.jetbrains.kotlin" -> useVersion(kotlinVersion)
				id.id == "org.jetbrains.dokka" -> useVersion(dokkaVersion)
			}
		}
	}
}

enableFeaturePreview("GRADLE_METADATA")

includeBuild("gradle-kotlin-plugins")

include("acornui-utils", "acornui-core", "acornui-game", "acornui-spine", "acornui-test-utils")
listOf("lwjgl", "webgl").forEach { backend ->
	val name = ":acornui-$backend-backend"
	include(name)
	project(name).projectDir = file("backends/acornui-$backend-backend")
}
listOf("acornui-texture-packer", "gdx-font-processor").forEach { tool ->
	val name = ":$tool"
	include(name)
	project(name).projectDir = file("tools/$tool")
}

include("gradle-app-plugins", "gradle-settings-plugins")