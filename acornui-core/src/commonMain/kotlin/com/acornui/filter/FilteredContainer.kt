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
import com.acornui.collection.forEach2
import com.acornui.collection.forEachReversed2
import com.acornui.component.*
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.gl.core.useCamera
import com.acornui.graphic.orthographicCamera
import com.acornui.graphic.setViewport
import com.acornui.math.Bounds
import com.acornui.math.Matrix4
import com.acornui.math.MinMaxRo
import com.acornui.math.Rectangle
import com.acornui.signal.bind
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * An element container which supports a list of [RenderFilter] objects to decorate its rendering.
 *
 * This container uses a [FillLayout].
 */
class FilteredContainer(owner: Context) : FillLayoutContainer<UiComponent>(owner) {

	private val _renderFilters = own(WatchedElementsActiveList<RenderFilter>().apply {
		bind { invalidate(ValidationFlags.VERTICES_GLOBAL) }
	})

	val renderFilters: MutableList<RenderFilter> = _renderFilters

	private val camera = orthographicCamera(false)

	override val useTransforms: Boolean = true

	init {
//		cameraOverride = camera
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

	private val localDrawRegion = Rectangle()

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		localDrawRegion.set(_bounds)
		_renderFilters.forEachReversed2 {
			it.region(localDrawRegion)
		}
	}

	override fun updateVerticesGlobal() {
		super.updateVerticesGlobal()
		val model = transformGlobal
		val tint = colorTintGlobal
		_renderFilters.forEach2 {
			it.updateGlobalVertices(model, tint)
		}
	}

	override fun render() {
		if (visible && colorTint.a > 0f) {
			draw()
		}
	}

	override fun draw() {
		val w = window.width
		val h = window.height
		camera.setViewport(w, h)
		camera.moveToLookAtRect(0f, 0f, w, h)
//		println(transformGlobalInv)
//		modelInv.set(transformGlobalInv)

		cameraOverride = camera
		transformGlobalOverride = Matrix4.IDENTITY
		update()
		gl.uniforms.useCamera(camera) {
			drawLocal(_renderFilters.lastIndex)
		}
		cameraOverride = null
		transformGlobalOverride = null
		update()

		val p = parent ?: return
		gl.uniforms.useCamera(p.viewProjectionTransform, p.viewTransform, Matrix4.IDENTITY) {
			draw(_renderFilters.lastIndex)
		}

	}

	private fun drawLocal(filterIndex: Int) {
		return if (filterIndex == -1) {
			super.draw()
		} else {
			val filter = _renderFilters[filterIndex]
			filter.renderLocal {
				drawLocal(filterIndex - 1)
			}
		}
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