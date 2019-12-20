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

package com.acornui.gl.core

import com.acornui.Disposable
import com.acornui.system.userInfo

interface ShaderProgram : Disposable {

	val program: GlProgramRef
}

object CommonShaderAttributes {

	const val A_POSITION: String = "a_position"
	const val A_NORMAL: String = "a_normal"
	const val A_TANGENT: String = "a_tangent"
	const val A_BITANGENT: String = "a_bitangent"
	const val A_COLOR_TINT: String = "a_colorTint"
	const val A_TEXTURE_COORD: String = "a_texCoord"
}

object CommonShaderUniforms {

	const val U_PROJ_TRANS: String = "u_projTrans"
	const val U_MODEL_TRANS: String = "u_modelTrans"
	const val U_VIEW_TRANS: String = "u_viewTrans"
	const val U_NORMAL_TRANS: String = "u_normalTrans"
	const val U_TEXTURE: String = "u_texture"
	const val U_TEXTURE_NORMAL: String = "u_textureNormal"

	const val U_COLOR_TRANS: String = "u_colorTrans"
	const val U_COLOR_OFFSET: String = "u_colorOffset"
	const val U_USE_COLOR_TRANS: String = "u_useColorTrans"
}

/**
 * @author nbilyk
 */
abstract class ShaderProgramBase(
		val gl: CachedGl20,
		vertexShaderSrc: String,
		fragmentShaderSrc: String,
		val vertexAttributes: Map<Int, String> = mapOf(
				VertexAttributeLocation.POSITION to CommonShaderAttributes.A_POSITION,
				VertexAttributeLocation.NORMAL to CommonShaderAttributes.A_NORMAL,
				VertexAttributeLocation.COLOR_TINT to CommonShaderAttributes.A_COLOR_TINT,
				VertexAttributeLocation.TEXTURE_COORD to CommonShaderAttributes.A_TEXTURE_COORD + "0"
		)
) : ShaderProgram {

	final override var program: GlProgramRef = gl.createProgram()
		private set

	private val vertexShader: GlShaderRef
	private val fragmentShader: GlShaderRef

	init {
		// Create the shader program
		vertexShader = compileShader(vertexShaderSrc, Gl20.VERTEX_SHADER)
		fragmentShader = compileShader(fragmentShaderSrc, Gl20.FRAGMENT_SHADER)
		for ((location, name) in vertexAttributes) {
			gl.bindAttribLocation(program, location, name)
		}
		gl.linkProgram(program)
		if (!gl.getProgramParameterb(program, Gl20.LINK_STATUS)) {
			throw Exception("Could not link shader.")
		}

		gl.useProgram(program) {
			gl.uniforms.putOptional(CommonShaderUniforms.U_TEXTURE, 0)
			gl.uniforms.putOptional(CommonShaderUniforms.U_TEXTURE_NORMAL, 1)
			@Suppress("LeakingThis")
			initUniforms(gl.uniforms)
		}
	}

	private fun compileShader(shaderSrc: String, shaderType: Int): GlShaderRef {
		val gl = gl
		val shader = gl.createShader(shaderType)
		gl.shaderSource(shader, shaderSrc)
		gl.compileShader(shader)
		if (!gl.getShaderParameterb(shader, Gl20.COMPILE_STATUS)) {
			val log = gl.getShaderInfoLog(shader)
			throw ShaderCompileException(log ?: "[Unknown]")
		}
		gl.attachShader(program, shader)
		return shader
	}

	protected open fun initUniforms(uniforms: Uniforms) {}

	override fun dispose() {
		val gl = gl
		if (gl.program == program)
			gl.useProgram(null)
		gl.deleteShader(vertexShader)
		gl.deleteShader(fragmentShader)
		gl.deleteProgram(program)
	}

}

class ShaderCompileException(message: String) : Throwable(message)


class DefaultShaderProgram(gl: CachedGl20) : ShaderProgramBase(
		gl, vertexShaderSrc = """

$DEFAULT_SHADER_HEADER

attribute vec3 a_position;
attribute vec4 a_colorTint;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_colorTint;
varying vec2 v_texCoord;

void main() {
	v_colorTint = a_colorTint;
	v_texCoord = a_texCoord0;
	gl_Position =  u_projTrans * vec4(a_position, 1.0);
}""",
		fragmentShaderSrc = """

$DEFAULT_SHADER_HEADER

varying LOW_P vec4 v_colorTint;
varying HIGH_P vec2 v_texCoord;

uniform sampler2D u_texture;

uniform bool u_useColorTrans;
uniform mat4 u_colorTrans;
uniform vec4 u_colorOffset;

void main() {
	vec4 final = v_colorTint * texture2D(u_texture, v_texCoord);
	if (u_useColorTrans) {
		gl_FragColor = u_colorTrans * final + u_colorOffset;
	} else {
		gl_FragColor = final;
	}
	if (gl_FragColor.a < 0.01) discard;
}""",
		vertexAttributes = mapOf(
				VertexAttributeLocation.POSITION to CommonShaderAttributes.A_POSITION,
				VertexAttributeLocation.COLOR_TINT to CommonShaderAttributes.A_COLOR_TINT,
				VertexAttributeLocation.TEXTURE_COORD to CommonShaderAttributes.A_TEXTURE_COORD + "0")
) {
	override fun initUniforms(uniforms: Uniforms) {
	}
}

val standardVertexAttributes = VertexAttributes(listOf(
		VertexAttribute(3, false, Gl20.FLOAT, VertexAttributeLocation.POSITION),
		VertexAttribute(3, false, Gl20.FLOAT, VertexAttributeLocation.NORMAL),
		VertexAttribute(4, false, Gl20.FLOAT, VertexAttributeLocation.COLOR_TINT),
		VertexAttribute(2, false, Gl20.FLOAT, VertexAttributeLocation.TEXTURE_COORD))
)

val DEFAULT_SHADER_HEADER: String
	get() = """
#version ${if (userInfo.isDesktop) "120" else "100"}
#ifdef GL_ES
#define LOW_P lowp
#define MED_P mediump
#define HIGH_P highp
precision mediump float;
#else
#define MED_P
#define LOW_P
#define HIGH_P
#endif
"""
