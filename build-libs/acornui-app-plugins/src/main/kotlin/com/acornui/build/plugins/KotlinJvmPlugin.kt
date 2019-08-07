@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class KotlinJvmPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
		target.pluginManager.apply("kotlinx-serialization")

		val kotlinJvmTarget: String by target.extra
		val kotlinLanguageVersion: String by target.extra
		val kotlinSerializationVersion: String by target.extra

		target.extensions.configure<KotlinMultiplatformExtension> {
			jvm {
				compilations.all {
					kotlinOptions {
						jvmTarget = kotlinJvmTarget
						languageVersion = kotlinLanguageVersion
						apiVersion = kotlinLanguageVersion
					}
				}
			}
			sourceSets {
				@Suppress("UNUSED_VARIABLE")
				val commonMain by getting {
					dependencies {
						implementation(kotlin("stdlib-common"))
						implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$kotlinSerializationVersion")
					}
				}
				jvm().compilations["main"].defaultSourceSet {
					dependencies {
						implementation(kotlin("stdlib-jdk8"))
						implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinSerializationVersion")
					}
				}
				jvm().compilations["test"].defaultSourceSet {
					dependencies {
						implementation(kotlin("test"))
						implementation(kotlin("test-junit"))
						implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinSerializationVersion")
					}
				}
			}
		}

		target.afterEvaluate {
			tasks.withType(Test::class.java).configureEach {
				jvmArgs("-ea")
			}
		}
	}
}