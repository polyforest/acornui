@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins

import com.acornui.build.AcornDependencies
import com.acornui.build.plugins.util.preventSnapshotDependencyCaching
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

@Suppress("unused")
class RootPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		val acornVersion: String by target.extra
		target.preventSnapshotDependencyCaching()

		target.pluginManager.apply("org.jetbrains.dokka")

		target.allprojects {
			AcornDependencies.putVersionProperties(project.extra)
			repositories {
				mavenLocal()
				jcenter()
				maven {
					url = project.uri("https://dl.bintray.com/kotlin/kotlin-dev/")
				}
				maven {
					url = project.uri("http://artifacts.acornui.com/mvn/")
				}
			}

			project.configurations.all {
				resolutionStrategy {
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