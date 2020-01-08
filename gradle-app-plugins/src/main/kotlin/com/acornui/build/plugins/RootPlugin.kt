package com.acornui.build.plugins

import com.acornui.build.AcornDependencies
import com.acornui.build.plugins.util.preventSnapshotDependencyCaching
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.repositories

@Suppress("unused")
class RootPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		val acornVersion: String by target.extra
		target.preventSnapshotDependencyCaching()

		target.pluginManager.apply("org.jetbrains.dokka")

		target.allprojects {
			AcornDependencies.addVersionProperties(it.extra)

//			it.pluginManager.apply("org.gradle.idea")

			it.repositories {
				mavenLocal()
				jcenter()
				maven { mavenArtifactRepository ->
					mavenArtifactRepository.url = it.uri("https://dl.bintray.com/kotlin/kotlin-dev/")
				}
				maven { mavenArtifactRepository ->
					mavenArtifactRepository.url = it.uri("http://artifacts.acornui.com/mvn/")
				}
			}

			it.configurations.all { configuration ->
				configuration.resolutionStrategy { resolutionStrategy ->
					resolutionStrategy.eachDependency { dependencyResolveDetails ->
						when {
							dependencyResolveDetails.requested.group.startsWith("com.acornui") -> {
								dependencyResolveDetails.useVersion(acornVersion)
							}
						}
					}
				}
			}
		}
	}
}