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

package com.acornui.filter

import com.acornui.collection.forEach2
import com.acornui.component.*
import com.acornui.core.Renderable
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.math.Bounds
import com.acornui.math.BoundsRo
import com.acornui.math.MinMaxRo

class FilteredContainer(owner: Owned) : ElementContainerImpl<UiComponent>(owner) {

	/**
	 * If there are render filters, inner renderable is set as the contents of the last render filter.
	 */
	private val innerRenderable: Renderable by lazy {

		object : Renderable {

			override val drawRegion: MinMaxRo
				get() = super@FilteredContainer.drawRegion

			override val bounds: BoundsRo
				get() = super@FilteredContainer.bounds

			override val naturalRenderContext: RenderContextRo
				get() = super@FilteredContainer.naturalRenderContext

			override var renderContextOverride: RenderContextRo?
				get() = super@FilteredContainer.renderContextOverride
				set(value) {
					super@FilteredContainer.renderContextOverride = value
				}

			@Suppress("RedundantOverride") // Erroneous warning
			override fun render() {
				super@FilteredContainer.render()
			}
		}
	}

	private val _renderFilters = own(RenderFilterList(innerRenderable).apply {
		changed.add {
			invalidate(ValidationFlags.BITMAP_CACHE)
		}
	})

	val renderFilters: MutableList<RenderFilter> = _renderFilters

	operator fun <T : RenderFilter> T.unaryPlus(): T {
		_renderFilters.add(this)
		return this
	}

	operator fun <T : RenderFilter> T.unaryMinus(): T {
		_renderFilters.remove(this)
		return this
	}

	override fun onInvalidated(flagsInvalidated: Int) {
		super.onInvalidated(flagsInvalidated)
		if (flagsInvalidated.containsFlag(ValidationFlags.BITMAP_CACHE)) {
			invalidateBitmapCache()
		}
	}

	fun invalidateBitmapCache() {
		window.requestRender()
		for (i in 0..renderFilters.lastIndex) {
			renderFilters[i].invalidateBitmapCache()
		}
	}

	//-------------------------------------------

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		elementsToLayout.forEach2 { element ->
			element.setSize(explicitWidth, explicitHeight)
			element.moveTo(0f, 0f)
			if (explicitWidth == null) {
				if (element.right > out.width)
					out.width = element.right
			}
			if (explicitHeight == null) {
				if (element.bottom > out.height)
					out.height = element.bottom
			}
			if (element.baseline > out.baseline)
				out.baseline = element.baseline
		}
	}

	//-------------------------------------------
	// Renderable
	//-------------------------------------------

	override val drawRegion: MinMaxRo
		get() = _renderFilters.drawRegion

	override var renderContextOverride: RenderContextRo?
		get() = _renderFilters.renderContextOverride
		set(value) {
			_renderFilters.renderContextOverride = value
		}

	override val naturalRenderContext: RenderContextRo
		get() = _renderFilters.naturalRenderContext

	override fun render() = _renderFilters.render()

	override val bounds: BoundsRo
		get() = _renderFilters.bounds
}

fun Owned.filtered(init: ComponentInit<FilteredContainer> = {}): FilteredContainer {
	val c = FilteredContainer(this)
	c.init()
	return c
}