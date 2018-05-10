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

import java.io.File
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.system.MemoryUtil.memAllocPointer
import org.lwjgl.util.nfd.NativeFileDialog
import org.lwjgl.util.nfd.NativeFileDialog.*
import org.lwjgl.util.nfd.NFDPathSet
import com.acornui.file.FileIoManager
import com.acornui.file.FileReaderWriter
import com.acornui.io.NativeBuffer



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

private fun normalizeExtensions(extensions: String?): String? {
	// TODO: returning null for MAC due to:  https://github.com/PolyForest/Acorn/issues/37
	return if (extensions == null || System.getProperty("os.name")?.startsWith("MAC", true) == true)
		null
	 else
		extensions.split(';').map { it.split(',')  // Deserialize
				.filter { it.startsWith('.') }  // Remove anything that isn't an extension type with STOP prefix (.)
				.map { it.removePrefix(".") } }  // Strip extension STOP prefix (.)
				.filter { it.isNotEmpty() }  // Remove empty filter groups
				.joinToString(";") { it.joinToString(",") }  // Serialize
}

class JvmFileIoManager : FileIoManager {
	override val saveSupported: Boolean = true

	override fun pickFileForOpen(extensions: String?, defaultPath: String, onSuccess: (FileReaderWriter?) -> Unit) {
		val filePath = filePrompt(normalizeExtensions(extensions), defaultPath, NativeFileDialog::NFD_OpenDialog)
		val fileReaderWriter = filePath?.let { JvmFileReaderWriter(it) }
		onSuccess(fileReaderWriter)
	}

	override fun pickFilesForOpen(extensions: String?, defaultPath: String, onSuccess: (List<FileReaderWriter>?) -> Unit) {
		val filePaths = openMultiplePrompt(normalizeExtensions(extensions), defaultPath)
		val fileReaderWriters = filePaths?.let { it.map { JvmFileReaderWriter(it) } }
		onSuccess(fileReaderWriters)
	}

	override fun pickFileForSave(extensions: String, defaultPath: String, onSuccess: (FileReaderWriter?) -> Unit) {
		val filePath = filePrompt(normalizeExtensions(extensions), defaultPath, NativeFileDialog::NFD_SaveDialog)
		val fileReaderWriter = filePath?.let { JvmFileReaderWriter(it) }
		onSuccess(fileReaderWriter)
	}

	override fun dispose() { }

	private fun filePrompt(filterList: String?, defaultPath: String, prompt: (String?, String, PointerBuffer?) -> Int): String? {
		val pathPtr = memAllocPointer(1)
		val pathStr: String?

		try {
			pathStr = checkResult(
					prompt(filterList, defaultPath, pathPtr),
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
			val result = NFD_OpenDialogMultiple(filterList, defaultPath, pathSet)
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

class JvmFileReaderWriter(private var path: String) : FileReaderWriter {
	override val name: String
	override val size: Long
	override val lastModified: Long

	private var file = File(path)

	init {
		name = file.name
		size = file.length()
		lastModified = file.lastModified()
	}
	override suspend fun readAsString(): String? {
		return file.readText()
	}

	override suspend fun readAsBinary(): NativeBuffer<Byte>? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override suspend fun saveToFileAsString(extension: String?, defaultPath: String?, value: String): Boolean {
		if (!path.endsWith(".$extension", true)) {
			path += ".$extension"
			file = File(path)
		}
		file.writeText(value)
		return true
	}

	override suspend fun saveToFileAsBinary(extension: String, defaultPath: String, value: NativeBuffer<Byte>): Boolean {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}