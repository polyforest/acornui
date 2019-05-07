/*
 * Copyright 2019 PolyForest
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
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.gl.core.*
import kotlin.math.ceil


private var curvedShader: ShaderProgram? = null

private data class SmoothCornerKey(
		val cornerRadiusX: Float,
		val cornerRadiusY: Float,
		val strokeThicknessX: Float?,
		val strokeThicknessY: Float?
)

private val smoothCornerMap = HashMap<SmoothCornerKey, Framebuffer>()

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
		spriteOut: Sprite = Sprite(inject(GlState)),
		useCache: Boolean = true
): Sprite {
	val sX = strokeThicknessX ?: cornerRadiusX + 1f
	val sY = strokeThicknessY ?: cornerRadiusY + 1f
	if (cornerRadiusX < 0.0001f || cornerRadiusY < 0.0001f || (sX < 0.0001f && sY < 0.0001f)) {
		spriteOut.clear()
		return spriteOut
	}
	val cacheKey = SmoothCornerKey(cornerRadiusX, cornerRadiusY, strokeThicknessX, strokeThicknessY)
	val framebuffer = if (useCache && smoothCornerMap.containsKey(cacheKey)) {
		smoothCornerMap[cacheKey]!!
	} else {
		val gl = inject(Gl20)
		val glState = inject(GlState)
		if (curvedShader == null)
			curvedShader = disposeOnShutdown(CurvedRectShaderProgram(gl))
		val framebuffer = framebuffer(ceil(cornerRadiusX).toInt() + 4, ceil(cornerRadiusY).toInt() + 4)
		val previousShader = glState.shader
		val curvedShader = curvedShader!!
		glState.shader = curvedShader
		framebuffer.begin()
		glState.blendMode(BlendMode.NONE, premultipliedAlpha = false)
		glState.useViewport(0, 0, framebuffer.width, framebuffer.height) {
			gl.uniform2f(curvedShader.getRequiredUniformLocation("u_cornerRadius"), cornerRadiusX, cornerRadiusY)

			curvedShader.getUniformLocation("u_strokeThickness")?.let {
				gl.uniform2f(it, sX, sY)
			}
			glState.blendMode(BlendMode.NONE, premultipliedAlpha = false)
			val batch = glState.batch
			batch.begin()
			batch.putIdtQuad()
		}
		framebuffer.end()
		glState.shader = previousShader
		if (useCache)
			smoothCornerMap[cacheKey] = disposeOnShutdown(framebuffer)
		framebuffer
	}
	val fBW = framebuffer.width.toFloat()
	val fBH = framebuffer.height.toFloat()
	val pW = cornerRadiusX / fBW
	val pH = cornerRadiusY / fBH
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

	return spriteOut
}

private class CurvedRectShaderProgram(gl: Gl20) : ShaderProgramBase(
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
	vec2 point = gl_FragCoord.xy - gl_PointCoord.xy + 0.5;

	float p = length(point / max(vec2(0.0), u_cornerRadius - 0.2));
	float p2;
	float a1;
	float a2;
	p2 = length(point / (u_cornerRadius + 1.0));
	a1 = smoothstep(p2, p, 1.0);

	p = length(point / max(vec2(0.0), u_cornerRadius - u_strokeThickness - 0.2));
	p2 = length(point / max(vec2(0.0), (u_cornerRadius - u_strokeThickness + 1.3)));
	a2 = 1.0 - smoothstep(p2, p, 1.0);

	float a = min(a1, a2);
	if (a < 0.001) discard;
	gl_FragColor = vec4(1.0, 1.0, 1.0, a);
}""",
		vertexAttributes = mapOf(
				VertexAttributeUsage.POSITION to CommonShaderAttributes.A_POSITION)
)