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
import com.acornui.core.Disposable
import com.acornui.core.DisposedException
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.graphic.Camera
import com.acornui.core.graphic.OrthographicCamera
import com.acornui.core.graphic.Texture
import com.acornui.core.graphic.flipYDown
import com.acornui.core.userInfo
import com.acornui.logging.Log
import com.acornui.math.IntRectangle

/**
 * @author nbilyk
 */
class Framebuffer(
		private val gl: Gl20,
		private val glState: GlState,
		val width: Int,
		val height: Int,
		hasDepth: Boolean = false,
		hasStencil: Boolean = false,
		val texture: Texture = BufferTexture(gl, glState, width, height)
) : Disposable {

	/**
	 * True if the depth render attachment was created.
	 */
	val hasDepth: Boolean

	/**
	 * True if the stencil render attachment was created.
	 */
	val hasStencil: Boolean

	private val _viewport = IntRectangle(0, 0, width, height)

	constructor(injector: Injector,
				width: Int,
				height: Int,
				hasDepth: Boolean = false,
				hasStencil: Boolean = false,
				texture: Texture = BufferTexture(injector.inject(Gl20), injector.inject(GlState), width, height)) : this(injector.inject(Gl20), injector.inject(GlState), width, height, hasDepth, hasStencil, texture)

	private var previousStencil: Boolean = false
	private val framebufferHandle: GlFramebufferRef
	private val depthbufferHandle: GlRenderbufferRef?
	private val stencilbufferHandle: GlRenderbufferRef?
	private val depthStencilbufferHandle: GlRenderbufferRef?

	init {
		if (width <= 0 || height <= 0) throw IllegalArgumentException("width or height cannot be less than zero.")
		if (hasDepth && hasStencil) {
			if (allowDepthAndStencil(gl)) {
				this.hasDepth = true
				this.hasStencil = true
			} else {
				Log.warn("No GL_OES_packed_depth_stencil or GL_EXT_packed_depth_stencil extension, ignoring depth")
				this.hasDepth = false
				this.hasStencil = true
			}

		} else {
			this.hasDepth = hasDepth
			this.hasStencil = hasStencil
		}

		texture.refInc()
		framebufferHandle = gl.createFramebuffer()
		gl.bindFramebuffer(Gl20.FRAMEBUFFER, framebufferHandle)
		gl.framebufferTexture2D(Gl20.FRAMEBUFFER, Gl20.COLOR_ATTACHMENT0, Gl20.TEXTURE_2D, texture.textureHandle!!, 0)
		if (this.hasDepth && this.hasStencil) {
			depthStencilbufferHandle = gl.createRenderbuffer()
			depthbufferHandle = null
			stencilbufferHandle = null
			gl.bindRenderbuffer(Gl20.RENDERBUFFER, depthStencilbufferHandle)
			gl.renderbufferStorage(Gl20.RENDERBUFFER, Gl20.DEPTH_STENCIL, width, height)
			gl.framebufferRenderbuffer(Gl20.FRAMEBUFFER, Gl20.DEPTH_STENCIL_ATTACHMENT, Gl20.RENDERBUFFER, depthStencilbufferHandle)
		} else {
			depthStencilbufferHandle = null
			if (this.hasDepth) {
				depthbufferHandle = gl.createRenderbuffer()
				gl.bindRenderbuffer(Gl20.RENDERBUFFER, depthbufferHandle)
				gl.renderbufferStorage(Gl20.RENDERBUFFER, Gl20.DEPTH_COMPONENT16, width, height)
				gl.framebufferRenderbuffer(Gl20.FRAMEBUFFER, Gl20.DEPTH_ATTACHMENT, Gl20.RENDERBUFFER, depthbufferHandle)
			} else {
				depthbufferHandle = null
			}
			if (this.hasStencil) {
				stencilbufferHandle = gl.createRenderbuffer()
				gl.bindRenderbuffer(Gl20.RENDERBUFFER, stencilbufferHandle)
				gl.renderbufferStorage(Gl20.RENDERBUFFER, Gl20.STENCIL_INDEX8, width, height)
				gl.framebufferRenderbuffer(Gl20.FRAMEBUFFER, Gl20.STENCIL_ATTACHMENT, Gl20.RENDERBUFFER, stencilbufferHandle)
			} else {
				stencilbufferHandle = null
			}
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
					throw IllegalStateException("framebuffer couldn't be constructed: unknown error $result")
			}
		}
	}

	/**
	 * When this frame buffer is bound, this will be the viewport.
	 */
	fun setViewport(x: Int, y: Int, width: Int, height: Int) {
		_viewport.set(x, y, width, height)
	}

	private var previousFramebuffer = FramebufferInfo()

	private val previousViewport = IntRectangle()

	fun begin() {
		glState.batch.flush()
		glState.getFramebuffer(previousFramebuffer)
		glState.setFramebuffer(framebufferHandle, width, height, 1f, 1f)
		previousViewport.set(glState.viewport)
		glState.setViewport(_viewport)
		previousStencil = gl.getParameterb(Gl20.STENCIL_TEST)
		if (previousStencil)
			gl.disable(Gl20.STENCIL_TEST)
	}

	fun end() {
		glState.batch.flush()
		glState.setFramebuffer(previousFramebuffer)
		glState.setViewport(previousViewport)
		previousFramebuffer.clear()
		if (previousStencil)
			gl.enable(Gl20.STENCIL_TEST)
	}

	/**
	 * Sugar to wrap the inner method in [begin] and [end] calls.
	 */
	inline fun drawTo(inner: () -> Unit) {
		begin()
		inner()
		end()
	}

	private fun delete() {
		if (depthbufferHandle != null) {
			gl.deleteRenderbuffer(depthbufferHandle)
		}
		if (stencilbufferHandle != null) {
			gl.deleteRenderbuffer(stencilbufferHandle)
		}
		if (depthStencilbufferHandle != null) {
			gl.deleteRenderbuffer(depthStencilbufferHandle)
		}
		gl.deleteFramebuffer(framebufferHandle)
	}


	/**
	 * Configures a Camera to match the viewport used in this framebuffer.
	 * This will set the viewport and positioning to 'see' the framebuffer.
	 *
	 * @param camera The camera to configure. (A newly constructed Sprite is the default)
	 */
	fun camera(camera: Camera = OrthographicCamera().apply { flipYDown() }): Camera {
		return camera.apply {
			setViewport(_viewport.width.toFloat(), _viewport.height.toFloat())
			moveToLookAtRect(_viewport.x.toFloat(), _viewport.y.toFloat(), viewportWidth, viewportHeight)
		}
	}

	/**
	 * Configures a Sprite for rendering this framebuffer.
	 *
	 * @param sprite The sprite to configure. (A newly constructed Sprite is the default)
	 */
	fun sprite(sprite: Sprite = Sprite(glState)): Sprite {
		return sprite.apply {
			setUv(0f, 0f, 1f, 1f, false)
			texture = this@Framebuffer.texture
			updateVertices()
		}
	}

	private var isDisposed = false

	override fun dispose() {
		if (isDisposed)
			throw DisposedException()
		isDisposed = true
		texture.refDec()
		delete()
	}

	companion object {

		/**
		 * Returns true if Framebuffers support the packed depth and stencil attachment.
		 */
		fun allowDepthAndStencil(gl: Gl20): Boolean {
			if (userInfo.isBrowser) return true
			val extensions = gl.getSupportedExtensions()
			return extensions.contains("GL_OES_packed_depth_stencil") || extensions.contains("GL_EXT_packed_depth_stencil")
		}

	}

}

class BufferTexture(gl: Gl20,
					glState: GlState,
					override val widthPixels: Int = 0,
					override val heightPixels: Int = 0
) : GlTextureBase(gl, glState) {

	override fun uploadTexture() {
		gl.texImage2Db(target.value, 0, pixelFormat.value, widthPixels, heightPixels, 0, pixelFormat.value, pixelType.value, null)
	}
}

fun Scoped.framebuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = false, init: ComponentInit<Framebuffer> = {}): Framebuffer {
	val f = Framebuffer(injector, width, height, hasDepth, hasStencil)
	f.init()
	return f
}
