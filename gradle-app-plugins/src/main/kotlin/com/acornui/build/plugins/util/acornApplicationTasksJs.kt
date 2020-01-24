@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.util

import com.acornui.build.plugins.acornuiApp
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariantKind
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

fun Project.configureWebTasks() {
	val jsExt = extensions.create<AcornUiJsExtension>("acornuiJs")
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

			filesMatching(jsExt.textFilePatterns.map { "*.$it" }) {
				this.filter(mapOf("tokens" to mapOf("jsFile" to webpackTask.outputFile.name)), ReplaceTokens::class.java)
			}
		}
	}
	tasks.register("jsDev") {
		dependsOn("jsAcornBrowserDevelopmentDistribution")
		group = "application"
		description = "An alias for jsAcornBrowserDevelopmentDistribution"
	}
	tasks.register("jsProd") {
		dependsOn("jsAcornBrowserProductionDistribution")
		group = "application"
		description = "An alias for jsAcornBrowserProductionDistribution"
	}
	tasks.named<Copy>("jsBrowserDistribution").configure {
		enabled = false
		//finalizedBy("jsAcornBrowserDevelopmentDistribution", "jsAcornBrowserProductionDistribution")
	}
	tasks.named("build").configure {
		dependsOn("jsProd")
	}

}

open class AcornUiJsExtension {

	/**
	 * The file extensions to be considered text files and transformed with token replacement. (lowercase)
	 */
	var textFilePatterns = listOf("asp", "aspx", "cfm", "cshtml", "css", "go", "htm", "html", "json", "jsp", "jspx",
			"php", "php3", "php4", "phtml", "rhtml", "txt", "properties")
}

fun Project.acornuiJs(init: AcornUiJsExtension.() -> Unit) {
	the<AcornUiJsExtension>().apply(init)
}

val Project.acornuiJs: AcornUiJsExtension
	get() = the()