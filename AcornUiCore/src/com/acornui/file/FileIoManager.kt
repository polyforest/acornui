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

/**
 * An object which allows for selecting file(s) with native pickers for the purposes of reading or writing to disk.
 *
 * @see FileReader
 * @see FileWriter
 */
interface FileIoManager : Disposable {

	/** If false, [pickFileForSave] will fail. */
	val saveSupported: Boolean

	/**
	 * Invokes native file picker to select a file for opening from disk [onSuccess].
	 * @param fileFilterGroups a list of groups of extensions to filter picking selection
	 * @param onSuccess callback using a [FileWriter] to be executed upon a successful pick
	 * @see FileFilterGroup
	 */
	fun pickFileForOpen(fileFilterGroups: List<FileFilterGroup>? = null, onSuccess: (FileReader) -> Unit)

	/**
	 * Invokes native file picker to select multiple files for opening from disk [onSuccess].
	 * @param fileFilterGroups a list of groups of extensions to filter picking selection
	 * @param onSuccess callback using a list of [FileWriter]s to be executed upon a successful pick
	 * @see FileFilterGroup
	 */
	fun pickFilesForOpen(fileFilterGroups: List<FileFilterGroup>? = null, onSuccess: (List<FileReader>) -> Unit)

	/**
	 * Invokes native file picker to select an existing or new file for saving to disk [onSuccess].
	 * @param fileFilterGroups a list of groups of extensions to filter picking selection
	 * @param defaultExtension an extension to ensure is the suffix of the filename (e.g. 'filename' -> 'filename.txt')
	 * @param onSuccess callback using a [FileWriter] to be executed upon a successful pick
	 * @see FileFilterGroup
	 */
	fun pickFileForSave(fileFilterGroups: List<FileFilterGroup>? = null, defaultExtension: String? = null, onSuccess: (FileWriter) -> Unit)

	companion object : DKey<FileIoManager>
}

/**
 * A list of [extensions] to filter for when picking files.
 *
 * An extension must *not* include the STOP character (e.g. ".txt", *not* "txt").
 *
 * For MAC and iOS (JVM and Safari), filtering defaults to all extensions due to underlying library limitations.
 * For JS (sans Safari), all filter groups will be merged into a single filter group due to browser implementations.
 * For JVM (sans MAC), each filter group will appear as a selection in the Options drop-down of the native picker.
 */
class FileFilterGroup(
		extensions: List<String>
) {
	val extensions: List<String> = extensions.map { it.substringAfter(".").trim() }
}

/**
 * An object that holds metadata properties for a picked file and allows for asynchronous reading of that file from disk.
 *
 * File reading (String and binary) is limited to files that are 2mb in size.
 */
interface FileReader {

	/** Name of the file including aboslute path (JS excludes path for security purposes) */
	val name: String
	/** Size of the file in bytes */
	val size: Long
	/** Last date the file was modified as the number of milliseconds since the Unix Epoch */
	val lastModified: Long

	/** Read file ([name]) from disk as a String */
	suspend fun readAsString(): String
	/** Read file ([name]) from disk binary */
	suspend fun readAsBinary(): NativeBuffer<Byte>
}

/**
 * An object that holds metadata properties for a picked file and allows for asynchronous writing of that file to disk.
 *
 * File writing (String and binary) is limited to files that are 2mb in size.
 */
interface FileWriter {

	/** Name of the file including absolute path */
	val name: String
	/** Size of the file in bytes */
	val size: Long
	/** Last date the file was modified as the number of milliseconds since the Unix Epoch */
	val lastModified: Long

	/** Write file ([name]) to disk as a String */
	suspend fun saveToFileAsString(value: String)
	/** Write file ([name]) to disk as binary */
	suspend fun saveToFileAsBinary(value: NativeBuffer<Byte>)
}