@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.util

import com.acornui.build.plugins.acornuiApp
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

fun Project.configureWebTasks() {

	for (variant in arrayOf(BuildVariantKind.DEVELOPMENT, BuildVariantKind.PRODUCTION)) {
		val variantName = variant.name.toLowerCase().capitalize()

		tasks.register<Sync>("jsAcornBrowser${variantName}Distribution") {
			dependsOn("jsProcessResources", "jsBrowser${variantName}Webpack")

			@Suppress("LiftReturnOrAssignment")
			when (variant) {
				BuildVariantKind.DEVELOPMENT -> {
					into(acornuiApp.www)
					description = "Combines all dependent resources into the acornuiApp.www directory <${acornuiApp.www}>."
				}
				BuildVariantKind.PRODUCTION -> {
					into(acornuiApp.wwwProd)
					description = "Combines all dependent resources into the acornuiApp.wwwProd directory <${acornuiApp.wwwProd}>."
				}
			}

			val compilation = project.getRunnableCompilation("js", "main")
			from({
				compilation.runtimeDependencyFiles.filter { file ->
					file.name.endsWith("jar")
				}.map { file ->
					project.zipTree(file).matching {
						include("assets/**")
					}
				}
			})
			from(compilation.output.resourcesDir)

			val webpackTask = tasks.named<KotlinWebpack>("jsBrowser${variantName}Webpack").get()
			from(webpackTask.outputFile)
			from(webpackTask.outputFile.resolveSibling(webpackTask.outputFile.nameWithoutExtension + ".js.map"))
		}

	}
	tasks.named<Copy>("jsBrowserDistribution").configure {
		enabled = false
		finalizedBy("jsAcornBrowserDevelopmentDistribution", "jsAcornBrowserProductionDistribution")
	}

}