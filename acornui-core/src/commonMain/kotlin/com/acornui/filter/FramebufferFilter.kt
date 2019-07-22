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

import com.acornui.component.*
import com.acornui.core.*
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.di.own
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Texture
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.clearAndReset
import com.acornui.gl.core.resizeableFramebuffer
import com.acornui.gl.core.setViewport
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.signal.Signal0
import com.acornui.signal.bind
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Creates a Framebuffer and provides utility to draw a [Renderable] object to it and then draw the frame buffer to the
 * screen.
 */
class FramebufferFilter(
		owner: Owned,
		hasDepth: Boolean = owner.inject(AppConfig).gl.depth,
		hasStencil: Boolean = owner.inject(AppConfig).gl.stencil
) : RenderFilterBase(owner) {

	override var drawPadding: PadRo = Pad.EMPTY_PAD

	var clearMask = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT
	var clearColor = Color.CLEAR
	var blendMode = BlendMode.NORMAL
	var premultipliedAlpha = false

	private val defaultRenderContext by RenderContextRo

	private val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	val texture: Texture
		get() = framebuffer.texture

	private val viewport = IntRectangle()
	private val sprite = Sprite(glState)
	private val drawable = PaddedDrawable(sprite)
	private val drew = own(Signal0())

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		if (!bitmapCacheIsValid)
			drawToFramebuffer()
		drawToScreen()
	}

	fun drawToFramebuffer() {
		val contents = contents ?: return
		val drawRegion = drawRegion
		val scaleX = 1f
		val scaleY = 1f
		framebuffer.setSize(drawRegion.width, drawRegion.height)
		val bounds = bounds
		drawable.padding.set(drawRegion.xMin, drawRegion.yMin, bounds.width - drawRegion.xMax, bounds.height - drawRegion.yMax)

		viewport.set(glState.viewport)
		framebuffer.begin()
		glState.setViewport(
				floor(-drawRegion.xMin).toInt(),
				floor((drawRegion.yMin.toInt() - renderContext.canvasTransform.height + framebuffer.texture.heightPixels)).toInt(),
				ceil(renderContext.canvasTransform.width).toInt(),
				ceil(renderContext.canvasTransform.height).toInt()
		)
		if (clearMask != 0)
			clearAndReset(clearColor, clearMask)

		contents.render(defaultRenderContext)

		framebuffer.end()
		framebuffer.sprite(sprite)
		sprite.setUv(sprite.u, 1f - sprite.v, sprite.u2, 1f - sprite.v2, isRotated = false)
		sprite.setScaling(scaleX, scaleY)
		drawable.setSize(null, null)
		glState.setViewport(viewport)
		drew.dispatch()
	}

	fun drawToScreen() {
		drawable.renderContextOverride = renderContext
		drawable.render()
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
	fun drawable(out: PaddedDrawable<Sprite> = PaddedDrawable(Sprite(glState))): PaddedDrawable<Sprite> {
		out.inner.set(sprite)
		out.padding.set(drawable.padding)
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
fun Owned.framebufferFilter(init: ComponentInit<FramebufferFilter> = {}): FramebufferFilter {
	val b = FramebufferFilter(this)
	b.init()
	return b
}
