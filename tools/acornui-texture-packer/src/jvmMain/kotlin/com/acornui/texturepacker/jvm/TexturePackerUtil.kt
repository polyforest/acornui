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

package com.acornui.texturepacker.jvm

import com.acornui.async.launch
import com.acornui.core.asset.AssetManager
import com.acornui.core.di.inject
import com.acornui.core.io.file.Files
import com.acornui.io.file.relativePath2
import com.acornui.jvm.JvmHeadlessApplication
import com.acornui.logging.Log
import com.acornui.serialization.json
import com.acornui.texturepacker.AcornTexturePacker
import com.acornui.texturepacker.jvm.writer.JvmTextureAtlasWriter
import java.io.File

/**
 * @author nbilyk
 */
object TexturePackerUtil {

	/**
	 * Finds all directories with the name (.*)_unpacked descending from the provided directory, then
	 * uses TexturePacker to create an atlas by the matched name $1
	 */
	fun packAssets(dest: File, root: File, quiet: Boolean = false) {
		JvmHeadlessApplication(dest.path).start {
			val files = inject(Files)
			val assets = inject(AssetManager)

			val writer = JvmTextureAtlasWriter()
			for (i in dest.walkTopDown()) {
				if (i.isDirectory) {
					val name = i.name
					if (name.endsWith("_unpacked")) {
						val atlasName = name.substring(0, name.length - "_unpacked".length)

						Log.info("Packing assets: " + i.path)
						val dirEntry = files.getDir(root.relativePath2(i))!!
						launch {
							val packedData = AcornTexturePacker(assets, json).pack(dirEntry, quiet = quiet)
							writer.writeAtlas("$atlasName.json", "$atlasName{0}", packedData, i.parentFile)
							Log.info("Deleting directory: " + i.path)
							i.deleteRecursively()
						}
					}
				}
			}
		}
	}
}
