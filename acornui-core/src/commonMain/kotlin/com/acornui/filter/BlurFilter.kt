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
import com.acornui.ceilInt
import com.acornui.component.ComponentInit
import com.acornui.component.Sprite
import com.acornui.component.drawing.putIdtQuad
import com.acornui.di.Owned
import com.acornui.di.own
import com.acornui.gl.core.*
import com.acornui.graphic.BlendMode
import com.acornui.graphic.Color
import com.acornui.math.IntPad
import com.acornui.math.IntPadRo
import com.acornui.math.IntRectangleRo
import com.acornui.math.Matrix4
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class BlurFilter(owner: Owned) : RenderFilterBase(owner) {

	var blurX by bindable(1f)
	var blurY by bindable(1f)

	var quality by bindable(BlurQuality.NORMAL)

	private val blurFramebufferA = own(resizeableFramebuffer())
	private val blurFramebufferB = own(resizeableFramebuffer())

	private val sprite = Sprite(glState)
	private val transform = Matrix4()

	private val _drawPadding = IntPad()
	override val drawPadding: IntPadRo
		get() {
			val hPad = ceilInt(blurX * quality.passes * glState.framebuffer.scaleX)
			val vPad = ceilInt(blurY * quality.passes * glState.framebuffer.scaleY)
			return _drawPadding.set(top = vPad, right = hPad, bottom = vPad, left = hPad)
		}

	private val framebufferFilter = FramebufferFilter(this)

	init {
		framebufferFilter.clearColor = Color(0.5f, 0.5f, 0.5f, 0f)
	}

	override fun render(region: IntRectangleRo, inner: () -> Unit) {
		drawToPingPongBuffers(region, inner)
		drawBlurToScreen()
	}

	private val framebufferInfo = FramebufferInfo()

	fun drawToPingPongBuffers(region: IntRectangleRo, inner: () -> Unit) {
		val fB = framebufferInfo.set(glState.framebuffer)
		val vY = fB.height - region.bottom
		val framebufferFilter = framebufferFilter
		framebufferFilter.drawToFramebuffer(region, inner)
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
		val uniforms = blurShader.uniforms
		glState.useShader(blurShader) {
			uniforms.put("u_resolutionInv", 1f / textureToBlur.widthPixels.toFloat(), 1f / textureToBlur.heightPixels.toFloat())
			glState.setTexture(framebufferFilter.texture)
			glState.blendMode(BlendMode.NONE, premultipliedAlpha = false)
			val passes = quality.passes
			for (i in 1..passes) {
				val p = i.toFloat() / passes.toFloat()
				blurFramebufferA.begin()
				uniforms.put("u_dir", 0f, blurY * p * 0.25f * fB.scaleY)
				glState.batch.putIdtQuad()
				blurFramebufferA.end()
				blurFramebufferB.begin()
				glState.setTexture(blurFramebufferA.texture)
				uniforms.put("u_dir", blurX * p * 0.25f * fB.scaleX, 0f)
				glState.batch.putIdtQuad()
				blurFramebufferB.end()
				glState.setTexture(blurFramebufferB.texture)
			}
		}

		framebufferFilter.drawable(sprite)
		sprite.texture = blurFramebufferB.texture
		transform.setTranslation(region.x.toFloat() / fB.scaleX, vY.toFloat() / fB.scaleY)
		sprite.updateWorldVertices(transform = transform)
	}

	fun drawBlurToScreen() {
		sprite.render()
	}

	fun drawOriginalToScreen() {
		framebufferFilter.drawToScreen()
	}

	/**
	 * Configures a drawable to match what was last rendered.
	 */
	fun drawable(out: Sprite = Sprite(glState)): Sprite {
		return out.set(sprite)
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
