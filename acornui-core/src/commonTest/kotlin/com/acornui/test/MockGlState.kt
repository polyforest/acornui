package com.acornui.test

import com.acornui.gl.core.*
import com.acornui.graphic.*
import com.acornui.math.*

object MockGlState : GlState {

	override var batch: ShaderBatch = MockShaderBatch
	override var shader: ShaderProgram?
		get() = null
		set(value) {}
	override var blendingEnabled: Boolean
		get() = false
		set(value) {}
	override val blendMode: BlendMode
		get() = BlendMode.NONE
	override val premultipliedAlpha: Boolean
		get() = false
	override var viewProjection: Matrix4Ro
		get() = Matrix4.IDENTITY
		set(value) {}
	override var model: Matrix4Ro
		get() = Matrix4.IDENTITY
		set(value) {}
	override var colorTransformation: ColorTransformationRo?
		get() = ColorTransformation.IDENTITY
		set(value) {}
	override var scissorEnabled: Boolean
		get() = false
		set(value) {}

	private val defaultWhitePixel by lazy {
		rgbTexture(MockGl20, this, rgbData(1, 1, hasAlpha = true))
	}

	override val whitePixel: TextureRo
		get() = defaultWhitePixel

	override fun activeTexture(value: Int) {
	}

	override fun getTexture(unit: Int): TextureRo? = null

	override fun setTexture(texture: TextureRo?, unit: Int) {
	}

	override fun unsetTexture(texture: TextureRo) {
	}

	override fun blendMode(blendMode: BlendMode, premultipliedAlpha: Boolean) {
	}

	override val scissor: IntRectangleRo
		get() = IntRectangle.EMPTY

	override fun setScissor(x: Int, y: Int, width: Int, height: Int) {
	}

	override fun setCamera(viewProjection: Matrix4Ro, viewTransform: Matrix4Ro, model: Matrix4Ro) {
	}

	override val viewport: IntRectangleRo
		get() = IntRectangle.EMPTY

	override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
	}

	override fun getFramebuffer(out: FramebufferInfo) {
	}

	override fun setFramebuffer(framebuffer: GlFramebufferRef?, width: Int, height: Int, scaleX: Float, scaleY: Float) {
	}
}
