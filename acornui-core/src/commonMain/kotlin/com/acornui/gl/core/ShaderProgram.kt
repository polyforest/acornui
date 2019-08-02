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

import com.acornui.collection.stringMapOf
import com.acornui.Disposable
import com.acornui.system.userInfo


interface ShaderProgram : Disposable {

	val program: GlProgramRef

	fun getAttributeLocationByUsage(usage: Int): Int
	fun getAttributeLocation(name: String): Int
	fun getUniformLocation(name: String): GlUniformLocationRef?

	fun getRequiredUniformLocation(name: String): GlUniformLocationRef {
		return getUniformLocation(name) ?: throw Exception("Uniform not found $name")
	}

	fun bind()
	fun unbind()
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
		val gl: Gl20,
		vertexShaderSrc: String,
		fragmentShaderSrc: String,
		private val vertexAttributes: Map<Int, String> = mapOf(
				VertexAttributeUsage.POSITION to CommonShaderAttributes.A_POSITION,
				VertexAttributeUsage.NORMAL to CommonShaderAttributes.A_NORMAL,
				VertexAttributeUsage.COLOR_TINT to CommonShaderAttributes.A_COLOR_TINT,
				VertexAttributeUsage.TEXTURE_COORD to CommonShaderAttributes.A_TEXTURE_COORD + "0"
		)
) : ShaderProgram {

	private val _program: GlProgramRef = gl.createProgram()
	private val vertexShader: GlShaderRef
	private val fragmentShader: GlShaderRef

	private val uniformLocationCache = stringMapOf<GlUniformLocationRef?>()
	private val attributeLocationCache = stringMapOf<Int>()

	init {
		// Create the shader program
		vertexShader = compileShader(vertexShaderSrc, Gl20.VERTEX_SHADER)
		fragmentShader = compileShader(fragmentShaderSrc, Gl20.FRAGMENT_SHADER)
		gl.linkProgram(_program)
		if (!gl.getProgramParameterb(_program, Gl20.LINK_STATUS)) {
			throw Exception("Could not link shader.")
		}
	}

	override val program: GlProgramRef
		get() = _program

	override fun bind() {
		gl.useProgram(_program)
	}

	override fun unbind() {
		gl.useProgram(null)
	}

	override fun getAttributeLocationByUsage(usage: Int): Int {
		val name = vertexAttributes[usage] ?: return -1
		return getAttributeLocation(name)
	}

	override fun getAttributeLocation(name: String): Int {
		if (!attributeLocationCache.containsKey(name)) {
			attributeLocationCache[name] = gl.getAttribLocation(_program, name)
		}
		return attributeLocationCache[name]!!
	}

	override fun getUniformLocation(name: String): GlUniformLocationRef? {
		if (!uniformLocationCache.containsKey(name)) {
			uniformLocationCache[name] = gl.getUniformLocation(_program, name)
		}
		return uniformLocationCache[name]
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
		gl.attachShader(_program, shader)
		return shader
	}

	override fun dispose() {
		val gl = gl
		gl.deleteShader(vertexShader)
		gl.deleteShader(fragmentShader)
		gl.deleteProgram(_program)
	}


}

class ShaderCompileException(message: String) : Throwable(message)


class DefaultShaderProgram(gl: Gl20) : ShaderProgramBase(
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
				VertexAttributeUsage.POSITION to CommonShaderAttributes.A_POSITION,
				VertexAttributeUsage.COLOR_TINT to CommonShaderAttributes.A_COLOR_TINT,
				VertexAttributeUsage.TEXTURE_COORD to CommonShaderAttributes.A_TEXTURE_COORD + "0")
) {

	override fun bind() {
		super.bind()
		gl.uniform1i(getUniformLocation(CommonShaderUniforms.U_TEXTURE)!!, 0)  // set the fragment shader's texture to unit 0
	}

}

val uiVertexAttributes = VertexAttributes(listOf(
		VertexAttribute(3, false, Gl20.FLOAT, VertexAttributeUsage.POSITION),
		VertexAttribute(4, false, Gl20.FLOAT, VertexAttributeUsage.COLOR_TINT),
		VertexAttribute(2, false, Gl20.FLOAT, VertexAttributeUsage.TEXTURE_COORD))
)

val standardVertexAttributes = VertexAttributes(listOf(
		VertexAttribute(3, false, Gl20.FLOAT, VertexAttributeUsage.POSITION),
		VertexAttribute(3, false, Gl20.FLOAT, VertexAttributeUsage.NORMAL),
		VertexAttribute(4, false, Gl20.FLOAT, VertexAttributeUsage.COLOR_TINT),
		VertexAttribute(2, false, Gl20.FLOAT, VertexAttributeUsage.TEXTURE_COORD))
)

val DEFAULT_SHADER_HEADER: String
		get() = """
#version ${if (userInfo.isDesktop) "120" else "100"}
#ifdef GL_ES
#define LOW_P lowp
#define MED_P mediump
#define HIGH_P highp
precision lowp float;
#else
#define MED_P
#define LOW_P
#define HIGH_P
#endif
"""
