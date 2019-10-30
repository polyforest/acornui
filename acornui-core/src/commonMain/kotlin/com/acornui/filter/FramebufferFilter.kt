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

package com.acornui.filter

import com.acornui.AppConfig
import com.acornui.Renderable
import com.acornui.component.*
import com.acornui.di.Owned
import com.acornui.di.inject
import com.acornui.di.own
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.clearAndReset
import com.acornui.gl.core.resizeableFramebuffer
import com.acornui.graphic.*
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.signal.Signal0
import com.acornui.signal.bind
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Creates a Framebuffer and provides utility to draw a [Renderable] object to it and then draw the frame buffer to the
 * screen.
 */
class FramebufferFilter(
		owner: Owned,
		hasDepth: Boolean = owner.inject(AppConfig).gl.depth,
		hasStencil: Boolean = owner.inject(AppConfig).gl.stencil
) : RenderFilterBase(owner) {

	private val window by Window

	private var _drawPadding: PadRo = Pad.EMPTY_PAD

	/**
	 * Extra padding when the contents are rendered to the frame buffer.
	 * The values of this padding are ceiled in order to prevent rendering on a fraction of a pixel.
	 */
	override var drawPadding: PadRo
		get() = _drawPadding
		set(value) {
			_drawPadding = value.copy().ceil()
		}

	var clearMask = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT
	var clearColor = Color.CLEAR
	var blendMode = BlendMode.NORMAL
	var premultipliedAlpha = false

	private val camera: Camera = orthographicCamera {
		yDown(false)
	}

	private val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	private val framebufferRenderContext = CustomRenderContext(camera)

	val texture: Texture
		get() = framebuffer.texture

	private val sprite = Sprite(glState)
	private val renderable = PaddedRenderable(sprite)
	private val drew = own(Signal0())

	override fun draw(renderContext: RenderContextRo) {
		if (!bitmapCacheIsValid)
			drawToFramebuffer()
		drawToScreen(renderContext)
	}

	fun drawToFramebuffer() {
		val contents = contents ?: return
		val drawRegion = drawRegion

		val renderContext = framebufferRenderContext
		val w = drawRegion.width
		val h = drawRegion.height
		camera.setViewport(w, h)
		camera.moveToLookAtRect(drawRegion)
		renderContext.clipRegion.set(drawRegion)
		renderContext.canvasTransform.set(drawRegion)

		framebuffer.setSize(w * window.scaleX, h * window.scaleY, window.scaleX, window.scaleY)

		framebuffer.begin()
		clearAndReset(clearColor, clearMask)
		glState.uniforms.useCamera(renderContext) {
			contents.render(renderContext)
		}
		framebuffer.end()
		framebuffer.drawable(sprite)
		setDrawPadding(renderable.padding)
		renderable.setSize(null, null)
		drew.dispatch()
	}

	fun drawToScreen(renderContext: RenderContextRo) {
		renderable.render(renderContext)
	}

	/**
	 * Creates a component that renders the sprite that represents the last time [drawToFramebuffer] was called.
	 */
	fun createSnapshot(owner: Owned): UiComponent {
		val drawable = drawable()
		return owner.drawableC(drawable) {
			own(drew.bind {
				drawable(drawable)
				invalidate(ValidationFlags.LAYOUT)
			})
		}
	}

	/**
	 * Configures a drawable to match what was last rendered.
	 */
	fun drawable(out: PaddedRenderable<Sprite> = PaddedRenderable(Sprite(glState))): PaddedRenderable<Sprite> {
		out.inner.set(sprite)
		out.padding.set(renderable.padding)
		return out
	}

	override fun dispose() {
		super.dispose()
		framebuffer.dispose()
	}
}

/**
 * A frame buffer filter will cache the render target as a bitmap.
 */
inline fun Owned.framebufferFilter(init: ComponentInit<FramebufferFilter> = {}): FramebufferFilter  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val b = FramebufferFilter(this)
	b.init()
	return b
}
