@file:Suppress("UnstableApiUsage", "UNUSED_VARIABLE", "EXPERIMENTAL_API_USAGE")

package com.acornui.build.plugins

import com.acornui.build.plugins.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

@Suppress("unused")
open class AcornUiApplicationPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		project.extensions.create<AcornUiApplicationExtension>("acornuiApp").apply {
			www = project.buildDir.resolve("www")
			wwwProd = project.buildDir.resolve("wwwProd")
		}
		project.pluginManager.apply("com.acornui.kotlin-mpp")
		project.extensions.configure(multiPlatformConfig(project))

		project.configureResourceProcessingTasks()
		project.configureWebTasks()
		project.configureRunJvmTask()
		project.configureUberJarTask()

	}

	private fun multiPlatformConfig(target: Project): KotlinMultiplatformExtension.() -> Unit = {
		js {
			compilations.all {
				kotlinOptions {
					main = "call"
					metaInfo = false
					sourceMap = true
				}
			}

			browser {
				webpackTask {
					enabled = true
					val baseConventions = project.convention.plugins["base"] as BasePluginConvention?
					outputFileName =  baseConventions?.archivesBaseName + "-${mode.code}.js"
					sourceMaps = true
				}
			}
		}

		sourceSets {
			all {
				languageSettings.progressiveMode = true
			}

			val commonMain by getting {
				dependencies {
					implementation("com.acornui:acornui-core")
					implementation("com.acornui:acornui-utils")
				}
			}

			val jvmMain by getting {
				dependencies {
					api("com.acornui:acornui-lwjgl-backend")

					val lwjglVersion: String by target.extra
					val lwjglGroup = "org.lwjgl"
					val lwjglName = "lwjgl"

					// FIXME: I have no idea why this can't be in lwjgl-backend
					val extensions = arrayOf("glfw", "jemalloc", "opengl", "openal", "stb", "nfd", "tinyfd")
					for (os in listOf("linux", "macos", "windows")) {
						runtimeOnly("$lwjglGroup:$lwjglName:$lwjglVersion:natives-$os")
						extensions.forEach {
							runtimeOnly("$lwjglGroup:$lwjglName-$it:$lwjglVersion:natives-$os")
						}
					}
				}
			}

			val jsMain by getting {
				dependencies {
//					compileOnly(npm("html-webpack-plugin", version = "3.2.0"))
					implementation("com.acornui:acornui-webgl-backend")
				}
			}
		}
	}
}

open class AcornUiApplicationExtension {

	lateinit var www: File
	lateinit var wwwProd: File

	/**
	 * The directory to place the .js files.
	 * Relative to the [www] and [wwwProd] directories.
	 */
	var jsLibPath = "lib"

}

fun Project.acornuiApp(init: AcornUiApplicationExtension.() -> Unit) {
	the<AcornUiApplicationExtension>().apply(init)
}

val Project.acornuiApp
	get() : AcornUiApplicationExtension {
		return the()
	}