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

package com.acornui.jvm.opengl

import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.GlTextureBase
import com.acornui.graphic.*
import com.acornui.io.GlobalProgressReporter
import com.acornui.io.JvmBufferUtil
import com.acornui.io.ProgressReporter
import com.acornui.io.Bandwidth
import com.acornui.io.UrlRequestData
import com.acornui.io.load
import java.io.InputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

/**
 * @author nbilyk
 */
class JvmTexture(gl: Gl20,
				 glState: GlState,
				 private val _rgbData: RgbData
) : GlTextureBase(gl, glState) {

	var bytes: ByteBuffer? = JvmBufferUtil.wrap(_rgbData.bytes)

	override val widthPixels: Int
		get() = _rgbData.width

	override val heightPixels: Int
		get() = _rgbData.height

	override val rgbData: RgbData
		get() = _rgbData
}



/**
 * Creates an http request, processing the results as a [Texture].
 */
suspend fun loadTexture(gl: Gl20, glState: GlState, requestData: UrlRequestData, progressReporter: ProgressReporter = GlobalProgressReporter, initialTimeEstimate: Float = Bandwidth.downBpsInv * 100_000): Texture {
	return JvmTexture(gl, glState, loadRgbData(requestData, progressReporter, initialTimeEstimate))
}

suspend fun loadRgbData(requestData: UrlRequestData, progressReporter: ProgressReporter = GlobalProgressReporter, initialTimeEstimate: Float = Bandwidth.downBpsInv * 100_000): RgbData {
	return load(requestData, progressReporter, initialTimeEstimate) { inputStream ->
		createImageData(inputStream) ?: throw Exception("Could not load image at path \"${requestData.toUrlStr()}\"")
	}
}

/**
 */
fun createImageData(input: InputStream): RgbData? {
	val image = ImageIO.read(input)
	input.close()
	if (image == null) return null

	val width = image.width
	val height = image.height

	val raster = image.raster
	val colorModel = image.colorModel
	val numBands = raster.numBands
	val colorData = ByteArray(numBands)

	val data = RgbData(width, height, hasAlpha = true)
	var i = 0
	for (y in 0 until height) {
		for (x in 0 until width) {
			raster.getDataElements(x, y, colorData)
			data[i++] = colorModel.getRed(colorData).toByte()
			data[i++] = colorModel.getGreen(colorData).toByte()
			data[i++] = colorModel.getBlue(colorData).toByte()
			data[i++] = colorModel.getAlpha(colorData).toByte()
		}
	}
	return data
}
