@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.util

import com.acornui.build.plugins.acornuiApp
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.targets.js.dsl.BuildVariant
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

fun Project.appAssetsWebTasks() {

	val buildVariants = container(BuildVariant::class.java)
	println("Build variants ${buildVariants.joinToString()}")


	tasks.register<Sync>("jsAcornBrowserProductionDistribution") {
		dependsOn("jsProcessResources", "jsBrowserProductionWebpack")
		into(acornuiApp.wwwProd)

		description = "Combines all dependent resources into the acornuiApp.wwwProd directory <${acornuiApp.wwwProd}>."

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

		from(tasks.named<KotlinWebpack>("jsBrowserProductionWebpack").get().outputFile)
	}

	tasks.named<Copy>("jsBrowserDistribution").configure {
		enabled = false
		finalizedBy("jsAcornBrowserProductionDistribution")
	}
}