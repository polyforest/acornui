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

package com.acornui.io.file

import com.acornui.serialization.*

/**
 * @author nbilyk
 */
data class FilesManifest(
		val files: List<ManifestEntry>
)

object FilesManifestSerializer : To<FilesManifest>, From<FilesManifest> {
	override fun FilesManifest.write(writer: Writer) {
		writer.array("files", files, ManifestEntrySerializer)
	}

	override fun read(reader: Reader): FilesManifest {
		return FilesManifest(files = reader.arrayList("files", ManifestEntrySerializer)!!)
	}
}

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
		if (depth() == other.depth()) {
			return path.compareTo(other.path)
		} else {
			return depth().compareTo(other.depth())
		}
	}

	override fun toString(): String {
		return "ManifestEntry(path = '$path')"
	}

}

object ManifestEntrySerializer : To<ManifestEntry>, From<ManifestEntry> {
	override fun ManifestEntry.write(writer: Writer) {
		writer.string("path", path)
		writer.long("modified", modified)
		writer.long("size", size)
		writer.string("mimeType", mimeType)
	}

	override fun read(reader: Reader): ManifestEntry {
		return ManifestEntry(
				path = reader.string("path")!!,
				modified = reader.long("modified")!!,
				size = reader.long("size")!!,
				mimeType = reader.string("mimeType")
		)
	}
}
