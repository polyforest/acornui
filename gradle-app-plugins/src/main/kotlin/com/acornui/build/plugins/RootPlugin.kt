@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins

import com.acornui.build.AcornDependencies
import com.acornui.build.plugins.util.isAcornUiComposite
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

		if (target.isAcornUiComposite) {
			// Sets the build dir for the included acorn projects so that multiple acorn composite projects don't
			// conflict. This is mainly for the JS side - the npm dependencies will include the root project name, which
			// won't match from one faux-composite project to the next.
			val acornLibraries = listOf("lwjgl-backend", "webgl-backend", "core", "game", "spine", "utils", "test-utils").map { ":acornui-$it" }
			acornLibraries.forEach { id ->
				target.findProject(id)?.let { foundProject ->
					foundProject.group = "com.acornui"
					foundProject.version = acornVersion
					foundProject.buildDir = target.buildDir.resolve("acornui/${foundProject.name}")
				}
			}
		}

		target.allprojects {
			AcornDependencies.putVersionProperties(project.extra)
			repositories {
				if (acornVersion.endsWith("-SNAPSHOT")) {
					maven("https://oss.sonatype.org/content/repositories/snapshots")
					mavenLocal()
				}
				mavenCentral()
				jcenter()
				maven("https://dl.bintray.com/kotlin/kotlin-eap/")
			}

			if (!target.isAcornUiComposite) {
				project.configurations.configureEach {
					resolutionStrategy {
						eachDependency {
							if (requested.group.startsWith("com.acornui")) {
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