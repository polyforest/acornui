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
import com.acornui.file.*
import com.acornui.io.NativeBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.files.File as DomFile
import org.w3c.files.get
import org.w3c.files.FileReader as JsFileApiReader
import kotlin.browser.document
import kotlin.browser.window

@Suppress("NOTHING_TO_INLINE")
private inline fun notSupported(reason: String = "Platform does not allow file saving to local disk."): Nothing =
		throw NotImplementedError("Operation is not implemented: $reason")

class JsFileIoManager : FileIoManager {

	override val saveSupported: Boolean = false
	private var filePicker: HTMLInputElement? = null

	private fun changeHandler(onSuccess: (Any?) -> Unit): (Event) -> Unit = {
		val fileList = filePicker?.files
		val fileReaders = if (fileList == null || fileList.length == 0) {
			null
		} else {
			val tempList = mutableListOf<JsFileReader>()
			for (i in 0..fileList.length - 1) {
				tempList.add(JsFileReader(fileList[i] ?: continue))
			}
			tempList
		}
		if (filePicker?.multiple == true)
			onSuccess(fileReaders)
		else
			onSuccess(fileReaders?.get(0))
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

	private fun destroyFilePicker() {
		val body = document.body
		if (body != null && body.contains(filePicker)) {
			body.removeChild(filePicker as Node)
			filePicker = null
		}
	}

	private fun getFileReaders(fileFilterGroups: List<FileFilterGroup>?, onSuccess: (Any?) -> Unit) {
		// TODO:  Test this as "" might cause the user to not be able to select anything
		filePicker?.accept = fileFilterGroups?.toFilterListStr() ?: ""
		filePicker?.onchange = changeHandler(onSuccess)

		filePicker?.click()
		// Always clear the list before the change event fires to normalize cancel
		onSuccess(null)
	}

	private fun List<FileFilterGroup>.toFilterListStr(): String? {
		// If this is Safari, default to all file types.
		val ua = window.navigator.userAgent
		if (ua.contains("Safari/") && !ua.contains("Chrome/", true) && !ua.contains("Chromium", true))
			return ""
		return joinToString(",") { it.toFilterListStr() }
	}

	private fun FileFilterGroup.toFilterListStr(): String = extensions.joinToString(",") { it.toExtension() }

	private fun String.toExtension(): String = "." + trim().substringAfter('.')

	override fun pickFileForOpen(fileFilterGroups: List<FileFilterGroup>?, onSuccess: (FileReader?) -> Unit) {
		destroyFilePicker()
		filePicker = createFilePicker()
		@Suppress("UNCHECKED_CAST")
		getFileReaders(fileFilterGroups, onSuccess as (Any?) -> Unit)
	}

	override fun pickFilesForOpen(fileFilterGroups: List<FileFilterGroup>?, onSuccess: (List<FileReader>?) -> Unit) {
		destroyFilePicker()
		filePicker = createFilePicker()
		filePicker?.multiple = true
		@Suppress("UNCHECKED_CAST")
		getFileReaders(fileFilterGroups, onSuccess as (Any?) -> Unit)
	}

	override fun pickFileForSave(fileFilterGroups: List<FileFilterGroup>?, defaultExtension: String?, onSuccess: (FileWriter?) -> Unit) {
		notSupported()
	}

	override fun dispose() {
		destroyFilePicker()
	}
}

class JsFileReader(val file: DomFile) : FileReader {

	override val name = file.name
	override val size = file.size.toLong()
	override val lastModified = file.lastModified.toLong()
	private val reader: JsFileApiReader = JsFileApiReader()

	private fun getContentsPromise() =
			object : Promise<String>() {
				init {
					launch {
						reader.onload = { _: Event ->
							success(reader.result as String)
						}
						reader.onerror = { _: Event ->
							fail(Exception(reader.error.toString()))
						}
						reader.readAsText(file)
					}
				}
			}

	override suspend fun readAsString(): String? {
		return getContentsPromise().await()
	}

	override suspend fun readAsBinary(): NativeBuffer<Byte>? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}

class JsFileWriter(file: DomFile) : FileWriter {

	override val name: String = file.name
	override val size = file.size.toLong()
	override val lastModified = file.lastModified.toLong()

	override suspend fun saveToFileAsString(value: String) {
		notSupported()
	}

	override suspend fun saveToFileAsBinary(value: NativeBuffer<Byte>) {
		notSupported()
	}
}
