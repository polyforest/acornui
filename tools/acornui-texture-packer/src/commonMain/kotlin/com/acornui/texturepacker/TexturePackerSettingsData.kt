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

import com.acornui.gl.core.TextureMagFilter
import com.acornui.gl.core.TextureMinFilter
import com.acornui.gl.core.TexturePixelFormat
import com.acornui.gl.core.TexturePixelType
import com.acornui.serialization.*

/**
 * @author nbilyk
 */
data class TexturePackerSettingsData(

		/**
		 * 0f-1f describing what alpha value to consider as 'whitespace'. Only applicable if [stripWhitespace] is true.
		 */
		val alphaThreshold: Float = 0f,

		/**
		 * @see Texture.filterMag
		 */
		val filterMag: TextureMagFilter = TextureMagFilter.NEAREST,

		/**
		 * @see Texture.filterMin
		 */
		val filterMin: TextureMinFilter = TextureMinFilter.LINEAR_MIPMAP_LINEAR,

		/**
		 * @see Texture.pixelType
		 */
		val pixelType: TexturePixelType = TexturePixelType.UNSIGNED_BYTE,

		/**
		 * @see Texture.pixelFormat
		 */
		val pixelFormat: TexturePixelFormat = TexturePixelFormat.RGBA,

		val premultipliedAlpha: Boolean = false,

		/**
		 * jpg or png
		 */
		val compressionExtension: String = "png",

		/**
		 * Compression quality.
		 */
		val compressionQuality: Float = 0.9f,

		/**
		 * The maximum directory depth to find images to pack.
		 */
		val maxDirectoryDepth: Int = 10,

		/**
		 * Use BEST, unless debugging a problem.
		 */
		val packAlgorithm: TexturePackAlgorithm = TexturePackAlgorithm.BEST,

		/**
		 * If true, the sides of an image will be trimmed if the pixels have alpha < [alphaThreshold].
		 * The amount clipped will be set as padding values in the metadata. Use AtlasComponent to add the padding back
		 * virtually.
		 * Note that this could cause unexpected results if color transformation is applied.
		 */
		val stripWhitespace: Boolean = true,

		val algorithmSettings: PackerAlgorithmSettingsData = PackerAlgorithmSettingsData()
) {

	fun validate() {
	}
}

enum class TexturePackAlgorithm {
	BEST,
	GREEDY
}

data class PackerAlgorithmSettingsData(

		/**
		 * Allows the source image to be rotated in the atlas.
		 * This is currently not supported on the DOM backend.
		 */
		val allowRotation: Boolean = true,

		/**
		 * The x padding between images.
		 */
		val paddingX: Int = 2,

		/**
		 * The y padding between images.
		 */
		val paddingY: Int = 2,

		/**
		 * If true, the edges will have have [paddingX] and [paddingY]
		 */
		val edgePadding: Boolean = true,

		/**
		 * The maximum texture width per page.
		 */
		val pageMaxHeight: Int = 1024,

		/**
		 * The maximum texture height per page.
		 */
		val pageMaxWidth: Int = 1024,

		/**
		 * Must be true if mipmaps are used.
		 */
		val powerOfTwo: Boolean = true,

		/**
		 * In order to prevent frequent batch flushing when switching between vector and texture drawing, set this to
		 * true. A white pixel will be added to the 0,0 position on every atlas page.
		 * Note: This can be false for non-opengl back-ends.
		 */
		val addWhitePixel: Boolean = true


)

@Deprecated("use kotlinx serialization")
object TexturePackerSettingsSerializer : To<TexturePackerSettingsData>, From<TexturePackerSettingsData> {

	override fun read(reader: Reader): TexturePackerSettingsData {
		return TexturePackerSettingsData(
				alphaThreshold = reader.float("alphaThreshold") ?: 0f,
				filterMag = TextureMagFilter.valueOf(reader.string("filterMag") ?: TextureMagFilter.NEAREST.name),
				filterMin = TextureMinFilter.valueOf(reader.string("filterMin") ?: TextureMinFilter.NEAREST.name),
				pixelType = TexturePixelType.valueOf(reader.string("pixelType") ?: TexturePixelType.UNSIGNED_BYTE.name),
				pixelFormat = TexturePixelFormat.valueOf(reader.string("pixelFormat") ?: TexturePixelFormat.RGBA.name),
				compressionExtension = reader.string("compressionExtension") ?: "png",
				compressionQuality = reader.float("compressionQuality") ?: 0.9f,
				maxDirectoryDepth = reader.int("maxDirectoryDepth") ?: 10,
				packAlgorithm = TexturePackAlgorithm.valueOf(reader.string("packAlgorithm") ?: TexturePackAlgorithm.BEST.name),
				premultipliedAlpha = reader.bool("premultipliedAlpha") ?: false,
				stripWhitespace = reader.bool("stripWhitespace") ?: true,
				algorithmSettings = reader.obj("algorithmSettings", PackerAlgorithmSettingsDataSerializer)!!
		)
	}

	override fun TexturePackerSettingsData.write(writer: Writer) {
		writer.float("alphaThreshold", alphaThreshold)
		writer.string("filterMag", filterMag.name)
		writer.string("filterMin", filterMin.name)
		writer.string("pixelType", pixelType.name)
		writer.string("pixelFormat", pixelFormat.name)
		writer.string("compressionExtension", compressionExtension)
		writer.float("compressionQuality", compressionQuality)
		writer.int("maxDirectoryDepth", maxDirectoryDepth)
		writer.string("packAlgorithm", packAlgorithm.name)
		writer.bool("premultipliedAlpha", premultipliedAlpha)
		writer.bool("stripWhitespace", stripWhitespace)
		writer.obj("algorithmSettings", algorithmSettings, PackerAlgorithmSettingsDataSerializer)
	}


}

@Deprecated("Use kotlinx serialization")
object PackerAlgorithmSettingsDataSerializer : To<PackerAlgorithmSettingsData>, From<PackerAlgorithmSettingsData> {
	override fun read(reader: Reader): PackerAlgorithmSettingsData {
		return PackerAlgorithmSettingsData(
				allowRotation = reader.bool("allowRotation") ?: true,
				paddingX = reader.int("paddingX") ?: 2,
				paddingY = reader.int("paddingY") ?: 2,
				edgePadding = reader.bool("edgePadding") ?: true,
				pageMaxHeight = reader.int("pageMaxHeight") ?: 1024,
				pageMaxWidth = reader.int("pageMaxWidth") ?: 1024,
				powerOfTwo = reader.bool("powerOfTwo") ?: true,
				addWhitePixel = reader.bool("addWhitePixel") ?: true
		)
	}


	override fun PackerAlgorithmSettingsData.write(writer: Writer) {
		writer.bool("allowRotation", allowRotation)
		writer.int("paddingX", paddingX)
		writer.int("paddingY", paddingY)
		writer.bool("edgePadding", edgePadding)
		writer.int("pageMaxHeight", pageMaxHeight)
		writer.int("pageMaxWidth", pageMaxWidth)
		writer.bool("powerOfTwo", powerOfTwo)
		writer.bool("addWhitePixel", addWhitePixel)
	}
}
