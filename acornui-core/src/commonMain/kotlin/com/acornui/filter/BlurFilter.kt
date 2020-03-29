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

@file:Suppress("unused")

package com.acornui.filter

import com.acornui.component.ComponentInit
import com.acornui.component.Sprite
import com.acornui.component.drawing.putIdtQuad
import com.acornui.di.Context
import com.acornui.di.dependencyFactory
import com.acornui.di.own
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4
import com.acornui.math.Matrix4Ro
import com.acornui.math.Rectangle
import com.acornui.math.Vector2
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class BlurFilter(owner: Context) : RenderFilterBase(owner) {

	var blurX by bindable(5f)
	var blurY by bindable(5f)

	var quality by bindable(BlurQuality.NORMAL)

	private val blurFramebufferA = own(resizeableFramebuffer())
	private val blurFramebufferB = own(resizeableFramebuffer())

	private val sprite = Sprite(gl)

	private val framebufferFilter = FramebufferFilter(this)

	private val blurShader by BlurShaderKey

	init {
		framebufferFilter.clearColor = Color(0.5f, 0.5f, 0.5f, 0f)
	}

	override fun region(region: Rectangle) {
		region.inflate(blurX, blurY, blurX, blurY)
		framebufferFilter.region(region)
	}

	private val framebufferTransform = Matrix4()

	override fun updateGlobalVertices(transform: Matrix4Ro, tint: ColorRo) {
		val framebufferFilter = framebufferFilter
		framebufferFilter.updateGlobalVertices(transform, tint)
		val textureToBlur = framebufferFilter.texture

		blurFramebufferA.setSize(textureToBlur.widthPixels, textureToBlur.heightPixels)
		blurFramebufferA.texture.filterMin = TextureMinFilter.LINEAR
		blurFramebufferA.texture.filterMag = TextureMagFilter.LINEAR
		blurFramebufferB.setSize(textureToBlur.widthPixels, textureToBlur.heightPixels)
		blurFramebufferB.texture.filterMin = TextureMinFilter.LINEAR
		blurFramebufferB.texture.filterMag = TextureMagFilter.LINEAR

		framebufferFilter.drawable(sprite)
		sprite.texture = blurFramebufferB.texture
		sprite.updateGlobalVertices(transform = transform - Vector2(blurX, blurY), tint = tint)
	}

	override fun renderLocal(inner: () -> Unit) {
		val framebufferFilter = framebufferFilter
		framebufferFilter.renderLocal(inner)
		val textureToBlur = framebufferFilter.texture

		gl.bindTexture(textureToBlur)

		gl.useProgram(blurShader.program) {
			val uniforms = gl.uniforms
			uniforms.put("u_resolutionInv", 1f / textureToBlur.widthPixels.toFloat(), 1f / textureToBlur.heightPixels.toFloat())
			gl.batch.begin(framebufferFilter.texture)
			blurFramebufferA.begin()
			gl.clearAndReset()
			blurFramebufferA.end()
			blurFramebufferB.begin()
			gl.clearAndReset()
			blurFramebufferB.end()

			val passes = quality.passes
			val scl = 0.25f / passes.toFloat() // 1f / BEST.passes
			val sclX = scl * scaleX
			val sclY = scl * scaleY
			for (i in 1..passes) {
				val iF = i.toFloat()
				blurFramebufferA.begin()
				uniforms.put("u_dir", blurX * iF * sclX, 0f)
				gl.batch.putIdtQuad()
				blurFramebufferA.end()
				blurFramebufferB.begin()
				gl.batch.begin(blurFramebufferA.texture)
				uniforms.put("u_dir", 0f, blurY * iF * sclY)
				gl.batch.putIdtQuad()
				blurFramebufferB.end()
				gl.batch.begin(blurFramebufferB.texture)
			}
		}
	}

	override fun render(inner: () -> Unit) = render()

	fun render() {
		sprite.render()
	}

	fun drawOriginalToScreen() {
		framebufferFilter.render()
	}

	/**
	 * Configures a drawable to match what was last rendered. The world coordinates will not be updated.
	 */
	fun drawable(out: Sprite = Sprite(gl)): Sprite {
		return out.set(sprite)
	}
}

internal class BlurShader(gl: CachedGl20) : ShaderProgramBase(gl,
		vertexShaderSrc = """
$DEFAULT_SHADER_HEADER

attribute vec4 a_position;

void main() {
	gl_Position = a_position;
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

""") {

	override fun initUniforms(uniforms: Uniforms) {
	}
}

inline fun Context.blurFilter(init: ComponentInit<BlurFilter> = {}): BlurFilter {
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

/**
 * The shader used to create a gaussian blur.
 */
object BlurShaderKey : Context.Key<ShaderProgram> {
	override val factory: Context.DependencyFactory<ShaderProgram> = dependencyFactory { BlurShader(it.inject(CachedGl20)) }
}