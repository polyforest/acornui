/*
 * Copyright 2018 Nicholas Bilyk
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
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Window
import com.acornui.function.as3
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.Matrix4

/**
 * Wraps a frame buffer, keeping it the size of the screen.
 */
class FullScreenFramebuffer(override val injector: Injector, hasDepth: Boolean = false, hasStencil: Boolean = false) : Scoped, Disposable {

	private val window = inject(Window)
	private val frameBuffer = ResizeableFrameBuffer(injector, window.width.toInt(), window.height.toInt(), hasDepth, hasStencil)

	var blendMode: BlendMode
		get() = frameBuffer.blendMode
		set(value) {
			frameBuffer.blendMode = value
		}

	init {
		window.sizeChanged.add(this::resize.as3)
		frameBuffer.updateWorldVertices(Matrix4.IDENTITY, 2f, 2f, -1f, -1f)
	}

	private fun resize() {
		frameBuffer.setSize(window.width.toInt(), window.height.toInt())
	}

	/**
	 * Begins drawing to the frame buffer.
	 */
	fun begin() {
		frameBuffer.begin()
	}

	/**
	 * Ends drawing to the frame buffer.
	 */
	fun end() {
		frameBuffer.end()
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
		frameBuffer.render(colorTint)
	}

	override fun dispose() {
		window.sizeChanged.remove(this::resize.as3)
		frameBuffer.dispose()
	}
}

fun Scoped.fullScreenFramebuffer(hasDepth: Boolean = false, hasStencil: Boolean = false, init: ComponentInit<FullScreenFramebuffer> = {}): FullScreenFramebuffer {
	val f = FullScreenFramebuffer(injector, hasDepth, hasStencil)
	f.init()
	return f
}