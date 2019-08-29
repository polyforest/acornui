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

@file:Suppress("UNUSED_VARIABLE", "UnstableApiUsage")

package com.acornui.build.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("unused")
class KotlinMppPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
		target.pluginManager.apply("kotlinx-serialization")

		val kotlinVersion: String by target.extra
		val kotlinJvmTarget: String by target.extra
		val kotlinLanguageVersion: String by target.extra
		val kotlinSerializationVersion: String by target.extra
		val kotlinCoroutinesVersion: String by target.extra

		target.tasks.withType<KotlinCompile>().all {
			kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
		}

		target.extensions.configure<KotlinMultiplatformExtension> {
			sourceSets {
				all {
					languageSettings.useExperimentalAnnotation("kotlin.Experimental")
					languageSettings.useExperimentalAnnotation("kotlin.time.ExperimentalTime")
				}
			}
			js {
				browser {}
				compilations.all {
					kotlinOptions {
						moduleKind = "amd"
						sourceMap = true
						sourceMapEmbedSources = "always"
						main = "noCall"
					}
				}
			}
			jvm {
				compilations.all {
					kotlinOptions {
						jvmTarget = kotlinJvmTarget
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

			sourceSets.all {
				languageSettings.progressiveMode = true
			}

			sourceSets {
				all {
					languageSettings.progressiveMode = true
				}

				@Suppress("UNUSED_VARIABLE")
				val commonMain by getting {
					dependencies {
						implementation(kotlin("stdlib-common", version = kotlinVersion))
						implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$kotlinSerializationVersion")
						implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$kotlinCoroutinesVersion")
					}
				}

				val commonTest by getting {
					dependencies {
						implementation(kotlin("test-common", version = kotlinVersion))
						implementation(kotlin("test-annotations-common", version = kotlinVersion))

						implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$kotlinSerializationVersion")
						implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$kotlinCoroutinesVersion")
					}
				}

				val jvmMain by getting {
					dependencies {
						implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
						implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinSerializationVersion")
						implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
					}
				}

				val jvmTest by getting {
					dependencies {
						implementation(kotlin("test", version = kotlinVersion))
						implementation(kotlin("test-junit", version = kotlinVersion))
						implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinSerializationVersion")
						implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
					}
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
						implementation(kotlin("test-js", version = kotlinVersion))
						implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$kotlinSerializationVersion")
						implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlinCoroutinesVersion")
					}
				}
			}
		}
	}
}