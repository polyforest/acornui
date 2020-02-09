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

package com.acornui.gl.core

import com.acornui.Disposable
import com.acornui.ceilInt
import com.acornui.component.ComponentInit
import com.acornui.component.Sprite
import com.acornui.di.Context
import com.acornui.graphic.Texture
import com.acornui.math.MathUtils.nextPowerOfTwo

/**
 * A ResizableFramebuffer has a backing frame buffer that is discarded and replaced when [setSize] requests a size
 * that is too large for the current backing frame buffer.
 *
 * Note: There will be no backing texture until [setSize] has been called.
 */
class ResizeableFramebuffer(
		private val gl: CachedGl20,
		private val hasDepth: Boolean = false,
		private val hasStencil: Boolean = false
) : Disposable {

	private var framebuffer: Framebuffer? = null
	val texture: Texture
		get() = framebuffer?.texture ?: error("Call setSize first.")

	var widthPixels: Float = 0f
		private set

	var heightPixels: Float = 0f
		private set

	var scaleX: Float = 1f
		private set

	var scaleY: Float = 1f
		private set

	/**
	 * The width of this frame buffer, in points.
	 */
	val width: Float
		get() = widthPixels / scaleX

	/**
	 * The height of this frame buffer, in points.
	 */
	val height: Float
		get() = heightPixels / scaleY

	private fun nextSize(size: Int): Int {
		return nextPowerOfTwo(size)
	}

	fun setSize(widthPixels: Int, heightPixels: Int, scaleX: Float = 1f, scaleY: Float = 1f) = setSize(widthPixels.toFloat(), heightPixels.toFloat(), scaleX, scaleY)

	/**
	 * Sets the viewport on the wrapped frame buffer. If the new viewport's size is greater than the current frame
	 * buffer size, a new frame buffer will be allocated with dimensions being the next power of two.
	 */
	fun setSize(widthPixels: Float, heightPixels: Float, scaleX: Float, scaleY: Float) {
		this.widthPixels = maxOf(0f, widthPixels)
		this.heightPixels = maxOf(0f, heightPixels)
		this.scaleX = scaleX
		this.scaleY = scaleY
		val widthInt = ceilInt(widthPixels)
		val heightInt = ceilInt(heightPixels)
		val oldFramebuffer = framebuffer
		val oldW = oldFramebuffer?.widthPixels ?: 0
		val oldH = oldFramebuffer?.heightPixels ?: 0
		val newW = nextSize(widthInt)
		val newH = nextSize(heightInt)

		if (newW > oldW || newH > oldH) {
			framebuffer?.dispose()
			framebuffer = Framebuffer(gl, maxOf(oldW, newW, 1), maxOf(oldH, newH, 1), hasDepth, hasStencil)
			framebuffer!!.setScaling(scaleX, scaleY)
		}
		framebuffer!!.setViewport(0, 0, widthInt, heightInt)
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
	 * Configures a Sprite for rendering this frame buffer.
	 *
	 * @param sprite The sprite to configure. (A newly constructed Sprite is the default)
	 */
	fun drawable(sprite: Sprite = Sprite(gl)): Sprite {
		sprite.texture = framebuffer?.texture
		val textureW = framebuffer?.widthPixels?.toFloat() ?: 0f
		val textureH = framebuffer?.heightPixels?.toFloat() ?: 0f
		sprite.setUv(0f, heightPixels / textureH, widthPixels / textureW, 0f, isRotated = false)
		sprite.setScaling(scaleX, scaleY)
		return sprite
	}

	override fun dispose() {
		framebuffer?.dispose()
	}
}

fun Context.resizeableFramebuffer(hasDepth: Boolean = false, hasStencil: Boolean = false, init: ComponentInit<ResizeableFramebuffer> = {}): ResizeableFramebuffer {
	val f = ResizeableFramebuffer(inject(CachedGl20), hasDepth, hasStencil)
	f.init()
	return f
}
