package com.acornui.build.plugins.util

import com.acornui.build.plugins.acornui
import com.acornui.build.plugins.tasks.*
import com.acornui.io.file.FilesManifest
import com.acornui.io.file.ManifestUtil
import com.acornui.serialization.jsonStringify
import com.acornui.toCamelCase
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import java.io.File

fun Project.applicationResourceTasks(targets: Iterable<String>, compilations: Iterable<String>) {
	createBitmapFontGeneratorConfig()
	createPackTexturesConfig()

	targets.forEach { target ->
		val platformCapitalized = target.capitalize()
		compilations.forEach { compilationName ->
			val compilationNameCapitalized = compilationName.capitalize()
			val nameOrEmpty = if (compilationName == "main") "" else compilationNameCapitalized

			val unprocessedResourcesAllMain = project.buildDir.resolve("unprocessedResources/$target/all$compilationNameCapitalized")
			val processedResourcesAllMain = project.buildDir.resolve("processedResources/$target/all$compilationNameCapitalized")

			val combineAcornResources =
					project.tasks.register<Sync>("${target}CombineAcornResources") {
						into(unprocessedResourcesAllMain)

						description = """
							Combines all resources from dependent project source sets and the `assets` directory from 
							dependent jar artifacts into build/unprocessedResources/$target/all$compilationNameCapitalized.
							In order to improve iteration speed, this does not depend on the `jar` task. Any runtime 
							dependency that is within the build directory of an included project is swapped for that 
							projects' source resources.
						""".trimIndent()

						val compilation = project.getRunnableCompilation(target, compilationName)
						from({
							compilation.runtimeDependencyFiles.filter { file ->
								file.name.endsWith("jar")
							}.map { file ->
								val fromProject = rootProject.allprojects.find { file.startsWith(it.buildDir) }
								if (fromProject == null) {
									project.zipTree(file).matching {
										include("assets/**")
									}
								} else {
									val c = fromProject.getRunnableCompilation(target, compilationName)
									c.allKotlinSourceSets.map {
										it.resources.srcDirs
									}
								}
							}
						})

						compilation.allKotlinSourceSets.forEach {
							from(it.resources.srcDirs)
						}
					}

			val processAcornResources =
					project.tasks.register<AcornUiResourceProcessorTask>("${target}ProcessAcornResources") {

						dependsOn(combineAcornResources)
						from(unprocessedResourcesAllMain)
						into(processedResourcesAllMain)

						finalizedBy("${target}WriteResourcesManifest")
					}

			val writeManifest = tasks.register("${target}WriteResourcesManifest") {
				dependsOn(processAcornResources)
				onlyIfDidWork(processAcornResources)
				doLast {
					val assetsDir = processedResourcesAllMain.resolve("assets")
					if (assetsDir.exists()) {
						val manifest = ManifestUtil.createManifest(assetsDir, processedResourcesAllMain)
						assetsDir.resolve("files.json").writeText(
								jsonStringify(
										FilesManifest.serializer(),
										manifest
								)
						)
					}
				}
			}

			val processResources = tasks.named<ProcessResources>("$target${nameOrEmpty}ProcessResources") {
				dependsOn(processAcornResources, writeManifest)
				exclude("*")
			}

			val assemblePlatform = tasks.register("${target}Assemble") {
				group = "build"
				dependsOn(processResources, "compileKotlin$platformCapitalized")
			}

			tasks.named("assemble") {
				dependsOn(assemblePlatform)
			}
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
	val combinedResourcesDir = project.acornui.appResources.resolve("js/allMain")
	from(combinedResourcesDir) {
		filesMatching(replaceVersionStrPatterns) {
			filter { line ->
				replaceVersionWithModTime(line, combinedResourcesDir)
			}
		}
	}
}

fun Project.appAssetsWebTasks() {

	val assembleJs = tasks.named("jsAssemble")

	// Register the assembleWeb task that builds the www directory.

	val webAssemble = tasks.register<Sync>("webAssemble") {
		dependsOn(assembleJs)
		group = "build"

		into(acornui.www)

		from(kotlinMppRuntimeDependencies(project, "js")) {
			include("*.js", "*.js.map")
			into(acornui.jsLibPath)
		}

		addCombinedJsResources(project)

		doLast {
			File(acornui.www.resolve(acornui.jsLibPath), "files.js").writeText(
					"var manifest = " + File(
							acornui.www,
							"assets/files.json"
					).readText()
			)
		}
	}

	val webProdAssemble = tasks.register<Sync>("webProdAssemble") {
		dependsOn(assembleJs)
		group = "build"

		into(acornui.wwwProd)
		addCombinedJsResources(project)

		finalizedBy("jsDce", "jsOptimize")

		doLast {
			File(
					acornui.wwwProd.resolve(acornui.jsLibPath),
					"files.js"
			).writeText("var manifest = " + File(acornui.wwwProd, "assets/files.json").readText())
		}
	}

	tasks.named("assemble") {
		dependsOn(webAssemble, webProdAssemble)
	}

	val jsDce = tasks.register<DceTask>("jsDce") {
		val prodLibDir = acornui.wwwProd.resolve(acornui.jsLibPath)
		source.from(kotlinMppRuntimeDependencies(project, "js"))
		outputDir.set(prodLibDir)

		doLast {
			project.delete(fileTree(prodLibDir).include("*.map"))
		}
	}

	tasks.register<KotlinJsMonkeyPatcherTask>("jsOptimize") {
		shouldRunAfter(jsDce)
		sourceDir.set(acornui.wwwProd.resolve(acornui.jsLibPath))
	}

}

// https://kotlinlang.slack.com/archives/C3PQML5NU/p1548431040452500?thread_ts=1548348039.408400&cid=C3PQML5NU
// https://kotlinlang.org/docs/tutorials/javascript/getting-started-gradle/getting-started-with-gradle.html

/**
 * The following file patterns will be scanned for file.ext?version=%VERSION% matches, substituting %VERSION% with
 * the relative file's modified timestamp.
 */
var replaceVersionStrPatterns = listOf(
		"asp",
		"aspx",
		"cshtml",
		"cfm",
		"go",
		"jsp",
		"jspx",
		"php",
		"php3",
		"php4",
		"phtml",
		"html",
		"htm",
		"rhtml",
		"css"
).map { "*.$it" }

private val versionMatch = Regex("""([\w./\\]+)(\?[\w=&]*)(%VERSION%)""")

/**
 * Replaces %VERSION% tokens with the last modified timestamp.
 * The source must match the format:
 * foo/bar.ext?baz=%VERSION%
 * foo/bar.ext must be a local file.
 */
fun replaceVersionWithModTime(src: String, root: File): String {
	return versionMatch.replace(src) { match ->
		val path = match.groups[1]!!.value
		val relativeFile = root.resolve(path)
		if (relativeFile.exists()) path + match.groups[2]!!.value + relativeFile.lastModified()
		else path + match.groups[2]!!.value + System.currentTimeMillis()
	}
}

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
	val jvmArgs: String? by extra
	tasks.register<JavaExec>("runJvm") {
		dependsOn("jvmAssemble")
		group = "application"
		val jvmTarget: KotlinTarget = kotlinExt.targets["jvm"]
		val compilation =
				jvmTarget.compilations["main"] as KotlinCompilationToRunnableFiles<KotlinCommonOptions>

		val classes = files(
				compilation.runtimeDependencyFiles,
				compilation.output.allOutputs
		)
		classpath = classes
		workingDir = acornui.appResources.resolve("jvm/allMain")
		main =
				"${rootProject.group}.${rootProject.name}.jvm.${rootProject.name.toCamelCase().capitalize()}JvmKt"

		@Suppress("INACCESSIBLE_TYPE")
		this.jvmArgs = (jvmArgs?.split(" ") ?: listOf(
				"-ea",
				"-Ddebug=true"
		)) + if (OperatingSystem.current() == OperatingSystem.MAC_OS) listOf("-XstartOnFirstThread") else emptyList()
	}
}

fun Project.uberJarTask() {
	tasks.register<Jar>("uberJar") {
		dependsOn("jvmAssemble")
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
		from(acornui.appResources.resolve("jvm/allMain"))
		with(tasks["jvmJar"] as CopySpec)
	}
}

fun Project.jsBrowserTasks() {
//	tasks.register<KotlinWebpack>("asdf") {
//
//	}
}