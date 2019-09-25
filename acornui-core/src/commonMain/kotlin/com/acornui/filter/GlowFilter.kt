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

package com.acornui.filter

import com.acornui.Renderable
import com.acornui.component.ComponentInit
import com.acornui.component.RenderContextRo
import com.acornui.component.childRenderContext
import com.acornui.di.Owned
import com.acornui.gl.core.useColorTransformation
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.ColorTransformationRo
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.math.colorTransformation
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class GlowFilter(owner: Owned) : RenderFilterBase(owner) {

	var colorTransformation: ColorTransformationRo by bindable(defaultColorTransformation)

	private val blurFilter = BlurFilter(this)

	var blurX by bindableAndCall(1f) {
		blurFilter.blurX = it
	}

	var blurY by bindableAndCall(1f) {
		blurFilter.blurY = it
	}

	var quality by bindableAndCall(BlurQuality.NORMAL) {
		blurFilter.quality = it
	}

	/**
	 * The x offset to translate the rendering of the framebuffer.
	 */
	var offsetX by bindable(0f)

	/**
	 * The y offset to translate the rendering of the frame buffer.
	 */
	var offsetY by bindable(0f)

	private val _drawPadding = Pad()
	override val drawPadding: PadRo
		get() {
			val blurPadding = blurFilter.drawPadding
			return _drawPadding.set(
					blurPadding.top + maxOf(0f, -offsetY),
					blurPadding.right + maxOf(0f, offsetX),
					blurPadding.bottom + maxOf(0f, offsetY),
					blurPadding.left + maxOf(0f, -offsetX)
			)
		}

	override val shouldSkipFilter: Boolean
		get() = !enabled

	override var contents: Renderable?
		get() = super.contents
		set(value) {
			super.contents = value
			blurFilter.contents = value
		}

	override fun draw(renderContext: RenderContextRo) {
		if (!bitmapCacheIsValid)
			drawToFramebuffer()

		glState.useColorTransformation(colorTransformation) {
			renderContext.childRenderContext {
				modelTransformLocal.translate(offsetX, offsetY)
				blurFilter.drawBlurToScreen(this)
			}
		}
		blurFilter.drawOriginalToScreen(renderContext)
	}

	fun drawToFramebuffer() {
		blurFilter.drawToPingPongBuffers()
	}

	companion object {
		private val defaultColorTransformation = colorTransformation { tint(Color(0f, 0f, 0f, 0.5f)) }
	}
}

inline fun Owned.dropShadowFilter(init: ComponentInit<GlowFilter> = {}): GlowFilter  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = GlowFilter(this)
	b.offsetX = 3f
	b.offsetY = 3f
	b.init()
	return b
}

inline fun Owned.glowFilter(color: ColorRo, init: ComponentInit<GlowFilter> = {}): GlowFilter  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = GlowFilter(this)
	b.offsetX = 0f
	b.offsetY = 0f
	b.colorTransformation = colorTransformation {
		tint(0f, 0f, 0f, color.a)
		offset(color.r, color.g, color.b)
	}
	b.init()
	return b
}