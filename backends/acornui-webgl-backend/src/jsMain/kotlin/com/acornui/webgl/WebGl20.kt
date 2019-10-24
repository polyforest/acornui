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

package com.acornui.webgl

import com.acornui.collection.FloatArrayListRo
import com.acornui.collection.IntArrayListRo
import com.acornui.gl.core.*
import com.acornui.graphic.Texture
import com.acornui.io.NativeReadBuffer
import org.khronos.webgl.*

/**
 * @author nbilyk
 */
class WebGl20(private val context: WebGLRenderingContext) : Gl20 {

	override fun activeTexture(texture: Int) {
		context.activeTexture(texture)
	}

	override fun attachShader(program: GlProgramRef, shader: GlShaderRef) {
		context.attachShader((program as WebGlProgramRef).o, (shader as WebGlShaderRef).o)
	}

	override fun bindAttribLocation(program: GlProgramRef, index: Int, name: String) {
		context.bindAttribLocation((program as WebGlProgramRef).o, index, name)
	}

	override fun bindBuffer(target: Int, buffer: GlBufferRef?) {
		context.bindBuffer(target, (buffer as? WebGlBufferRef)?.o)
	}

	override fun bindFramebuffer(target: Int, framebuffer: GlFramebufferRef?) {
		context.bindFramebuffer(target, (framebuffer as? WebGlFramebufferRef)?.o)
	}

	override fun bindRenderbuffer(target: Int, renderbuffer: GlRenderbufferRef?) {
		context.bindRenderbuffer(target, (renderbuffer as? WebGlRenderbufferRef)?.o)
	}

	override fun bindTexture(target: Int, texture: GlTextureRef?) {
		context.bindTexture(target, (texture as? WebGlTextureRef)?.o)
	}

