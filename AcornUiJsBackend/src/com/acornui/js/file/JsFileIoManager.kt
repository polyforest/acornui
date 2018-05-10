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

package com.acornui.js.file

import com.acornui.async.Promise
import com.acornui.async.launch
import com.acornui.file.FileIoManager
import com.acornui.file.FileReaderWriter
import com.acornui.io.NativeBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.files.File
import org.w3c.files.get
import org.w3c.files.FileReader as JsFileApiReader
import kotlin.browser.document


@Suppress("NOTHING_TO_INLINE")
private inline fun NOTSUPPORTED(reason: String = "Platform does not allow file saving to local disk.") : Nothing =
		throw NotImplementedError("Operation is not implemented: $reason")

private fun normalizeExtensions(extensions: String?): String? {
	// TODO: Test that duplicates don't cause issues
	// Remove NFD extension filter groups
	return extensions?.replace(';', ',')
}

class JsFileIoManager : FileIoManager {
	override val saveSupported: Boolean = false

	private var filePicker: HTMLInputElement? = createFilePicker()

	private var fileReaderWriters: List<FileReaderWriter>? = null
	private fun changeHandler(onSuccess: (Any?) -> Unit) : (Event) -> Unit = {
		val fileList = filePicker?.files
		fileReaderWriters = if (fileList == null || fileList.length == 0) {
			null
		} else {
			val tempList = mutableListOf<FileReaderWriter>()
			for (i in 0..fileList.length - 1) {
				tempList.add(JsFileReaderWriter(fileList[i] ?: continue))
			}
			tempList
		}
		if (filePicker?.multiple != null && filePicker!!.multiple)
			onSuccess(fileReaderWriters)
		else
			onSuccess(fileReaderWriters?.get(0) as FileReaderWriter)
	}
	private fun createFilePicker(): HTMLInputElement {
		if (document.body == null) {
			document.createElement("body").also { document.appendChild(it) }
		}
		val newFilePicker = document.createElement("input") as HTMLInputElement
		newFilePicker.type = "file"
		// Required for iOS Safari
		newFilePicker.setAttribute("style", "width: 0px; height: 0px; overflow: hidden;")
		newFilePicker.style.visibility = "hidden"
		newFilePicker.onclick = null
		document.body?.appendChild(newFilePicker)
		return newFilePicker
	}

	private fun getFileReaders(extensions: String?, onSuccess: (Any?) -> Unit) {
		// TODO:  Test this as "" might cause the user to not be able to select anything
		filePicker?.accept = normalizeExtensions(extensions) ?: ""
		filePicker?.onchange = changeHandler(onSuccess)
		fileReaderWriters = null

		filePicker?.click()
	}

	override fun pickFileForOpen(extensions: String?, defaultPath: String, onSuccess: (FileReaderWriter?) -> Unit) {
		@Suppress("UNCHECKED_CAST")
		getFileReaders(extensions, onSuccess as (Any?) -> Unit)
	}

	override fun pickFilesForOpen(extensions: String?, defaultPath: String, onSuccess: (List<FileReaderWriter>?) -> Unit) {
		filePicker?.multiple = true
		@Suppress("UNCHECKED_CAST")
		getFileReaders(extensions, onSuccess as (Any?) -> Unit)
	}

	override fun pickFileForSave(extensions: String, defaultPath: String, onSuccess: (FileReaderWriter?) -> Unit) {
		NOTSUPPORTED()
	}

	override fun dispose() {
		val body = document.body
		if (body != null && body.contains(filePicker)) {
			body.removeChild(filePicker as Node)
			filePicker = null
		}
	}
}

class JsFileReaderWriter(private val file: File) : FileReaderWriter {
	override val name: String = file.name
	override val size: Long = file.size.toLong()
	override val lastModified: Long = file.lastModified.toLong()

	private val reader: JsFileApiReader = JsFileApiReader()
	private val contents: Promise<String> = getContentsPromise()

	private fun getContentsPromise() =
			object : Promise<String>() {
				init {
					launch {
						reader.onload = { _: Event ->
							success(reader.result as String)
						}
						reader.onerror = { _: Event ->
							val error: dynamic = reader.error
							var msg: String = when(error.code) {
								error.ENCODING_ERR -> "Encoding error"
								error.NOT_FOUND_ERR -> "File not found"
								error.NOT_READABLE_ERR -> "File could not be read"
								error.SECURITY_ERR -> "File has a security issue"
								else -> "File cannot be opened due to the following error"
							}
							msg = "$msg: ${error.code}"
							fail(Exception(msg))
						}
					}
				}
			}

	override suspend fun readAsString(): String? {
		reader.readAsText(file)
		return contents.await()
	}

	override suspend fun readAsBinary(): NativeBuffer<Byte>? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override suspend fun saveToFileAsString(extension: String?, defaultPath: String?, value: String): Boolean {
		NOTSUPPORTED()
	}

	override suspend fun saveToFileAsBinary(extension: String, defaultPath: String, value: NativeBuffer<Byte>): Boolean {
		NOTSUPPORTED()
	}

}
