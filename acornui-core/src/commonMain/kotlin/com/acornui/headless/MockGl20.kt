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

package com.acornui.headless

import com.acornui.gl.core.*
import com.acornui.graphic.Texture
import com.acornui.io.NativeReadBuffer

object MockGl20 : Gl20 {
	override fun activeTexture(texture: Int) {
	}

	override fun attachShader(program: GlProgramRef, shader: GlShaderRef) {
	}

	override fun bindAttribLocation(program: GlProgramRef, index: Int, name: String) {
	}

	override fun bindBuffer(target: Int, buffer: GlBufferRef?) {
	}

	override fun bindFramebuffer(target: Int, framebuffer: GlFramebufferRef?) {
	}

	override fun bindRenderbuffer(target: Int, renderbuffer: GlRenderbufferRef?) {
	}

	override fun bindTexture(target: Int, texture: GlTextureRef?) {
	}

	override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float) {
	}

	override fun blendEquation(mode: Int) {
	}

	override fun blendEquationSeparate(modeRgb: Int, modeAlpha: Int) {
	}

	override fun blendFunc(sfactor: Int, dfactor: Int) {
	}

	override fun blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) {
	}

	override fun bufferData(target: Int, size: Int, usage: Int) {
	}

	override fun bufferDatabv(target: Int, data: NativeReadBuffer<Byte>, usage: Int) {
	}

	override fun bufferDatafv(target: Int, data: NativeReadBuffer<Float>, usage: Int) {
	}

	override fun bufferDatasv(target: Int, data: NativeReadBuffer<Short>, usage: Int) {
	}

	override fun bufferSubDatafv(target: Int, offset: Int, data: NativeReadBuffer<Float>) {
	}

	override fun bufferSubDatasv(target: Int, offset: Int, data: NativeReadBuffer<Short>) {
	}

	override fun checkFramebufferStatus(target: Int): Int = 0

	override fun clear(mask: Int) {
	}

	override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) {
	}

	override fun clearDepth(depth: Float) {
	}

	override fun clearStencil(s: Int) {
	}

	override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
	}

	override fun compileShader(shader: GlShaderRef) {
	}

	override fun copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) {
	}

	override fun copyTexSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, x: Int, y: Int, width: Int, height: Int) {
	}

	override fun createBuffer(): GlBufferRef = object : GlBufferRef {}

	override fun createFramebuffer(): GlFramebufferRef = object : GlFramebufferRef {}

	override fun createProgram(): GlProgramRef = object : GlProgramRef {}

	override fun createRenderbuffer(): GlRenderbufferRef = object : GlRenderbufferRef {}

	override fun createShader(type: Int): GlShaderRef  = object : GlShaderRef {}

	override fun createTexture(): GlTextureRef = object : GlTextureRef {}

	override fun cullFace(mode: Int) {
	}

	override fun deleteBuffer(buffer: GlBufferRef) {
	}

	override fun deleteFramebuffer(framebuffer: GlFramebufferRef) {
	}

	override fun deleteProgram(program: GlProgramRef) {
	}

	override fun deleteRenderbuffer(renderbuffer: GlRenderbufferRef) {
	}

	override fun deleteShader(shader: GlShaderRef) {
	}

	override fun deleteTexture(texture: GlTextureRef) {
	}

	override fun depthFunc(func: Int) {
	}

	override fun depthMask(flag: Boolean) {
	}

	override fun depthRange(zNear: Float, zFar: Float) {
	}

	override fun detachShader(program: GlProgramRef, shader: GlShaderRef) {
	}

	override fun disable(cap: Int) {
	}

	override fun disableVertexAttribArray(index: Int) {
	}

	override fun drawArrays(mode: Int, first: Int, count: Int) {
	}

	override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) {
	}

	override fun enable(cap: Int) {
	}

	override fun enableVertexAttribArray(index: Int) {
	}

	override fun finish() {
	}

	override fun flush() {
	}

	override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbufferTarget: Int, renderbuffer: GlRenderbufferRef) {
	}

	override fun framebufferTexture2D(target: Int, attachment: Int, textureTarget: Int, texture: GlTextureRef, level: Int) {
	}

	override fun frontFace(mode: Int) {
	}

	override fun generateMipmap(target: Int) {
	}

	override fun getActiveAttrib(program: GlProgramRef, index: Int): GlActiveInfoRef = MockGlActiveInfoRef

	override fun getActiveUniform(program: GlProgramRef, index: Int) = MockGlActiveInfoRef

	override fun getAttachedShaders(program: GlProgramRef): Array<GlShaderRef> = emptyArray()

	override fun getAttribLocation(program: GlProgramRef, name: String): Int = 0

	override fun getError(): Int = 0

	override fun getProgramInfoLog(program: GlProgramRef): String? = null

	override fun getShaderInfoLog(shader: GlShaderRef): String? = null

	override fun getUniformLocation(program: GlProgramRef, name: String): GlUniformLocationRef? = null

	override fun hint(target: Int, mode: Int) {
	}

	override fun isBuffer(buffer: GlBufferRef): Boolean = false

	override fun isEnabled(cap: Int): Boolean = false

	override fun isFramebuffer(framebuffer: GlFramebufferRef): Boolean = false

	override fun isProgram(program: GlProgramRef): Boolean = false

	override fun isRenderbuffer(renderbuffer: GlRenderbufferRef): Boolean = false

	override fun isShader(shader: GlShaderRef): Boolean = false

	override fun isTexture(texture: GlTextureRef): Boolean = false

	override fun lineWidth(width: Float) {
	}

	override fun linkProgram(program: GlProgramRef) {
	}

	override fun pixelStorei(pName: Int, param: Int) {
	}

	override fun polygonOffset(factor: Float, units: Float) {
	}

	override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: NativeReadBuffer<Byte>) {
	}

	override fun renderbufferStorage(target: Int, internalFormat: Int, width: Int, height: Int) {
	}

	override fun sampleCoverage(value: Float, invert: Boolean) {
	}

	override fun scissor(x: Int, y: Int, width: Int, height: Int) {
	}

	override fun shaderSource(shader: GlShaderRef, source: String) {
	}

	override fun stencilFunc(func: Int, ref: Int, mask: Int) {
	}

	override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
	}

	override fun stencilMask(mask: Int) {
	}

	override fun stencilMaskSeparate(face: Int, mask: Int) {
	}

	override fun stencilOp(fail: Int, zFail: Int, zPass: Int) {
	}

	override fun stencilOpSeparate(face: Int, fail: Int, zFail: Int, zPass: Int) {
	}

	override fun texImage2Db(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeReadBuffer<Byte>?) {
	}

	override fun texImage2Df(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeReadBuffer<Float>?) {
	}

	override fun texImage2D(target: Int, level: Int, internalFormat: Int, format: Int, type: Int, texture: Texture) {
	}

	override fun texParameterf(target: Int, pName: Int, param: Float) {
	}

	override fun texParameteri(target: Int, pName: Int, param: Int) {
	}

	override fun texSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, format: Int, type: Int, texture: Texture) {
	}

	override fun uniform1f(location: GlUniformLocationRef, x: Float) {
	}

	override fun uniform1fv(location: GlUniformLocationRef, v: FloatArray) {
	}

	override fun uniform1i(location: GlUniformLocationRef, x: Int) {
	}

	override fun uniform1iv(location: GlUniformLocationRef, v: IntArray) {
	}

	override fun uniform2f(location: GlUniformLocationRef, x: Float, y: Float) {
	}

	override fun uniform2fv(location: GlUniformLocationRef, v: FloatArray) {
	}

	override fun uniform2i(location: GlUniformLocationRef, x: Int, y: Int) {
	}

	override fun uniform2iv(location: GlUniformLocationRef, v: IntArray) {
	}

	override fun uniform3f(location: GlUniformLocationRef, x: Float, y: Float, z: Float) {
	}

	override fun uniform3fv(location: GlUniformLocationRef, v: FloatArray) {
	}

	override fun uniform3i(location: GlUniformLocationRef, x: Int, y: Int, z: Int) {
	}

	override fun uniform3iv(location: GlUniformLocationRef, v: IntArray) {
	}

	override fun uniform4f(location: GlUniformLocationRef, x: Float, y: Float, z: Float, w: Float) {
	}

	override fun uniform4fv(location: GlUniformLocationRef, v: FloatArray) {
	}

	override fun uniform4i(location: GlUniformLocationRef, x: Int, y: Int, z: Int, w: Int) {
	}

	override fun uniform4iv(location: GlUniformLocationRef, v: IntArray) {
	}

	override fun uniformMatrix2fv(location: GlUniformLocationRef, transpose: Boolean, value: FloatArray) {
	}

	override fun uniformMatrix3fv(location: GlUniformLocationRef, transpose: Boolean, value: FloatArray) {
	}

	override fun uniformMatrix4fv(location: GlUniformLocationRef, transpose: Boolean, value: FloatArray) {
	}

	override fun useProgram(program: GlProgramRef?) {
	}

	override fun validateProgram(program: GlProgramRef) {
	}

	override fun vertexAttrib1f(index: Int, x: Float) {
	}

	override fun vertexAttrib1fv(index: Int, values: FloatArray) {
	}

	override fun vertexAttrib2f(index: Int, x: Float, y: Float) {
	}

	override fun vertexAttrib2fv(index: Int, values: FloatArray) {
	}

	override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float) {
	}

	override fun vertexAttrib3fv(index: Int, values: FloatArray) {
	}

	override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) {
	}

	override fun vertexAttrib4fv(index: Int, values: FloatArray) {
	}

	override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
	}

	override fun viewport(x: Int, y: Int, width: Int, height: Int) {
	}

	override fun getUniformb(program: GlProgramRef, location: GlUniformLocationRef): Boolean = false

	override fun getUniformi(program: GlProgramRef, location: GlUniformLocationRef): Int = 0

	override fun getUniformiv(program: GlProgramRef, location: GlUniformLocationRef, out: IntArray): IntArray = out

	override fun getUniformf(program: GlProgramRef, location: GlUniformLocationRef): Float = 0f

	override fun getUniformfv(program: GlProgramRef, location: GlUniformLocationRef, out: FloatArray): FloatArray = out

	override fun getVertexAttribi(index: Int, pName: Int): Int = 0

	override fun getVertexAttribb(index: Int, pName: Int): Boolean = false

	override fun getTexParameter(target: Int, pName: Int): Int = 0

	override fun getShaderParameterb(shader: GlShaderRef, pName: Int): Boolean = false

	override fun getShaderParameteri(shader: GlShaderRef, pName: Int): Int = 0

	override fun getRenderbufferParameter(target: Int, pName: Int): Int = 0

	override fun getParameterb(pName: Int): Boolean = false

	override fun getParameterbv(pName: Int, out: BooleanArray): BooleanArray = out

	override fun getParameteri(pName: Int): Int = 0

	override fun getParameteriv(pName: Int, out: IntArray): IntArray = out

	override fun getParameterf(pName: Int): Float = 0f

	override fun getParameterfv(pName: Int, out: FloatArray): FloatArray = out

	override fun getProgramParameterb(program: GlProgramRef, pName: Int): Boolean = false

	override fun getProgramParameteri(program: GlProgramRef, pName: Int): Int = 0

	override fun getBufferParameter(target: Int, pName: Int): Int = 0

	override fun getFramebufferAttachmentParameteri(target: Int, attachment: Int, pName: Int): Int = 0

	override fun getSupportedExtensions(): List<String> = emptyList()
}


object MockGlActiveInfoRef : GlActiveInfoRef {
	override var name: String = ""
	override var size: Int = 0
	override var type: Int = 0
}