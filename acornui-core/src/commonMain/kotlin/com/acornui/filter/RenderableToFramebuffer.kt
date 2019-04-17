package com.acornui.filter

import com.acornui.component.Sprite
import com.acornui.core.AppConfig
import com.acornui.core.Renderable
import com.acornui.core.di.Owned
import com.acornui.core.di.OwnedImpl
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Texture
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Creates a Framebuffer and provides utility to draw a [Renderable] object to it and then draw the frame buffer to the
 * screen.
 */
class RenderableToFramebuffer(
		owner: Owned,
		hasDepth: Boolean = owner.inject(AppConfig).gl.depth,
		hasStencil: Boolean = owner.inject(AppConfig).gl.stencil
) : OwnedImpl(owner) {

	var clearMask = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT
	var clearColor = Color.CLEAR
	var blendMode = BlendMode.NORMAL
	var premultipliedAlpha = false

	private val glState = inject(GlState)

	private val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	val texture: Texture
		get() = framebuffer.texture

	private val _drawRegion = MinMax()
	val drawRegion: MinMaxRo = _drawRegion

	private val viewport = IntRectangle()
	private val sprite = Sprite(glState)
	private val mvp = Matrix4()

	fun drawToFramebuffer(clip: MinMaxRo, contents: Renderable, padding: PadRo = Pad.EMPTY_PAD) {
		val region = _drawRegion
		contents.drawRegion(region).inflate(padding)
		region.xMin = floor(region.xMin)
		region.yMin = floor(region.yMin)
		region.xMax = ceil(region.xMax)
		region.yMax = ceil(region.yMax)

		framebuffer.setSize(region.width, region.height)

		glState.getViewport(viewport)
		framebuffer.begin()
		glState.setViewport(-region.xMin.toInt(), region.yMin.toInt() - viewport.height + framebuffer.texture.height, viewport.width, viewport.height)
		if (clearMask != 0)
			clearAndReset(clearColor, clearMask)

		contents.render(clip, Matrix4.IDENTITY, Color.WHITE)

		framebuffer.end()
		framebuffer.sprite(sprite)
		sprite.setUv(sprite.u, 1f - sprite.v, sprite.u2, 1f - sprite.v2, isRotated = false)
		glState.setViewport(viewport)
	}

	fun drawToScreen(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		glState.getViewport(viewport)
		mvp.idt().scl(2f / viewport.width, -2f / viewport.height, 1f).trn(-1f, 1f, 0f) // Projection transform
		val region = drawRegion
		mvp.mul(transform).translate(region.xMin, region.yMin, 0f) // Model transform

		glState.viewProjection = mvp
		glState.model = Matrix4.IDENTITY
		sprite.render(clip, Matrix4.IDENTITY, tint)
	}

	override fun dispose() {
		super.dispose()
		framebuffer.dispose()
	}
}