/*
 * Copyright 2019 Nicholas Bilyk
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

package com.acornui.build.util

import java.io.File

@Deprecated("Use apply", ReplaceWith("apply(f)"))
inline fun <T> T.with(f: T.() -> Unit): T { this.f(); return this }

fun ArrayList<File>.toStringList(): MutableList<String> {
	val arr = ArrayList<String>()
	for (i in this) {
		arr.add(i.absolutePath)
	}
	return arr
}

val PATH_SEPARATOR: String = System.getProperty("path.separator")

val ACORNUI_HOME_PATH: String by lazy {
	val acornUiHome = System.getenv()["ACORNUI_HOME"] ?: throw Exception("Environment variable ACORNUI_HOME must be set.")
	if (!File(acornUiHome).exists()) throw Exception("ACORNUI_HOME '$acornUiHome' does not exist.")
	acornUiHome
}

val ACORNUI_HOME = File(ACORNUI_HOME_PATH)

val ACORNUI_DIST by lazy {
	val f = File(ACORNUI_HOME_PATH, "dist")
	f.mkdir()
	f
}

val ACORNUI_OUT by lazy {
	val f = File(ACORNUI_HOME_PATH, "out")
	f.mkdir()
	f
}