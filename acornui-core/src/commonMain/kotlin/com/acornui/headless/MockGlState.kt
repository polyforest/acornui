/*
 * Copyright 2019 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.headless

import com.acornui.gl.core.*
import com.acornui.graphic.BlendMode
import com.acornui.graphic.TextureRo
import com.acornui.graphic.rgbData
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

	override val viewport: IntRectangleRo
		get() = IntRectangle.EMPTY

	override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
	}

	override fun getFramebuffer(out: FramebufferInfo) {
	}

	override fun setFramebuffer(framebuffer: GlFramebufferRef?, width: Int, height: Int, scaleX: Float, scaleY: Float) {
	}
}
