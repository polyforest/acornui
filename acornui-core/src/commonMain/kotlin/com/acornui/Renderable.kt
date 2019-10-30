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

@file:Suppress("PropertyName", "MemberVisibilityCanBePrivate")

package com.acornui

import com.acornui.component.RenderContextRo
import com.acornui.component.layout.Sizable
import com.acornui.component.layout.SizableRo
import com.acornui.math.Bounds
import com.acornui.math.BoundsRo
import com.acornui.math.MinMax
import com.acornui.math.MinMaxRo

interface RenderableRo : SizableRo {

	/**
	 * The local drawing region of this renderable component.
	 * Use `renderContext.localToCanvas` to convert this region to canvas coordinates.
	 * The draw region is not used for a typical [render], but may be used for render filters or components that
	 * need to set a region for a frame buffer.
	 *
	 * @see com.acornui.component.localToCanvas
	 */
	val drawRegion: MinMaxRo

	/**
	 * Renders any graphics using the given [renderContext].
	 */
	fun render(renderContext: RenderContextRo)
}

interface Renderable : RenderableRo, Sizable

abstract class RenderableBase : Renderable, Sizable {

	protected val _bounds = Bounds()
	override val bounds: BoundsRo
		get() = _bounds

	protected val _drawRegion = MinMax()
	override val drawRegion: MinMaxRo
		get() = _drawRegion.set(0f, 0f, width, height)

	/**
	 * The explicit width, as set by width(value)
	 * Typically one would use `width` in order to retrieve the explicit or actual width.
	 */
	final override var explicitWidth: Float? = null
		private set

	/**
	 * The explicit height, as set by height(value)
	 * Typically one would use `height` in order to retrieve the explicit or actual height.
	 */
	final override var explicitHeight: Float? = null
		private set

	/**
	 * Does the same thing as setting width and height individually.
	 */
	final override fun setSize(width: Float?, height: Float?) {
		if (width?.isNaN() == true || height?.isNaN() == true) throw Exception("May not set the size to be NaN")
		val oldW = explicitWidth
		val oldH = explicitHeight
		explicitWidth = width
		explicitHeight = height
		onSizeSet(oldW, oldH, width, height)
	}

	/**
	 * Invoked when the size has been set, providing the new and old explicit width and height.
	 */
	abstract fun onSizeSet(oldW: Float?, oldH: Float?, newW: Float?, newH: Float?)

	/**
	 * Renders any graphics using the provided [renderContext].
	 */
	abstract override fun render(renderContext: RenderContextRo)
}