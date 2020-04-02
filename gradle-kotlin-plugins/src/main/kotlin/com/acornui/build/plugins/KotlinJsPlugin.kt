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
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
open class KotlinJsPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		KotlinCommonOptions.configure(target)
		configure(target)
	}

	companion object {

		fun configure(project: Project) {
			// We have a problem with jsTest nodejs dependencies trying to get resolved from
			project.extensions.configure<KotlinMultiplatformExtension> {
				val kotlinVersion: String by project
				val kotlinSerializationVersion: String by project
				val kotlinCoroutinesVersion: String by project
				js {
					browser {
						webpackTask {
//							// Assume project is a library, not an application, by default.
//							enabled = false

							val baseConventions = project.convention.plugins["base"] as BasePluginConvention?
							outputFileName = baseConventions?.archivesBaseName + ".js"
						}
						testTask {
							useMocha {
								// To be consistent, asynchronous tests should use `runTest`, which has its own timeout.
								timeout = "30s"
							}
						}
					}

					compilations.configureEach {
						kotlinOptions {
							moduleKind = "umd"
							sourceMap = true
							sourceMapEmbedSources = "always"
							main = "noCall"
						}
					}
				}

				sourceSets {
					all {
						languageSettings.progressiveMode = true
					}

					val jsMain by getting {
						dependencies {
							implementation(npm("promise-polyfill", version = "8.1.3")) // For IE11
							implementation(kotlin("stdlib-js", version = kotlinVersion))
							implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$kotlinSerializationVersion")
							implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlinCoroutinesVersion")
						}
					}

					val jsTest by getting {
						dependencies {
							implementation(kotlin("test", version = kotlinVersion))
							implementation(kotlin("test-js", version = kotlinVersion))
						}
					}
				}
			}
		}
	}
}