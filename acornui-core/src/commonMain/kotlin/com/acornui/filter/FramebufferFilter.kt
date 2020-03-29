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
import com.acornui.di.Context
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.clearAndReset
import com.acornui.gl.core.resizeableFramebuffer
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.Texture
import com.acornui.math.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Draws a region to a frame buffer.
 */
class FramebufferFilter(
		owner: Context,
		hasDepth: Boolean = owner.inject(AppConfig).gl.depth,
		hasStencil: Boolean = owner.inject(AppConfig).gl.stencil
) : RenderFilterBase(owner) {

	var clearMask = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT
	var clearColor = Color.CLEAR

	private val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	val texture: Texture
		get() = framebuffer.texture

	private val sprite = Sprite(gl)

	private val regionScaled = MinMax()
	private val regionUnscaled = MinMax()
	private val _transform = Matrix4()

	override fun region(region: Rectangle) {
		regionScaled.set(region).scl(scaleX, scaleY).ceil()
		regionUnscaled.set(regionScaled).scl(1f / scaleX, 1f / scaleY)
		framebuffer.setSize(regionScaled.width, regionScaled.height, scaleX, scaleY)
	}

	override fun updateGlobalVertices(transform: Matrix4Ro, tint: ColorRo) {
		_transform.set(transform).translate(regionUnscaled.x, regionUnscaled.y)
		sprite.updateGlobalVertices(transform = _transform, tint = tint)
	}

	override fun renderLocal(inner: () -> Unit) {
		framebuffer.begin()
		gl.viewport(-regionScaled.left.toInt(), -(window.framebufferHeight - regionScaled.bottom.toInt()), window.framebufferWidth, window.framebufferHeight)
		gl.clearAndReset(clearColor, clearMask)
		inner()
		framebuffer.end() // Previous viewport set in framebuffer end.
		framebuffer.drawable(sprite)
	}

	override fun render(inner: () -> Unit) = render()

	fun render() {
		sprite.render()
	}

	/**
	 * Configures a drawable to match what was last rendered. The world vertices will not be updated.
	 */
	fun drawable(out: Sprite = Sprite(gl)): Sprite {
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
inline fun Context.framebufferFilter(init: ComponentInit<FramebufferFilter> = {}): FramebufferFilter {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = FramebufferFilter(this)
	b.init()
	return b
}

private fun MinMax.ceil(): MinMax {
	return set(floor(x), floor(y), ceil(xMax), ceil(yMax))
}