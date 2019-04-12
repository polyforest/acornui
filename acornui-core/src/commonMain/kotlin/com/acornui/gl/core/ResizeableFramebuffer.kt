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

import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.ComponentInit
import com.acornui.component.Sprite
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.graphic.Camera
import com.acornui.core.graphic.OrthographicCamera
import com.acornui.core.graphic.Texture
import kotlin.math.ceil

/**
 * A ResizableFramebuffer has a backing frame buffer that is discarded and replaced when [setSize] requests a size
 * that is too large for the current backing frame buffer.
 */
class ResizeableFramebuffer(
		private val gl: Gl20,
		private val glState: GlState,
		initialWidth: Float,
		initialHeight: Float,
		private val hasDepth: Boolean = false,
		private val hasStencil: Boolean = false
) : Disposable {

	constructor(injector: Injector, initialWidth: Float, initialHeight: Float, hasDepth: Boolean, hasStencil: Boolean) : this(
			injector.inject(Gl20),
			injector.inject(GlState),
			initialWidth,
			initialHeight,
			hasDepth,
			hasStencil
	)

	private var framebuffer: Framebuffer? = null
	val texture: Texture
		get() = if (framebuffer == null) throw Exception("Call setSize first.") else framebuffer!!.texture

	private val allowedSizes = listOf(16, 32, 64, 128, 256, 512, 768, 1024, 1536, 2048)

	var width: Float = 0f
		private set

	var height: Float = 0f
		private set

	init {
		setSize(initialWidth, initialHeight)
	}

	private fun nextSize(size: Int): Int {
		val index = allowedSizes.sortedInsertionIndex(size, matchForwards = false)
		return allowedSizes.getOrNull(index) ?: allowedSizes.last()
	}

	fun setSize(width: Int, height: Int) = setSize(width.toFloat(), height.toFloat())
	fun setSize(width: Float, height: Float) {
		this.width = maxOf(0f, width)
		this.height = maxOf(0f, height)
		val widthInt = ceil(width).toInt()
		val heightInt = ceil(height).toInt()
		val oldFramebuffer = framebuffer
		val oldW = oldFramebuffer?.width ?: 0
		val oldH = oldFramebuffer?.height ?: 0
		val newW = nextSize(widthInt)
		val newH = nextSize(heightInt)
		if (newW <= 0 || newH <= 0) {
			framebuffer?.dispose()
			framebuffer = null
		} else {
			if (oldW < newW || oldH < newH) {
				framebuffer?.dispose()
				framebuffer = Framebuffer(gl, glState, maxOf(oldW, newW), maxOf(oldH, newH), hasDepth, hasStencil)
			}
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

	/**
	 * Configures a Camera for rendering this frame buffer.
	 * This will set the viewport and positioning to 'see' the frame buffer.
	 *
	 * @param camera The camera to configure. (A newly constructed Sprite is the default)
	 */
	fun camera(camera: Camera = OrthographicCamera()): Camera {
		framebuffer?.camera(camera)
		return camera
	}

	/**
	 * Configures a Sprite for rendering this frame buffer.
	 *
	 * @param sprite The sprite to configure. (A newly constructed Sprite is the default)
	 */
	fun sprite(sprite: Sprite = Sprite()): Sprite {
		return sprite.apply {
			texture = framebuffer?.texture
			val textureW = framebuffer?.width?.toFloat() ?: 0f
			val textureH = framebuffer?.height?.toFloat() ?: 0f
			setUv(0f, 0f, width / textureW, height / textureH, isRotated = false)
		}
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