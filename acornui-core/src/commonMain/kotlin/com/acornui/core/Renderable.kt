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

package com.acornui.core

import com.acornui.component.RenderContextRo
import com.acornui.component.layout.Sizable
import com.acornui.component.layout.SizableRo
import com.acornui.gl.core.GlState
import com.acornui.graphic.ColorRo
import com.acornui.math.*

interface Renderable : Sizable {

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

abstract class RenderableBase : Renderable, Sizable {

	protected val _bounds = Bounds()
	override val bounds: BoundsRo
		get() = _bounds

	protected val _drawRegion = MinMax()
	override val drawRegion: MinMaxRo
		get() = _drawRegion.set(0f, 0f, width, height)

	override var renderContextOverride: RenderContextRo? = null

	override val naturalRenderContext: RenderContextRo
		get() = error("This component has no default render context. Set one via renderContextOverride.")

	/**
	 * The explicit width, as set by width(value)
	 * Typically one would use width() in order to retrieve the explicit or actual width.
	 */
	final override var explicitWidth: Float? = null
		private set

	/**
	 * The explicit height, as set by height(value)
	 * Typically one would use height() in order to retrieve the explicit or actual height.
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

	override fun render() {
		val renderContext = renderContext
		if (renderContext.colorTint.a <= 0f)
			return // Nothing visible.
		draw(renderContext.clipRegion, renderContext.modelTransform, renderContext.colorTint)
	}

	/**
	 * The core drawing method for this component.
	 * For convenience, draw is provided commonly used rendering properties from the render context:
	 * renderContext.clipRegion, renderContext.modelTransform, and renderContext.colorTint
	 */
	protected open fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {}
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