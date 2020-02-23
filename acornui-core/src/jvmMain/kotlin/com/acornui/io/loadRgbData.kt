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

import com.acornui.graphic.RgbData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.ByteArrayInputStream
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

suspend fun loadRgbData(requestData: UrlRequestData, settings: RequestSettings): RgbData {
	return load(requestData, settings) { inputStream ->
		try {
			createImageData(inputStream)
		} catch (e: CancellationException) {
			throw e
		} catch (e: Throwable) {
			throw Exception("Could not load image at path \"${requestData.toUrlStr(settings.rootPath)}\"", e)
		}
	}
}

/**
 * Creates the RgbData for the given input stream.
 *
 * Due to https://bugs.openjdk.java.net/browse/JDK-8058973,
 */
suspend fun createImageData(inputStream: InputStream): RgbData = withContext(Dispatchers.IO) {
	val byteArrayInputStream = ByteArrayInputStream(inputStream.use {
		it.readAllBytes2()
	})

	val image: BufferedImage = try {
		ImageIO.read(byteArrayInputStream)
	} catch (e: Throwable) {
		@Suppress("BlockingMethodInNonBlockingContext")
		withContext(Dispatchers.Main) {
			byteArrayInputStream.reset()
			ImageIO.read(byteArrayInputStream)
		}
	} finally {
		byteArrayInputStream.close()
	}

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