/*
 * Copyright 2018 PolyForest
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
import com.acornui.math.ceil

/**
 * Wraps a frame buffer, keeping it the size of the screen.
 */
class ResizeableFramebuffer(
		override val injector: Injector,
		initialWidth: Float,
		initialHeight: Float,
		private val hasDepth: Boolean = false,
		private val hasStencil: Boolean = false
) : Scoped, Disposable {

	private var framebuffer: Framebuffer? = null

	private val sprite = Sprite()

	var blendMode: BlendMode
		get() = sprite.blendMode
		set(value) {
			sprite.blendMode = value
		}

	init {
		setSize(initialWidth, initialHeight)
	}

	fun setSize(width: Int, height: Int) = setSize(width.toFloat(), height.toFloat())
	fun setSize(width: Float, height: Float) {
		val widthInt = width.ceil()
		val heightInt = height.ceil()
		val oldFramebuffer = framebuffer
		val oldW = oldFramebuffer?.width ?: 0
		val oldH = oldFramebuffer?.height ?: 0
		val newW = MathUtils.nextPowerOfTwo(widthInt)
		val newH = MathUtils.nextPowerOfTwo(heightInt)
		if (newW <= 0 || newH <= 0) {
			framebuffer?.dispose()
			framebuffer = null
			sprite.texture = null
		} else {
			if (oldW < newW || oldH < newH) {
				framebuffer?.dispose()
				framebuffer = Framebuffer(injector, maxOf(oldW, newW), maxOf(oldH, newH), hasDepth, hasStencil)
				sprite.texture = framebuffer?.texture
			}
			val framebuffer = this.framebuffer!!
			framebuffer.setViewport(0, 0, widthInt, heightInt)
			sprite.setUv(0f, 0f, width / framebuffer.width.toFloat(), height / framebuffer.height.toFloat(), isRotated = false)
		}
	}

	/**
	 * Begins drawing to the frame buffer.
	 */
	fun begin() {
		framebuffer?.begin()
	}

	/**
	 * Ends drawing to the frame buffer.
	 */
	fun end() {
		framebuffer?.end()
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
		framebuffer?.dispose()
	}
}

fun Scoped.resizeableFramebuffer(initialWidth: Float = 256f, initialHeight: Float = 256f, hasDepth: Boolean = false, hasStencil: Boolean = false, init: ComponentInit<ResizeableFramebuffer> = {}): ResizeableFramebuffer {
	val f = ResizeableFramebuffer(injector, initialWidth, initialHeight, hasDepth, hasStencil)
	f.init()
	return f
}