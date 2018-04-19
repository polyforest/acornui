/*
 * Copyright 2018 Nicholas Bilyk
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

import com.acornui.file.FileReadWriteManager
import com.acornui.io.NativeBuffer
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil.memAllocPointer
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.util.nfd.NativeFileDialog.*
import java.io.File

class JvmFileReadWriteManager : FileReadWriteManager {

	override suspend fun loadFromFileAsString(extensions: List<String>, defaultPath: String): String? {
		val filePath = openPrompt(extensions.joinToString(";"), "") ?: return null
		return File(filePath).readText()
	}

	override suspend fun saveToFileAsString(extension: String, defaultPath: String, value: String): Boolean {
		var filePath = savePrompt(extension, "") ?: return false
		if (!filePath.contains(".")) filePath += ".$extension"
		File(filePath).writeText(value)
		return true
	}

	override suspend fun loadFromFileAsBinary(extensions: List<String>, defaultPath: String): NativeBuffer<Byte>? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override suspend fun saveToFileAsBinary(extension: String, defaultPath: String, value: NativeBuffer<Byte>): Boolean {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	private fun openPrompt(filterList: String, defaultPath: String): String? {
		val pathPtr = memAllocPointer(1)
		val pathStr: String?

		try {
			pathStr = checkResult(
					NFD_OpenDialog(filterList, defaultPath, pathPtr),
					pathPtr
			)
		} finally {
			memFree(pathPtr)
		}
		return pathStr
	}

	private fun savePrompt(filterList: String, defaultPath: String): String? {
		val pathPtr = memAllocPointer(1)
		val pathStr: String?

		try {
			pathStr = checkResult(
					NFD_SaveDialog(filterList, defaultPath, pathPtr),
					pathPtr
			)
		} finally {
			memFree(pathPtr)
		}
		return pathStr
	}

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
}