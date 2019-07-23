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

import com.acornui.core.Renderable
import com.acornui.core.RenderableBase
import com.acornui.math.MinMaxRo
import com.acornui.math.Pad

class PaddedDrawable<T : Renderable>(

		/**
		 * The inner drawable this padding decorator wraps.
		 */
		val inner: T

) : RenderableBase() {

	/**
	 * The padding to add to the [inner] drawable's natural size.  (In points)
	 */
	val padding = Pad()

	override val drawRegion: MinMaxRo
		get() = _drawRegion.set(inner.drawRegion).translate(padding.left, padding.top)

	override fun onSizeSet(oldW: Float?, oldH: Float?, newW: Float?, newH: Float?) {
		inner.setSize(padding.reduceWidth(newW), padding.reduceHeight(newH))
		_bounds.set(padding.expandWidth2(inner.width), padding.expandHeight2(inner.height))
	}

	private val drawableRenderContext = RenderContext()

	override fun render(renderContext: RenderContextRo) {
		drawableRenderContext.parentContext = renderContext
		drawableRenderContext.modelTransformLocal.setTranslation(padding.left, padding.top, 0f)
		inner.render(drawableRenderContext)
	}
}
