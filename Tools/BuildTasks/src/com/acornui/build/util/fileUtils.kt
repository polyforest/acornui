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

fun File.lastModifiedRecursive(): Long {
	var lastModified = 0L
	for (i in walkTopDown()) {
		if (i.isDirectory) continue
		val iLastModified = i.lastModified()
		if (iLastModified > lastModified) {
			lastModified = iLastModified
		}
	}
	return lastModified
}

fun List<File>.lastModifiedRecursive(): Long {
	var lastModified = 0L
	for (i in this) {
		val iLastModified = i.lastModifiedRecursive()
		if (iLastModified > lastModified) {
			lastModified = iLastModified
		}
	}
	return lastModified
}

fun sourcesAreNewer(sources: List<File>, targets: List<File>): Boolean {
	return sources.lastModifiedRecursive() > targets.lastModifiedRecursive()
}

fun sourcesAreNewer(sources: List<File>, target: File): Boolean {
	return sources.lastModifiedRecursive() > target.lastModifiedRecursive()
}

fun sourcesAreNewer(source: File, target: File): Boolean {
	return source.lastModifiedRecursive() > target.lastModifiedRecursive()
}

fun File.copyIfNewer(dest: File): Boolean {
	if (!dest.exists() || lastModified() > dest.lastModified()) {
		copyTo(dest, overwrite = true)
		return true
	}
	return false
}

/**
 * Deletes this folder recursively then recreates the directory.
 */
fun File.clean() {
	deleteRecursively()
	mkdirs()
}

/**
 * Sets the last modified date to the current time.
 */
fun File.touch() {
	setLastModified(System.currentTimeMillis())
}