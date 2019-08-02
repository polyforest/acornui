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

package com.acornui.jvm.files

import com.acornui.system.Platform
import com.acornui.file.FileFilterGroup
import com.acornui.file.FileIoManager
import com.acornui.file.FileReader
import com.acornui.io.NativeReadBuffer
import com.acornui.io.NativeReadByteBuffer
import com.acornui.io.byteBuffer
import com.acornui.io.toByteArray
import com.acornui.system.userInfo
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil.memAllocPointer
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.util.nfd.NFDPathSet
import org.lwjgl.util.nfd.NativeFileDialog
import org.lwjgl.util.nfd.NativeFileDialog.*
import java.io.File

private fun checkResult(result: Int, path: PointerBuffer): String? = when (result) {
	NFD_OKAY -> {
		val str = path.getStringUTF8(0)
		nNFD_Free(path.get(0))
		str
	}
	NFD_CANCEL -> null
	else // NFD_ERROR
	-> throw Exception(NFD_GetError())
}

class JvmFileIoManager : FileIoManager {

	override val saveSupported: Boolean = true

	override fun pickFileForOpen(fileFilterGroups: List<FileFilterGroup>?, onSuccess: (FileReader) -> Unit) {
		val filePath = filePrompt(fileFilterGroups?.toFilterListStr(), null, NativeFileDialog::NFD_OpenDialog)
		if (filePath != null)
			onSuccess(JvmFileReader(File(filePath)))
	}

	override fun pickFilesForOpen(fileFilterGroups: List<FileFilterGroup>?, onSuccess: (List<FileReader>) -> Unit) {
		val filePaths = openMultiplePrompt(fileFilterGroups?.toFilterListStr())
		if (filePaths != null)
			onSuccess(filePaths.map { JvmFileReader(File(it)) })
	}

	private fun pickFileForSave(fileFilterGroups: List<FileFilterGroup>?, defaultFilename: String?, defaultExtension: String?): File? {
		val filePath = filePrompt(fileFilterGroups?.toFilterListStr(), defaultFilename, NativeFileDialog::NFD_SaveDialog) ?: return null

		val filePathWithExtension = if (defaultExtension == null) {
			filePath
		} else {
			val ext = defaultExtension.normalizeExtension()
			if (filePath.endsWith(ext, true)) filePath else "$filePath.$ext"
		}
		return File(filePathWithExtension)
	}

	override fun saveText(text: String, fileFilterGroups: List<FileFilterGroup>?, defaultFilename: String, defaultExtension: String?) {
		pickFileForSave(fileFilterGroups, defaultFilename, defaultExtension)?.writeText(text)
	}

	override fun saveBinary(data: NativeReadBuffer<Byte>, fileFilterGroups: List<FileFilterGroup>?, defaultFilename: String, defaultExtension: String?) {
		pickFileForSave(fileFilterGroups, defaultFilename, defaultExtension)?.writeBytes(data.toByteArray())
	}

	private fun List<FileFilterGroup>.toFilterListStr(): String? {
		if (userInfo.platform == Platform.APPLE) return null // Issue #36
		return joinToString(";") { it.toFilterListStr() }
	}

	private fun FileFilterGroup.toFilterListStr(): String = extensions.joinToString(",")

	private fun String.normalizeExtension(): String = substringAfter('.').trim()

	override fun dispose() {}

	private fun filePrompt(filterList: String?, filename: String?, prompt: (String?, String?, PointerBuffer?) -> Int): String? {
		val pathPtr = memAllocPointer(1)
		val pathStr: String?

		try {
			pathStr = checkResult(
					prompt(filterList, filename, pathPtr),
					pathPtr
			)
		} finally {
			memFree(pathPtr)
		}
		return pathStr
	}

	private fun openMultiplePrompt(filterList: String?): List<String>? {
		val pathSet = NFDPathSet.calloc()
		val pathList = mutableListOf<String>()

		try {
			val result = NFD_OpenDialogMultiple(filterList, null, pathSet)
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

class JvmFileReader(private val file: File) : FileReader {

	override val name: String
		get() = file.name
	override val size: Long
		get() = file.length()
	override val lastModified: Long
		get() = file.lastModified()

	override suspend fun readAsString(): String {
		return file.readText()
	}

	override suspend fun readAsBinary(): NativeReadByteBuffer {
		// TODO: There could be some utility here.
		val bytes = file.readBytes()
		val buffer = byteBuffer(bytes.size)
		for (i in 0..bytes.lastIndex) {
			buffer.put(bytes[i])
		}
		buffer.flip()
		return buffer
	}
}
