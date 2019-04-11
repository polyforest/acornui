package com.acornui.core

import com.acornui.component.canvasToLocal
import com.acornui.component.localToCanvas
import com.acornui.math.MinMax
import com.acornui.math.MinMaxRo

interface Renderable {

	/**
	 * True if the component should be rendered.
	 */
	val visible: Boolean

	/**
	 * Calculates the region of the canvas that this component will draw.
	 */
	fun canvasDrawRegion(out: MinMax): MinMax

	/**
	 * Renders any graphics.
	 * [render] does not check the [visible] flag; that is the responsibility of the caller.
	 *
	 * Canvas coordinates are 0,0 top left, and bottom right is the canvas width/height without dpi scaling.
	 *
	 * You may convert the window coordinate clip region to local coordinates via [canvasToLocal], but in general it is
	 * faster to convert the local coordinates to window coordinates [localToCanvas], as no matrix inversion is
	 * required.
	 *
	 * @param clip The visible region (in viewport coordinates.) If you wish to render a component with a no
	 * clipping, you may use [MinMaxRo.POSITIVE_INFINITY]. This is used in order to potentially avoid drawing things
	 * the user cannot see. (Due to the screen size, stencil buffers, or scissors)
	 */
	fun render(clip: MinMaxRo)
}