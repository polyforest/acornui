package com.acornui.filter

import com.acornui.component.ComponentInit
import com.acornui.core.Renderable
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.gl.core.GlState
import com.acornui.gl.core.useColorTransformation
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.reflect.observableAndCall

open class DropShadowFilter(owner: Owned) : RenderFilterBase(owner) {

	val colorTransformation: ColorTransformationRo by bindable(defaultColorTransformation)

	private val blurFilter = BlurFilter(this)

	var blurX by observableAndCall(1f) {
		blurFilter.blurX = it
	}

	var blurY by observableAndCall(1f) {
		blurFilter.blurY = it
	}

	var quality by observableAndCall(BlurQuality.NORMAL) {
		blurFilter.quality = it
	}

	/**
	 * The x offset to translate the rendering of the frame buffer.
	 */
	var offsetX by bindable(3f)

	/**
	 * The y offset to translate the rendering of the frame buffer.
	 */
	var offsetY by bindable(3f)

	private val tmpMinMax = MinMax()

	override fun drawRegion(out: MinMax): MinMax {
		return blurFilter.drawRegion(out).translate(offsetX, offsetY).ext(super.drawRegion(tmpMinMax))
	}

	private val glState = inject(GlState)

	override val shouldSkipFilter: Boolean
		get() = !enabled

	override var contents: Renderable? = null
		set(value) {
			field = value
			blurFilter.contents = value
		}

	private val offsetTransform = Matrix4()

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		offsetTransform.set(transform).translate(offsetX, offsetY, 0f)
		if (!bitmapCacheIsValid)
			blurFilter.drawToPingPongBuffers(clip)

		glState.useColorTransformation(colorTransformation) {
			blurFilter.drawBlurToScreen(clip, offsetTransform, tint)
		}
		blurFilter.drawOriginalToScreen(clip, transform, tint)
	}

	companion object {
		private val defaultColorTransformation = colorTransformation { tint(Color(0f, 0f, 0f, 0.5f)) }
	}
}

fun Owned.dropShadowFilter(init: ComponentInit<DropShadowFilter> = {}): DropShadowFilter {
	val b = DropShadowFilter(this)
	b.init()
	return b
}