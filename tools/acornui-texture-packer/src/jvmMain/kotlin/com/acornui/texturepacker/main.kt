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

package com.acornui.texturepacker

import com.acornui.core.asset.AssetManager
import com.acornui.core.di.inject
import com.acornui.core.io.file.Files
import com.acornui.jvm.JvmHeadlessApplication
import com.acornui.texturepacker.jvm.TexturePackerUtil
import java.io.File

/**
 * The texture packer either expects two arguments, a source and a destination, or two arguments,
 * a source and -replaceMode to indicate that the _unpacked directories within source should be processed then deleted.
 * The flag is for safety so that one doesn't accidentally delete their unpacked directories without confirmation.
 */
fun main(args: Array<String>) {
	if (args.size != 2) {
		println("Texture Packer Usage: path/to/source path/to/dest    OR  path/to/source -replaceMode")
		return
	}

	val dest: File
	@Suppress("SpellCheckingInspection")
	if (args[1].toLowerCase() == "-replacemode") {
		dest = File(args[0])
		if (!dest.exists())
			throw IllegalArgumentException(dest.path + " does not exist.")

	} else {
		val source = File(args[0])
		dest = File(args[1])
		if (source.path == dest.path)
			throw IllegalAccessException("Source and destination directories cannot be the same. Use: source -replaceMode")
		println("Source directory: " + source.path)
		if (source != dest) {
			println("Assets directory: " + dest.path)
			copyAssets(source, dest)
		}
	}

	JvmHeadlessApplication(dest.path).start {
		// Pack the assets in all directories in the dest folder with a name ending in "_unpacked"
		TexturePackerUtil(inject(Files), inject(AssetManager)).packAssets(dest, File("."))

		dest.setLastModified(System.currentTimeMillis())
	}
}

private fun copyAssets(source: File, dest: File) {
	if (!source.exists()) throw IllegalArgumentException(source.path + " does not exist.")
	if (dest.exists())
		dest.delete()
	dest.mkdirs()
	source.copyRecursively(dest, true)
}