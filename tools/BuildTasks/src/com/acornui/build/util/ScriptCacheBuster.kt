/*
 * Copyright 2017 Nicholas Bilyk
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

/**
 * Searches for script and css include elements and adds a cache buster query parameter to them.
 */
object ScriptCacheBuster {

	val extensions = SourceExtensions.HTML_SOURCE_EXTENSIONS + "css"

	private val regex = Regex("""([\w./\\]+)(\?[\w=&]*)(%VERSION%)""")

	/**
	 * Replaces %VERSION% tokens with the last modified timestamp.
	 * The source must match the format:
	 * foo/bar.ext?baz=%VERSION%
	 * foo/bar.ext must be a local file.
	 */
	fun replaceVersionWithModTime(src: String, file: File): String {
		return regex.replace(src) {
			match ->
			val path = match.groups[1]!!.value
			val relativeFile = File(file.parent, path)
			if (relativeFile.exists()) path + match.groups[2]!!.value + relativeFile.lastModified()
			else match.value
		}
	}
}