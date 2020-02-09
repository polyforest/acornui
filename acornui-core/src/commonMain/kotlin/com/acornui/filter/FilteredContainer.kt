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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.acornui.filter

import com.acornui.collection.WatchedElementsActiveList
import com.acornui.component.*
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.math.MinMaxRo
import com.acornui.math.Rectangle
import com.acornui.math.RectangleRo
import com.acornui.signal.bind
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class FilteredContainer(owner: Context) : FillLayoutContainer<UiComponent>(owner) {

	private val _renderFilters = own(WatchedElementsActiveList<RenderFilter>().apply {
		bind { invalidate(ValidationFlags.VERTICES_GLOBAL) }
	})

	val renderFilters: MutableList<RenderFilter> = _renderFilters

	init {
		canvasClipRegionOverride = MinMaxRo.POSITIVE_INFINITY
	}

	operator fun <T : RenderFilter> T.unaryPlus(): T {
		_renderFilters.add(this)
		return this
	}

	operator fun <T : RenderFilter> T.unaryMinus(): T {
		_renderFilters.remove(this)
		return this
	}

	private var expandedDrawRegion: RectangleRo = RectangleRo.EMPTY

	override fun updateVerticesGlobal() {
		super.updateVerticesGlobal()
		var drawRegionCanvas: RectangleRo = localToCanvas(Rectangle(0f, 0f, _bounds.width, _bounds.height))
		val model = transformGlobal
		val tint = colorTintGlobal
		for (i in _renderFilters.lastIndex downTo 0) {
			val renderFilter = _renderFilters[i]
			drawRegionCanvas = renderFilter.updateGlobalVertices(drawRegionCanvas, model, tint)
		}
		expandedDrawRegion = drawRegionCanvas
	}

	override fun draw() {
		draw(_renderFilters.lastIndex)
	}
	
	private fun draw(filterIndex: Int) {
		if (filterIndex == -1)
			super.draw()
		else {
			val filter = _renderFilters[filterIndex]
			filter.render {
				draw(filterIndex - 1)
			}
		}
	}

}

inline fun Context.filtered(init: ComponentInit<FilteredContainer> = {}): FilteredContainer  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = FilteredContainer(this)
	c.init()
	return c
}