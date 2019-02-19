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
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.File
import java.util.jar.JarFile

//------------------------------------------
// AcornUi Definitions
//------------------------------------------


object AcornUtils : Module(File(ACORNUI_HOME, "acornui-utils"), out = ACORNUI_OUT, dist = ACORNUI_DIST)

object AcornUiCore : Module(File(ACORNUI_HOME, "acornui-core"), out = ACORNUI_OUT, dist = ACORNUI_DIST) {
	init {
		moduleDependencies = listOf(AcornUtils)
	}
}

object AcornGame : Module(File(ACORNUI_HOME, "acornui-game"), out = ACORNUI_OUT, dist = ACORNUI_DIST) {

	init {
		moduleDependencies = listOf(AcornUtils, AcornUiCore)
	}
}

object AcornSpine : Module(File(ACORNUI_HOME, "acornui-spine"), out = ACORNUI_OUT, dist = ACORNUI_DIST) {

	init {
		moduleDependencies = listOf(AcornUtils, AcornUiCore)
	}
}


object AcornUiWebGlBackend : Module(File(ACORNUI_HOME, "backends/acornui-webgl-backend"), out = ACORNUI_OUT, dist = ACORNUI_DIST) {
	init {
		moduleDependencies = listOf(AcornUtils, AcornUiCore)
	}

	override fun buildAssets() {
		// Pull the kotlin-jslib out of the kotlin runtime jar.
		val cp = System.getProperty("java.class.path")

		val stdlibJs = File(cp.split(File.pathSeparatorChar).find { it.contains("kotlin-compiler-${KotlinCompilerVersion.VERSION}.jar") })
		if (sourcesAreNewer(listOf(stdlibJs.parentFile), File(outAssets, "lib/kotlin.js"))) {
			println("Extracting kotlin.js")
			outAssets.clean()
			val stdlibJsJar = JarFile(stdlibJs)
			JarUtil.extractFromJar(stdlibJsJar, outAssets, {
				if (it.name == "kotlin.js") "lib/kotlin.js" // place the kotlin.js file in the outAssets/lib directory.
				else null
			})
		}
	}
}

object AcornUiLwjglBackend : Module(File(ACORNUI_HOME, "backends/acornui-lwjgl-backend"), out = ACORNUI_OUT, dist = ACORNUI_DIST) {
	init {
		moduleDependencies = listOf(AcornUtils, AcornUiCore)
		jvmLibraryDependencies += listOf(rel("externalLib/compile"))
	}
}

val ALL_ACORNUI_MODULES = listOf(AcornUiWebGlBackend, AcornUiLwjglBackend, AcornUiCore, AcornGame, AcornSpine, AcornUtils)