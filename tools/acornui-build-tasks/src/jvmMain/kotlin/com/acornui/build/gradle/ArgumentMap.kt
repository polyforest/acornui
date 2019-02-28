/*
 * Copyright 2019 PolyForest
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

package com.acornui.build.gradle

import java.io.File

/**
 * Represents the argument array (such as it is used in the command line) in a way that makes it easy to retrieve
 * specific properties.
 */
class ArgumentMap(private val args: Array<String>) {

	fun exists(name: String): Boolean {
		val str = "-" + name.toLowerCase()
		val str2 = str + "="
		val found = args.firstOrNull { it.toLowerCase() == str || it.toLowerCase().startsWith(str2) }
		return found != null
	}

	fun get(name: String): String? {
		val str = "-" + name.toLowerCase() + "="
		val index = args.indexOfFirst { it.toLowerCase().startsWith(str) }
		if (index == -1) return null
		return args[index].substring(name.length + 2)
	}

	fun get(name: String, alias: String, default: String): String {
		if (exists(name)) return get(name, default)
		return get(alias, default)
	}

	fun get(name: String, default: String): String {
		val str = "-" + name.toLowerCase() + "="
		val index = args.indexOfFirst { it.toLowerCase().startsWith(str) }
		if (index == -1) return default
		return args[index].substring(name.length + 2)
	}
}

fun ArgumentMap.getFileArg(arg: String) = File(get(arg)?.removeSurrounding("[", "]"))

fun ArgumentMap.getFileArgs(vararg args: String): List<File> {
	val result = mutableListOf<File>()
	for (arg in args)
		getFileArg(arg).let { result.add(it) }
	return result.toList()
}
