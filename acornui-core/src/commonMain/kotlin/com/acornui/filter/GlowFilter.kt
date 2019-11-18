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
import com.acornui.di.Owned
import com.acornui.gl.core.useColorTransformation
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
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
	 * The offset to translate the glow.
	 */
	var offset: Vector3Ro by bindable(Vector3.ZERO)

	private val blurSprite = Sprite(glState)

	private val offsetWorld = Vector3()
	private val transform = Matrix4()
	var hasUpdatedWorld = false

	override fun updateWorldVertices(region: RectangleRo, transform: Matrix4Ro, tint: ColorRo): RectangleRo {
		hasUpdatedWorld = true
		transform.rot(offsetWorld.set(offset))
		val blurredRegion = blurFilter.updateWorldVertices(region, transform, tint)
		blurFilter.drawable(blurSprite)
		this.transform.set(blurFilter.transform).translate(offsetWorld)
		blurSprite.updateWorldVertices(transform = this.transform, tint = tint)
		return blurredRegion.copy().inflate(
				top = maxOf(0f, -offsetWorld.y),
				right = maxOf(0f, offsetWorld.x),
				bottom = maxOf(0f, offsetWorld.y),
				left = maxOf(0f, -offsetWorld.x))
	}

	override fun render(inner: () -> Unit) {
		if (!hasUpdatedWorld)
			throw Exception("...")
		drawToFramebuffer(inner)
		glState.useColorTransformation(colorTransformation) {
			blurSprite.render()
		}
		blurFilter.drawOriginalToScreen()
	}

	private fun drawToFramebuffer(inner: () -> Unit) {
		blurFilter.drawToFramebuffer(inner)
	}

	companion object {
		private val defaultColorTransformation = colorTransformation { tint(Color(0f, 0f, 0f, 0.5f)) }
	}
}

inline fun Owned.dropShadowFilter(init: ComponentInit<GlowFilter> = {}): GlowFilter {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = GlowFilter(this)
	b.offset = Vector3(3f, 3f, 0f)
	b.init()
	return b
}

inline fun Owned.glowFilter(color: ColorRo = Color.WHITE, init: ComponentInit<GlowFilter> = {}): GlowFilter {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = GlowFilter(this)
	b.colorTransformation = colorTransformation {
		tint(0f, 0f, 0f, color.a)
		offset(color.r, color.g, color.b)
	}
	b.init()
	return b
}