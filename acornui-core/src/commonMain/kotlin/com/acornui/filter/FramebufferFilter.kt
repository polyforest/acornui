package com.acornui.filter

import com.acornui.component.*
import com.acornui.core.AppConfig
import com.acornui.core.Renderable
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.di.own
import com.acornui.core.graphic.BlendMode
import com.acornui.core.graphic.Texture
import com.acornui.core.render
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.signal.Signal0
import com.acornui.signal.bind

/**
 * Creates a Framebuffer and provides utility to draw a [Renderable] object to it and then draw the frame buffer to the
 * screen.
 */
class FramebufferFilter(
		owner: Owned,
		hasDepth: Boolean = owner.inject(AppConfig).gl.depth,
		hasStencil: Boolean = owner.inject(AppConfig).gl.stencil
) : RenderFilterBase(owner) {

	override var padding: PadRo = Pad.EMPTY_PAD

	var clearMask = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT
	var clearColor = Color.CLEAR
	var blendMode = BlendMode.NORMAL
	var premultipliedAlpha = false

	private val glState = inject(GlState)
	private val defaultRenderContext = inject(RenderContextRo)

	private val framebuffer = resizeableFramebuffer(hasDepth = hasDepth, hasStencil = hasStencil)

	val texture: Texture
		get() = framebuffer.texture

	private val viewport = IntRectangle()
	private val sprite = Sprite(glState)
	private val drawable = PaddedDrawable(sprite)
	private val mvp = Matrix4()
	private val drew = own(Signal0())

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		if (!bitmapCacheIsValid)
			drawToFramebuffer()
		drawToScreen(clip, transform, tint)
	}

	fun drawToFramebuffer() {
		val contents = contents ?: return
		val region = drawRegion
		framebuffer.setSize(region.width, region.height)
		val renderMargin = renderMargin
		drawable.padding.set(-renderMargin.left, -renderMargin.top, -renderMargin.right, -renderMargin.bottom)

		viewport.set(glState.viewport)
		framebuffer.begin()
		glState.setViewport(-region.xMin.toInt(), region.yMin.toInt() - renderContext.canvasTransform.height.toInt() + framebuffer.texture.height, renderContext.canvasTransform.width.toInt(), renderContext.canvasTransform.height.toInt())
		if (clearMask != 0)
			clearAndReset(clearColor, clearMask)

		contents.render(defaultRenderContext)

		framebuffer.end()
		framebuffer.sprite(sprite)
		drawable.updateVertices()

		sprite.setUv(sprite.u, 1f - sprite.v, sprite.u2, 1f - sprite.v2, isRotated = false)
		glState.setViewport(viewport)
		drew.dispatch()
	}

	fun drawToScreen(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		viewport.set(glState.viewport)
		mvp.idt().scl(2f / viewport.width, -2f / viewport.height, 1f).trn(-1f, 1f, 0f) // Projection transform
		mvp.mul(transform)//.translate(margin.left, margin.top)

		glState.viewProjection = mvp
		glState.model = Matrix4.IDENTITY
		drawable.render(clip, Matrix4.IDENTITY, tint)
	}

	/**
	 * Creates a component that renders the sprite that represents the last time [drawToFramebuffer] was called.
	 */
	fun createSnapshot(owner: Owned): UiComponent {
		return owner.drawableC(drawable) {
			own(drew.bind {
				invalidate(ValidationFlags.LAYOUT)
			})
		}
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
