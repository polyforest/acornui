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

package com.acornui.jvm.graphics

import com.acornui.core.assets.AssetTypes
import com.acornui.core.graphics.RgbData
import com.acornui.core.graphics.Texture
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.jvm.loader.JvmAssetLoaderBase
import java.io.InputStream
import javax.imageio.ImageIO

/**
 * An asset loader for textures (images).
 * @author nbilyk
 */
open class JvmTextureLoader(
		path: String,
		private val gl: Gl20,
		private val glState: GlState,
		isAsync: Boolean
) : JvmAssetLoaderBase<Texture>(path, AssetTypes.TEXTURE, isAsync) {

	init {
		init()
	}

	override fun create(fis: InputStream): Texture {
		return JvmTexture(gl, glState, createImageData(fis))
	}
}

open class JvmRgbDataLoader(
		path: String,
		isAsync: Boolean
) : JvmAssetLoaderBase<RgbData>(path, AssetTypes.RGB_DATA, isAsync) {

	init {
		init()
	}

	override fun create(fis: InputStream): RgbData {
		return createImageData(fis)
	}
}

fun createImageData(input: InputStream): RgbData {
	val image = ImageIO.read(input)
	input.close()

	val width = image.width
	val height = image.height

	val raster = image.raster
	val colorModel = image.colorModel
	val numBands = raster.numBands
	val colorData = ByteArray(numBands)

	val data = RgbData(width, height, hasAlpha = true)
	var i = 0
	for (y in 0..height - 1) {
		for (x in 0..width - 1) {
			raster.getDataElements(x, y, colorData)
			data[i++] = colorModel.getRed(colorData).toByte()
			data[i++] = colorModel.getGreen(colorData).toByte()
			data[i++] = colorModel.getBlue(colorData).toByte()
			data[i++] = colorModel.getAlpha(colorData).toByte()
		}
	}
	return data
}