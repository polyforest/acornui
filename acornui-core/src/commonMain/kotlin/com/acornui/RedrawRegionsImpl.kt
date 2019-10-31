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

package com.acornui

import com.acornui.collection.any2
import com.acornui.math.IntRectangleRo
import com.acornui.math.IntRegionSet
import com.acornui.recycle.Clearable

interface RedrawRegionsRo {

	val regions: List<IntRectangleRo>

	fun needsRedraw(region: IntRectangleRo): Boolean
	
}

interface RedrawRegions : RedrawRegionsRo, Clearable {

	fun invalidate(x: Int, y: Int, width: Int, height: Int)
	fun invalidate(region: IntRectangleRo) = invalidate(region.x, region.y, region.width, region.height)

	companion object {

		/**
		 * An implementation that always recommends redraw.
		 */
		val ALWAYS: RedrawRegions = object : RedrawRegions {

			override val regions: List<IntRectangleRo> = emptyList()

			override fun invalidate(x: Int, y: Int, width: Int, height: Int) {}

			override fun needsRedraw(region: IntRectangleRo): Boolean = true

			override fun clear() {}
		}

		/**
		 * An implementation that never recommends redraw.
		 */
		val NEVER: RedrawRegions = object : RedrawRegions {

			override val regions: List<IntRectangleRo> = emptyList()

			override fun invalidate(x: Int, y: Int, width: Int, height: Int) {}

			override fun needsRedraw(region: IntRectangleRo): Boolean = false

			override fun clear() {}
		}
	}
}

class RedrawRegionsImpl : RedrawRegions {

	private var regionSet = IntRegionSet()

	override val regions: List<IntRectangleRo>
		get() = regionSet.regions

	/**
	 * Marks a region as needing to be redrawn.
	 */
	override fun invalidate(x: Int, y: Int, width: Int, height: Int) {
		regionSet.add(x, y, width, height)
	}

	/**
	 * Returns true if the region needs to be redrawn.
	 */
	override fun needsRedraw(region: IntRectangleRo): Boolean {
		if (region.isEmpty()) return false
		return regionSet.regions.any2 { it.intersects(region) }
	}

	override fun clear() {
		regionSet.clear()
	}
}