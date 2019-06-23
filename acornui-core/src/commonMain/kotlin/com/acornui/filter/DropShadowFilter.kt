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

	var colorTransformation: ColorTransformationRo by bindable(defaultColorTransformation)

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
	 * The x offset to translate the rendering of the framebuffer.
	 */
	var offsetX by bindable(3f)

	/**
	 * The y offset to translate the rendering of the frame buffer.
	 */
	var offsetY by bindable(3f)

	private val _drawPadding = Pad()
	override val drawPadding: PadRo
		get() {
			val blurPadding = blurFilter.drawPadding
			return _drawPadding.set(
					blurPadding.left + maxOf(0f, -offsetX),
					blurPadding.top + maxOf(0f, -offsetY),
					blurPadding.right + maxOf(0f, offsetX),
					blurPadding.bottom + maxOf(0f, offsetY)
			)
		}

	private val glState = inject(GlState)

	override val shouldSkipFilter: Boolean
		get() = !enabled

	override var contents: Renderable?
		get() = super.contents
		set(value) {
			super.contents = value
			blurFilter.contents = value
		}

	private val offsetTransform = Matrix4()

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		offsetTransform.set(transform).translate(offsetX, offsetY, 0f)
		if (!bitmapCacheIsValid)
			blurFilter.drawToPingPongBuffers()

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

fun Owned.glowFilter(color: ColorRo, init: ComponentInit<DropShadowFilter> = {}): DropShadowFilter {
	val b = DropShadowFilter(this)
	b.offsetX = 0f
	b.offsetY = 0f
	b.colorTransformation = colorTransformation {
		tint(0f, 0f, 0f, color.a)
		offset(color.r, color.g, color.b)
	}
	b.init()
	return b
}