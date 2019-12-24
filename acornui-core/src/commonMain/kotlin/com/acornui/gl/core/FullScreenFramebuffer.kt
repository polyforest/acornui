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

import com.acornui.component.ComponentInit
import com.acornui.component.Sprite
import com.acornui.Disposable
import com.acornui.di.Injector
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.graphic.BlendMode
import com.acornui.graphic.Window
import com.acornui.function.as2
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4

/**
 * Wraps a frame buffer, keeping it the size of the screen.
 */
class FullScreenFramebuffer(override val injector: Injector, hasDepth: Boolean = false, hasStencil: Boolean = false) : Scoped, Disposable {

	private val window = inject(Window)
	private val framebuffer = ResizeableFramebuffer(injector, hasDepth, hasStencil)

	private val transform = Matrix4().apply {
		setTranslation(-1f, -1f, 0f)
	}

	private val sprite = Sprite(inject(CachedGl20)).apply {
		updateGlobalVertices(2f, 2f, transform)
	}

	var blendMode: BlendMode
		get() = sprite.blendMode
		set(value) {
			sprite.blendMode = value
		}

	init {
		window.sizeChanged.add(::resize.as2)
		resize()
	}

	private fun resize() {
		framebuffer.setSize(window.framebufferWidth, window.framebufferHeight)
		sprite.apply {
			texture = framebuffer.texture
			val w = framebuffer.width / framebuffer.texture.widthPixels
			val h = framebuffer.height / framebuffer.texture.heightPixels

			setUv(0f, 0f, w, h, isRotated = false)
		}
	}

	/**
	 * Begins drawing to the frame buffer.
	 */
	fun begin() {
		framebuffer.begin()
	}

	/**
	 * Ends drawing to the frame buffer.
	 */
	fun end() {
		framebuffer.end()
	}

	/**
	 * Sugar to wrap the inner method in [begin] and [end] calls.
	 * Note that this does not call gl clear for you.
	 */
	inline fun drawTo(inner: ()->Unit) {
		begin()
		inner()
		end()
	}

	fun colorTint(tint: ColorRo) {
		sprite.updateGlobalVertices(2f, 2f, transform, tint)
	}

	/**
	 * Renders the frame buffer to the screen.
	 */
	fun render() {
		sprite.render()
	}

	override fun dispose() {
		window.sizeChanged.remove(::resize.as2)
		framebuffer.dispose()
	}
}

fun Scoped.fullScreenFramebuffer(hasDepth: Boolean = false, hasStencil: Boolean = false, init: ComponentInit<FullScreenFramebuffer> = {}): FullScreenFramebuffer {
	val f = FullScreenFramebuffer(injector, hasDepth, hasStencil)
	f.init()
	return f
}
