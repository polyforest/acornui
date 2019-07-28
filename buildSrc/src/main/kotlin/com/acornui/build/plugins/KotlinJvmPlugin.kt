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

		val kotlinJvmTarget: String by target.extra
		val kotlinLanguageVersion: String by target.extra

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
				jvm().compilations["main"].defaultSourceSet {
					dependencies {
						implementation(kotlin("stdlib-jdk8"))
					}
				}
				jvm().compilations["test"].defaultSourceSet {
					dependencies {
						implementation(kotlin("test"))
						implementation(kotlin("test-junit"))
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