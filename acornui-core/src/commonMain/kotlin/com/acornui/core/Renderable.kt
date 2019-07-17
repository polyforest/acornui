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
import com.acornui.gl.core.GlState
import com.acornui.math.MinMaxRo

interface Renderable : SizableRo {

	/**
	 * The local drawing region of this renderable component.
	 * Use `renderContext.localToCanvas` to convert this region to canvas coordinates.
	 * The draw region is not used for a typical [render], but may be used for render filters or components that
	 * need to set a region for a frame buffer.
	 *
	 * @see Renderable.renderContext
	 * @see com.acornui.component.localToCanvas
	 */
	val drawRegion: MinMaxRo

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
	 * Renders any graphics using the current [renderContext].
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
 * Sets the camera on the [glState] using this Renderable's current [renderContext].
 */
fun Renderable.useCamera(glState: GlState, useModel: Boolean = false) {
	val renderContext = renderContext
	if (useModel) glState.setCamera(renderContext.viewProjectionTransform, renderContext.viewTransform, renderContext.modelTransform)
	else glState.setCamera(renderContext.viewProjectionTransform, renderContext.viewTransform)
}