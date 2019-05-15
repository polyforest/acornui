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
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Window
import com.acornui.function.as3
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4
import com.acornui.math.MinMaxRo

/**
 * Wraps a frame buffer, keeping it the size of the screen.
 */
class FullScreenFramebuffer(override val injector: Injector, hasDepth: Boolean = false, hasStencil: Boolean = false) : Scoped, Disposable {

	private val window = inject(Window)
	private val framebuffer = ResizeableFramebuffer(injector, window.width, window.height, hasDepth, hasStencil)
	private val sprite = framebuffer.sprite()

	var blendMode: BlendMode
		get() = sprite.blendMode
		set(value) {
			sprite.blendMode = value
		}

	init {
		window.sizeChanged.add(::resize.as3)
	}

	private fun resize() {
		framebuffer.setSize(window.width.toInt(), window.height.toInt())
		framebuffer.sprite(sprite)
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

	private val glState = inject(GlState)

	/**
	 * Renders the frame buffer to the screen.
	 */
	fun render(colorTint: ColorRo = Color.WHITE) {
		glState.viewProjection = Matrix4.IDENTITY
		glState.model = Matrix4.IDENTITY
		sprite.render(MinMaxRo.POSITIVE_INFINITY, Matrix4.IDENTITY, colorTint)
	}

	override fun dispose() {
		window.sizeChanged.remove(::resize.as3)
		framebuffer.dispose()
	}
}

fun Scoped.fullScreenFramebuffer(hasDepth: Boolean = false, hasStencil: Boolean = false, init: ComponentInit<FullScreenFramebuffer> = {}): FullScreenFramebuffer {
	val f = FullScreenFramebuffer(injector, hasDepth, hasStencil)
	f.init()
	return f
}
