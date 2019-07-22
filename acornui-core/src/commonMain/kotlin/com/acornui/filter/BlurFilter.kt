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

package com.acornui.filter

import com.acornui.async.disposeOnShutdown
import com.acornui.component.ComponentInit
import com.acornui.component.PaddedDrawable
import com.acornui.component.Sprite
import com.acornui.component.drawing.putIdtQuad
import com.acornui.core.Renderable
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.graphic.BlendMode
import com.acornui.core.renderContext
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMaxRo
import com.acornui.math.Pad
import com.acornui.math.PadRo

open class BlurFilter(owner: Owned) : RenderFilterBase(owner) {

	var blurX by bindable(1f)
	var blurY by bindable(1f)

	var quality by bindable(BlurQuality.NORMAL)

	private val gl by Gl20
	private val blurFramebufferA = own(resizeableFramebuffer())
	private val blurFramebufferB = own(resizeableFramebuffer())

	private val sprite = Sprite(glState)
	private val drawable = PaddedDrawable(sprite)

	private val _drawPadding = Pad()
	override val drawPadding: PadRo
		get() {
			val hPad = blurX * 4f * quality.passes
			val vPad = blurY * 4f * quality.passes
			return _drawPadding.set(left = hPad, top = vPad, right = hPad, bottom = vPad)
		}

	override val shouldSkipFilter: Boolean
		get() = super.shouldSkipFilter || (blurX <= 0f && blurY <= 0f)

	private val framebufferFilter = FramebufferFilter(this)

	override var contents: Renderable?
		get() = super.contents
		set(value) {
			super.contents = value
			framebufferFilter.contents = value
		}

	init {
		framebufferFilter.clearColor = Color(0.5f, 0.5f, 0.5f, 0f)
	}

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		if (!bitmapCacheIsValid)
			drawToPingPongBuffers()
		drawBlurToScreen()
	}

	fun drawToPingPongBuffers() {
		val framebufferUtil = framebufferFilter
		val drawPadding = drawPadding
		framebufferUtil.drawPadding = drawPadding
		framebufferUtil.drawToFramebuffer()
		val textureToBlur = framebufferUtil.texture

		blurFramebufferA.setSize(textureToBlur.widthPixels, textureToBlur.heightPixels)
		blurFramebufferA.texture.filterMin = TextureMinFilter.LINEAR
		blurFramebufferA.texture.filterMag = TextureMagFilter.LINEAR
		blurFramebufferB.setSize(textureToBlur.widthPixels, textureToBlur.heightPixels)
		blurFramebufferB.texture.filterMin = TextureMinFilter.LINEAR
		blurFramebufferB.texture.filterMag = TextureMagFilter.LINEAR

		glState.setTexture(textureToBlur)

		if (blurShader == null) {
			blurShader = disposeOnShutdown(BlurShader(gl))
		}
		val blurShader = blurShader!!
		glState.useShader(blurShader) {
			gl.uniform2f(blurShader.getRequiredUniformLocation("u_resolutionInv"), 1f / textureToBlur.widthPixels.toFloat(), 1f / textureToBlur.heightPixels.toFloat())
			glState.setTexture(framebufferUtil.texture)
			glState.blendMode(BlendMode.NONE, premultipliedAlpha = false)
			val passes = quality.passes
			for (i in 1..passes) {
				val p = i.toFloat() / passes.toFloat()
				blurFramebufferA.begin()
				gl.uniform2f(blurShader.getRequiredUniformLocation("u_dir"), 0f, blurY * p)
				glState.batch.putIdtQuad()
				blurFramebufferA.end()
				blurFramebufferB.begin()
				glState.setTexture(blurFramebufferA.texture)
				gl.uniform2f(blurShader.getRequiredUniformLocation("u_dir"), blurX * p, 0f)
				glState.batch.putIdtQuad()
				blurFramebufferB.end()
				glState.setTexture(blurFramebufferB.texture)
			}
		}

		val fBT = blurFramebufferB.texture
		val w = fBT.widthPixels.toFloat()
		val h = fBT.heightPixels.toFloat()
		val region = framebufferUtil.drawRegion
		drawable.padding.set(-drawPadding.left, -drawPadding.top, -drawPadding.right, -drawPadding.bottom)

		sprite.texture = fBT
		sprite.setUv(0f, 1f, region.width / w, 1f - (region.height / h), isRotated = false)
		drawable.setSize(null, null)
		//sprite.setScaling(window.scaleX, window.scaleY)
	}

	fun drawBlurToScreen() {
		drawable.renderContextOverride = renderContext
		drawable.render()
	}

	fun drawOriginalToScreen(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		framebufferFilter.drawToScreen()
	}

	/**
	 * Configures a drawable to match what was last rendered.
	 */
	fun drawable(out: PaddedDrawable<Sprite> = PaddedDrawable(Sprite(glState))): PaddedDrawable<Sprite> {
		out.inner.set(sprite)
		out.padding.set(drawable.padding)
		return out
	}

	companion object {
		private var blurShader: ShaderProgram? = null
	}
}

internal class BlurShader(gl: Gl20) : ShaderProgramBase(gl,
		vertexShaderSrc = """
$DEFAULT_SHADER_HEADER

attribute vec4 a_position;

void main() {
	gl_Position =  a_position;
}

""", fragmentShaderSrc = """

$DEFAULT_SHADER_HEADER

uniform vec2 u_dir;
uniform vec2 u_resolutionInv;
uniform sampler2D u_texture;

void main() {
	vec2 coord = gl_FragCoord.xy * u_resolutionInv;
	vec2 m = u_dir * u_resolutionInv;
	vec4 sum = vec4(0.0);

	sum += texture2D(u_texture, coord - m * 4.0) * 0.05;
	sum += texture2D(u_texture, coord - m * 3.0) * 0.09;
	sum += texture2D(u_texture, coord - m * 2.0) * 0.12;
	sum += texture2D(u_texture, coord - m * 1.0) * 0.15;

	sum += texture2D(u_texture, coord) * 0.18;

	sum += texture2D(u_texture, coord + m * 1.0) * 0.15;
	sum += texture2D(u_texture, coord + m * 2.0) * 0.12;
	sum += texture2D(u_texture, coord + m * 3.0) * 0.09;
	sum += texture2D(u_texture, coord + m * 4.0) * 0.05;

	gl_FragColor = sum;
}

""")

fun Owned.blurFilter(init: ComponentInit<BlurFilter> = {}): BlurFilter {
	val b = BlurFilter(this)
	b.init()
	return b
}

enum class BlurQuality(val passes: Int) {
	LOW(1),
	NORMAL(2),
	HIGH(3),
	BEST(4),
}
