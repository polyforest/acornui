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

import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphics.BlendMode
import com.acornui.core.graphics.Window
import com.acornui.function.as3
import com.acornui.graphics.Color
import com.acornui.math.Matrix4
import com.acornui.math.Vector3

/**
 * Wraps a frame buffer, keeping it the size of the screen.
 */
class FullScreenFramebuffer(override val injector: Injector, private val hasDepth: Boolean = false, private val hasStencil: Boolean = false) : Scoped, Disposable {

	private val window = inject(Window)
	private var frameBuffer: Framebuffer? = null

	private val tL = Vertex(Vector3(-1f, -1f, 0f), Vector3.NEG_Z.copy(), Color.WHITE.copy(), 0f, 0f)
	private val tR = Vertex(Vector3(1f, -1f, 0f), Vector3.NEG_Z.copy(), Color.WHITE.copy(), 1f, 0f)
	private val bR = Vertex(Vector3(1f, 1f, 0f), Vector3.NEG_Z.copy(), Color.WHITE.copy(), 1f, 1f)
	private val bL = Vertex(Vector3(-1f, 1f, 0f), Vector3.NEG_Z.copy(), Color.WHITE.copy(), 0f, 1f)

	init {
		window.sizeChanged.add(this::resize.as3)
		resize()
	}

	private fun resize() {
		val w = window.width.toInt()
		val h = window.height.toInt()

		frameBuffer?.dispose()
		frameBuffer = if (w <= 0 || h <= 0) null else Framebuffer(injector, w, h, hasDepth, hasStencil)
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

	private val glState = inject(GlState)

	/**
	 * Renders the frame buffer to the screen.
	 */
	fun render() {
		val frameBuffer = frameBuffer ?: return
		glState.setTexture(frameBuffer.texture)
		glState.viewProjection(Matrix4.IDENTITY.values)
		glState.model(Matrix4.IDENTITY.values)
		glState.blendMode(BlendMode.NORMAL, false)
		val batch = glState.batch
		batch.putVertex(tL)
		batch.putVertex(tR)
		batch.putVertex(bR)
		batch.putVertex(bL)
		batch.putQuadIndices()
	}

	override fun dispose() {
		window.sizeChanged.remove(this::resize.as3)
		frameBuffer?.dispose()
	}
}