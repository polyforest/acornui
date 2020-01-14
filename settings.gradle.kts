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
	val version: String by extra
	val kotlinVersion: String by extra
	val dokkaVersion: String by extra
	repositories {
		mavenLocal()
		gradlePluginPortal()
		maven {
			url = uri("https://dl.bintray.com/kotlin/kotlin-dev/")
		}
	}
	resolutionStrategy {
		eachPlugin {
			val id = requested.id
			when {
				id.namespace == "org.jetbrains.kotlin" -> useVersion(kotlinVersion)
				id.namespace == "com.acornui" -> useVersion(version)
				id.id == "org.jetbrains.dokka" -> useVersion(dokkaVersion)
			}
		}
	}
}

includeBuild("gradle-kotlin-plugins")

include("acornui-utils", "acornui-core", "acornui-game", "acornui-spine", "acornui-test-utils")
include("backends:acornui-lwjgl-backend", "backends:acornui-webgl-backend")
include("tools:acornui-texture-packer", "tools:gdx-font-processor")
//include("skins:basic")

include("gradle-app-plugins")
includeBuild("skins")