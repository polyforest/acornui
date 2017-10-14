/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui.core.Disposable
import com.acornui.graphics.ColorRo
import com.acornui.io.ReadBuffer
import com.acornui.io.WriteBuffer
import com.acornui.math.Vector3Ro


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

	companion object {
		// Some naming conventions for attributes and uniforms:
		val A_POSITION: String = "a_position"
		val A_NORMAL: String = "a_normal"
		val A_COLOR_TINT: String = "a_colorTint"
		val A_TEXTURE_COORD: String = "a_texCoord"

		val U_PROJ_TRANS: String = "u_projTrans"
		val U_MODEL_TRANS: String = "u_modelTrans"
		val U_COLOR_TRANS: String = "u_colorTrans"
		val U_COLOR_OFFSET: String = "u_colorOffset"
		val U_TEXTURE: String = "u_texture"
	}

}

/**
 * @author nbilyk
 */
abstract class ShaderProgramBase(
		val gl: Gl20,
		vertexShaderSrc: String,
		fragmentShaderSrc: String,
		private val vertexAttributes: Map<Int, String> = hashMapOf(
				Pair(VertexAttributeUsage.POSITION, ShaderProgram.A_POSITION),
				Pair(VertexAttributeUsage.NORMAL, ShaderProgram.A_NORMAL),
				Pair(VertexAttributeUsage.COLOR_TINT, ShaderProgram.A_COLOR_TINT),
				Pair(VertexAttributeUsage.TEXTURE_COORD, ShaderProgram.A_TEXTURE_COORD + "0")
		)
) : ShaderProgram {

	protected val _program: GlProgramRef = gl.createProgram()
	private val vertexShader: GlShaderRef
	private val fragmentShader: GlShaderRef

	private val uniformLocationCache = HashMap<String, GlUniformLocationRef?>()
	private val attributeLocationCache = HashMap<String, Int>()

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


class DefaultShaderProgram(gl: Gl20) : ShaderProgramBase (
		gl, vertexShaderSrc = """
attribute vec4 a_position;
attribute vec4 a_colorTint;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_colorTint;
varying vec2 v_texCoord;

void main() {
	v_colorTint = a_colorTint;
	v_texCoord = a_texCoord0;
	gl_Position =  u_projTrans * a_position;
}""",
		fragmentShaderSrc = """
#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying LOWP vec4 v_colorTint;
varying vec2 v_texCoord;

uniform sampler2D u_texture;

void main() {
	gl_FragColor = v_colorTint * texture2D(u_texture, v_texCoord);
}"""
) {

	override fun bind() {
		super.bind()
		gl.uniform1i(getUniformLocation(ShaderProgram.U_TEXTURE)!!, 0);  // set the fragment shader's texture to unit 0
	}

}

val standardVertexAttributes = object : VertexAttributes(listOf(
		VertexAttribute(3, false, Gl20.FLOAT, VertexAttributeUsage.POSITION),
		VertexAttribute(3, false, Gl20.FLOAT, VertexAttributeUsage.NORMAL),
		VertexAttribute(4, false, Gl20.FLOAT, VertexAttributeUsage.COLOR_TINT),
		VertexAttribute(2, false, Gl20.FLOAT, VertexAttributeUsage.TEXTURE_COORD))
) {

	override fun getVertex(vertexData: ReadBuffer<Float>, out: Vertex) {
		vertexData.apply {
			out.position.x = get()
			out.position.y = get()
			out.position.z = get()
			out.normal.x = get()
			out.normal.y = get()
			out.normal.z = get()
			out.colorTint.r = get()
			out.colorTint.g = get()
			out.colorTint.b = get()
			out.colorTint.a = get()
			out.u = get()
			out.v = get()
		}
	}

	override fun putVertex(vertexData: WriteBuffer<Float>, position: Vector3Ro, normal: Vector3Ro, colorTint: ColorRo, u: Float, v: Float) {
		vertexData.apply {
			put(position.x)
			put(position.y)
			put(position.z)
			put(normal.x)
			put(normal.y)
			put(normal.z)
			put(colorTint.r)
			put(colorTint.g)
			put(colorTint.b)
			put(colorTint.a)
			put(u)
			put(v)
		}
	}
}