@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins

import com.acornui.build.AcornDependencies
import com.acornui.build.plugins.util.preventSnapshotDependencyCaching
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

@Suppress("unused")
class RootPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		target.extensions.create<AcornUiRootExtension>("acornuiRoot")

		val acornVersion: String by target.extra
		target.preventSnapshotDependencyCaching()

		target.pluginManager.apply("org.jetbrains.dokka")

		target.allprojects { project ->
			AcornDependencies.addVersionProperties(project.extra)
			project.repositories {
				mavenLocal()
				jcenter()
				maven { mavenArtifactRepository ->
					mavenArtifactRepository.url = project.uri("https://dl.bintray.com/kotlin/kotlin-dev/")
				}
				maven { mavenArtifactRepository ->
					mavenArtifactRepository.url = project.uri("http://artifacts.acornui.com/mvn/")
				}
			}

			project.configurations.all { configuration ->
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

			project.tasks.withType<ProcessResources>().configureEach { task: ProcessResources ->
				task.filteringCharset = "UTF-8"
				task.filesMatching(target.acornuiRoot.textFilePatterns) { fileCopyDetails ->
					val props = HashMap<String, String>()
					project.properties.forEach { props[it.key.toString()] = it.value.toString() }
					fileCopyDetails.filter(ReplaceTokens::class, "tokens" to props)
				}
			}
		}
	}
}


open class AcornUiRootExtension {

	/**
	 * The ant-style patterns for which resource files will have token replacement.
	 */
	var textFilePatterns = listOf("asp", "aspx", "cfm", "cshtml", "css", "go", "htm", "html", "json", "jsp", "jspx",
			"php", "php3", "php4", "phtml", "rhtml", "txt").map { "*.$it" }
}

fun Project.acornuiRoot(init: AcornUiRootExtension.() -> Unit) {
	the<AcornUiRootExtension>().apply(init)
}

val Project.acornuiRoot
	get() : AcornUiRootExtension {
		return the()
	}