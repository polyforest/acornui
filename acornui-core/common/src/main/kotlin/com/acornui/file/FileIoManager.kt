/*
 * Copyright 2018 Poly Forest, LLC
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
import com.acornui.io.*

/**
 * An object which allows for selecting file(s) with native pickers for the purposes of reading or writing to disk.
 *
 * @see FileReader
 */
expect class FileIoManager : Disposable {

	/**
	 * If false, [pickFileForSave] will fail.
	 */
	val saveSupported: Boolean

	/**
	 * Invokes native file picker to select a file for opening from disk [onSuccess].
	 * @param fileFilterGroups a list of groups of extensions to filter picking selection
	 * @param onSuccess callback using a [FileReader] to be executed upon a successful pick
	 * @see FileFilterGroup
	 */
	fun pickFileForOpen(fileFilterGroups: List<FileFilterGroup>? = null, onSuccess: (FileReader) -> Unit)

	/**
	 * Invokes native file picker to select multiple files for opening from disk [onSuccess].
	 * @param fileFilterGroups a list of groups of extensions to filter picking selection
	 * @param onSuccess callback using a list of [FileReader]s to be executed upon a successful pick
	 * @see FileFilterGroup
	 */
	fun pickFilesForOpen(fileFilterGroups: List<FileFilterGroup>? = null, onSuccess: (List<FileReader>) -> Unit)

	/**
	 * Saves the given text contents to disk. This will invoke a native file picker.
	 *
	 * @param fileFilterGroups a list of groups of extensions to filter picking selection
	 * @param defaultFilename The default file name to save.
	 * @param defaultExtension If set, an extension to ensure the suffix of the filename
	 * (e.g. 'filename' -> 'filename.txt')
	 *
	 * @see FileFilterGroup
	 */
	fun saveText(text: String, fileFilterGroups: List<FileFilterGroup>? = null, defaultFilename: String, defaultExtension: String? = null)

	/**
	 * Saves the given binary contents to disk. This will invoke a native file picker.
	 *
	 * @param data The binary data to save.
	 * @param fileFilterGroups a list of groups of extensions to filter picking selection
	 * @param defaultFilename The default file name to save.
	 * @param defaultExtension If set, an extension to ensure the suffix of the filename
	 * (e.g. 'filename' -> 'filename.txt')
	 * @see FileFilterGroup
	 */
	fun saveBinary(data: NativeReadBuffer<Byte>, fileFilterGroups: List<FileFilterGroup>? = null, defaultFilename: String, defaultExtension: String? = null)

	// TODO - MP: Do I need this?
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
class FileFilterGroup(extensions: List<String>) {
	val extensions: List<String> = extensions.map { it.substringAfter(".").trim() }
}

/**
 * An object that holds metadata properties for a picked file and allows for asynchronous reading of that file from disk.
 *
 * File reading (String and binary) is limited to files that are 2mb in size.
 */
expect class FileReader {

	/**
	 * Name of the file including aboslute path (JS excludes path for security purposes)
	 */
	val name: String
	/**
	 * Size of the file in bytes
	 */
	val size: Long
	/**
	 * Last date the file was modified as the number of milliseconds since the Unix Epoch
	 */
	val lastModified: Long

	/**
	 * Read file ([name]) from disk as a String
	 */
	suspend fun readAsString(): String
	/**
	 * Read file ([name]) from disk binary
	 */
	suspend fun readAsBinary(): NativeReadByteBuffer
}