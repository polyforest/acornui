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

import com.acornui.AppConfig
import com.acornui.component.ComponentInit
import com.acornui.component.Sprite
import com.acornui.di.Owned
import com.acornui.di.inject
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.graphic.Texture
import com.acornui.math.IntRectangleRo
import com.acornui.math.Matrix4
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Draws a region to a frame buffer.
 */
class FramebufferFilter(
		owner: Owned,
		hasDepth: Boolean = owner.inject(AppConfig).gl.depth,
		hasStencil: Boolean = owner.inject(AppConfig).gl.stencil
) : RenderFilterBase(owner) {

	var clearMask = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT
	var clearColor = Color.CLEAR

	private val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	val texture: Texture
		get() = framebuffer.texture

	private val transform = Matrix4()
	private val sprite = Sprite(glState)

	override fun render(region: IntRectangleRo, inner: () -> Unit) {
		drawToFramebuffer(region, inner)
		drawToScreen()
	}

	private val framebufferInfo = FramebufferInfo()

	fun drawToFramebuffer(region: IntRectangleRo, inner: () -> Unit) {
		val fB = framebufferInfo.set(glState.framebuffer)
		val vY = fB.height - region.bottom
		framebuffer.setSize(region.width, region.height, fB.scaleX, fB.scaleY)
		framebuffer.begin()
		gl.clearAndReset(clearColor, clearMask)
		glState.setViewport(-region.x, -vY, fB.width, fB.height)
		inner()

		framebuffer.end()
		framebuffer.drawable(sprite)
		transform.setTranslation(region.x.toFloat() / fB.scaleX, vY.toFloat() / fB.scaleY)
		sprite.updateWorldVertices(transform = transform)
	}

	fun drawToScreen() {
		sprite.render()
	}

	/**
	 * Configures a drawable to match what was last rendered.
	 */
	fun drawable(out: Sprite = Sprite(glState)): Sprite {
		return out.set(sprite)
	}

	override fun dispose() {
		super.dispose()
		framebuffer.dispose()
	}
}

/**
 * A frame buffer filter will cache the render target as a bitmap.
 */
inline fun Owned.framebufferFilter(init: ComponentInit<FramebufferFilter> = {}): FramebufferFilter {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = FramebufferFilter(this)
	b.init()
	return b
}
