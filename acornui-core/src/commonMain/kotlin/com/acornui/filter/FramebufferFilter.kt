package com.acornui.filter

import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.component.localToCanvas
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Window
import com.acornui.core.graphic.orthographicCamera
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.math.*
import kotlin.math.ceil
import kotlin.math.floor

// TODO: WIP

/**
 * WIP
 * A filter that draws the target component to a resizable frame buffer.
 */
class FramebufferFilter(
		target: UiComponent,
		hasDepth: Boolean = true,
		hasStencil: Boolean = true
) : RenderFilterBase(target) {

	private val gl = inject(Gl20)
	private val glState = inject(GlState)
	private val window = inject(Window)

	var padding = Pad(16f)

	private val framebuffer = framebuffer(512, 512, hasDepth = hasDepth, hasStencil = hasStencil)

	private val globalBounds = MinMax()
	private val worldTransform = Matrix4()

	init {
//		target.cameraOverride = target.orthographicCamera {
//			setViewport(112f, 42f)
//			moveToLookAtRect(0f, 0f, 256f, 256f)
//		}
	}

	override fun beforeRender(clip: MinMaxRo) {
//		target.localToGlobal
		(globalBounds.set(target.x, target.y, target.right, target.bottom)).inflate(padding)
//		globalBounds.xMin = floor(globalBounds.xMin)
//		globalBounds.yMin = floor(globalBounds.yMin)
//		globalBounds.xMax = ceil(globalBounds.xMax)
//		globalBounds.yMax = ceil(globalBounds.yMax)
		//worldTransform.setTranslation(globalBounds.xMin, globalBounds.yMin, 0f)
//		framebuffer.setSize(512f, 512f)
		framebuffer.begin()
		glState.setViewport(0, -600+target.height.toInt(), 1000, 600)
		//glState.setViewport(globalBounds.xMin.toInt(), globalBounds.yMin.toInt(), globalBounds.width.toInt(), globalBounds.height.toInt())
		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
	}

	override fun afterRender(clip: MinMaxRo) {
		framebuffer.end()
		val batch = glState.batch
		batch.begin()
		glState.setCamera(target.camera)
		glState.setTexture(framebuffer.texture)
		glState.blendMode(BlendMode.NORMAL, premultipliedAlpha = false)

		// Top left
		batch.putVertex(0f, 0f, 0f, u = 0f, v = 1f, colorTint = Color.BLUE)
		// Top right
		batch.putVertex(512f, 0f, 0f, u = 1f, v = 1f, colorTint = Color.BLUE)
		// Bottom right
		batch.putVertex(512f, 512f, 0f, u = 1f, v = 0f, colorTint = Color.RED)
		// Bottom left
		batch.putVertex(0f, 512f, 0f, u = 0f, v = 0f, colorTint = Color.RED)
		batch.putQuadIndices()


	}
}

fun UiComponent.framebufferFilter(hasDepth: Boolean = true,
							 hasStencil: Boolean = true): FramebufferFilter {
	return FramebufferFilter(this, hasDepth, hasStencil)
}