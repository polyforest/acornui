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

@file:Suppress("UnstableApiUsage", "UNUSED_VARIABLE")

package com.acornui.build.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class KotlinJsPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		KotlinCommonOptions.configure(target)
		configure(target)
	}

	companion object {
		fun configure(project: Project) {
			project.extensions.configure<KotlinMultiplatformExtension> {
				val kotlinVersion: String by project.extra
				val kotlinLanguageVersion: String by project.extra
				val kotlinSerializationVersion: String by project.extra
				val kotlinCoroutinesVersion: String by project.extra

				js {
					browser {
						testTask {
							useKarma {
								useChromeHeadless()
								useSafari()
								useIe()
								useFirefox()
							}
						}
					}
					
					compilations.all {
						kotlinOptions {
							moduleKind = "umd"
							sourceMap = true
							sourceMapEmbedSources = "always"
							main = "noCall"
						}
					}
				}

				targets.all {
					compilations.all {
						kotlinOptions {
							languageVersion = kotlinLanguageVersion
							apiVersion = kotlinLanguageVersion
						}
					}
				}

				sourceSets {
					all {
						languageSettings.progressiveMode = true
					}

					val jsMain by getting {
						dependencies {
							implementation(kotlin("stdlib-js", version = kotlinVersion))
							implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$kotlinSerializationVersion")
							implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlinCoroutinesVersion")
						}
					}

					val jsTest by getting {
						dependencies {
							implementation(kotlin("test", version = kotlinVersion))
							implementation(kotlin("test-js", version = kotlinVersion))
							implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$kotlinSerializationVersion")
							implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlinCoroutinesVersion")
						}
					}
				}
			}
		}
	}
}