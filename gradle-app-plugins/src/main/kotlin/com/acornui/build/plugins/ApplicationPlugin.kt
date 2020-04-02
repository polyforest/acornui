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
		val acornVersion: String by project
		println("Applying application plugin acornVersion=$acornVersion")
		project.extensions.create<AcornUiApplicationExtension>("acornuiApp").apply {
			www = project.buildDir.resolve("www")
		}
		project.pluginManager.apply(KotlinMppPlugin::class.java)
		project.extensions.configure(multiPlatformConfig(project))

		project.configureResourceProcessingTasks()
		if (project.jsEnabled)
			project.configureWebTasks()
		if (project.jvmEnabled) {
			project.configureRunJvmTask()
			project.configureUberJarTask()
		}
	}

	private fun multiPlatformConfig(target: Project): KotlinMultiplatformExtension.() -> Unit = {
		if (target.jsEnabled) {
			js {
				compilations.named("main") {
					kotlinOptions {
						main = "call"
					}
				}
				browser {
					webpackTask {
						enabled = true
						sourceMaps = true
					}
				}
			}
		}

		sourceSets {
			all {
				languageSettings.progressiveMode = true
			}

			val commonMain by getting {
				dependencies {
					api(acorn(target, "utils"))
					api(acorn(target, "core"))
				}
			}

			if (target.jvmEnabled) {
				val jvmMain by getting {
					dependencies {
						api(acorn(target, "lwjgl-backend"))

						val lwjglVersion: String by target
						val jorbisVersion: String by target
						val jlayerVersion: String by target
						val lwjglGroup = "org.lwjgl"
						val lwjglName = "lwjgl"
						val extensions = arrayOf("glfw", "jemalloc", "opengl", "openal", "stb", "nfd", "tinyfd")

						for (os in listOf("linux", "macos", "windows")) {
							runtimeOnly("$lwjglGroup:$lwjglName:$lwjglVersion:natives-$os")
							extensions.forEach {
								runtimeOnly("$lwjglGroup:$lwjglName-$it:$lwjglVersion:natives-$os")
							}
						}
					}
				}
			}

			if (target.jsEnabled) {
				val jsMain by getting {
					dependencies {
						api(acorn(target, "webgl-backend"))
					}
				}
			}
		}
	}
}

open class AcornUiApplicationExtension {

	lateinit var www: File

	@Deprecated("wwwProd and www no longer separated.", ReplaceWith("www"), DeprecationLevel.ERROR)
	lateinit var wwwProd: File

}

fun Project.acornuiApp(init: AcornUiApplicationExtension.() -> Unit) {
	the<AcornUiApplicationExtension>().apply(init)
}

val Project.acornuiApp
	get() : AcornUiApplicationExtension = the()