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

val kotlinVersion: String by extra
val acornPluginVersion: String by extra

pluginManagement {
    repositories {
        maven {
            url = uri("https://github.com/polyforest/acornui-gradle-plugin/raw/repository")
        }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            when {
                requested.id.namespace == "org.jetbrains.kotlin" ->
                    useVersion(kotlinVersion)
                requested.id.namespace == "com.acornui.plugins" ->
                    useVersion(acornPluginVersion)
            }
        }
    }
}
rootProject.name = "acornui"

include("acornui-utils", "acornui-core", "acornui-game", "acornui-spine", "backends:acornui-lwjgl-backend", "backends:acornui-webgl-backend", "tools:acornui-texture-packer", "acornui-test-utils")
include("skins:basic")

enableFeaturePreview("GRADLE_METADATA")