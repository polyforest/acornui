/*
 * Copyright 2015 Nicholas Bilyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.build

import com.acornui.build.util.clean
import com.acornui.logging.Log
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


object BuildUtil {

	val buildVersion = File("build.txt")

	val copyIfNewer: Function2<File, IOException, OnErrorAction> = {
		_, exception ->
		if (exception is FileAlreadyExistsException && exception.other != null) {
			val other = exception.other!!
			if (exception.file.lastModified() > other.lastModified()) {
				exception.file.copyTo(other, true)
			}
			OnErrorAction.SKIP
		} else {
			OnErrorAction.TERMINATE
		}
	}

	var ACORNUI_HOME_PATH: String = System.getenv()["ACORNUI_HOME"] ?: throw Exception("Environment variable ACORNUI_HOME must be set.")
	var ACORNUI_DIST: File = File(ACORNUI_HOME_PATH, "dist")
	var ACORNUI_OUT: File = File(ACORNUI_HOME_PATH, "out")
	private val ACORNUI_WEBDIST_PATH: String = System.getenv()["ACORNWEBDIST_HOME"] ?: throw Exception("Environment variable ACORNSEBDIST_HOME must be set.")
	val ACORNUI_DOCS: File = File(ACORNUI_HOME_PATH, "docs")

	init {
		if (!File(ACORNUI_HOME_PATH).exists()) throw Exception("ACORNUI_HOME: '$ACORNUI_HOME_PATH' does not exist.")
		ACORNUI_DIST.mkdir()
		ACORNUI_OUT.mkdir()
		incBuildNumber()

		val buildToolsKotlinVer = KotlinCompilerVersion.VERSION
		val compilerJar = File(System.getProperty("java.class.path").split(System.getProperty("path.separator")).find { it.contains("kotlin-compiler.jar") })
		val kotlinVersion = File(compilerJar.parentFile.parentFile, "build.txt").readText().substringBefore("-")
		Log.info("Kotlin version $buildToolsKotlinVer")
		if (kotlinVersion.trim() != buildToolsKotlinVer.trim()) {
			Log.warn("Build tools may need to be rebuilt. Kotlin Version: $kotlinVersion")
		}
	}

	/**
	 * Every time the build utility fires up, increment the build version string from the [buildVersion] file.
	 */
	private fun incBuildNumber() {
		if (buildVersion.exists()) {
			val str = buildVersion.readText()
			val str2 = (str.toInt() + 1).toString()
			buildVersion.writeText(str2)
		} else {
			buildVersion.writeText("1")
		}
	}

	fun execute(allModules: List<Module>, args: Array<String>) {
		val argMap = ArgumentMap(args)
		Module.force = argMap.exists("force")

		val target = getTarget(argMap.get("target", default = Targets.BUILD.name))
		if (target == null) {
			println("Usage: -target=[build|clean|documentation|deploy|exe] -modules=[moduleName1,moduleName2|all] [-js] [-jvm]")
			System.exit(-1)
		}
		println("Target $target")
		val selectedModules = ArrayList<Module>()

		val moduleNames = argMap.get("modules", default = "all").toLowerCase()
		if (moduleNames == "all") selectedModules.addAll(allModules)
		else {
			val modulesSplit = moduleNames.split(",")
			for (i in 0..modulesSplit.lastIndex) {
				val moduleName = modulesSplit[i].trim()
				val found = allModules.firstOrNull { it.name.toLowerCase() == moduleName.toLowerCase() } ?: throw Exception("No module found with the name $moduleName")
				selectedModules.add(found)
			}
		}
		Module.verbose = argMap.exists("verbose")

		var js = argMap.exists("js")
		var jvm = argMap.exists("jvm")
		if (!js && !jvm) {
			js = true
			jvm = true
		}

		when (target) {
			Targets.CLEAN -> {
				selectedModules.map(Module::clean)
			}
			Targets.ASSETS -> {
				assets(selectedModules, js, jvm)
			}
			Targets.DOCUMENTATION -> {
				documentation(selectedModules)
			}
			Targets.BUILD -> {
				build(selectedModules, js, jvm)
			}
			Targets.DEPLOY -> {
				deploy(selectedModules, js, jvm)
			}
			Targets.WIN32 -> throw Exception("win32 is not currently supported.")
			Targets.WIN64 -> getFirstJvmModule(selectedModules).win64()
			Targets.MAC64 -> getFirstJvmModule(selectedModules).mac64()
			Targets.LINUX64 -> getFirstJvmModule(selectedModules).linux64()
			null -> TODO()
		}
	}

	private fun getTarget(target: String): Targets? {
		return try {
			Targets.valueOf(target.toUpperCase())
		} catch (e: Throwable) {
			null
		}
	}

	private fun deploy(selectedModules: List<Module>, js: Boolean, jvm: Boolean) {
		build(selectedModules, js, jvm)
		selectedModules.walkDependenciesBottomUp {
			if (js) it.deployJs()
			if (jvm) it.deployJvm()
			it.deploySources()
		}
	}

	private fun getFirstJvmModule(selectedModules: List<Module>): JvmModule {
		if (selectedModules.size != 1) {
			println("Requires one jvm module. Use -modules=moduleName")
			System.exit(-1)
		}
		deploy(selectedModules, js = false, jvm = true)
		return selectedModules.first() as? JvmModule ?: throw Exception("Only JvmModules may be used to create executables.")
	}

	private fun build(selectedModules: List<Module>, js: Boolean, jvm: Boolean) {
		selectedModules.walkDependenciesBottomUp {
			if (js && it.hasJs || jvm && it.hasJvm) {
				println("Building ${it.name}")
				it.buildAssets()
				if (js) it.buildJs()
				if (jvm) it.buildJvm()
				it.mergeAssets()
			}
		}
	}

	private fun assets(selectedModules: List<Module>, js: Boolean, jvm: Boolean) {
		selectedModules.walkDependenciesBottomUp {
			if (js && it.hasJs || jvm && it.hasJvm) {
				println("Build Assets ${it.name}")
				it.buildAssets()
				println("Deploying assets ${it.name}")
				it.mergeAssets()
			}
		}
	}

	private fun documentation(selectedModules: List<Module>) {
		if (!File(ACORNUI_WEBDIST_PATH).exists()) throw Exception("ACORNWEBDIST_HOME: '$ACORNUI_WEBDIST_PATH' does not exist.")
		val acornUiWebDocs = File(ACORNUI_WEBDIST_PATH, "docs")
		acornUiWebDocs.mkdir()

		var sources: Set<File> = setOf()
		var classpath: Set<String> = System.getProperty("java.class.path").split(PATH_SEPARATOR).toSet()

		ACORNUI_DOCS.clean()
		selectedModules.walkDependenciesBottomUp {
			if (it.hasJvm) {
				it.buildJvm()
				it.deployJvm()
			}

			sources += it.sources

			val libraryFiles = ArrayList<String>()
			for (i in it.moduleDependencies) {
				libraryFiles.add(i.jvmJar.absolutePath)
			}
			classpath += libraryFiles
		}
		val jar = "Tools/BuildTasks/externalLib/runtime/dokka-fatjar-0.9.17.jar"
		val cmd = arrayOf("java", "-jar", jar, "-src", sources.joinToString(PATH_SEPARATOR), "-output", "docs", "-classpath", classpath.joinToString(PATH_SEPARATOR))
		cmd.runCommand()

		acornUiWebDocs.clean()
		ACORNUI_DOCS.copyRecursively(acornUiWebDocs)
	}

	private fun Array<String>.runCommand(workingDir: File = File(".")) {
		ProcessBuilder(*this)
				.directory(workingDir)
				.redirectOutput(ProcessBuilder.Redirect.INHERIT)
				.redirectError(ProcessBuilder.Redirect.INHERIT)
				.start()
				.waitFor(60, TimeUnit.MINUTES)
	}

	private fun List<Module>.walkDependenciesBottomUp(callback: (Module) -> Unit) {
		val exclude = HashMap<Module, Boolean>()
		map {
			it.walkDependenciesBottomUp(exclude) {
				d ->
				callback(d)
			}
		}
	}
}

enum class Targets {
	CLEAN,
	ASSETS,
	DOCUMENTATION,
	BUILD,
	DEPLOY,
	WIN32,
	WIN64,
	MAC64,
	LINUX64
}