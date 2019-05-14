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

package com.acornui.jvm.graphic

import com.acornui.core.graphic.*
import java.awt.image.*
import java.io.*
import javax.imageio.*

object JvmImageUtils {
	fun rgbDataToFile(rgbData:RgbData, path: String, extension: String = "png", hasAlpha: Boolean, premultipliedAlpha: Boolean) {
		val pageImage = BufferedImage(rgbData.width, rgbData.height, if (hasAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB)
		val sampleModel = ComponentSampleModel(DataBuffer.TYPE_BYTE, rgbData.width, rgbData.height, rgbData.numBands, rgbData.scanSize, if (hasAlpha) intArrayOf(0, 1, 2, 3) else intArrayOf(0, 1, 2))
		val raster = Raster.createRaster(sampleModel, DataBufferByte(rgbData.bytes, rgbData.bytes.size), null)
		pageImage.data = raster
		val output = File(path)
		output.mkdirs()
		if (premultipliedAlpha) pageImage.colorModel.coerceData(pageImage.raster, premultipliedAlpha)
		ImageIO.write(pageImage, extension, output)
	}
}