	override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float) {
		context.blendColor(red, green, blue, alpha)
	}

	override fun blendEquation(mode: Int) {
		context.blendEquation(mode)
	}

	override fun blendEquationSeparate(modeRgb: Int, modeAlpha: Int) {
		context.blendEquationSeparate(modeRgb, modeAlpha)
	}

	override fun blendFunc(sfactor: Int, dfactor: Int) {
		context.blendFunc(sfactor, dfactor)
	}

	override fun blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) {
		context.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
	}

	override fun bufferData(target: Int, size: Int, usage: Int) {
		context.bufferData(target, size, usage)
	}

	override fun bufferDatabv(target: Int, data: NativeReadBuffer<Byte>, usage: Int) {
		@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
		context.bufferData(target, data.native as BufferDataSource, usage)
	}

	override fun bufferDatafv(target: Int, data: NativeReadBuffer<Float>, usage: Int) {
		@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
		context.bufferData(target, data.native as BufferDataSource, usage)
	}

	override fun bufferDatasv(target: Int, data: NativeReadBuffer<Short>, usage: Int) {
		@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
		context.bufferData(target, data.native as BufferDataSource, usage)
	}

	override fun bufferSubDatafv(target: Int, offset: Int, data: NativeReadBuffer<Float>) {
		@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
		context.bufferSubData(target, offset, data.native as BufferDataSource)
	}

	override fun bufferSubDatasv(target: Int, offset: Int, data: NativeReadBuffer<Short>) {
		@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
		context.bufferSubData(target, offset, data.native as BufferDataSource)
	}

	override fun checkFramebufferStatus(target: Int): Int {
		return context.checkFramebufferStatus(target)
	}

	override fun clear(mask: Int) {
		context.clear(mask)
	}

	override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) {
		context.clearColor(red, green, blue, alpha)
	}

	override fun clearDepth(depth: Float) {
		context.clearDepth(depth)
	}

	override fun clearStencil(s: Int) {
		context.clearStencil(s)
	}

	override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
		context.colorMask(red, green, blue, alpha)
	}

	override fun compileShader(shader: GlShaderRef) {
		context.compileShader((shader as WebGlShaderRef).o)
	}

	override fun copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) {
		context.copyTexImage2D(target, level, internalFormat, x, y, width, height, border)
	}

	override fun copyTexSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, x: Int, y: Int, width: Int, height: Int) {
		context.copyTexSubImage2D(target, level, xOffset, yOffset, x, y, width, height)
	}

	override fun createBuffer(): GlBufferRef {
		return WebGlBufferRef(context.createBuffer()!!)
	}

	override fun createFramebuffer(): GlFramebufferRef {
		return WebGlFramebufferRef(context.createFramebuffer()!!)
	}

	override fun createProgram(): GlProgramRef {
		return WebGlProgramRef(context.createProgram()!!)
	}

	override fun createRenderbuffer(): GlRenderbufferRef {
		return WebGlRenderbufferRef(context.createRenderbuffer()!!)
	}

	override fun createShader(type: Int): GlShaderRef {
		return WebGlShaderRef(context.createShader(type)!!)
	}

	override fun createTexture(): GlTextureRef {
		return WebGlTextureRef(context.createTexture()!!)
	}

	override fun cullFace(mode: Int) {
		context.cullFace(mode)
	}

	override fun deleteBuffer(buffer: GlBufferRef) {
		context.deleteBuffer((buffer as WebGlBufferRef).o)
	}

	override fun deleteFramebuffer(framebuffer: GlFramebufferRef) {
		context.deleteFramebuffer((framebuffer as WebGlFramebufferRef).o)
	}

	override fun deleteProgram(program: GlProgramRef) {
		context.deleteProgram((program as WebGlProgramRef).o)
	}

	override fun deleteRenderbuffer(renderbuffer: GlRenderbufferRef) {
		context.deleteRenderbuffer((renderbuffer as WebGlRenderbufferRef).o)
	}

	override fun deleteShader(shader: GlShaderRef) {
		context.deleteShader((shader as WebGlShaderRef).o)
	}

	override fun deleteTexture(texture: GlTextureRef) {
		context.deleteTexture((texture as WebGlTextureRef).o)
	}

	override fun depthFunc(func: Int) {
		context.depthFunc(func)
	}

	override fun depthMask(flag: Boolean) {
		context.depthMask(flag)
	}

	override fun depthRange(zNear: Float, zFar: Float) {
		context.depthRange(zNear, zFar)
	}

	override fun detachShader(program: GlProgramRef, shader: GlShaderRef) {
		context.detachShader((program as WebGlProgramRef).o, (shader as WebGlShaderRef).o)
	}

	override fun disable(cap: Int) {
		context.disable(cap)
	}

	override fun disableVertexAttribArray(index: Int) {
		context.disableVertexAttribArray(index)
	}

	override fun drawArrays(mode: Int, first: Int, count: Int) {
		context.drawArrays(mode, first, count)
	}

	override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) {
		context.drawElements(mode, count, type, offset)
	}

	override fun enable(cap: Int) {
		context.enable(cap)
	}

	override fun enableVertexAttribArray(index: Int) {
		context.enableVertexAttribArray(index)
	}

	override fun finish() {
		context.finish()
	}

	override fun flush() {
		context.flush()
	}

	override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbufferTarget: Int, renderbuffer: GlRenderbufferRef) {
		context.framebufferRenderbuffer(target, attachment, renderbufferTarget, (renderbuffer as WebGlRenderbufferRef).o)
	}

	override fun framebufferTexture2D(target: Int, attachment: Int, textureTarget: Int, texture: GlTextureRef, level: Int) {
		context.framebufferTexture2D(target, attachment, textureTarget, (texture as WebGlTextureRef).o, level)
	}

	override fun frontFace(mode: Int) {
		context.frontFace(mode)
	}

	override fun generateMipmap(target: Int) {
		context.generateMipmap(target)
	}

	override fun getActiveAttrib(program: GlProgramRef, index: Int): GlActiveInfoRef {
		return WebGlActiveInfoRef(context.getActiveAttrib((program as WebGlProgramRef).o, index)!!)
	}

	override fun getActiveUniform(program: GlProgramRef, index: Int): GlActiveInfoRef {
		return WebGlActiveInfoRef(context.getActiveUniform((program as WebGlProgramRef).o, index)!!)
	}

	override fun getAttachedShaders(program: GlProgramRef): Array<GlShaderRef> {
		val src = context.getAttachedShaders((program as WebGlProgramRef).o) ?: emptyArray()
		val out = Array<GlShaderRef>(src.size, { WebGlShaderRef(src[it]) })
		return out
	}

	override fun getAttribLocation(program: GlProgramRef, name: String): Int {
		return context.getAttribLocation((program as WebGlProgramRef).o, name)
	}

	override fun getError(): Int {
		return context.getError()
	}

	override fun getProgramInfoLog(program: GlProgramRef): String? {
		return context.getProgramInfoLog((program as WebGlProgramRef).o)
	}

	override fun getShaderInfoLog(shader: GlShaderRef): String? {
		return context.getShaderInfoLog((shader as WebGlShaderRef).o)
	}

	//	override fun getShaderSource(shader: GlShader): String {
	//		return context.getShaderSource((shader as WebGlShader).o)
	//	}

	override fun getUniformLocation(program: GlProgramRef, name: String): GlUniformLocationRef? {
		val uniformLocation = context.getUniformLocation((program as WebGlProgramRef).o, name) ?: return null
		return WebGlUniformLocationRef(uniformLocation)
	}

	override fun hint(target: Int, mode: Int) {
		return context.hint(target, mode)
	}

	override fun isBuffer(buffer: GlBufferRef): Boolean {
		return context.isBuffer((buffer as WebGlBufferRef).o)
	}

	override fun isEnabled(cap: Int): Boolean {
		return context.isEnabled(cap)
	}

	override fun isFramebuffer(framebuffer: GlFramebufferRef): Boolean {
		return context.isFramebuffer((framebuffer as WebGlFramebufferRef).o)
	}

	override fun isProgram(program: GlProgramRef): Boolean {
		return context.isProgram((program as WebGlProgramRef).o)
	}

	override fun isRenderbuffer(renderbuffer: GlRenderbufferRef): Boolean {
		return context.isRenderbuffer((renderbuffer as WebGlRenderbufferRef).o)
	}

	override fun isShader(shader: GlShaderRef): Boolean {
		return context.isShader((shader as WebGlShaderRef).o)
	}

	override fun isTexture(texture: GlTextureRef): Boolean {
		return context.isTexture((texture as WebGlTextureRef).o)
	}

	override fun lineWidth(width: Float) {
		context.lineWidth(width)
	}

	override fun linkProgram(program: GlProgramRef) {
		context.linkProgram((program as WebGlProgramRef).o)
	}

	override fun pixelStorei(pName: Int, param: Int) {
		context.pixelStorei(pName, param)
	}

	override fun polygonOffset(factor: Float, units: Float) {
		context.polygonOffset(factor, units)
	}

	override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: NativeReadBuffer<Byte>) {
		@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
		context.readPixels(x, y, width, height, format, type, pixels.native as ArrayBufferView)
	}

	override fun renderbufferStorage(target: Int, internalFormat: Int, width: Int, height: Int) {
		context.renderbufferStorage(target, internalFormat, width, height)
	}

	override fun sampleCoverage(value: Float, invert: Boolean) {
		context.sampleCoverage(value, invert)
	}

	override fun scissor(x: Int, y: Int, width: Int, height: Int) {
		context.scissor(x, y, width, height)
	}

	override fun shaderSource(shader: GlShaderRef, source: String) {
		context.shaderSource((shader as WebGlShaderRef).o, source)
	}

	override fun stencilFunc(func: Int, ref: Int, mask: Int) {
		context.stencilFunc(func, ref, mask)
	}

	override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
		context.stencilFuncSeparate(face, func, ref, mask)
	}

	override fun stencilMask(mask: Int) {
		context.stencilMask(mask)
	}

	override fun stencilMaskSeparate(face: Int, mask: Int) {
		context.stencilMaskSeparate(face, mask)
	}

	override fun stencilOp(fail: Int, zFail: Int, zPass: Int) {
		context.stencilOp(fail, zFail, zPass)
	}

	override fun stencilOpSeparate(face: Int, fail: Int, zFail: Int, zPass: Int) {
		context.stencilOpSeparate(face, fail, zFail, zPass)
	}

	override fun texImage2Db(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeReadBuffer<Byte>?) {
		@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
		context.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels?.native as ArrayBufferView?)
	}

	override fun texImage2Df(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeReadBuffer<Float>?) {
		@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
		context.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels?.native as ArrayBufferView?)
	}

	override fun texImage2D(target: Int, level: Int, internalFormat: Int, format: Int, type: Int, texture: Texture) {
		context.texImage2D(target, level, internalFormat, format, type, (texture as WebGlTexture).image)
	}

	override fun texParameterf(target: Int, pName: Int, param: Float) {
		context.texParameterf(target, pName, param)
	}

	override fun texParameteri(target: Int, pName: Int, param: Int) {
		context.texParameteri(target, pName, param)
	}

	override fun texSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, format: Int, type: Int, texture: Texture) {
		context.texSubImage2D(target, level, xOffset, yOffset, format, type, (texture as WebGlTexture).image)
	}

	override fun uniform1f(location: GlUniformLocationRef, x: Float) {
		context.uniform1f((location as WebGlUniformLocationRef).o, x)
	}

	override fun uniform1fv(location: GlUniformLocationRef, v: FloatArrayListRo) {
		context.uniform1fv((location as WebGlUniformLocationRef).o, v.asNative2())
	}

	override fun uniform1i(location: GlUniformLocationRef, x: Int) {
		context.uniform1i((location as WebGlUniformLocationRef).o, x)
	}

	override fun uniform1iv(location: GlUniformLocationRef, v: IntArrayListRo) {
		context.uniform1iv((location as WebGlUniformLocationRef).o, v.asNative2())
	}

	override fun uniform2f(location: GlUniformLocationRef, x: Float, y: Float) {
		context.uniform2f((location as WebGlUniformLocationRef).o, x, y)
	}

	override fun uniform2fv(location: GlUniformLocationRef, v: FloatArrayListRo) {
		context.uniform2fv((location as WebGlUniformLocationRef).o, v.asNative2())
	}

	override fun uniform2i(location: GlUniformLocationRef, x: Int, y: Int) {
		context.uniform2i((location as WebGlUniformLocationRef).o, x, y)
	}

	override fun uniform2iv(location: GlUniformLocationRef, v: IntArrayListRo) {
		context.uniform2iv((location as WebGlUniformLocationRef).o, v.asNative2())
	}

	override fun uniform3f(location: GlUniformLocationRef, x: Float, y: Float, z: Float) {
		context.uniform3f((location as WebGlUniformLocationRef).o, x, y, z)
	}

	override fun uniform3fv(location: GlUniformLocationRef, v: FloatArrayListRo) {
		context.uniform3fv((location as WebGlUniformLocationRef).o, v.asNative2())
	}

	override fun uniform3i(location: GlUniformLocationRef, x: Int, y: Int, z: Int) {
		context.uniform3i((location as WebGlUniformLocationRef).o, x, y, z)
	}

	override fun uniform3iv(location: GlUniformLocationRef, v: IntArrayListRo) {
		context.uniform3iv((location as WebGlUniformLocationRef).o, v.asNative2())
	}

	override fun uniform4f(location: GlUniformLocationRef, x: Float, y: Float, z: Float, w: Float) {
		context.uniform4f((location as WebGlUniformLocationRef).o, x, y, z, w)
	}

	override fun uniform4fv(location: GlUniformLocationRef, v: FloatArrayListRo) {
		context.uniform4fv((location as WebGlUniformLocationRef).o, v.asNative2())
	}

	override fun uniform4i(location: GlUniformLocationRef, x: Int, y: Int, z: Int, w: Int) {
		context.uniform4i((location as WebGlUniformLocationRef).o, x, y, z, w)
	}

	override fun uniform4iv(location: GlUniformLocationRef, v: IntArrayListRo) {
		context.uniform4iv((location as WebGlUniformLocationRef).o, v.asNative2())
	}

	override fun uniformMatrix2fv(location: GlUniformLocationRef, transpose: Boolean, value: FloatArrayListRo) {
		context.uniformMatrix2fv((location as WebGlUniformLocationRef).o, transpose, value. asNative2())
	}

	override fun uniformMatrix3fv(location: GlUniformLocationRef, transpose: Boolean, value: FloatArrayListRo) {
		context.uniformMatrix3fv((location as WebGlUniformLocationRef).o, transpose, value. asNative2())
	}

	override fun uniformMatrix4fv(location: GlUniformLocationRef, transpose: Boolean, value: FloatArrayListRo) {
		context.uniformMatrix4fv((location as WebGlUniformLocationRef).o, transpose, value. asNative2())
	}

	override fun useProgram(program: GlProgramRef?) {
		context.useProgram((program as? WebGlProgramRef)?.o)
	}

	override fun validateProgram(program: GlProgramRef) {
		context.validateProgram((program as WebGlProgramRef).o)
	}

	override fun vertexAttrib1f(index: Int, x: Float) {
		context.vertexAttrib1f(index, x)
	}

	override fun vertexAttrib1fv(index: Int, values: FloatArrayListRo) {
		context.vertexAttrib1fv(index, values.asNative())
	}

	override fun vertexAttrib2f(index: Int, x: Float, y: Float) {
		context.vertexAttrib2f(index, x, y)
	}

	override fun vertexAttrib2fv(index: Int, values: FloatArrayListRo) {
		context.vertexAttrib2fv(index, values.asNative())
	}

	override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float) {
		context.vertexAttrib3f(index, x, y, z)
	}

	override fun vertexAttrib3fv(index: Int, values: FloatArrayListRo) {
		context.vertexAttrib3fv(index, values.asNative())
	}

	override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) {
		context.vertexAttrib4f(index, x, y, z, w)
	}

	override fun vertexAttrib4fv(index: Int, values: FloatArrayListRo) {
		context.vertexAttrib4fv(index, values.asNative())
	}

	override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
		context.vertexAttribPointer(index, size, type, normalized, stride, offset)
	}

	override fun viewport(x: Int, y: Int, width: Int, height: Int) {
		context.viewport(x, y, width, height)
	}

	override fun getUniformb(program: GlProgramRef, location: GlUniformLocationRef): Boolean {
		return context.getUniform((program as WebGlProgramRef).o, (location as WebGlUniformLocationRef).o) as Boolean
	}

	override fun getUniformi(program: GlProgramRef, location: GlUniformLocationRef): Int {
		return context.getUniform((program as WebGlProgramRef).o, (location as WebGlUniformLocationRef).o) as Int
	}

	override fun getUniformiv(program: GlProgramRef, location: GlUniformLocationRef, out: IntArray): IntArray {
		val r = context.getUniform((program as WebGlProgramRef).o, (location as WebGlUniformLocationRef).o) as Int32Array
		for (i in 0..out.lastIndex) {
			out[i] = r[i]
		}
		return out
	}

	override fun getUniformf(program: GlProgramRef, location: GlUniformLocationRef): Float {
		return context.getUniform((program as WebGlProgramRef).o, (location as WebGlUniformLocationRef).o) as Float
	}

	override fun getUniformfv(program: GlProgramRef, location: GlUniformLocationRef, out: FloatArray): FloatArray {
		val r = context.getUniform((program as WebGlProgramRef).o, (location as WebGlUniformLocationRef).o) as Float32Array
		for (i in 0..out.lastIndex) {
			out[i] = r[i]
		}
		return out
	}

	override fun getVertexAttribi(index: Int, pName: Int): Int {
		return context.getVertexAttrib(index, pName) as Int
	}

	override fun getVertexAttribb(index: Int, pName: Int): Boolean {
		return context.getVertexAttrib(index, pName) as Boolean
	}

	override fun getTexParameter(target: Int, pName: Int): Int {
		return context.getTexParameter(target, pName) as Int
	}

	override fun getShaderParameterb(shader: GlShaderRef, pName: Int): Boolean {
		return context.getShaderParameter((shader as WebGlShaderRef).o, pName) as Boolean
	}

	override fun getShaderParameteri(shader: GlShaderRef, pName: Int): Int {
		return context.getShaderParameter((shader as WebGlShaderRef).o, pName) as Int
	}

	override fun getRenderbufferParameter(target: Int, pName: Int): Int {
		return context.getRenderbufferParameter(target, pName) as Int
	}

	override fun getParameterb(pName: Int): Boolean {
		return context.getParameter(pName) as Boolean
	}

	override fun getParameterbv(pName: Int, out: BooleanArray): BooleanArray {
		val r = context.getParameter(pName).asDynamic()
		for (i in 0..out.lastIndex) {
			out[i] = r[i] as Boolean
		}
		return out
	}

	override fun getParameteri(pName: Int): Int {
		return context.getParameter(pName) as Int
	}

	override fun getParameteriv(pName: Int, out: IntArray): IntArray {
		val r = context.getParameter(pName) as Int32Array
		for (i in 0..out.lastIndex) {
			out[i] = r[i]
		}
		return out
	}

	override fun getParameterf(pName: Int): Float {
		return context.getParameter(pName) as Float
	}

	override fun getParameterfv(pName: Int, out: FloatArray): FloatArray {
		val r = context.getParameter(pName) as Float32Array
		for (i in 0..out.lastIndex) {
			out[i] = r[i]
		}
		return out
	}

	override fun getProgramParameterb(program: GlProgramRef, pName: Int): Boolean {
		return context.getProgramParameter((program as WebGlProgramRef).o, pName) as Boolean
	}

	override fun getProgramParameteri(program: GlProgramRef, pName: Int): Int {
		val r = context.getProgramParameter((program as WebGlProgramRef).o, pName)
		return (r as Number).toInt()
	}

	override fun getBufferParameter(target: Int, pName: Int): Int {
		return context.getBufferParameter(target, pName) as Int
	}

	override fun getFramebufferAttachmentParameteri(target: Int, attachment: Int, pName: Int): Int {
		return context.getFramebufferAttachmentParameter(target, attachment, pName) as Int
	}

	override fun getSupportedExtensions(): List<String> {
		return context.getSupportedExtensions()?.toList() ?: emptyList()
	}

	fun getExtension(name: String): dynamic {
		return context.getExtension(name)
	}

}

