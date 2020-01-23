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
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
open class KotlinJvmPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		KotlinCommonOptions.configure(target)
		configure(target)
	}

	companion object {

		fun configure(project: Project) {
			val kotlinVersion: String by project.extra
			val kotlinJvmTarget: String by project.extra
			val kotlinSerializationVersion: String by project.extra
			val kotlinCoroutinesVersion: String by project.extra

			project.extensions.configure<KotlinMultiplatformExtension> {
				jvm {
					compilations.all {
						kotlinOptions {
							jvmTarget = kotlinJvmTarget
						}
					}
				}
				sourceSets {
					all {
						languageSettings.progressiveMode = true
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
				}
			}

			project.afterEvaluate {
				tasks.withType(Test::class.java).configureEach {
					jvmArgs("-ea")
				}
			}
		}
	}
}