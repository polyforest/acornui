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

import kotlinx.serialization.Serializable

/**
 * @author nbilyk
 */
@Serializable
data class FilesManifest(
		val files: List<ManifestEntry>
)

@Serializable
data class ManifestEntry(
		val path: String,
		val modified: Long,
		val size: Long,

		/**
		 * The mime type of this file, if it can be determined.
		 */
		val mimeType: String?
) : Comparable<ManifestEntry> {

	fun name(): String {
		return path.substringAfterLast('/')
	}

	fun nameNoExtension(): String {
		return path.substringAfterLast('/').substringBeforeLast('.')
	}

	fun extension(): String {
		return path.substringAfterLast('.')
	}

	fun hasExtension(extension: String): Boolean {
		return extension().equals(extension, ignoreCase = true)
	}

	/**
	 * Calculates the number of directories deep this file entry is.
	 */
	fun depth(): Int {
		var count = -1
		var index = -1
		do {
			count++
			index = path.indexOf('/', index + 1)
		} while (index != -1)
		return count
	}

	override fun compareTo(other: ManifestEntry): Int {
		return if (depth() == other.depth()) {
			path.compareTo(other.path)
		} else {
			depth().compareTo(other.depth())
		}
	}

}