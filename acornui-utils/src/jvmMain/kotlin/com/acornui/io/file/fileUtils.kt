/*
 * Copyright 2019 Poly Forest, LLC
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

package com.acornui.io.file


import java.io.File
import java.io.IOException

/**
 * It's a copy of the deprecated relativePath method, because relativeTo doesn't do the same thing.
 */
fun File.relativePath2(descendant: File): String {
	val prefix = canonicalPath
	val answer = descendant.canonicalPath
	return if (answer.startsWith(prefix)) {
		val prefixSize = prefix.length
		if (answer.length > prefixSize) {
			answer.substring(prefixSize + 1)
		} else ""
	} else {
		answer
	}
}

fun File.copyRecursively(
		target: File,
		filter: (sourceFile: File, destFile: File?) -> Boolean
): Boolean {
	if (!exists()) {
		return false
	}
	for (src in walkTopDown()) {
		val relPath = src.toRelativeString(this)
		val dstFile = File(target, relPath)

		if (filter(src, dstFile)) {
			if (src.isDirectory) {
				dstFile.mkdirs()
			} else {
				if (src.copyTo(dstFile, overwrite = true).length() != src.length()) {
					throw IOException("Source file wasn't copied completely, length of destination file differs.")
				}
			}
		}

	}
	return true
}