class WebGlProgramRef(val o: WebGLProgram) : GlProgramRef
class WebGlShaderRef(val o: WebGLShader) : GlShaderRef
class WebGlBufferRef(val o: WebGLBuffer) : GlBufferRef
class WebGlFramebufferRef(val o: WebGLFramebuffer) : GlFramebufferRef
class WebGlRenderbufferRef(val o: WebGLRenderbuffer) : GlRenderbufferRef
class WebGlTextureRef(val o: WebGLTexture) : GlTextureRef
class WebGlActiveInfoRef(val o: WebGLActiveInfo) : GlActiveInfoRef {
	override var name: String
		get() = o.name
		set(value) {
			throw UnsupportedOperationException()
		}
	override var size: Int
		get() = o.size
		set(value) {
			throw UnsupportedOperationException()
		}
	override var type: Int
		get() = o.type
		set(value) {
			throw UnsupportedOperationException()
		}
}

class WebGlUniformLocationRef(val o: WebGLUniformLocation) : GlUniformLocationRef

class WebGl20Debug(private val context: WebGLRenderingContext) : WrappedGl20(WebGl20(context), {}, after = {
	val errorCode = context.getError()
	if (errorCode != WebGLRenderingContext.NO_ERROR) {
		WebGLRenderingContext.apply {
			val msg = when (errorCode) {
				INVALID_ENUM -> "Invalid enum"
				INVALID_VALUE -> "Invalid value"
				INVALID_OPERATION -> "Invalid operation"
				INVALID_FRAMEBUFFER_OPERATION -> "Invalid framebuffer operation"
				OUT_OF_MEMORY -> "Out of memory"
				else -> "Unknown"
			}
			throw Exception("GL Error: $msg")
		}
	}
})

@Suppress("CAST_NEVER_SUCCEEDS", "NOTHING_TO_INLINE")
private inline fun FloatArrayListRo.asNative2(): Float32Array = asNative() as Float32Array

@Suppress("CAST_NEVER_SUCCEEDS", "NOTHING_TO_INLINE")
private inline fun IntArrayListRo.asNative2(): Int32Array = asNative() as Int32Array