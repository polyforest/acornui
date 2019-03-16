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

import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.graphic.BlendMode
import com.acornui.gl.core.*
import com.acornui.math.ceil


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
		spriteOut: Sprite = Sprite(),
		useCache: Boolean = true
): Sprite {
	if (cornerRadiusX <= 0.0001f || cornerRadiusY <= 0.0001f) {
		spriteOut.clear()
		return spriteOut
	}
	val cacheKey = SmoothCornerKey(cornerRadiusX, cornerRadiusY, strokeThicknessX, strokeThicknessY)
	val framebuffer = if (useCache && smoothCornerMap.containsKey(cacheKey)) {
		smoothCornerMap[cacheKey]!!
	} else {
		val gl = inject(Gl20)
		val glState = inject(GlState)
		if (curvedShader == null) curvedShader = CurvedRectShaderProgram(gl)
		val framebuffer = framebuffer(cornerRadiusX.ceil(), cornerRadiusY.ceil())
		val fBW = framebuffer.width.toFloat()
		val fBH = framebuffer.height.toFloat()
		val pW = cornerRadiusX / fBW
		val pH = cornerRadiusY / fBH
		val previousShader = glState.shader
		glState.shader = curvedShader!!
		framebuffer.drawTo {
			gl.uniform2f(curvedShader!!.getRequiredUniformLocation("u_resolutionInv"), 1.0f / fBW, 1.0f / fBH)
			gl.uniform2f(curvedShader!!.getRequiredUniformLocation("u_strokeThickness"), strokeThicknessX ?: fBW, strokeThicknessY ?: fBH)
			glState.blendMode(BlendMode.NONE, premultipliedAlpha = false)
			val batch = glState.batch
			batch.begin()
			val w = pW * 2f - 1f
			val h = pH * 2f - 1f
			batch.putVertex(-1f, -1f, 0f, u = 0f, v = 0f)
			batch.putVertex(w, -1f, -1f, u = 1f, v = 0f)
			batch.putVertex(w, h, -1f, u = 1f, v = 1f)
			batch.putVertex(-1f, h, -1f, u = 0f, v = 1f)
			batch.putQuadIndices()
		}
		glState.shader = previousShader
		if (useCache) smoothCornerMap[cacheKey] = framebuffer
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
attribute vec2 a_texCoord0;

varying vec2 v_texCoord;

void main() {
	v_texCoord = a_texCoord0;
	gl_Position =  vec4(a_position, 1.0);
}""",
		fragmentShaderSrc = """

$DEFAULT_SHADER_HEADER

uniform vec2 u_strokeThickness;
uniform vec2 u_resolutionInv;
varying vec2 v_texCoord;

void main() {
	float halfStroke = u_strokeThickness.x * 0.5 * u_resolutionInv.x;
	float dist = abs(length(v_texCoord) - 1.0 + halfStroke);
	float smooth = u_resolutionInv.x * 0.8;
	gl_FragColor = vec4(1.0, 1.0, 1.0, smoothstep(halfStroke + smooth, halfStroke - smooth, dist));
}""",
		vertexAttributes = mapOf(
				VertexAttributeUsage.POSITION to CommonShaderAttributes.A_POSITION,
				VertexAttributeUsage.TEXTURE_COORD to CommonShaderAttributes.A_TEXTURE_COORD + "0")
)