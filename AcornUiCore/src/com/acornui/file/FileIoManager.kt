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

interface FileIoManager: Disposable {
	val saveSupported: Boolean

	fun pickFileForOpen(extensions: String?, defaultPath: String, onSuccess: (FileReaderWriter?) -> Unit)
	fun pickFilesForOpen(extensions: String?, defaultPath: String, onSuccess: (List<FileReaderWriter>?) -> Unit)

	fun pickFileForSave(extensions: String, defaultPath: String, onSuccess: (FileReaderWriter?) -> Unit)

	companion object : DKey<FileIoManager>
}

interface FileReaderWriter {
	val name: String
	val size: Long
	val lastModified: Long

	suspend fun readAsString(): String?
	suspend fun readAsBinary(): NativeBuffer<Byte>?

	suspend fun saveToFileAsString(extension: String? = null, defaultPath: String? = null, value: String): Boolean
	suspend fun saveToFileAsBinary(extension: String, defaultPath: String, value: NativeBuffer<Byte>): Boolean
}