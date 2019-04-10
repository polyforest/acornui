package com.acornui.filter

import com.acornui.component.UiComponentRo
import com.acornui.core.AppConfig
import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Texture
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.math.IntRectangle
import com.acornui.math.MinMax
import com.acornui.math.MinMaxRo
import com.acornui.math.Pad
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
) : RenderFilterBase(owner), Disposable {

	private val glState = inject(GlState)

	var blendMode = BlendMode.NORMAL
	var premultipliedAlpha = false
	var clearColor = Color.CLEAR
	var padding by bindable(Pad(16f))

	protected val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	private val globalBounds = MinMax()

	private val previousViewport = IntRectangle()

	override fun beforeRender(target: UiComponentRo, clip: MinMaxRo) {
		target.localToCanvas(globalBounds.set(0f, 0f, target.width, target.height)).inflate(padding).intersection(clip)
		framebuffer.setSize(globalBounds.width, globalBounds.height)

		glState.getViewport(previousViewport)
		framebuffer.begin()
		glState.setViewport(-globalBounds.xMin.toInt(), globalBounds.yMin.toInt() - previousViewport.height + framebuffer.texture.height, previousViewport.width, previousViewport.height)
		clearAndReset(clearColor)
	}

	override fun afterRender(target: UiComponentRo, clip: MinMaxRo) {
		framebuffer.end()
		draw(target, framebuffer.texture)
	}

	protected open fun draw(target: UiComponentRo, texture: Texture) {
		val batch = glState.batch
		batch.begin()
		glState.setCamera(target.camera)
		glState.setTexture(texture)
		glState.blendMode(blendMode, premultipliedAlpha)

		val w = ceil(globalBounds.width)
		val h = ceil(globalBounds.height)

		val x = floor(globalBounds.xMin)
		val y = floor(globalBounds.yMin)

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

fun Owned.framebufferFilter(hasDepth: Boolean = true,
							hasStencil: Boolean = true): FramebufferFilter {
	return FramebufferFilter(this, hasDepth, hasStencil)
}