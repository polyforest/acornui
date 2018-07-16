/*
 * Copyright 2015 Nicholas Bilyk
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
import com.acornui.core.graphics.Texture
import com.acornui.core.graphics.Window

/**
 * @author nbilyk
 */
class Framebuffer(
		injector: Injector,
		val width: Int = 0,
		val height: Int = 0,
		val hasDepth: Boolean = false,
		val hasStencil: Boolean = false,
		val texture: Texture = BufferTexture(injector.inject(Gl20), injector.inject(GlState), width, height)
) : Disposable {

	private val gl = injector.inject(Gl20)
	private val glState = injector.inject(GlState)
	private val window = injector.inject(Window)

	private val framebufferHandle: GlFramebufferRef
	private val depthbufferHandle: GlRenderbufferRef?
	private val stencilbufferHandle: GlRenderbufferRef?

	init {
		if (width <= 0 || height <= 0) throw IllegalArgumentException("width or height cannot be less than zero.")
		texture.refInc()
		framebufferHandle = gl.createFramebuffer()
		gl.bindFramebuffer(Gl20.FRAMEBUFFER, framebufferHandle)
		gl.framebufferTexture2D(Gl20.FRAMEBUFFER, Gl20.COLOR_ATTACHMENT0, Gl20.TEXTURE_2D, texture.textureHandle!!, 0)
		if (hasDepth) {
			depthbufferHandle = gl.createRenderbuffer()
			gl.bindRenderbuffer(Gl20.RENDERBUFFER, depthbufferHandle)
			gl.renderbufferStorage(Gl20.RENDERBUFFER, Gl20.DEPTH_COMPONENT16, width, height)
			gl.framebufferRenderbuffer(Gl20.FRAMEBUFFER, Gl20.DEPTH_ATTACHMENT, Gl20.RENDERBUFFER, depthbufferHandle)
		} else {
			depthbufferHandle = null
		}
		if (hasStencil) {
			stencilbufferHandle = gl.createRenderbuffer()
			gl.bindRenderbuffer(Gl20.RENDERBUFFER, stencilbufferHandle)
			gl.renderbufferStorage(Gl20.RENDERBUFFER, Gl20.STENCIL_INDEX8, width, height)
			gl.framebufferRenderbuffer(Gl20.FRAMEBUFFER, Gl20.STENCIL_ATTACHMENT, Gl20.RENDERBUFFER, stencilbufferHandle)
		} else {
			stencilbufferHandle = null
		}

		val result = gl.checkFramebufferStatus(Gl20.FRAMEBUFFER)
		gl.bindFramebuffer(Gl20.FRAMEBUFFER, null)
		gl.bindRenderbuffer(Gl20.RENDERBUFFER, null)

		if (result != Gl20.FRAMEBUFFER_COMPLETE) {
			delete()

			when (result) {
				Gl20.FRAMEBUFFER_INCOMPLETE_ATTACHMENT ->
					throw IllegalStateException("framebuffer couldn't be constructed: incomplete attachment")
				Gl20.FRAMEBUFFER_INCOMPLETE_DIMENSIONS ->
					throw IllegalStateException("framebuffer couldn't be constructed: incomplete dimensions")
				Gl20.FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT ->
					throw IllegalStateException("framebuffer couldn't be constructed: missing attachment")
				Gl20.FRAMEBUFFER_UNSUPPORTED ->
					throw IllegalStateException("framebuffer couldn't be constructed: unsupported combination of formats")
				else ->
					throw IllegalStateException("framebuffer couldn't be constructed: unknown error " + result)
			}
		}
	}

	private var previousFrameBuffer: Framebuffer? = null

	fun begin() {
		glState.batch.flush()
		previousFrameBuffer = currentFrameBuffer
		currentFrameBuffer = this
		gl.bindFramebuffer(Gl20.FRAMEBUFFER, framebufferHandle)
		gl.viewport(0, 0, width, height)
	}

	fun end() {
		glState.batch.flush()
		val gl = gl
		if (previousFrameBuffer == null) {
			gl.bindFramebuffer(Gl20.FRAMEBUFFER, null)
			gl.viewport(0, 0, (window.width * window.scaleX).toInt(), (window.height * window.scaleY).toInt())
		} else {
			gl.bindFramebuffer(Gl20.FRAMEBUFFER, previousFrameBuffer!!.framebufferHandle)
			gl.viewport(0, 0, previousFrameBuffer!!.width, previousFrameBuffer!!.height)
		}
		currentFrameBuffer = previousFrameBuffer
		previousFrameBuffer = null
	}

	private fun delete() {
		if (depthbufferHandle != null) {
			gl.deleteRenderbuffer(depthbufferHandle)
		}
		if (stencilbufferHandle != null) {
			gl.deleteRenderbuffer(stencilbufferHandle)
		}
		gl.deleteFramebuffer(framebufferHandle)
	}

	override fun dispose() {
		delete()
		texture.refDec()
	}

	companion object {
		var currentFrameBuffer: Framebuffer? = null
	}

}

class BufferTexture(gl: Gl20,
					glState: GlState,
					override val width: Int = 0,
					override val height: Int = 0
) : GlTextureBase(gl, glState) {

	override fun uploadTexture() {
		gl.texImage2Db(target.value, 0, pixelFormat.value, width, height, 0, pixelFormat.value, pixelType.value, null)
	}
}