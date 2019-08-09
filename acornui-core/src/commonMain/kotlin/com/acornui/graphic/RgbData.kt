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

package com.acornui.graphic

import com.acornui.graphic.*
import com.acornui.math.*
import com.acornui.serialization.*

/**
 * A byte array of pixels.
 */
class RgbData(

		/**
		 * The width of this image.
		 */
		initialWidth: Int,

		/**
		 * The height of this image.
		 */
		initialHeight: Int,

		val hasAlpha: Boolean) {

	var width = initialWidth
		private set

	var height = initialHeight
		private set

	val numBands: Int = if (hasAlpha) 4 else 3

	var scanSize: Int = width * numBands
		private set

	var bytes: ByteArray = ByteArray(width * height * numBands)
		private set

	operator fun get(index: Int): Byte {
		return bytes[index]
	}

	operator fun set(index: Int, value: Byte) {
		bytes[index] = value
	}

	val lastIndex: Int
		get(): Int = bytes.lastIndex

	/**
	 * @param out The color to populate.
	 * @return The provided color object.
	 */
	fun getPixel(x: Int, y: Int, out: Color): Color {
		var i = y * scanSize + x * numBands
		out.r = bytes[i++].toFloatRange()
		out.g = bytes[i++].toFloatRange()
		out.b = bytes[i++].toFloatRange()
		if (hasAlpha) {
			out.a = bytes[i].toFloatRange()
		} else {
			out.a = 1f
		}
		return out
	}

	/**
	 * Returns a 0-1 alpha value for the given pixel. If hasAlpha is false, 1f will be returned.
	 */
	fun getAlpha(x: Int, y: Int): Float {
		if (!hasAlpha) return 1f
		val i = y * scanSize + x * numBands
		return bytes[i + 3].toFloatRange()
	}

	/**
	 * Writes the color to the provided location.
	 */
	fun setPixel(x: Int, y: Int, color: ColorRo) {
		var i = y * scanSize + x * numBands
		bytes[i++] = (color.r * 255f).toByte()
		bytes[i++] = (color.g * 255f).toByte()
		bytes[i++] = (color.b * 255f).toByte()
		if (hasAlpha) {
			bytes[i] = (color.a * 255f).toByte()
		}
	}

	/**
	 * Fills the supplied area a solid color.
	 */
	fun fillRect(x: Int, y: Int, width: Int, height: Int, color: ColorRo) {
		val lastY = minOf(this.height, y + height) - 1
		val lastX = minOf(this.width, x + width) - 1
		for (y2 in y..lastY) {
			for (x2 in x..lastX) {
				setPixel(x2, y2, color)
			}
		}
	}

	/**
	 * Fills the entire rgb data with a single color.
	 */
	fun flood(color: ColorRo = Color.CLEAR) {
		val r = (color.r * 255f).toByte()
		val g = (color.g * 255f).toByte()
		val b = (color.b * 255f).toByte()
		val a = (color.a * 255f).toByte()
		for (i in 0..lastIndex step numBands) {
			var j = i
			bytes[j++] = r
			bytes[j++] = g
			bytes[j++] = b
			if (hasAlpha) {
				bytes[j] = a
			}
		}
	}

	private fun transferPixelTo(sourceX: Int, sourceY: Int, dest: RgbData, destX: Int, destY: Int) {
		transferPixelTo(sourceX, sourceY, dest.bytes, dest.scanSize, dest.hasAlpha, dest.numBands, destX, destY)
	}

	private fun transferPixelTo(sourceX: Int, sourceY: Int, destBytes: ByteArray, destScanSize: Int, destHasAlpha: Boolean, destNumBands: Int, destX: Int, destY: Int) {
		var sourceIndex = sourceY * scanSize + sourceX * numBands
		var destIndex = destY * destScanSize + destX * destNumBands
		destBytes[destIndex++] = bytes[sourceIndex++]
		destBytes[destIndex++] = bytes[sourceIndex++]
		destBytes[destIndex++] = bytes[sourceIndex++]
		if (hasAlpha && destHasAlpha) {
			destBytes[destIndex] = bytes[sourceIndex]
		} else if (destHasAlpha) {
			destBytes[destIndex] = -1
		}
	}

	fun copySubRgbData(region: Rectangle): RgbData {
		return copySubRgbData(region.x.toInt(), region.y.toInt(), region.width.toInt(), region.height.toInt())
	}

	/**
	 * Creates a new RgbData object with the pixels set to the sub-region specified.
	 */
	fun copySubRgbData(sourceX: Int, sourceY: Int, width: Int, height: Int): RgbData {
		val subData = RgbData(width, height, hasAlpha)
		subData.setRect(0, 0, this, sourceX, sourceY, width, height)
		return subData
	}

	/**
	 * Rotates the image 90 degrees clockwise.
	 */
	fun rotate90CW() {
		val newScanSize: Int = height * numBands
		val newBytes = ByteArray(width * height * numBands);
		for (y in 0..height - 1) {
			val x2 = height - 1 - y
			for (x in 0..width - 1) {
				val y2 = x
				transferPixelTo(x, y, newBytes, newScanSize, hasAlpha, numBands, x2, y2)
			}
		}
		bytes = newBytes
		scanSize = newScanSize
		val tmp = width
		width = height
		height = tmp
	}

	/**
	 * Rotates the image 90 degrees clockwise.
	 */
	fun rotate90CCW() {
		val newScanSize: Int = height * numBands
		val newBytes = ByteArray(width * height * numBands);
		for (y in 0..height - 1) {
			val x2 = y
			for (x in 0..width - 1) {
				val y2 = width - 1 - x
				transferPixelTo(x, y, newBytes, newScanSize, hasAlpha, numBands, x2, y2)
			}
		}
		bytes = newBytes
		scanSize = newScanSize
		val tmp = width
		width = height
		height = tmp
	}

	fun setRect(destX: Int, destY: Int, source: RgbData, sourceX: Int = 0, sourceY: Int = 0, width: Int = source.width - sourceX, height: Int = source.height - sourceY) {
		val lastY = minOf(minOf(height, this.height - destY), source.height - sourceY) - 1
		val lastX = minOf(minOf(width, this.width - destX), source.width - sourceX) - 1
		for (y in 0..lastY) {
			for (x in 0..lastX) {
				source.transferPixelTo(sourceX + x, sourceY + y, this, destX + x, destY + y)
			}
		}
	}

	private fun Byte.toFloatRange(): Float {
		return (this.toInt() and 0xFF) * INV_BYTE
	}

	companion object {
		private const val INV_BYTE = 1f / 255f
	}
}

fun rgbData(width: Int, height: Int, hasAlpha: Boolean = true, init: RgbData.() -> Unit = {}): RgbData {
	val r = RgbData(width, height, hasAlpha)
	r.init()
	return r
}

object RgbDataSerializer : To<RgbData>, From<RgbData> {

	override fun read(reader: Reader): RgbData {
		return RgbData(
				initialWidth = reader.int("width")!!,
				initialHeight = reader.int("height")!!,
				hasAlpha = reader.bool("hasAlpha")!!
		).apply {
			val bytes = reader.byteArray("bytes")!!
			for (i in 0..bytes.lastIndex) {
				this[i] = bytes[i]
			}
		}
	}

	override fun RgbData.write(writer: Writer) {
		writer.int("width", width)
		writer.int("height", height)
		writer.bool("hasAlpha", hasAlpha)
		writer.byteArray(bytes)
	}
}
