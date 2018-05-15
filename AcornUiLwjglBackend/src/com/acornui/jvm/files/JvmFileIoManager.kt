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

import java.io.File as JavaFile
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.system.MemoryUtil.memAllocPointer
import org.lwjgl.util.nfd.NativeFileDialog
import org.lwjgl.util.nfd.NativeFileDialog.*
import org.lwjgl.util.nfd.NFDPathSet
import com.acornui.file.*
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
		extensions.split(';').map {
			it.split(',')  // Deserialize
					.filter { it.startsWith('.') }  // Remove anything that isn't an extension type with STOP prefix (.)
					.map { it.removePrefix(".") }
		}  // Strip extension STOP prefix (.)
				.filter { it.isNotEmpty() }  // Remove empty filter groups
				.joinToString(";") { it.joinToString(",") }  // Serialize
}

class JvmFileIoManager : FileIoManager {

	override val saveSupported: Boolean = true

	override fun pickFileForOpen(extensions: String?, defaultPath: String, onSuccess: (FileReader?) -> Unit) {
		val filePath = filePrompt(normalizeExtensions(extensions), defaultPath, NativeFileDialog::NFD_OpenDialog)
		onSuccess(filePath?.let { JvmFileReader(it) })
	}

	override fun pickFilesForOpen(extensions: String?, defaultPath: String, onSuccess: (List<FileReader>?) -> Unit) {
		val filePaths = openMultiplePrompt(normalizeExtensions(extensions), defaultPath)
		onSuccess(filePaths?.let { it.map { JvmFileReader(it) } })
	}

	override fun pickFileForSave(extensions: String, defaultPath: String, onSuccess: (FileWriter?) -> Unit) {
		val filePath = filePrompt(normalizeExtensions(extensions), defaultPath, NativeFileDialog::NFD_SaveDialog)
		onSuccess(filePath?.let { JvmFileWriter(it) })
	}

	override fun dispose() {}

	private fun filePrompt(filterList: String?, defaultPath: String, prompt: (String?, String, PointerBuffer?) -> Int): String? {
		val pathPtr = memAllocPointer(1)
		val pathStr: String?

		try {
			pathStr = checkResult(
					prompt(filterList, verifyWindowsPath(defaultPath), pathPtr),
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
			val result = NFD_OpenDialogMultiple(filterList, verifyWindowsPath(defaultPath), pathSet)
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

	private fun verifyWindowsPath(path: String): String {
		// Windows file picker throws error if defaultPath contains forward slash
		return if (System.getProperty("os.name")?.startsWith("Windows", true) == true && path.contains('/'))
			""
		else
			path
	}
}

class JvmFileReader(private var path: String) : FileReader {

	override val name: String
		get() = file.name
	override val size: Long
		get() = file.length()
	override val lastModified: Long
		get() = file.lastModified()
	private var file = JavaFile(path)
		set(value) {
			path = value.path
			field = value
		}

	override suspend fun readAsString(): String? {
		return file.readText()
	}

	override suspend fun readAsBinary(): NativeBuffer<Byte>? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}

class JvmFileWriter(private var path: String) : FileWriter {

	override val name: String
		get() = file.name
	override val size: Long
		get() = file.length()
	override val lastModified: Long
		get() = file.lastModified()
	private var file = JavaFile(path)
		set(value) {
			path = value.path
			field = value
		}

	override suspend fun saveToFileAsString(extension: String?, value: String): Boolean {
		if (extension != null && extension != "") {
			var thisExtension = extension

			if (!thisExtension.startsWith('.')) thisExtension = ".$thisExtension"
			if (!path.endsWith(thisExtension, true)) {
				file = JavaFile(path + thisExtension)
			}
		}
		file.writeText(value)
		return true
	}

	override suspend fun saveToFileAsBinary(extension: String, value: NativeBuffer<Byte>): Boolean {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}