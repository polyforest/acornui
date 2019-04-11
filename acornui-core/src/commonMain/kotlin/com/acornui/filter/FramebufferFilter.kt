package com.acornui.filter

import com.acornui.core.AppConfig
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Window
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import kotlin.math.ceil
import kotlin.math.floor

// TODO: investigate reusing frame buffers

/**
 * A filter that draws the target component to a resizable frame buffer.
 */
open class FramebufferFilter(
		owner: Owned,
		hasDepth: Boolean = owner.inject(AppConfig).gl.depth,
		hasStencil: Boolean = owner.inject(AppConfig).gl.stencil
) : RenderFilterBase(owner) {

	/**
	 * The color tint for the rendering of the frame buffer.
	 */
	var colorTint: ColorRo by bindable(Color.WHITE)

	private val glState = inject(GlState)
	private val window = inject(Window)

	protected var blendMode = BlendMode.NORMAL
	protected var premultipliedAlpha = false

	protected var clearMask = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT
	protected var clearColor = Color.CLEAR

	protected open val padding: PadRo = Pad.EMPTY_PAD

	protected val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	private val canvasRegion = MinMax()

	private val previousViewport = IntRectangle()

	override fun canvasDrawRegion(out: MinMax): MinMax {
		super.canvasDrawRegion(out)
		out.inflate(padding)
		out.xMin = floor(out.xMin)
		out.yMin = floor(out.yMin)
		out.xMax = ceil(out.xMax)
		out.yMax = ceil(out.yMax)
		return out
	}

	override fun draw(clip: MinMaxRo) {
		beginFramebuffer(clip)
		renderContents(clip)
		framebuffer.end()
		drawToScreen()
	}

	/**
	 * Initializes and begins the frame buffer.
	 *
	 */
	protected open fun beginFramebuffer(clip: MinMaxRo) {
		canvasDrawRegion(canvasRegion).intersection(clip)
		framebuffer.setSize(canvasRegion.width, canvasRegion.height)

		glState.getViewport(previousViewport)
		framebuffer.begin()
		glState.setViewport(-canvasRegion.xMin.toInt(), canvasRegion.yMin.toInt() - previousViewport.height + framebuffer.texture.height, previousViewport.width, previousViewport.height)
		if (clearMask != 0)
			clearAndReset(clearColor, clearMask)
	}

	protected open fun drawToScreen(offsetX: Float = 0f, offsetY: Float = 0f) {
		val texture = framebuffer.texture
		val batch = glState.batch
		batch.begin()
		glState.viewProjection = Matrix4.IDENTITY
		glState.model = Matrix4.IDENTITY
		glState.setTexture(texture)
		glState.blendMode(blendMode, premultipliedAlpha)

		val w = canvasRegion.width
		val h = canvasRegion.height

		val canvasX = canvasRegion.xMin + offsetX
		val canvasY = canvasRegion.yMin + offsetY

		val u = w / texture.width
		val v = 1f - (h / texture.height)

		val x1 = canvasX / window.width * 2f - 1f
		val x2 = (canvasX + w) / window.width * 2f - 1f
		val y1 = -(canvasY / window.height * 2f - 1f)
		val y2 = -((canvasY + h) / window.height * 2f - 1f)

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