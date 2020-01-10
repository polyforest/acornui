@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.util

import com.acornui.build.plugins.acornuiApp
import com.acornui.build.plugins.tasks.createBitmapFontGeneratorConfig
import com.acornui.build.plugins.tasks.createPackTexturesConfig
import com.acornui.toCamelCase
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.gradle.kotlin.dsl.named

fun Project.applicationResourceTasks(targets: Iterable<String>, compilations: Iterable<String>) {
	createBitmapFontGeneratorConfig()
	createPackTexturesConfig()

	targets.forEach { target ->
		val platformCapitalized = target.capitalize()
		compilations.forEach { compilationName ->
			val compilationNameCapitalized = compilationName.capitalize()
			val unprocessedResourcesAllMain = project.buildDir.resolve("unprocessedResources/$target/all$compilationNameCapitalized")

//			val combineAcornResources =
//					project.tasks.register<Sync>("${target}CombineAcornResources") {
//						into(unprocessedResourcesAllMain)
//
//						description = """
//							Combines all dependent resources into ${unprocessedResourcesAllMain.path}.
//						""".trimIndent()
//
//						val compilation = project.getRunnableCompilation(target, compilationName)
//						from({
//							compilation.runtimeDependencyFiles.filter { file ->
//								file.name.endsWith("jar")
//							}.map { file ->
//								val fromProject = rootProject.allprojects.find { file.startsWith(it.buildDir) }
//								if (fromProject == null) {
//									project.zipTree(file).matching {
//										it.include("assets/**")
//									}
//								} else {
//									val c = fromProject.getRunnableCompilation(target, compilationName)
//									c.allKotlinSourceSets.map {
//										it.resources.srcDirs
//									}
//								}
//							}
//						})
//
//						compilation.allKotlinSourceSets.forEach {
//							from(it.resources.srcDirs)
//						}
//					}
//
//			val nameOrEmpty = if (compilationName == "main") "" else compilationNameCapitalized
//			val processedResourcesAllMain = project.buildDir.resolve("processedResources/$target/all$compilationNameCapitalized")
//
//			val processAcornResources =
//					project.tasks.register<AcornUiResourceProcessorTask>("${target}ProcessAcornResources") {
//						dependsOn(combineAcornResources)
//						from(unprocessedResourcesAllMain)
//						into(processedResourcesAllMain)
//
//						finalizedBy("${target}WriteResourcesManifest")
//					}
//
//			val writeManifest = tasks.register("${target}WriteResourcesManifest") {
//				it.dependsOn(processAcornResources)
//				it.onlyIfDidWork(processAcornResources)
//				it.doLast {
//					val assetsDir = processedResourcesAllMain.resolve("assets")
//					if (assetsDir.exists()) {
//						val manifest = ManifestUtil.createManifest(assetsDir, processedResourcesAllMain)
//						assetsDir.resolve("files.json").writeText(
//								jsonStringify(
//										FilesManifest.serializer(),
//										manifest
//								)
//						)
//					}
//				}
//			}
//
//			val processResources = tasks.named("$target${nameOrEmpty}ProcessResources") {
//				it as ProcessResources
//				it.dependsOn(processAcornResources, writeManifest)
//				it.exclude("*")
//			}

		}
	}
}

fun Project.getRunnableCompilation(target: String, compilationName: String): AbstractKotlinCompilationToRunnableFiles<*> {
	val unconfiguredDepError = "Target platform \"$target\" was not found for $displayName. Ensure that this dependency applies a kotlin multiplatform plugin."
	val kotlinTarget: KotlinTarget = project.kotlinExt.targets.named(target).orNull
			?: error(unconfiguredDepError)
	return (kotlinTarget.compilations.named(compilationName).orNull
			?: error(unconfiguredDepError)) as AbstractKotlinCompilationToRunnableFiles<*>
}

private fun Sync.addCombinedJsResources(project: Project) {
	val combinedResourcesDir = project.acornuiApp.appResources.resolve("js/allMain")
	from(combinedResourcesDir)
}

fun Project.appAssetsWebTasks() {
//	tasks.named<Copy>("jsBrowserDistribution").configure {
//		enabled = false
//
//		println("destinationDir: " + this.destinationDir)
//	}
}

// https://kotlinlang.slack.com/archives/C3PQML5NU/p1548431040452500?thread_ts=1548348039.408400&cid=C3PQML5NU
// https://kotlinlang.org/docs/tutorials/javascript/getting-started-gradle/getting-started-with-gradle.html

/**
 * Returns a file collection of all runtime dependencies.
 */
fun kotlinMppRuntimeDependencies(project: Project, platform: String, compilationName: String = "main"): ConfigurableFileCollection {
	return project.files().apply {
		builtBy("$platform${compilationName.capitalize()}Classes")
		val main =
				project.kotlinExt.targets[platform].compilations[compilationName] as AbstractKotlinCompilationToRunnableFiles<*>
		main.output.classesDirs.forEach { folder ->
			from(project.fileTree(folder))
		}
		main.runtimeDependencyFiles.forEach { file ->
			from(project.zipTree(file))
		}
	}
}

fun Project.runJvmTask() {
	tasks.register<RunJvmTask>("runJvm") {
		debugMode = true
	}
}

open class RunJvmTask : JavaExec() {

	init {
		dependsOn("jvmMainClasses", "jvmProcessResources")
		group = "application"
		val jvmTarget: KotlinTarget = project.kotlinExt.targets["jvm"]
		val compilation =
				jvmTarget.compilations["main"] as KotlinJvmCompilation

		classpath = project.files(
				compilation.runtimeDependencyFiles,
				compilation.output.allOutputs
		)
		workingDir = project.acornuiApp.appResources.resolve("jvm/allMain")
		main =
				"${project.rootProject.group}.${project.rootProject.name}.jvm.${project.rootProject.name.toCamelCase().capitalize()}JvmKt"

		@Suppress("INACCESSIBLE_TYPE")
		this.jvmArgs = if (OperatingSystem.current() == OperatingSystem.MAC_OS) listOf("-XstartOnFirstThread") else emptyList()
	}

	@Input
	var debugMode: Boolean = false
		set(value) {
			field = value
			val debugArg = "-Ddebug=true"
			jvmArgs = if (value)
				jvmArgs!! + debugArg
			else
				jvmArgs!! - debugArg
		}
}

fun Project.addUberJarTask() {
	tasks.register<Jar>("uberJar") {
		dependsOn("jvmJar")
		group = "build"
		archiveBaseName.set("${project.name}-uber")
		val mainClass = tasks.getByName<JavaExec>("runJvm").main
		manifest {
			attributes["Implementation-Version"] = project.version.toString()
			attributes["Main-Class"] = mainClass
		}
		val jvmTarget: KotlinTarget = kotlinExt.targets["jvm"]
		val compilation = jvmTarget.compilations["main"] as KotlinCompilationToRunnableFiles<KotlinCommonOptions>
		from({
			compilation.runtimeDependencyFiles.filter { it.name.endsWith("jar") }.map { zipTree(it) }
		})
		from(acornuiApp.appResources.resolve("jvm/allMain"))
		with(tasks["jvmJar"] as CopySpec)
	}
}