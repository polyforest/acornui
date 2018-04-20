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

import com.acornui.build.util.*
import java.io.File
import java.util.jar.JarFile

//------------------------------------------
// AcornUi Definitions
//------------------------------------------


private val ACORNUI_HOME = BuildUtil.ACORNUI_HOME_PATH
private val ACORNUI_DIST = BuildUtil.ACORNUI_DIST
private val ACORNUI_OUT = BuildUtil.ACORNUI_OUT

object AcornUtils : Module(File(ACORNUI_HOME, "AcornUtils"), out = ACORNUI_OUT, dist = ACORNUI_DIST)

object AcornUiCore : Module(File(ACORNUI_HOME, "AcornUiCore"), out = ACORNUI_OUT, dist = ACORNUI_DIST) {
	init {
		moduleDependencies = listOf(AcornUtils)
	}
}

object AcornGame : Module(File(ACORNUI_HOME, "AcornGame"), out = ACORNUI_OUT, dist = ACORNUI_DIST) {

	init {
		moduleDependencies = listOf(AcornUtils, AcornUiCore)
	}
}

object AcornSpine : Module(File(ACORNUI_HOME, "AcornSpine"), out = ACORNUI_OUT, dist = ACORNUI_DIST) {

	init {
		moduleDependencies = listOf(AcornUtils, AcornUiCore)
	}
}


object AcornUiJsBackend : Module(File(ACORNUI_HOME, "AcornUiJsBackend"), out = ACORNUI_OUT, dist = ACORNUI_DIST) {
	init {
		moduleDependencies = listOf(AcornUtils, AcornUiCore)
		hasJvm = false
	}

	override fun buildAssets() {
		// Pull the kotlin-jslib out of the kotlin runtime jar.
		val cp = System.getProperty("java.class.path")
		val indexB = cp.indexOf("kotlin-runtime.jar")
		val indexA = cp.lastIndexOf(";", indexB) + 1
		val runtimeLibFolder = File(cp.substring(indexA, indexB))
		if (sourcesAreNewer(listOf(runtimeLibFolder) + resources, File(outAssets, "lib/kotlin.js"))) {
			println("Extracting kotlin.js")
			outAssets.clean()

			val jsLib = File(runtimeLibFolder, "kotlin-jslib.jar")
			val jsLibJar = JarFile(jsLib)
			JarUtil.extractFromJar(jsLibJar, outAssets, {
				if (it.name == "kotlin.js") "lib/kotlin.js" // place the kotlin.js file in the outAssets/lib directory.
				else null
			})

			for (resDir in resources) {
				if (resDir.exists()) resDir.copyRecursively(File(outAssets, "assets/"))
			}
		}
	}
}

object AcornUiLwjglBackend : Module(File(ACORNUI_HOME, "AcornUiLwjglBackend"), out = ACORNUI_OUT, dist = ACORNUI_DIST) {
	init {
		moduleDependencies = listOf(AcornUtils, AcornUiCore)
		hasJs = false
		jvmLibraryDependencies += listOf(rel("externalLib/compile"))
	}
}

val ALL_ACORNUI_MODULES = listOf(AcornUiJsBackend, AcornUiLwjglBackend, AcornUiCore, AcornGame, AcornSpine, AcornUtils)