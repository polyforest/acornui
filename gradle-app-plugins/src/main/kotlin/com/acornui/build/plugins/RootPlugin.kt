@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins

import com.acornui.build.AcornDependencies
import com.acornui.build.plugins.util.preventSnapshotDependencyCaching
import com.acornui.build.util.addAcornRepositories
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.repositories
import java.net.URI

@Suppress("unused")
class RootPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		val acornUiHome: String? by target.extra
		val acornVersion: String by target.extra
		target.logger.lifecycle("**** APPLY " + acornUiHome + " : " + target.projectDir)
		val isComposite = acornUiHome != null && target.file(acornUiHome!!).exists() && !target.projectDir.startsWith(acornUiHome!!)
		target.logger.lifecycle("isComposite=$isComposite")

		target.preventSnapshotDependencyCaching()

		target.pluginManager.apply("org.jetbrains.dokka")

		val acornLibraries = listOf("utils", "core", "game", "spine", "test-utils", "lwjgl-backend", "webgl-backend").map { ":acornui-$it" }
		if (isComposite) {
			acornLibraries.forEach { id ->
				target.findProject(id)?.let { foundProject ->
					foundProject.group = "com.acornui"
					foundProject.buildDir = target.rootProject.buildDir.resolve("acornui/${foundProject.name}")
				}
			}
		}

		target.allprojects {
			AcornDependencies.putVersionProperties(project.extra)
			repositories {
				mavenLocal()
				maven {
					url = URI("https://maven.pkg.github.com/polyforest/acornui")
					credentials {
						username = "anonymous"
						password = "a1b92e4b7ff208be2b7f0f8524bb2a48566079f9"
					}
				}
				gradlePluginPortal()
				jcenter()
				maven {
					url = URI("https://dl.bintray.com/kotlin/kotlin-eap/")
				}
			}

			project.configurations.configureEach {
				resolutionStrategy {
					// A workaround to composite builds not working - https://youtrack.jetbrains.com/issue/KT-30285
					if (isComposite) {
						dependencySubstitution {
							acornLibraries.forEach { id ->
								if (findProject(id) != null) {
									substitute(module("com.acornui$id")).with(project(id))
								}
							}
						}
					}
					eachDependency {
						when {
							requested.group.startsWith("com.acornui") -> {
								useVersion(acornVersion)
							}
						}
					}
				}
			}

			tasks.findByPath("jsBrowserDistribution")?.let {
				// In Kotlin 1.3.70 this isn't ready yet, it will be overridden in application projects.
				it.enabled = false
			}
		}
	}
}