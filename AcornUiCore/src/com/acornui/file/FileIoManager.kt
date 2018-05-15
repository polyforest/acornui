/*
 * Copyright 2018 PolyForest
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

package com.acornui.file

import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.io.NativeBuffer

interface FileIoManager : Disposable {

	/**
	 * If false, [pickFileForSave] will fail.
	 */
	val saveSupported: Boolean

	fun pickFileForOpen(fileFilterGroups: List<FileFilterGroup>?, defaultPath: String, onSuccess: (FileReader?) -> Unit)
	fun pickFilesForOpen(fileFilterGroups: List<FileFilterGroup>?, defaultPath: String, onSuccess: (List<FileReader>?) -> Unit)

	fun pickFileForSave(fileFilterGroups: List<FileFilterGroup>?, defaultPath: String, defaultExtension: String? = null, onSuccess: (FileWriter?) -> Unit)

	companion object : DKey<FileIoManager>
}

/**
 * A list of extension groups to filter for when picking files.
 * @param extensions The list of extensions in this group.
 */
data class FileFilterGroup(
		val extensions: List<String>
)

interface FileReader {

	val name: String
	val size: Long
	val lastModified: Long

	suspend fun readAsString(): String?
	suspend fun readAsBinary(): NativeBuffer<Byte>?
}

interface FileWriter {

	val name: String
	val size: Long
	val lastModified: Long

	suspend fun saveToFileAsString(value: String)
	suspend fun saveToFileAsBinary(value: NativeBuffer<Byte>)
}