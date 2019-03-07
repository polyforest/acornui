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
import com.acornui.file.FileFilterGroup
import com.acornui.file.FileIoManager
import com.acornui.file.FileReader
import com.acornui.io.NativeReadBuffer
import com.acornui.io.NativeReadByteBuffer
import com.acornui.js.io.JsByteBuffer
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.get
import kotlin.browser.document
import kotlin.browser.window
import org.w3c.files.File as DomFile
import org.w3c.files.FileReader as JsFileApiReader

class JsFileIoManager : FileIoManager {

	override val saveSupported: Boolean = true
	private var filePicker: HTMLInputElement? = null

	private fun changeHandler(onSuccess: (Any) -> Unit): (Event) -> Unit = {
		val filePicker = filePicker
		if (filePicker != null) {
			val fileList = filePicker.files
			val fileReaders = if (fileList == null || fileList.length == 0) {
				null
			} else {
				val tempList = mutableListOf<JsFileReader>()
				for (i in 0..fileList.length - 1) {
					tempList.add(JsFileReader(fileList[i] ?: continue))
				}
				tempList
			}
			if (fileReaders != null) {
				if (filePicker.multiple)
					onSuccess(fileReaders)
				else
					onSuccess(fileReaders[0])
			}
		}
	}

	private fun createFilePicker(): HTMLInputElement {
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

	private fun getFileReaders(fileFilterGroups: List<FileFilterGroup>?, onSuccess: (Any) -> Unit) {
		// TODO:  Test this as "" might cause the user to not be able to select anything
		val filePicker = filePicker ?: throw IllegalStateException("Internal error: filePicker not created")
		filePicker.accept = fileFilterGroups?.toFilterListStr() ?: ""
		filePicker.onchange = changeHandler(onSuccess)
		filePicker.click()
	}

	private fun List<FileFilterGroup>.toFilterListStr(): String? {
		// If this is Safari, default to all file types.
		val ua = window.navigator.userAgent
		if (ua.contains("Safari/") && !ua.contains("Chrome/", true) && !ua.contains("Chromium", true))
			return ""
		return joinToString(",") { it.toFilterListStr() }
	}

	private fun FileFilterGroup.toFilterListStr(): String = extensions.joinToString(",") { ".$it" }

	override fun pickFileForOpen(fileFilterGroups: List<FileFilterGroup>?, onSuccess: (FileReader) -> Unit) {
		destroyFilePicker()
		filePicker = createFilePicker()
		@Suppress("UNCHECKED_CAST")
		getFileReaders(fileFilterGroups, onSuccess as (Any?) -> Unit)
	}

	override fun pickFilesForOpen(fileFilterGroups: List<FileFilterGroup>?, onSuccess: (List<FileReader>) -> Unit) {
		destroyFilePicker()
		filePicker = createFilePicker()
		filePicker?.multiple = true
		@Suppress("UNCHECKED_CAST")
		getFileReaders(fileFilterGroups, onSuccess as (Any?) -> Unit)
	}

	override fun saveText(text: String, fileFilterGroups: List<FileFilterGroup>?, defaultFilename: String, defaultExtension: String?) {
		saveData(text, defaultFilename)
	}

	override fun saveBinary(data: NativeReadBuffer<Byte>, fileFilterGroups: List<FileFilterGroup>?, defaultFilename: String, defaultExtension: String?) {
		saveData(data.native, defaultFilename)
	}

	private fun saveData(data: Any, defaultFilename: String) {
		val file = Blob(arrayOf(data), BlobPropertyBag(type = "application/octet-stream"))
		val nav = window.navigator.asDynamic()
		@Suppress("UnsafeCastFromDynamic")
		if (nav.msSaveOrOpenBlob) // IE10+
			nav.msSaveOrOpenBlob(file, defaultFilename)
		else { // Others
			val a = document.createElement("a") as HTMLAnchorElement
			val url = URL.createObjectURL(file)
			a.href = url
			a.download = defaultFilename
			document.body?.appendChild(a)
			a.click()
			window.setTimeout({
				document.body?.removeChild(a)
				URL.revokeObjectURL(url)
			}, 0)
		}
	}

	override fun dispose() {
		destroyFilePicker()
	}
}

class JsFileReader(val file: DomFile) : FileReader {

	override val name = file.name
	override val size = file.size.toLong()
	override val lastModified = file.lastModified.toLong()
	private val reader = JsFileApiReader()

	override suspend fun readAsString(): String {
		return object : Promise<String>() {
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
		}.await()
	}

	override suspend fun readAsBinary(): NativeReadByteBuffer {
		return object : Promise<NativeReadByteBuffer>() {
			init {
				launch {
					reader.onload = { _: Event ->
						val byteBuffer = JsByteBuffer(Uint8Array(reader.result as ArrayBuffer))
						success(byteBuffer)
					}
					reader.onerror = { _: Event ->
						fail(Exception(reader.error.toString()))
					}
					reader.readAsArrayBuffer(file)
				}
			}
		}.await()
	}
}