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

package com.acornui.texturepacker.jvm.writer

import com.acornui.collection.ArrayList
import com.acornui.core.graphic.TextureAtlasData
import com.acornui.core.graphic.TextureAtlasDataSerializer
import com.acornui.core.replaceTokens
import com.acornui.gl.core.TexturePixelFormat
import com.acornui.jvm.graphic.JvmImageUtils
import com.acornui.serialization.json
import com.acornui.serialization.write
import com.acornui.texturepacker.PackedTextureData
import java.io.File

/**
 * @author nbilyk
 */
class JvmTextureAtlasWriter {

	/**
	 * Writes the texture atlas to their respective files.
	 * @param atlasFilename The filename of the atlas JSON file. This should include the file extension but not directory.
	 * 		E.g. "myTextures.atlas"
	 * @param pagesFilename The filename of the atlas pages, not including the extension. This should not include the
	 * directory, and must have a {0} token representing the page index. Example: "myTexturePage{0}
	 */
	fun writeAtlas(atlasFilename: String, pagesFilename: String, packedData: PackedTextureData, dir: File) {
		dir.mkdirs()
		if (pagesFilename.indexOf("{0}") == -1)
			throw IllegalArgumentException("pagesFilename must have \"{0}\" as a replacement token to represent the page index.")

		val packedDataModified = packedData.copy(pages = ArrayList(packedData.pages.size) {
			index ->
			val textureData = packedData.pages[index].second
			packedData.pages[index].copy(second = textureData.copy(texturePath = "${pagesFilename.replaceTokens("$index")}.${packedData.settings.compressionExtension}"))
		})
		for (i in 0..packedDataModified.pages.lastIndex) {
			val page = packedDataModified.pages[i]
			JvmImageUtils.rgbDataToFile(page.first, dir.path + "/" + page.second.texturePath, packedData.settings.compressionExtension, packedData.settings.pixelFormat == TexturePixelFormat.RGBA, packedData.settings.premultipliedAlpha)
		}

		val atlas = TextureAtlasData(List(packedDataModified.pages.size, { packedDataModified.pages[it].second }))
		val json = json.write(atlas, TextureAtlasDataSerializer)
		val atlasFile = File(dir, atlasFilename)
		atlasFile.writeText(json)
	}
}