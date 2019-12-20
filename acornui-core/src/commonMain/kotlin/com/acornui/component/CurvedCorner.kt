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

package com.acornui.component

import com.acornui.async.disposeOnShutdown
import com.acornui.component.drawing.putIdtQuad
import com.acornui.di.Scoped
import com.acornui.di.inject
import com.acornui.gl.core.*
import com.acornui.graphic.BlendMode
import com.acornui.graphic.Window
import kotlin.math.ceil


private var curvedShader: ShaderProgram? = null

private data class SmoothCornerKey(
		val cornerRadiusX: Float,
		val cornerRadiusY: Float,
		val strokeThicknessX: Float?,
		val strokeThicknessY: Float?
)

private val smoothCornerMap: MutableMap<SmoothCornerKey, Framebuffer> = HashMap()

/**
 * Generates a smooth, antialiased corner.
 * @param cornerRadiusX The elliptical width of the corner.
 * @param cornerRadiusY The elliptical height of the corner.
 * @param strokeThicknessX The horizontal thickness of the stroke.
 * @param strokeThicknessY The vertical thickness of the stroke.
 * @param flipX If true, the u and u2 values will be flipped.
 * @param flipY If true, the v and v2 values will be flipped.
 * @param spriteOut The Sprite to populate with the uv coordinates and texture.
 * @param useCache If true, the frame buffer used will be saved for matching corner properties.
 * @return Returns [spriteOut].
 */
fun Scoped.createSmoothCorner(
		cornerRadiusX: Float,
		cornerRadiusY: Float,
		strokeThicknessX: Float? = null,
		strokeThicknessY: Float? = null,
		flipX: Boolean = false,
		flipY: Boolean = false,
		spriteOut: Sprite = Sprite(inject(CachedGl20)),
		useCache: Boolean = true
): Sprite {
	val window = inject(Window)
	val scaleX = window.scaleX
	val scaleY = window.scaleY
	val sX = (strokeThicknessX ?: cornerRadiusX + 1f) * scaleX
	val sY = (strokeThicknessY ?: cornerRadiusY + 1f) * scaleY
	val cRX = cornerRadiusX * scaleX
	val cRY = cornerRadiusY * scaleY
	if (cRX < 0.0001f || cRY < 0.0001f || (sX < 0.0001f && sY < 0.0001f)) {
		spriteOut.clear()
		return spriteOut
	}
	val cacheKey = SmoothCornerKey(cRX, cRY, strokeThicknessX, strokeThicknessY)
	val framebuffer = if (useCache && smoothCornerMap.containsKey(cacheKey)) {
		smoothCornerMap[cacheKey]!!
	} else {
		val gl = inject(CachedGl20)
		val batch = gl.batch
		if (curvedShader == null)
			curvedShader = disposeOnShutdown(CurvedRectShaderProgram(gl))
		val framebuffer = framebuffer(ceil(cRX).toInt(), ceil(cRY).toInt())
		val curvedShader = curvedShader!!
		gl.useProgram(curvedShader.program) {
			val uniforms = gl.uniforms
			framebuffer.begin()
			gl.clearAndReset()
			uniforms.put("u_cornerRadius", cRX, cRY)
			uniforms.putOptional("u_strokeThickness", sX, sY)
			batch.begin(blendMode = BlendMode.NONE, premultipliedAlpha = false)
			batch.putIdtQuad()
			framebuffer.end()
		}
		if (useCache)
			smoothCornerMap[cacheKey] = disposeOnShutdown(framebuffer)
		framebuffer
	}
	val fBW = framebuffer.widthPixels.toFloat()
	val fBH = framebuffer.heightPixels.toFloat()
	val pW = cRX / fBW
	val pH = cRY / fBH
	val u: Float
	val v: Float
	val u2: Float
	val v2: Float
	if (flipX) {
		u = pW
		u2 = 0f
	} else {
		u = 0f
		u2 = pW
	}
	if (flipY) {
		v = pH
		v2 = 0f
	} else {
		v = 0f
		v2 = pH
	}
	spriteOut.setUv(u, v, u2, v2, isRotated = false)
	spriteOut.texture = framebuffer.texture
	spriteOut.setScaling(scaleX, scaleY)

	return spriteOut
}

private class CurvedRectShaderProgram(gl: CachedGl20) : ShaderProgramBase(
		gl, vertexShaderSrc = """

$DEFAULT_SHADER_HEADER

attribute vec3 a_position;

void main() {
	gl_Position = vec4(a_position, 1.0);
}""",
		fragmentShaderSrc = """

$DEFAULT_SHADER_HEADER

uniform vec2 u_strokeThickness;
uniform vec2 u_cornerRadius;

void main() {
	// x^2/a^2 + y^2/b^2 = 1
	vec2 point = gl_FragCoord.xy;

	float p = length(point / max(vec2(0.001), u_cornerRadius - 0.5));
	float p2;
	float a1;
	float a2;
	p2 = length(point / (u_cornerRadius + 0.3));
	a1 = smoothstep(p2, p, 1.0);

	p = length(point / max(vec2(0.001), u_cornerRadius - u_strokeThickness - 0.5));
	p2 = length(point / max(vec2(0.001), u_cornerRadius - u_strokeThickness + 0.5));
	a2 = 1.0 - smoothstep(p2, p, 1.0);

	float a = min(a1, a2);
	if (a < 0.001) discard;
	gl_FragColor = vec4(1.0, 1.0, 1.0, a);
}""",
		vertexAttributes = mapOf(VertexAttributeLocation.POSITION to CommonShaderAttributes.A_POSITION)
)
