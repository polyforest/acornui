@file:Suppress("UnstableApiUsage", "UNUSED_VARIABLE")

package com.acornui.build.plugins

import com.acornui.build.plugins.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

@Suppress("unused")
open class AcornUiApplicationPlugin : Plugin<Project> {

	internal val targets = listOf("js", "jvm")

	override fun apply(project: Project) {
		project.pluginManager.apply("org.gradle.idea")
		project.pluginManager.apply("com.acornui.kotlin-mpp")

		project.extensions.create<AcornUiApplicationExtension>("acornui").apply {
			appResources = project.buildDir.resolve("processedResources")
			www = project.buildDir.resolve("www")
			wwwProd = project.buildDir.resolve("wwwProd")
		}
		project.extensions.configure(multiPlatformConfig(project))

		project.applicationResourceTasks(targets, listOf("main"))
		project.appAssetsWebTasks()
		project.runJvmTask()
		project.uberJarTask()

		project.tasks.named<Delete>("clean") {
			doLast {
				delete(project.acornui.www)
				delete(project.acornui.wwwProd)
			}
		}
	}

	private fun multiPlatformConfig(target: Project): KotlinMultiplatformExtension.() -> Unit = {
		js {
			compilations.all {
				kotlinOptions {
					main = "call"
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
					implementation("com.acornui:acornui-lwjgl-backend")

					val lwjglVersion: String by target.extra
					val lwjglGroup = "org.lwjgl"
					val lwjglName = "lwjgl"

					val oses = listOf("linux", "macos", "windows")
					val extensions = arrayOf("glfw", "jemalloc", "opengl", "openal", "stb", "nfd", "tinyfd")
					for (os in oses) {
						runtimeOnly("$lwjglGroup:$lwjglName:$lwjglVersion:natives-$os")
						extensions.forEach {
							implementation("$lwjglGroup:$lwjglName-$it:$lwjglVersion")
							runtimeOnly("$lwjglGroup:$lwjglName-$it:$lwjglVersion:natives-$os")
						}
					}
				}
			}

			val jsMain by getting {
				dependencies {
					implementation("com.acornui:acornui-webgl-backend")
				}
			}
		}
	}
}

open class AcornUiApplicationExtension {

	lateinit var appResources: File
	lateinit var www: File
	lateinit var wwwProd: File

	/**
	 * The directory to place the .js files.
	 * Relative to the [www] and [wwwProd] directories.
	 */
	var jsLibPath = "lib"
}

fun Project.acornui(init: AcornUiApplicationExtension.() -> Unit) {
	the<AcornUiApplicationExtension>().apply(init)
}

val Project.acornui
	get() : AcornUiApplicationExtension {
		return the()
	}