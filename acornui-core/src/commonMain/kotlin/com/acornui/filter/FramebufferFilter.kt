package com.acornui.filter

import com.acornui.component.UiComponentRo
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.CameraRo
import com.acornui.core.graphic.Window
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.math.Matrix4
import com.acornui.math.MinMaxRo
import com.acornui.math.Pad

// TODO: WIP

/**
 * WIP
 * A filter that draws the target component to a resizable frame buffer.
 */
class FramebufferFilter(
		override val injector: Injector,
		hasDepth: Boolean = true,
		hasStencil: Boolean = true
) : RenderFilter, Scoped {

	private val gl = inject(Gl20)
	private val glState = inject(GlState)
	private val window = inject(Window)

	override var enabled: Boolean = true

	var padding = Pad(16f)

	private val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

//	private val previousViewport = IntRectangle()

	private var cam: CameraRo? = null

	override fun begin(viewport: MinMaxRo, target: UiComponentRo) {
//		framebuffer.setSize(padding.expandWidth2(target.width), padding.expandHeight2(target.height))
		cam = target.camera
		framebuffer.updateWorldVertices(Matrix4.IDENTITY, 2f, 2f, -1f, -1f)
//		framebuffer.updateWorldVertices(target.concatenatedTransform, target.width, target.height)
		framebuffer.setSize(window.width, window.height)
		framebuffer.begin()
//		glState.setViewport(0, 0, 1000, 1000)
		gl.clear(Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT)
	}

	override fun end() {
		framebuffer.end()
		glState.viewProjection = Matrix4.IDENTITY
		glState.model = Matrix4.IDENTITY
		framebuffer.render(glState, Color.WHITE)
	}
}