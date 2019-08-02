package com.acornui.test

import com.acornui.gl.core.*
import com.acornui.graphic.BlendMode
import com.acornui.graphic.Texture
import com.acornui.math.ColorTransformationRo
import com.acornui.math.IntRectangleRo
import com.acornui.math.Matrix4Ro

object MockGlState : GlState {

	override var batch: ShaderBatch = MockShaderBatch
	override var shader: ShaderProgram?
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
		set(value) {}
	override var blendingEnabled: Boolean
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
		set(value) {}
	override val blendMode: BlendMode
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override val premultipliedAlpha: Boolean
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override var viewProjection: Matrix4Ro
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
		set(value) {}
	override var model: Matrix4Ro
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
		set(value) {}
	override var colorTransformation: ColorTransformationRo?
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
		set(value) {}
	override var scissorEnabled: Boolean
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
		set(value) {}
	override val whitePixel: Texture
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

	override fun activeTexture(value: Int) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getTexture(unit: Int): Texture? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun setTexture(texture: Texture?, unit: Int) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun unsetTexture(texture: Texture) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun blendMode(blendMode: BlendMode, premultipliedAlpha: Boolean) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override val scissor: IntRectangleRo
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

	override fun setScissor(x: Int, y: Int, width: Int, height: Int) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun setCamera(viewProjection: Matrix4Ro, viewTransform: Matrix4Ro, model: Matrix4Ro) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override val viewport: IntRectangleRo
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

	override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getFramebuffer(out: FramebufferInfo) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun setFramebuffer(framebuffer: GlFramebufferRef?, width: Int, height: Int, scaleX: Float, scaleY: Float) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}
