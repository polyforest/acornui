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

import com.acornui.Renderable
import com.acornui.async.disposeOnShutdown
import com.acornui.component.ComponentInit
import com.acornui.component.PaddedRenderable
import com.acornui.component.RenderContextRo
import com.acornui.component.Sprite
import com.acornui.component.drawing.putIdtQuad
import com.acornui.di.Owned
import com.acornui.di.inject
import com.acornui.di.own
import com.acornui.gl.core.*
import com.acornui.graphic.BlendMode
import com.acornui.graphic.Color
import com.acornui.graphic.Window
import com.acornui.math.Pad
import com.acornui.math.PadRo
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.ceil

open class BlurFilter(owner: Owned) : RenderFilterBase(owner) {

	var blurX by bindable(1f)
	var blurY by bindable(1f)

	var quality by bindable(BlurQuality.NORMAL)

	private val gl by Gl20
	private val window by Window
	private val blurFramebufferA = own(resizeableFramebuffer())
	private val blurFramebufferB = own(resizeableFramebuffer())

	private val sprite = Sprite(glState)
	private val renderable = PaddedRenderable(sprite)

	private val _drawPadding = Pad()
	override val drawPadding: PadRo
		get() {
			val hPad = ceil(blurX * quality.passes)
			val vPad = ceil(blurY * quality.passes)
			return _drawPadding.set(top = vPad, right = hPad, bottom = vPad, left = hPad)
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

	override fun draw(renderContext: RenderContextRo) {
		if (!bitmapCacheIsValid)
			drawToPingPongBuffers()
		drawBlurToScreen(renderContext)
	}

	fun drawToPingPongBuffers() {
		val framebufferFilter = framebufferFilter
		framebufferFilter.drawPadding = drawPadding
		framebufferFilter.drawToFramebuffer()
		val textureToBlur = framebufferFilter.texture

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
			glState.setTexture(framebufferFilter.texture)
			glState.blendMode(BlendMode.NONE, premultipliedAlpha = false)
			val passes = quality.passes
			for (i in 1..passes) {
				val p = i.toFloat() / passes.toFloat()
				blurFramebufferA.begin()
				gl.uniform2f(blurShader.getRequiredUniformLocation("u_dir"), 0f, blurY * p * 0.25f * window.scaleY)
				glState.batch.putIdtQuad()
				blurFramebufferA.end()
				blurFramebufferB.begin()
				glState.setTexture(blurFramebufferA.texture)
				gl.uniform2f(blurShader.getRequiredUniformLocation("u_dir"), blurX * p * 0.25f * window.scaleX, 0f)
				glState.batch.putIdtQuad()
				blurFramebufferB.end()
				glState.setTexture(blurFramebufferB.texture)
			}
		}

		val window = inject(Window)
		blurFramebufferB.drawable(sprite)
		sprite.setScaling(window.scaleX, window.scaleY)
		setDrawPadding(renderable.padding)
		renderable.setSize(null, null)
	}

	fun drawBlurToScreen(renderContext: RenderContextRo) {
		renderable.render(renderContext)
	}

	fun drawOriginalToScreen(renderContext: RenderContextRo) {
		framebufferFilter.drawToScreen(renderContext)
	}

	/**
	 * Configures a drawable to match what was last rendered.
	 */
	fun drawable(out: PaddedRenderable<Sprite> = PaddedRenderable(Sprite(glState))): PaddedRenderable<Sprite> {
		out.inner.set(sprite)
		out.padding.set(renderable.padding)
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

inline fun Owned.blurFilter(init: ComponentInit<BlurFilter> = {}): BlurFilter  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
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
