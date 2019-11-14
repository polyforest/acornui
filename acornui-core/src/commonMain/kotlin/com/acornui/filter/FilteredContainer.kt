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

import com.acornui.RedrawRegions
import com.acornui.collection.WatchedElementsActiveList
import com.acornui.collection.forEach2
import com.acornui.component.ComponentInit
import com.acornui.component.FillLayoutContainer
import com.acornui.component.UiComponent
import com.acornui.component.ValidationFlags
import com.acornui.di.Owned
import com.acornui.di.own
import com.acornui.math.*
import com.acornui.signal.bind
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class FilteredContainer(owner: Owned) : FillLayoutContainer<UiComponent>(owner) {

	private val _renderFilters = own(WatchedElementsActiveList<RenderFilter>().apply {
		bind {
			invalidate(ValidationFlags.REDRAW_REGIONS)
		}
	})

	val renderFilters: MutableList<RenderFilter> = _renderFilters

	init {
		draws = true
	}

	operator fun <T : RenderFilter> T.unaryPlus(): T {
		_renderFilters.add(this)
		return this
	}

	operator fun <T : RenderFilter> T.unaryMinus(): T {
		_renderFilters.remove(this)
		return this
	}

	override fun updateDrawRegionCanvas(out: Rectangle) {
		super.updateDrawRegionCanvas(out)
		_renderFilters.forEach2 {
			out += it.drawPadding
		}
	}

	override fun draw() {
		draw(_renderFilters.lastIndex, drawRegionCanvas)
	}
	
	private fun draw(filterIndex: Int, drawRegion: RectangleRo) {
		if (filterIndex == -1)
			super.draw()
		else {
			val filter = _renderFilters[filterIndex]
			filter.render(drawRegion) {
				draw(filterIndex - 1, drawRegion - filter.drawPadding)
			}
		}
	}
}

inline fun Owned.filtered(init: ComponentInit<FilteredContainer> = {}): FilteredContainer  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = FilteredContainer(this)
	c.init()
	return c
}