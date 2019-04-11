package com.acornui.filter

import com.acornui.component.ComponentInit
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.gl.core.GlState
import com.acornui.gl.core.useColorTransformation
import com.acornui.graphic.Color
import com.acornui.math.*

open class DropShadowFilter(owner: Owned) : BlurFilter(owner) {

	val colorTransformation: ColorTransformationRo by bindable(defaultColorTransformation)

	/**
	 * The x offset to translate the rendering of the frame buffer.
	 */
	var offsetX by bindable(0f)

	/**
	 * The y offset to translate the rendering of the frame buffer.
	 */
	var offsetY by bindable(0f)

	private val glState = inject(GlState)

	override val shouldSkipFilter: Boolean
		get() = !enabled

	init {
		offsetX = defaultOffsetX
		offsetY = defaultOffsetY
	}

	override fun draw(clip: MinMaxRo) {
		renderToFramebuffer(clip)
		glState.useColorTransformation(colorTransformation) {
			drawToScreen(offsetX, offsetY)
		}
		renderContents(clip)
	}

	companion object {
		var defaultColorTransformation = colorTransformation { tint(Color(0f, 0f, 0f, 0.5f)) }
		var defaultOffsetX = 3f
		var defaultOffsetY = 3f
	}
}

fun Owned.dropShadowFilter(init: ComponentInit<DropShadowFilter> = {}): DropShadowFilter {
	val b = DropShadowFilter(this)
	b.init()
	return b
}