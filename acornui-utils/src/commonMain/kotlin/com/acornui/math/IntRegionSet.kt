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

package com.acornui.math

import com.acornui.collection.firstOrNull2
import com.acornui.recycle.*

/**
 * A set of [IntRectangleRo] objects, when a new rectangle is added, intersections are eliminated by removing
 * regions where one is contained in another, and splitting intersections so there's no overlap.
 */
class IntRegionSet(
		private val pool: Pool<IntRectangle> = ObjectPool { IntRectangle() }
) : Clearable {

	// TODO: region grow - [0, 0, 4, 4] + [0, 4, 4, 4] = [0, 0, 4, 8]

	private val _regions = ArrayList<IntRectangle>()

	val regions: List<IntRectangleRo> = _regions

	private fun obtain(x: Int, y: Int, width: Int, height: Int): IntRectangle {
		return pool.obtain().set(x, y, width, height)
	}

	private fun obtain(region: IntRectangleRo): IntRectangle {
		return pool.obtain().set(region)
	}

	/**
	 * Adds a region to the set.
	 * Any overlap will be eliminated.
	 */
	fun add(x: Int, y: Int, width: Int, height: Int) {
		if (width == 0 || height == 0) return
		addInternal(obtain(x, y, width, height))
	}

	/**
	 * Adds a region to the set.
	 * Any overlap will be eliminated.
	 */
	fun add(region: IntRectangleRo) {
		if (region.isEmpty()) return
		addInternal(obtain(region))
	}

	private fun addInternal(region: IntRectangle) {
		var i = 0
		var n = _regions.size
		while (i < n) {
			// If the new region completely contains an existing region, remove the existing region.
			if (region.contains(_regions[i])) {
				pool.free(_regions.removeAt(i))
				i--; n--
			}
			i++
		}
		val intersected = _regions.firstOrNull2 {
			it.intersects(region)
		}
		if (intersected != null) {
			split(intersected, region)
			return
		}
		_regions.add(region)
	}

	/**
	 * Takes two intersecting rectangles, and splits [newRegion] into sub-rectangles that eliminate overlap.
	 */
	private fun split(existingRegion: IntRectangleRo, newRegion: IntRectangleRo) {
		if (existingRegion.contains(newRegion)) return
		if (newRegion.y < existingRegion.y) {
			addInternal(obtain(newRegion.x, newRegion.y, newRegion.width, existingRegion.y - newRegion.y))
		}
		if (newRegion.bottom > existingRegion.bottom) {
			addInternal(obtain(newRegion.x, existingRegion.bottom, newRegion.width, newRegion.bottom - existingRegion.bottom))
		}
		if (newRegion.x < existingRegion.x) {
			val top = maxOf(newRegion.y, existingRegion.y)
			val bottom = minOf(newRegion.bottom, existingRegion.bottom)
			addInternal(obtain(newRegion.x, top, existingRegion.x - newRegion.x, bottom - top))
		}
		if (newRegion.right > existingRegion.right) {
			val top = maxOf(newRegion.y, existingRegion.y)
			val bottom = minOf(newRegion.bottom, existingRegion.bottom)
			addInternal(obtain(existingRegion.right, top, newRegion.right - existingRegion.right, bottom - top))
		}
	}

	override fun clear() {
		_regions.freeTo(pool)
	}
}