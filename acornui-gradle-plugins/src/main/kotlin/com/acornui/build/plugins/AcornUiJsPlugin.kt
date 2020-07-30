@file:Suppress("UnstableApiUsage", "UNUSED_VARIABLE", "EXPERIMENTAL_API_USAGE")

package com.acornui.build.plugins

import com.acornui.build.plugins.tasks.replaceTokensFromProperties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension

@Suppress("unused")
open class AcornUiJsPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		project.repositories {
			maven("https://dl.bintray.com/kotlin/kotlin-eap")
			maven("https://oss.sonatype.org/content/repositories/snapshots")
			mavenCentral()
			jcenter()
			mavenLocal()
		}

		val kotlinVersion: String by project
		project.pluginManager.apply("org.jetbrains.kotlin.js")
		project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

		project.extensions.configure(kotlinJsConfig(project))
		project.replaceTokensFromProperties()
	}

	private fun kotlinJsConfig(project: Project): KotlinJsProjectExtension.() -> Unit = {
		val kotlinVersion: String by project
		val kotlinLanguageVersion: String by project
		val acornVersion: String by project

		js {
			browser {
				testTask {
					useMocha {
						// For async tests use runMainTest and runHeadlessTest which use their own timeout.
						timeout = "30s"
					}
				}
			}
		}

		sourceSets {
			all {
				languageSettings.apply {
					languageVersion = kotlinLanguageVersion
					apiVersion = kotlinLanguageVersion
					enableLanguageFeature("InlineClasses")
					useExperimentalAnnotation("kotlin.Experimental")
					useExperimentalAnnotation("kotlin.time.ExperimentalTime")
					useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
					useExperimentalAnnotation("kotlinx.coroutines.InternalCoroutinesApi")
				}
			}

			val main by getting {
				dependencies {
					// IE and Edge no longer supported.
//					implementation(npm("promise-polyfill", version = "8.1.3")) // For IE11
//					implementation(npm("resize-observer-polyfill", version = "1.5.1")) // For IE11 and Edge
					implementation("com.acornui:acornui-core:$acornVersion")
				}
			}

			val test by getting {
				dependencies {
					implementation(kotlin("test-js", version = kotlinVersion))
				}
			}
		}
	}
}
