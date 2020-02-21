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

import com.acornui.asset.cacheSet
import com.acornui.asset.loadAndCacheJson
import com.acornui.asset.loadTexture
import com.acornui.di.Context
import com.acornui.math.IntRectangle
import com.acornui.math.IntRectangleRo
import com.acornui.math.MathUtils.mod

object ExtrudeUtil {

	private val color = Color()

	private val checkDeltas = intArrayOf(0, -1, 0, 0, -1, 0, -1, -1) // Up, Right, Down, Left

	/**
	 * Calculates the perimeter of the texture region.
	 *
	 * @return a clockwise list of x,y,... perimeter points. The last point will always be the same as the first point.
	 */
	fun calculatePerimeter(texture: Texture, region: IntRectangleRo, alphaThreshold: Float = 0.1f): List<Int> {
		val rgbData = texture.rgbData!!
		// Find starting location
		val start = getStartingLocation(rgbData, region, alphaThreshold)
				?: return listOf(region.left, region.top, region.right, region.top, region.right, region.bottom, region.left, region.bottom, region.left, region.top) // No pixel was below the alpha threshold.
		val check = {
			x: Int, y: Int ->
			if (x < region.left || y < region.top || x >= region.right || y >= region.bottom) {
				false
			} else {
				rgbData.getPixel(x, y, color).a >= alphaThreshold
			}
		}

		val list = ArrayList<Int>()
		list.add(start.first)
		list.add(start.second)
		list.add(start.first + 1)
		list.add(start.second)
		var x = start.first + 1
		var y = start.second
		var lastDir = 0

		do {
			for (i in lastDir until lastDir + 4) {
				val d = mod(i, 4)
				val xD = checkDeltas[d * 2]
				val yD = checkDeltas[d * 2 + 1]
				if (check(x + xD, y + yD)) {
					lastDir = d - 1
					when (d) {
						0 -> y--
						1 -> x++
						2 -> y++
						3 -> x--
					}
					break
				}
			}
			list.add(x)
			list.add(y)
			if (list.size > 10000) throw Exception("Endless loop detected")
		} while (x != start.first || y != start.second)

		return list
	}

	private fun getStartingLocation(rgbData: RgbData, region: IntRectangleRo, alphaThreshold: Float): Pair<Int, Int>? {
		for (x in region.left until region.right) {
			for (y in region.top until region.bottom) {
				val pixel = rgbData.getPixel(x, y, color)
				if (pixel.a >= alphaThreshold) return Pair(x, y)
			}
		}
		return null
	}
}

/**
 * Given a graphic, this utility will load the png, trace its perimeter, then return a clockwise list of x,y,...
 * perimeter points.
 * This currently only works on the jvm backend (Texture.rgbData support), and these data points should be saved.
 */
suspend fun Context.calculatePerimeter(path: String, alphaThreshold: Float = 0.1f): List<Int> {
	val texture = loadTexture(path)
	return ExtrudeUtil.calculatePerimeter(texture, IntRectangle(0, 0, texture.widthPixels, texture.heightPixels), alphaThreshold)
}

suspend fun Context.calculatePerimeter(atlasPath: String, regionName: String, alphaThreshold: Float = 0.1f): List<Int> {
	val group = cacheSet()
	val atlasData = loadAndCacheJson(TextureAtlasData.serializer(), atlasPath, group)
	val (page, region) = atlasData.findRegion(regionName) ?: throw Exception("Region '$regionName' not found in atlas.")
	val texture = loadAndCacheAtlasPage(atlasPath, page, group)
	return ExtrudeUtil.calculatePerimeter(texture, region.bounds, alphaThreshold)
}
