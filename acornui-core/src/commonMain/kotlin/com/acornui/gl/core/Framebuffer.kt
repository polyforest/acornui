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
import com.acornui.DisposedException
import com.acornui.component.ComponentInit
import com.acornui.component.Sprite
import com.acornui.di.Context
import com.acornui.graphic.Camera
import com.acornui.graphic.OrthographicCamera
import com.acornui.graphic.Texture
import com.acornui.graphic.yDown
import com.acornui.logging.Log
import com.acornui.math.IntRectangle
import com.acornui.math.IntRectangleRo
import com.acornui.system.userInfo

/**
 * @author nbilyk
 */
class Framebuffer constructor(
		private val gl: CachedGl20,

		/**
		 * The width of the frame buffer, in pixels.
		 */
		val widthPixels: Int,

		/**
		 * The height of the frame buffer, in pixels.
		 */
		val heightPixels: Int,

		hasDepth: Boolean = false,

		hasStencil: Boolean = false,

		val texture: Texture = BufferTexture(gl, widthPixels, heightPixels)
) : Disposable {
	
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

	/**
	 * True if the depth render attachment was created.
	 */
	val hasDepth: Boolean

	/**
	 * True if the stencil render attachment was created.
	 */
	val hasStencil: Boolean

	private val _viewport = IntRectangle(0, 0, widthPixels, heightPixels)

	val viewport: IntRectangleRo = _viewport

	private var previousStencil: Boolean = false
	private val framebufferHandle: GlFramebufferRef
	private val depthbufferHandle: GlRenderbufferRef?
	private val stencilbufferHandle: GlRenderbufferRef?
	private val depthStencilbufferHandle: GlRenderbufferRef?

	/**
	 * The pixel size is divided by the scale factor to get points.
	 */
	var scaleX: Float = 1f

	/**
	 * The pixel size is divided by the scale factor to get points.
	 */
	var scaleY: Float = 1f

	fun setScaling(scaleX: Float, scaleY: Float) {
		this.scaleX = scaleX
		this.scaleY = scaleY
	}

	init {
		require(widthPixels > 0 && heightPixels > 0) { "width or height cannot be less than zero." }
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
		gl.bindFramebuffer(framebufferHandle)
		gl.framebufferTexture2D(Gl20.FRAMEBUFFER, Gl20.COLOR_ATTACHMENT0, Gl20.TEXTURE_2D, texture.textureHandle!!, 0)
		if (this.hasDepth && this.hasStencil) {
			depthStencilbufferHandle = gl.createRenderbuffer()
			depthbufferHandle = null
			stencilbufferHandle = null
			gl.bindRenderbuffer(depthStencilbufferHandle)
			gl.renderbufferStorage(Gl20.RENDERBUFFER, Gl20.DEPTH_STENCIL, widthPixels, heightPixels)
			gl.framebufferRenderbuffer(Gl20.FRAMEBUFFER, Gl20.DEPTH_STENCIL_ATTACHMENT, Gl20.RENDERBUFFER, depthStencilbufferHandle)
		} else {
			depthStencilbufferHandle = null
			if (this.hasDepth) {
				depthbufferHandle = gl.createRenderbuffer()
				gl.bindRenderbuffer(depthbufferHandle)
				gl.renderbufferStorage(Gl20.RENDERBUFFER, Gl20.DEPTH_COMPONENT16, widthPixels, heightPixels)
				gl.framebufferRenderbuffer(Gl20.FRAMEBUFFER, Gl20.DEPTH_ATTACHMENT, Gl20.RENDERBUFFER, depthbufferHandle)
			} else {
				depthbufferHandle = null
			}
			if (this.hasStencil) {
				stencilbufferHandle = gl.createRenderbuffer()
				gl.bindRenderbuffer(stencilbufferHandle)
				gl.renderbufferStorage(Gl20.RENDERBUFFER, Gl20.STENCIL_INDEX8, widthPixels, heightPixels)
				gl.framebufferRenderbuffer(Gl20.FRAMEBUFFER, Gl20.STENCIL_ATTACHMENT, Gl20.RENDERBUFFER, stencilbufferHandle)
			} else {
				stencilbufferHandle = null
			}
		}

		val result = gl.checkFramebufferStatus(Gl20.FRAMEBUFFER)
		gl.bindFramebuffer(null)
		gl.bindRenderbuffer(null)

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

	private val previousViewport = IntArray(4)
	private var previousFramebuffer: GlFramebufferRef? = null

	fun begin() {
		check(gl.batch.isDynamic) { "Framebuffer not expecting a static batch."}
		gl.batch.flush()
		previousFramebuffer = gl.framebuffer

		gl.getParameteriv(Gl20.VIEWPORT, previousViewport)
		gl.bindFramebuffer(framebufferHandle)
		gl.viewport(_viewport)
		previousStencil = gl.getParameterb(Gl20.STENCIL_TEST)
		if (previousStencil)
			gl.disable(Gl20.STENCIL_TEST)
	}

	fun end() {
		gl.batch.flush()
		gl.bindFramebuffer(previousFramebuffer)
		gl.viewport(previousViewport)
		previousFramebuffer = null
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
	 * Configures a Sprite for rendering this framebuffer.
	 *
	 * @param sprite The sprite to configure. (A newly constructed Sprite is the default)
	 */
	fun drawable(sprite: Sprite = Sprite(gl)): Sprite {
		sprite.setUv(0f, 1f, 1f, 0f, false)
		sprite.texture = texture
		sprite.setScaling(scaleX, scaleY)
		return sprite
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

/**
 * Configures a Camera to match the viewport used in this framebuffer.
 * This will set the viewport and positioning to 'see' the framebuffer.
 *
 * @param camera The camera to configure. (A newly constructed Sprite is the default)
 */
fun Framebuffer.camera(camera: Camera = OrthographicCamera().apply { yDown(false) }): Camera {
	val viewport = viewport
	return camera.apply {
		setViewport(viewport.width.toFloat() / scaleX, viewport.height.toFloat() / scaleY)
		moveToLookAtRect(viewport.x.toFloat() / scaleX, viewport.y.toFloat() / scaleY, viewportWidth, viewportHeight)
	}
}

class BufferTexture(gl: Gl20,
					override val widthPixels: Int = 0,
					override val heightPixels: Int = 0
) : GlTextureBase(gl) {

	override fun uploadTexture() {
		gl.texImage2Db(target.value, 0, pixelFormat.value, widthPixels, heightPixels, 0, pixelFormat.value, pixelType.value, null)
	}
}

fun Context.framebuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = false, init: ComponentInit<Framebuffer> = {}): Framebuffer {
	val gl = inject(CachedGl20)
	val f = Framebuffer(gl, width, height, hasDepth, hasStencil)
	f.init()
	return f
}
