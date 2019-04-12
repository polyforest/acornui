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

	var clearMask by bindable(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
	var clearColor by bindable(Color.CLEAR)

	protected open val padding: PadRo = Pad.EMPTY_PAD

	protected val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	private val _canvasRegion = MinMax()

	/**
	 * The region (in canvas coordinates) where the contents have been drawn.
	 */
	val canvasRegion: MinMaxRo = _canvasRegion

	private val previousViewport = IntRectangle()

	private var u: Float = 0f
	private var v: Float = 0f

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
		drawToFramebuffer(clip)
		drawToScreen()
	}

	protected fun drawToFramebuffer(clip: MinMaxRo) {
		beginFramebuffer(clip)
		renderContents(clip)
		framebuffer.end()
		val texture = framebuffer.texture
		u = _canvasRegion.width / texture.width
		v = 1f - (_canvasRegion.height / texture.height)
	}

	/**
	 * Initializes and begins the frame buffer.
	 */
	private fun beginFramebuffer(clip: MinMaxRo) {
		canvasDrawRegion(_canvasRegion).intersection(clip)
		framebuffer.setSize(_canvasRegion.width, _canvasRegion.height)

		glState.getViewport(previousViewport)
		framebuffer.begin()
		glState.setViewport(-_canvasRegion.xMin.toInt(), _canvasRegion.yMin.toInt() - previousViewport.height + framebuffer.texture.height, previousViewport.width, previousViewport.height)
		if (clearMask != 0)
			clearAndReset(clearColor, clearMask)
	}

	open fun drawToScreen(canvasX: Float = canvasRegion.xMin, canvasY: Float = canvasRegion.yMin) {
		val texture = framebuffer.texture
		val batch = glState.batch
		batch.begin()
		glState.viewProjection = Matrix4.IDENTITY
		glState.model = Matrix4.IDENTITY
		glState.setTexture(texture)
		glState.blendMode(blendMode, premultipliedAlpha)

		val w = _canvasRegion.width
		val h = _canvasRegion.height

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