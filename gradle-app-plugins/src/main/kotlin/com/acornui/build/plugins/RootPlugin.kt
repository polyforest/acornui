@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins

import com.acornui.build.AcornDependencies
import com.acornui.build.plugins.util.preventSnapshotDependencyCaching
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.repositories

@Suppress("unused")
class RootPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		val acornVersion: String by target
		target.preventSnapshotDependencyCaching()

		target.allprojects {
			AcornDependencies.putVersionProperties(project.extra)
			repositories {
				mavenCentral()
				jcenter()
				maven("https://dl.bintray.com/kotlin/kotlin-eap/")

				if (acornVersion.endsWith("-SNAPSHOT")) {
					maven("https://oss.sonatype.org/content/repositories/snapshots")
					mavenLocal()
				}
			}

			project.configurations.configureEach {
				resolutionStrategy {
					eachDependency {
						when {
							requested.group.startsWith("com.acornui") -> useVersion(acornVersion)
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