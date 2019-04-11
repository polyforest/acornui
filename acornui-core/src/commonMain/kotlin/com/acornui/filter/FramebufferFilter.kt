package com.acornui.filter

import com.acornui.core.AppConfig
import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.OrthographicCamera
import com.acornui.core.graphic.Texture
import com.acornui.core.graphic.Window
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.math.*
import kotlin.math.ceil
import kotlin.math.floor

// TODO: investigate reusing frame buffers

/**
 * A filter that draws the target component to a resizable frame buffer.
 */
abstract class FramebufferFilter(
		owner: Owned,
		hasDepth: Boolean = owner.inject(AppConfig).gl.depth,
		hasStencil: Boolean = owner.inject(AppConfig).gl.stencil
) : RenderFilterBase(owner), Disposable {

	private val glState = inject(GlState)
	private val window = inject(Window)

	protected var blendMode = BlendMode.NORMAL
	protected var premultipliedAlpha = false

	protected var clearMask = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT
	protected var clearColor = Color.CLEAR

	protected abstract val padding: PadRo

	protected val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	private val canvasRegion = MinMax()

	private val previousViewport = IntRectangle()
	private val camera = OrthographicCamera()

	override fun canvasDrawRegion(out: MinMax): MinMax {
		super.canvasDrawRegion(out)
		return out.inflate(padding)
	}

	protected open fun beginFramebuffer(clip: MinMaxRo) {
		canvasDrawRegion(canvasRegion).intersection(clip)
		framebuffer.setSize(canvasRegion.width, canvasRegion.height)

		glState.getViewport(previousViewport)
		framebuffer.begin()
		glState.setViewport(-canvasRegion.xMin.toInt(), canvasRegion.yMin.toInt() - previousViewport.height + framebuffer.texture.height, previousViewport.width, previousViewport.height)
		camera.setViewport(window.width, window.height)
		camera.moveToLookAtRect(0f, 0f, window.width, window.height)
		if (clearMask != 0)
			clearAndReset(clearColor, clearMask)
	}

	protected open fun endFramebuffer() {
		framebuffer.end()
		drawToCanvasRegion(framebuffer.texture)
	}

	protected fun drawToCanvasRegion(texture: Texture) {
		val batch = glState.batch
		batch.begin()
		glState.setCamera(camera)
		glState.setTexture(texture)
		glState.blendMode(blendMode, premultipliedAlpha)

		val w = ceil(canvasRegion.width)
		val h = ceil(canvasRegion.height)

		val x = floor(canvasRegion.xMin)
		val y = floor(canvasRegion.yMin)

		val u = w / texture.width
		val v = 1f - (h / texture.height)

		// Top left
		batch.putVertex(x, y, 0f, u = 0f, v = 1f)
		// Top right
		batch.putVertex(x + w, y, 0f, u = u, v = 1f)
		// Bottom right
		batch.putVertex(x + w, y + h, 0f, u = u, v = v)
		// Bottom left
		batch.putVertex(x, y + h, 0f, u = 0f, v = v)
		batch.putQuadIndices()
	}

	override fun dispose() {
		super.dispose()
		framebuffer.dispose()
	}
}