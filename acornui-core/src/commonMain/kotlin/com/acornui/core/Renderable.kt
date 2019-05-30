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

package com.acornui.core

import com.acornui.component.RenderContextRo
import com.acornui.component.layout.SizableRo
import com.acornui.math.MinMax
import com.acornui.math.Pad
import com.acornui.math.PadRo
import kotlin.math.ceil
import kotlin.math.floor

interface Renderable : SizableRo {

	/**
	 * The added area to the bounds needed for rendering.
	 */
	val renderMargin: PadRo
		get() = Pad.EMPTY_PAD

	/**
	 * Overrides the default render context.
	 */
	var renderContextOverride: RenderContextRo?

	/**
	 * The render context that will be used if there is no [renderContextOverride] value set.
	 * The render context is responsible for transformation of local vertices to global vertices at the [render] phase.
	 */
	val naturalRenderContext: RenderContextRo

	/**
	 * Renders any graphics.
	 */
	fun render()
}

/**
 * The rendering context to be used within [render].
 * This can be overriden via [Renderable.renderContextOverride]
 */
val Renderable.renderContext: RenderContextRo
	get() = renderContextOverride ?: naturalRenderContext

/**
 * Renders the target with the given render context.
 */
fun Renderable.render(renderContext: RenderContextRo) {
	val old = renderContextOverride
	renderContextOverride = renderContext
	render()
	renderContextOverride = old
}

/**
 * Calculates the local drawing region of this renderable component.
 * Use `renderContext.localToCanvas` to convert to canvas coordinates.
 * @see Renderable.renderContext
 * @see com.acornui.component.localToCanvas
 */
fun Renderable.drawRegion(out: MinMax): MinMax {
	val bounds = bounds
	out.set(0f, 0f, bounds.width, bounds.height)
	out.inflate(renderMargin)
	out.xMin = floor(out.xMin)
	out.yMin = floor(out.yMin)
	out.xMax = ceil(out.xMax)
	out.yMax = ceil(out.yMax)
	return out
}
