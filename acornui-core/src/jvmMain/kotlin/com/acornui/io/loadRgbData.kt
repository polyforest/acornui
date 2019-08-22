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

package com.acornui.io

import com.acornui.async.UI
import com.acornui.graphic.RgbData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.imageio.ImageIO

suspend fun loadRgbData(requestData: UrlRequestData, progressReporter: ProgressReporter = GlobalProgressReporter, initialTimeEstimate: Float = Bandwidth.downBpsInv * 100_000): RgbData {
	return load(requestData, progressReporter, initialTimeEstimate) { inputStream ->
		createImageData(inputStream)
				?: throw Exception("Could not load image at path \"${requestData.toUrlStr()}\"")
	}
}

/**
 * Creates the RgbData for the given input stream.
 *
 * Due to https://bugs.openjdk.java.net/browse/JDK-8058973, this will be executed on the main thread.
 */
suspend fun createImageData(input: InputStream): RgbData? = withContext(Dispatchers.UI) {
	val image = ImageIO.read(input)
	input.close()
	if (image == null) return@withContext null

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
	return@withContext data
}