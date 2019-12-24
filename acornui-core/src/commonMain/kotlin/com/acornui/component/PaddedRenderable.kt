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

package com.acornui.component

import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4
import com.acornui.math.Matrix4Ro
import com.acornui.math.Pad

/**
 * Adds padding to an [inner] renderable component.
 */
class PaddedRenderable<T : BasicRenderable>(

		/**
		 * The inner drawable this padding decorator wraps.
		 */
		val inner: T

) : BasicRenderable {

	/**
	 * The padding to add to the [inner] drawable's natural size.  (In points)
	 */
	val padding = Pad()

	override val naturalWidth: Float
		get() = padding.expandWidth(inner.naturalWidth)

	override val naturalHeight: Float
		get() = padding.expandHeight(inner.naturalHeight)

	private val innerTransform = Matrix4()

	override fun updateGlobalVertices(width: Float, height: Float, transform: Matrix4Ro, tint: ColorRo) {
		innerTransform.set(transform).translate(padding.left, padding.top, 0f)
		inner.updateGlobalVertices(padding.reduceWidth(width), padding.reduceHeight(height), innerTransform, tint)
	}

	override fun render() {
		inner.render()
	}
}
