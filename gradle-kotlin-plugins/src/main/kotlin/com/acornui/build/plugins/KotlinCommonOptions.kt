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

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

object KotlinCommonOptions {

	@Suppress("UnstableApiUsage")
	fun configure(project: Project) {
		val kotlinVersion: String by project
		val kotlinSerializationVersion: String by project
		val kotlinCoroutinesVersion: String by project
		val kotlinLanguageVersion: String by project

		project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
		project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

		project.extensions.configure<KotlinMultiplatformExtension> {
			targets.configureEach {
				compilations.configureEach {
					kotlinOptions {
						languageVersion = kotlinLanguageVersion
						apiVersion = kotlinLanguageVersion
					}
				}
			}
			sourceSets {
				all {
					languageSettings.apply {
						enableLanguageFeature("InlineClasses")
						useExperimentalAnnotation("kotlin.Experimental")
						useExperimentalAnnotation("kotlin.time.ExperimentalTime")
						useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
					}
				}

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
			}
		}
	}
}