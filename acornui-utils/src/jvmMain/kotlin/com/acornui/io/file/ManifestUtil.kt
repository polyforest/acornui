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

import com.acornui.replace2
import java.io.File
import java.nio.file.Files


/**
 * @author nbilyk
 */
object ManifestUtil {

	/**
	 * Creates a json manifest file of all the files in the given directory. Paths are relative to the provided
	 * root directory.
	 */
	fun createManifest(directory: File, root: File = directory): FilesManifest {
		if (!directory.exists() || !directory.isDirectory) throw IllegalArgumentException("directory does not exist ${directory.absolutePath}")
		if (!root.exists()) throw IllegalArgumentException("root does not exist ${root.absolutePath}")
		if (!root.isDirectory) throw IllegalArgumentException("root is not a directory")
		val fileEntries = ArrayList<ManifestEntry>()
		for (file in directory.walkTopDown()) {
			if (!file.isDirectory) {
				val relativePath = file.absoluteFile.toRelativeString(root.absoluteFile)
				val contentType = Files.probeContentType(file.toPath())
				fileEntries.add(ManifestEntry(relativePath.replace2('\\', '/'), file.lastModified(), file.length(), contentType))
			}
		}
		fileEntries.sort()

		return FilesManifest(fileEntries)
	}

}
