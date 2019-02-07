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

import com.acornui.logging.Log
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.File
import java.io.IOException


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

	init {
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
		if (args.isEmpty()) return // TODO: Temp
		val argMap = ArgumentMap(args)
		Module.force = argMap.exists("force")

		val target = getTarget(argMap.get("target", default = Targets.BUILD.name))
		if (target == null) {
			println("Usage: -target=[build|clean|deploy|exe] -modules=[moduleName1,moduleName2|all] [-js] [-jvm]")
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
			Targets.CLEAN -> selectedModules.map(Module::clean)
			Targets.ASSETS -> assets(selectedModules, js, jvm)
			Targets.BUILD -> build(selectedModules, js, jvm)
			Targets.DEPLOY -> deploy(selectedModules, js, jvm)
//			Targets.WIN32 -> throw Exception("win32 is not currently supported.")
//			Targets.WIN64 -> win64(selectedModules.appModules, js, jvm)
//			Targets.MAC64 -> mac64(selectedModules.appModules, js, jvm)
//			Targets.LINUX64 -> linux64(selectedModules.appModules, js, jvm)
			null -> TODO()
		}
	}

//	private fun win64(appModules: List<AppModule>, js: Boolean, jvm: Boolean) {
//		deploy(appModules, js, jvm)
//		for (appModule in appModules) {
//			appModule.win64()
//		}
//	}
//
//	private fun mac64(appModules: List<AppModule>, js: Boolean, jvm: Boolean) {
//		deploy(appModules, js, jvm)
//		for (appModule in appModules) {
//			appModule.mac64()
//		}
//	}
//
//	private fun linux64(appModules: List<AppModule>, js: Boolean, jvm: Boolean) {
//		deploy(appModules, js, jvm)
//		for (appModule in appModules) {
//			appModule.linux64()
//		}
//	}

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

	private val List<Module>.appModules: List<AppModule>
			get() = filterIsInstance<AppModule>()

	private fun build(selectedModules: List<Module>, js: Boolean, jvm: Boolean) {
		selectedModules.walkDependenciesBottomUp {
			println("Building ${it.name}")
			if (js) it.buildJs()
			if (jvm) it.buildJvm()
		}
		assets(selectedModules, js, jvm)
	}

	private fun assets(selectedModules: List<Module>, js: Boolean, jvm: Boolean) {
		selectedModules.walkDependenciesBottomUp {
			println("Build Assets ${it.name}")
			it.buildAssets()
		}
		selectedModules.appModules.map {
			if (js) it.mergeAssetsJs()
			if (jvm) it.mergeAssetsJvm()
		}
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
	BUILD,
	DEPLOY,
	WIN32,
	WIN64,
	MAC64,
	LINUX64
}