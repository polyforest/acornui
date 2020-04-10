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
import kotlinx.serialization.Serializable

/**
 * @author nbilyk
 */
@Serializable
data class TexturePackerSettingsData(

		/**
		 * 0f-1f describing what alpha value to consider as 'whitespace'. Only applicable if [stripWhitespace] is true.
		 */
		val alphaThreshold: Float = 0f,

		/**
		 * @see com.acornui.graphic.Texture.filterMag
		 */
		val filterMag: TextureMagFilter = TextureMagFilter.NEAREST,

		/**
		 * @see com.acornui.graphic.Texture.filterMin
		 */
		val filterMin: TextureMinFilter = TextureMinFilter.LINEAR_MIPMAP_LINEAR,

		/**
		 * @see com.acornui.graphic.Texture.pixelType
		 */
		val pixelType: TexturePixelType = TexturePixelType.UNSIGNED_BYTE,

		/**
		 * @see com.acornui.graphic.Texture.pixelFormat
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
)

enum class TexturePackAlgorithm {
	BEST,
	GREEDY
}

@Serializable
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
