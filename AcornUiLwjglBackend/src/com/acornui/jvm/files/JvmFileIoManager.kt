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

package com.acornui.jvm.files

import com.acornui.core.io.BufferFactory
import com.acornui.file.FileFilterGroup
import com.acornui.file.FileIoManager
import com.acornui.file.FileReader
import com.acornui.file.FileWriter
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.system.MemoryUtil.memAllocPointer
import org.lwjgl.util.nfd.NativeFileDialog
import org.lwjgl.util.nfd.NativeFileDialog.*
import org.lwjgl.util.nfd.NFDPathSet
import com.acornui.io.NativeBuffer
import com.acornui.io.toByteArray
import java.io.File

private fun checkResult(result: Int, path: PointerBuffer): String? = when (result) {
	NFD_OKAY -> {
		val str = path.getStringUTF8(0)
		nNFDi_Free(path.get(0))
		str
	}
	NFD_CANCEL -> null
	else // NFD_ERROR
	-> throw Exception(NFD_GetError())
}

class JvmFileIoManager : FileIoManager {

	override val saveSupported: Boolean = true

	override fun pickFileForOpen(fileFilterGroups: List<FileFilterGroup>?, defaultPath: String, onSuccess: (FileReader?) -> Unit) {
		val filePath = filePrompt(fileFilterGroups?.toFilterListStr(), defaultPath, NativeFileDialog::NFD_OpenDialog)
		onSuccess(filePath?.let { JvmFileReader(it) })
	}

	override fun pickFilesForOpen(fileFilterGroups: List<FileFilterGroup>?, defaultPath: String, onSuccess: (List<FileReader>?) -> Unit) {
		val filePaths = openMultiplePrompt(fileFilterGroups?.toFilterListStr(), defaultPath)
		onSuccess(filePaths?.let { it.map { JvmFileReader(it) } })
	}

	override fun pickFileForSave(fileFilterGroups: List<FileFilterGroup>?, defaultPath: String, defaultExtension: String?, onSuccess: (FileWriter?) -> Unit) {
		val filePath = filePrompt(fileFilterGroups?.toFilterListStr(), defaultPath, NativeFileDialog::NFD_SaveDialog) ?: return onSuccess(null)

		val filePathWithExtension = if (defaultExtension == null) {
			filePath
		} else {
			val ext = defaultExtension.toExtension()
			if (filePath.endsWith(ext, true)) filePath else "$filePath.$ext"
		}
		onSuccess(JvmFileWriter(File(filePathWithExtension)))
	}

	private fun List<FileFilterGroup>.toFilterListStr(): String? {
		//if (System.getProperty("os.name")?.startsWith("MAC", true) == true) return joinToString(",") { it.toFilterListStr() } // TODO: Uncomment out if Issue #37 is real.
		return joinToString(";") { it.toFilterListStr() }
	}

	private fun FileFilterGroup.toFilterListStr(): String = extensions.joinToString(",") { it.toExtension() }

	private fun String.toExtension(): String = trim().substringAfter('.')

	private val String.extension: String
		get() = substringAfterLast('.', "")

	override fun dispose() {}

	private fun filePrompt(filterList: String?, defaultPath: String, prompt: (String?, String, PointerBuffer?) -> Int): String? {
		val pathPtr = memAllocPointer(1)
		val pathStr: String?

		try {
			pathStr = checkResult(
					prompt(filterList, File(defaultPath).absolutePath, pathPtr),
					pathPtr
			)
		} finally {
			memFree(pathPtr)
		}
		return pathStr
	}

	private fun openMultiplePrompt(filterList: String?, defaultPath: String): List<String>? {
		val pathSet = NFDPathSet.calloc()
		val pathList = mutableListOf<String>()

		try {
			val result = NFD_OpenDialogMultiple(filterList, File(defaultPath).absolutePath, pathSet)
			@Suppress("UNUSED_EXPRESSION")
			when (result) {
				NFD_OKAY -> {
					val count = NFD_PathSet_GetCount(pathSet)
					for (i in 0..count - 1) {
						pathList.add(NFD_PathSet_GetPath(pathSet, i) ?: continue)
					}
				}

				NFD_CANCEL -> null
				else // NFD_ERROR
				-> System.err.format("Error: %s\n", NFD_GetError())
			}
		} finally {
			NFD_PathSet_Free(pathSet)
		}
		return if (pathList.isEmpty()) null else pathList
	}
}

class JvmFileReader(private var path: String) : FileReader {

	override val name: String
		get() = file.name
	override val size: Long
		get() = file.length()
	override val lastModified: Long
		get() = file.lastModified()
	private var file = File(path)
		set(value) {
			path = value.path
			field = value
		}

	override suspend fun readAsString(): String? {
		return file.readText()
	}

	override suspend fun readAsBinary(): NativeBuffer<Byte>? {
		// TODO: There could be some utility here.
		val bytes = file.readBytes()
		val buffer = BufferFactory.instance.byteBuffer(bytes.size)
		for (i in 0..bytes.lastIndex) {
			buffer.put(bytes[i])
		}
		buffer.flip()
		return buffer
	}
}

class JvmFileWriter(private val file: File) : FileWriter {

	override val name: String
		get() = file.name
	override val size: Long
		get() = file.length()
	override val lastModified: Long
		get() = file.lastModified()

	override suspend fun saveToFileAsString(value: String) {
		file.writeText(value)
	}

	override suspend fun saveToFileAsBinary(value: NativeBuffer<Byte>) {
		file.writeBytes(value.toByteArray())
	}
}