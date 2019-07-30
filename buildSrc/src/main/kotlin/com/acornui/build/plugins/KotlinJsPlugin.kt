@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class KotlinJsPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")

		val kotlinLanguageVersion: String by target.extra

		target.extensions.configure<KotlinMultiplatformExtension> {
			js {
//				browser {}
				compilations.all {
					kotlinOptions {
						moduleKind = "amd"
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
				js().compilations["main"].defaultSourceSet {
					dependencies {
						implementation(kotlin("stdlib-js"))
					}
				}
				js().compilations["test"].defaultSourceSet {
					dependencies {
						implementation(kotlin("test-js"))
					}
				}
			}
		}
	}
}