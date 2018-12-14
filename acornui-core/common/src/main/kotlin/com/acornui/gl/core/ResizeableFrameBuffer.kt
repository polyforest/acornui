/*
 * Copyright 2018 Poly Forest, LLC
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

package com.acornui.gl.core

import com.acornui.component.ComponentInit
import com.acornui.component.Sprite
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.MathUtils
import com.acornui.math.Matrix4Ro

/**
 * Wraps a frame buffer, keeping it the size of the screen.
 */
class ResizeableFrameBuffer(override val injector: Injector, initialWidth: Int, initialHeight: Int, private val hasDepth: Boolean = false, private val hasStencil: Boolean = false) : Scoped, Disposable {

	private var frameBuffer: Framebuffer? = null

	private val sprite = Sprite()

	var blendMode: BlendMode
		get() = sprite.blendMode
		set(value) {
			sprite.blendMode = value
		}

	init {
		setSize(initialWidth, initialHeight)
		sprite.setUv(0f, 0f, 1f, 1f, false)
	}

	fun setSize(width: Int, height: Int) {
		val newW = MathUtils.nextPowerOfTwo(width)
		val newH = MathUtils.nextPowerOfTwo(height)
		if (newW <= 0 || newH <= 0) {
			frameBuffer?.dispose()
			frameBuffer = null
			sprite.texture = null
		} else {
			val oldFramebuffer = frameBuffer
			if (oldFramebuffer == null || oldFramebuffer.width < newW || oldFramebuffer.height < newH) {
				frameBuffer?.dispose()
				frameBuffer = Framebuffer(injector, newW, newH, hasDepth, hasStencil)
				sprite.texture = frameBuffer?.texture
			}
			val frameBuffer = this.frameBuffer!!
			frameBuffer.setViewport(0, 0, width, height)
			sprite.setUv(0f, 0f, width.toFloat() / frameBuffer.width.toFloat(), height.toFloat() / frameBuffer.height.toFloat(), isRotated = false)
		}
	}

	/**
	 * Begins drawing to the frame buffer.
	 */
	fun begin() {
		frameBuffer?.begin()
	}

	/**
	 * Ends drawing to the frame buffer.
	 */
	fun end() {
		frameBuffer?.end()
	}

	/**
	 * Sugar to wrap the inner method in [begin] and [end] calls.
	 */
	inline fun drawTo(inner: () -> Unit) {
		begin()
		inner()
		end()
	}

	private val glState = inject(GlState)

	fun updateWorldVertices(worldTransform: Matrix4Ro, width: Float, height: Float, x: Float = 0f, y: Float = 0f, z: Float = 0f, rotation: Float = 0f, originX: Float = 0f, originY: Float = 0f) {
		sprite.updateWorldVertices(worldTransform, width, height, x, y, z, rotation, originX, originY)
	}

	/**
	 * Renders the frame buffer to the screen.
	 * Be sure to set [GlState.setCamera] before this call.
	 */
	fun render(colorTint: ColorRo = Color.WHITE) {
		sprite.draw(glState, colorTint)
	}

	override fun dispose() {
		frameBuffer?.dispose()
	}
}

fun Scoped.resizeableFramebuffer(hasDepth: Boolean = false, hasStencil: Boolean = false, init: ComponentInit<FullScreenFramebuffer> = {}): FullScreenFramebuffer {
	val f = FullScreenFramebuffer(injector, hasDepth, hasStencil)
	f.init()
	return f
}