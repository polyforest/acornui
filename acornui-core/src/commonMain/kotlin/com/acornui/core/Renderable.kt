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

	val renderContext: RenderContextRo

	/**
	 * Overrides the default render context.
	 */
	var renderContextOverride: RenderContextRo?

	/**
	 * Renders any graphics.
	 */
	fun render()
}

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
