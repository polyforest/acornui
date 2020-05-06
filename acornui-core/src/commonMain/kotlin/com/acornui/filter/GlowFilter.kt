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
import com.acornui.component.Sprite
import com.acornui.di.Context
import com.acornui.gl.core.mulColorTransformation
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class GlowFilter(owner: Context) : RenderFilterBase(owner) {

	var colorTransformation: ColorTransformationRo by bindable(defaultColorTransformation)

	private val blurFilter = BlurFilter(this)

	var blurX by bindableAndCall(5f) {
		blurFilter.blurX = it
	}

	var blurY by bindableAndCall(5f) {
		blurFilter.blurY = it
	}

	var quality by bindableAndCall(BlurQuality.NORMAL) {
		blurFilter.quality = it
	}

	/**
	 * The offset to translate the glow.
	 */
	var offset: Vector3Ro by bindable(Vector3.ZERO)

	private val blurSprite = Sprite(gl)

	override fun region(region: Rectangle) {
		blurFilter.region(region)
		region.inflate(
				top = maxOf(0f, -offset.y),
				right = maxOf(0f, offset.x),
				bottom = maxOf(0f, offset.y),
				left = maxOf(0f, -offset.x))
	}

	override fun updateGlobalVertices(transform: Matrix4Ro, tint: ColorRo) {
		blurFilter.updateGlobalVertices(transform, Color.WHITE)
		blurFilter.drawable(blurSprite)
		blurSprite.updateGlobalVertices(transform = transform, tint = tint)
	}

	override fun renderLocal(inner: () -> Unit) {
		blurFilter.renderLocal(inner)
	}

	override fun render(inner: () -> Unit) {
		gl.uniforms.mulColorTransformation(colorTransformation) {
			blurSprite.render()
		}
		blurFilter.drawOriginalToScreen()
	}

	companion object {
		private val defaultColorTransformation = colorTransformation { tint(Color(0f, 0f, 0f, 0.5f)) }
	}
}

inline fun Context.dropShadowFilter(init: ComponentInit<GlowFilter> = {}): GlowFilter {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = GlowFilter(this)
	b.offset = vec3(25f, 5f, 0f)
	b.init()
	return b
}

inline fun Context.glowFilter(color: ColorRo = Color.WHITE, init: ComponentInit<GlowFilter> = {}): GlowFilter {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = GlowFilter(this)
	b.colorTransformation = colorTransformation {
		tint(0f, 0f, 0f, color.a)
		offset(color.r, color.g, color.b)
	}
	b.init()
	return b
}