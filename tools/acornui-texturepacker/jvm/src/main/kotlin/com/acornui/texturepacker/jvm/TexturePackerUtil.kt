/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.texturepacker.jvm

import com.acornui.async.launch
import com.acornui.core.asset.AssetManager
import com.acornui.core.io.file.Files
import com.acornui.jvm.io.file.relativePath2
import com.acornui.serialization.Serializer
import com.acornui.texturepacker.AcornTexturePacker
import com.acornui.texturepacker.jvm.writer.JvmTextureAtlasWriter
import java.io.File

/**
 * @author nbilyk
 */
class TexturePackerUtil(
		private val files: Files,
		private val assets: AssetManager,
		private val json: Serializer<String>
) {

	/**
	 * Finds all directories with the name (.*)_unpacked descending from the provided directory, then
	 * uses TexturePacker to create an atlas by the matched name $1
	 */
	fun packAssets(dest: File, root: File) {
		val writer = JvmTextureAtlasWriter(json)
		for (i in dest.walkTopDown()) {
			if (i.isDirectory) {
				val name = i.name
				if (name.endsWith("_unpacked")) {
					val atlasName = name.substring(0, name.length - "_unpacked".length)

					println("Packing assets: " + i.path)
					val dirEntry = files.getDir(root.relativePath2(i))!!
					launch {
						val packedData = AcornTexturePacker(assets, json).pack(dirEntry)
						writer.writeAtlas("$atlasName.json", "$atlasName{0}", packedData, i.parentFile)
						println("Deleting directory: " + i.path)
						i.deleteRecursively()
					}
				}
			}
		}
	}
}