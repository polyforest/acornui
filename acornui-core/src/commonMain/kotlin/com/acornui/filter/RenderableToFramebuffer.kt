package com.acornui.filter

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

	/**
	 * The color tint for the rendering of the frame buffer.
	 */
	var colorTint: ColorRo = Color.WHITE

	var clearMask = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT
	var clearColor = Color.CLEAR
	var blendMode = BlendMode.NORMAL
	var premultipliedAlpha = false

	private val glState = inject(GlState)

	private val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	val texture: Texture
		get() = framebuffer.texture

	private val _canvasRegion = MinMax()

	/**
	 * The region (in canvas coordinates) where the contents have been drawn, using [drawToFramebuffer].
	 *
	 * Note: This will include the padding provided to [drawToFramebuffer].
	 */
	val canvasRegion: MinMaxRo = _canvasRegion

	private val viewport = IntRectangle()

	private var u: Float = 0f
	private var v: Float = 0f

	fun drawToFramebuffer(clip: MinMaxRo, contents: Renderable, padding: PadRo = Pad.EMPTY_PAD) {
		val region = _canvasRegion
		contents.canvasDrawRegion(region).inflate(padding)
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

		contents.render(clip)

		framebuffer.end()
		val texture = framebuffer.texture
		u = region.width / texture.width
		v = 1f - (region.height / texture.height)
	}

	fun drawToScreen(canvasX: Float = canvasRegion.xMin, canvasY: Float = canvasRegion.yMin) {
		glState.getViewport(viewport)
		val texture = framebuffer.texture
		val batch = glState.batch
		batch.begin()
		glState.viewProjection = Matrix4.IDENTITY
		glState.model = Matrix4.IDENTITY
		glState.setTexture(texture)
		glState.blendMode(blendMode, premultipliedAlpha)

		val w = _canvasRegion.width
		val h = _canvasRegion.height

		val x1 = canvasX / viewport.width * 2f - 1f
		val x2 = (canvasX + w) / viewport.width * 2f - 1f
		val y1 = -(canvasY / viewport.height * 2f - 1f)
		val y2 = -((canvasY + h) / viewport.height * 2f - 1f)

		// Top left
		batch.putVertex(x1, y1, 0f, u = 0f, v = 1f, colorTint = colorTint)
		// Top right
		batch.putVertex(x2, y1, 0f, u = u, v = 1f, colorTint = colorTint)
		// Bottom right
		batch.putVertex(x2, y2, 0f, u = u, v = v, colorTint = colorTint)
		// Bottom left
		batch.putVertex(x1, y2, 0f, u = 0f, v = v, colorTint = colorTint)
		batch.putQuadIndices()
	}

	override fun dispose() {
		super.dispose()
		framebuffer.dispose()
	}
}