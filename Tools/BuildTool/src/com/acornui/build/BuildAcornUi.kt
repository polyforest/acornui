package com.acornui.build

import java.io.File

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

fun main(args: Array<String>) {
	val allModules = ArrayList(ALL_ACORNUI_MODULES)

	BuildUtil.execute(allModules, args)
}

/**
 * Creates [Module] objects for the js, jvm, and core sub-modules.
 */
fun generateModules(dir: File, out: ArrayList<Module>, skin: String?) {

	val coreModule = object : CoreModule(File(dir, "core"), name = dir.name + "Core", dist = File(dir, "core/dist")) {
		init {
			moduleDependencies = arrayListOf(AcornUtils, AcornUiCore)
		}
	}
	out.add(coreModule)

	val jsModule = object : JsModule(File(dir, "js"), name = dir.name + "Js", dist = File(dir, "js/dist")) {
		init {
			moduleDependencies = arrayListOf(coreModule, AcornUtils, AcornUiCore, AcornUiJsBackend)
			hasJvm = false
		}
	}
	out.add(jsModule)

	val jvmModule = object : JvmModule(File(dir, "jvm"), name = dir.name + "Jvm", dist = File(dir, "jvm/dist")) {
		init {
			moduleDependencies = arrayListOf(coreModule, AcornUtils, AcornUiCore, AcornUiLwjglBackend)
			hasJs = false
		}
	}
	out.add(jvmModule)
}

private fun <T> Array<T>.get(index: Int, default: T): T {
	if (size > index) return this[index]
	else return default
}