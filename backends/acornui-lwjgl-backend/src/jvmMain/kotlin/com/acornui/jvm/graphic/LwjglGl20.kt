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

package com.acornui.jvm.graphic

import com.acornui.core.graphic.Texture
import com.acornui.gl.core.*
import com.acornui.io.NativeReadBuffer
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL11.nglGetBooleanv
import org.lwjgl.system.MemoryStack.stackGet
import org.lwjgl.system.MemoryUtil.memAddress
import java.nio.*

/**
 * @author nbilyk
 */
open class LwjglGl20 : Gl20 {

	override fun activeTexture(texture: Int) {
		GL13.glActiveTexture(texture)
	}

	override fun attachShader(program: GlProgramRef, shader: GlShaderRef) {
		GL20.glAttachShader((program as JvmGlProgram).o, (shader as JvmGlShader).o)
	}

	override fun bindAttribLocation(program: GlProgramRef, index: Int, name: String) {
		GL20.glBindAttribLocation((program as JvmGlProgram).o, index, name)
	}

	override fun bindBuffer(target: Int, buffer: GlBufferRef?) {
		if (buffer == null) GL15.glBindBuffer(target, 0)
		else GL15.glBindBuffer(target, (buffer as JvmGlBuffer).o)
	}

	override fun bindFramebuffer(target: Int, framebuffer: GlFramebufferRef?) {
		if (framebuffer == null) EXTFramebufferObject.glBindFramebufferEXT(target, 0) // TODO: This might not work on iOS OpenGLES
		else EXTFramebufferObject.glBindFramebufferEXT(target, (framebuffer as JvmGlFramebuffer).o)
	}

	override fun bindRenderbuffer(target: Int, renderbuffer: GlRenderbufferRef?) {
		if (renderbuffer == null) EXTFramebufferObject.glBindRenderbufferEXT(target, 0)
		else EXTFramebufferObject.glBindRenderbufferEXT(target, (renderbuffer as JvmGlRenderbuffer).o)
	}

	override fun bindTexture(target: Int, texture: GlTextureRef?) {
		if (texture == null) GL11.glBindTexture(target, 0)
		else GL11.glBindTexture(target, (texture as JvmGlTexture).o)
	}

	override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float) {
		GL14.glBlendColor(red, green, blue, alpha)
	}

	override fun blendEquation(mode: Int) {
		GL14.glBlendEquation(mode)
	}

	override fun blendEquationSeparate(modeRgb: Int, modeAlpha: Int) {
		GL20.glBlendEquationSeparate(modeRgb, modeAlpha)
	}

	override fun blendFunc(sfactor: Int, dfactor: Int) {
		GL11.glBlendFunc(sfactor, dfactor)
	}

	override fun blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) {
		GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
	}

	override fun bufferData(target: Int, size: Int, usage: Int) {
		GL15.glBufferData(target, size.toLong(), usage)
	}

	override fun bufferDatabv(target: Int, data: NativeReadBuffer<Byte>, usage: Int) {
		GL15.glBufferData(target, data.native as ByteBuffer, usage)
	}

	override fun bufferDatafv(target: Int, data: NativeReadBuffer<Float>, usage: Int) {
		GL15.glBufferData(target, data.native as FloatBuffer, usage)
	}

	override fun bufferDatasv(target: Int, data: NativeReadBuffer<Short>, usage: Int) {
		GL15.glBufferData(target, data.native as ShortBuffer, usage)
	}

	override fun bufferSubDatafv(target: Int, offset: Int, data: NativeReadBuffer<Float>) {
		GL15.glBufferSubData(target, offset.toLong(), data.native as FloatBuffer)
	}

	override fun bufferSubDatasv(target: Int, offset: Int, data: NativeReadBuffer<Short>) {
		GL15.glBufferSubData(target, offset.toLong(), data.native as ShortBuffer)
	}

	override fun checkFramebufferStatus(target: Int): Int {
		return EXTFramebufferObject.glCheckFramebufferStatusEXT(target)
	}

	override fun clear(mask: Int) {
		GL11.glClear(mask)
	}

	override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) {
		GL11.glClearColor(red, green, blue, alpha)
	}

	override fun clearDepth(depth: Float) {
		GL11.glClearDepth(depth.toDouble())
	}

	override fun clearStencil(s: Int) {
		GL11.glClearStencil(s)
	}

	override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
		GL11.glColorMask(red, green, blue, alpha)
	}

	override fun compileShader(shader: GlShaderRef) {
		GL20.glCompileShader((shader as JvmGlShader).o)
	}

	override fun copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) {
		GL11.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border)
	}

	override fun copyTexSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, x: Int, y: Int, width: Int, height: Int) {
		GL11.glCopyTexSubImage2D(target, level, xOffset, yOffset, x, y, width, height)
	}

	override fun createBuffer(): GlBufferRef {
		return JvmGlBuffer(GL15.glGenBuffers())
	}

	override fun createFramebuffer(): GlFramebufferRef {
		return JvmGlFramebuffer(EXTFramebufferObject.glGenFramebuffersEXT())
	}

	override fun createProgram(): GlProgramRef {
		return JvmGlProgram(GL20.glCreateProgram())
	}

	override fun createRenderbuffer(): GlRenderbufferRef {
		return JvmGlRenderbuffer(EXTFramebufferObject.glGenRenderbuffersEXT())
	}

	override fun createShader(type: Int): GlShaderRef {
		return JvmGlShader(GL20.glCreateShader(type))
	}

	override fun createTexture(): GlTextureRef {
		return JvmGlTexture(GL11.glGenTextures())
	}

	override fun cullFace(mode: Int) {
		GL11.glCullFace(mode)
	}

	override fun deleteBuffer(buffer: GlBufferRef) {
		GL15.glDeleteBuffers((buffer as JvmGlBuffer).o)
	}

	override fun deleteFramebuffer(framebuffer: GlFramebufferRef) {
		EXTFramebufferObject.glDeleteFramebuffersEXT((framebuffer as JvmGlFramebuffer).o)
	}

	override fun deleteProgram(program: GlProgramRef) {
		GL20.glDeleteProgram((program as JvmGlProgram).o)
	}

	override fun deleteRenderbuffer(renderbuffer: GlRenderbufferRef) {
		EXTFramebufferObject.glDeleteRenderbuffersEXT((renderbuffer as JvmGlRenderbuffer).o)
	}

	override fun deleteShader(shader: GlShaderRef) {
		GL20.glDeleteShader((shader as JvmGlShader).o)
	}

	override fun deleteTexture(texture: GlTextureRef) {
		GL11.glDeleteTextures((texture as JvmGlTexture).o)
	}

	override fun depthFunc(func: Int) {
		GL11.glDepthFunc(func)
	}

	override fun depthMask(flag: Boolean) {
		GL11.glDepthMask(flag)
	}

	override fun depthRange(zNear: Float, zFar: Float) {
		GL11.glDepthRange(zNear.toDouble(), zFar.toDouble())
	}

	override fun detachShader(program: GlProgramRef, shader: GlShaderRef) {
		GL20.glDetachShader((program as JvmGlProgram).o, (shader as JvmGlShader).o)
	}

	override fun disable(cap: Int) {
		GL11.glDisable(cap)
	}

	override fun disableVertexAttribArray(index: Int) {
		GL20.glDisableVertexAttribArray(index)
	}

	override fun drawArrays(mode: Int, first: Int, count: Int) {
		GL11.glDrawArrays(mode, first, count)
	}

	override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) {
		GL11.glDrawElements(mode, count, type, offset.toLong())
	}

	override fun enable(cap: Int) {
		GL11.glEnable(cap)
	}

	override fun enableVertexAttribArray(index: Int) {
		GL20.glEnableVertexAttribArray(index)
	}

	override fun finish() {
		GL11.glFinish()
	}

	override fun flush() {
		GL11.glFlush()
	}

	override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbufferTarget: Int, renderbuffer: GlRenderbufferRef) {
		EXTFramebufferObject.glFramebufferRenderbufferEXT(target, attachment, renderbufferTarget, (renderbuffer as JvmGlRenderbuffer).o)
	}

	override fun framebufferTexture2D(target: Int, attachment: Int, textureTarget: Int, texture: GlTextureRef, level: Int) {
		EXTFramebufferObject.glFramebufferTexture2DEXT(target, attachment, textureTarget, (texture as JvmGlTexture).o, level)
	}

	override fun frontFace(mode: Int) {
		GL11.glFrontFace(mode)
	}

	override fun generateMipmap(target: Int) {
		EXTFramebufferObject.glGenerateMipmapEXT(target)
	}

	override fun getActiveAttrib(program: GlProgramRef, index: Int): GlActiveInfoRef {
		val sizeBuffer = BufferUtils.createIntBuffer(1)
		val typeBuffer = BufferUtils.createIntBuffer(1)
		val name = GL20.glGetActiveAttrib((program as JvmGlProgram).o, index, 256, sizeBuffer, typeBuffer)
		return JvmGlActiveInfo(name, sizeBuffer.get(0), typeBuffer.get(0))
	}

	override fun getActiveUniform(program: GlProgramRef, index: Int): GlActiveInfoRef {
		val sizeBuffer = BufferUtils.createIntBuffer(1)
		val typeBuffer = BufferUtils.createIntBuffer(1)
		val name = GL20.glGetActiveUniform((program as JvmGlProgram).o, index, 256, sizeBuffer, typeBuffer)
		return JvmGlActiveInfo(name, sizeBuffer.get(0), typeBuffer.get(0))
	}

	override fun getAttachedShaders(program: GlProgramRef): Array<GlShaderRef> {
//		val idBuffer = GL20.glGetAttachedShaders((program as JvmGlProgram).o)
//		val ids = idBuffer.array()
//		val out = Array<GlShaderRef>(ids.size, { JvmGlShader(ids[it])})
//		return out
		return arrayOf()
	}

	override fun getAttribLocation(program: GlProgramRef, name: String): Int {
		return GL20.glGetAttribLocation((program as JvmGlProgram).o, name)
	}

	override fun getError(): Int {
		return GL11.glGetError()
	}

	override fun getProgramInfoLog(program: GlProgramRef): String? {
		val buffer = ByteBuffer.allocateDirect(1024 * 10)
		buffer.order(ByteOrder.nativeOrder())
		val tmp = ByteBuffer.allocateDirect(4)
		tmp.order(ByteOrder.nativeOrder())
		val intBuffer = tmp.asIntBuffer()

		GL20.glGetProgramInfoLog((program as JvmGlProgram).o, intBuffer, buffer)
		val numBytes = intBuffer.get(0)
		val bytes = ByteArray(numBytes)
		buffer.get(bytes)
		return String(bytes)
	}

	override fun getShaderInfoLog(shader: GlShaderRef): String? {
		val buffer = ByteBuffer.allocateDirect(1024 * 10)
		buffer.order(ByteOrder.nativeOrder())
		val tmp = ByteBuffer.allocateDirect(4)
		tmp.order(ByteOrder.nativeOrder())
		val intBuffer = tmp.asIntBuffer()

		GL20.glGetShaderInfoLog((shader as JvmGlShader).o, intBuffer, buffer)
		val numBytes = intBuffer.get(0)
		val bytes = ByteArray(numBytes)
		buffer.get(bytes)
		return String(bytes)
	}

	override fun getUniformLocation(program: GlProgramRef, name: String): GlUniformLocationRef? {
		val uniformLocation = GL20.glGetUniformLocation((program as JvmGlProgram).o, name)
		if (uniformLocation == -1) return null
		return JvmGlUniformLocation(uniformLocation)
	}

	override fun hint(target: Int, mode: Int) {
		return GL11.glHint(target, mode)
	}

	override fun isBuffer(buffer: GlBufferRef): Boolean {
		return GL15.glIsBuffer((buffer as JvmGlBuffer).o)
	}

	override fun isEnabled(cap: Int): Boolean {
		return GL11.glIsEnabled(cap)
	}

	override fun isFramebuffer(framebuffer: GlFramebufferRef): Boolean {
		return EXTFramebufferObject.glIsFramebufferEXT((framebuffer as JvmGlFramebuffer).o)
	}

	override fun isProgram(program: GlProgramRef): Boolean {
		return GL20.glIsProgram((program as JvmGlProgram).o)
	}

	override fun isRenderbuffer(renderbuffer: GlRenderbufferRef): Boolean {
		return EXTFramebufferObject.glIsRenderbufferEXT((renderbuffer as JvmGlRenderbuffer).o)
	}

	override fun isShader(shader: GlShaderRef): Boolean {
		return GL20.glIsShader((shader as JvmGlShader).o)
	}

	override fun isTexture(texture: GlTextureRef): Boolean {
		return GL11.glIsTexture((texture as JvmGlTexture).o)
	}

	override fun lineWidth(width: Float) {
		GL11.glLineWidth(width)
	}

	override fun linkProgram(program: GlProgramRef) {
		GL20.glLinkProgram((program as JvmGlProgram).o)
	}

	override fun pixelStorei(pName: Int, param: Int) {
		GL11.glPixelStorei(pName, param)
	}

	override fun polygonOffset(factor: Float, units: Float) {
		GL11.glPolygonOffset(factor, units)
	}

	override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: NativeReadBuffer<Byte>) {
		val pixelsBuffer = pixels.native as ByteBuffer
		GL11.glReadPixels(x, y, width, height, format, type, pixelsBuffer)
		pixels.rewind()
	}

	override fun renderbufferStorage(target: Int, internalFormat: Int, width: Int, height: Int) {
		EXTFramebufferObject.glRenderbufferStorageEXT(target, internalFormat, width, height)
	}

	override fun sampleCoverage(value: Float, invert: Boolean) {
		GL13.glSampleCoverage(value, invert)
	}

	override fun scissor(x: Int, y: Int, width: Int, height: Int) {
		GL11.glScissor(x, y, width, height)
	}

	override fun shaderSource(shader: GlShaderRef, source: String) {
		GL20.glShaderSource((shader as JvmGlShader).o, source)
	}

	override fun stencilFunc(func: Int, ref: Int, mask: Int) {
		GL11.glStencilFunc(func, ref, mask)
	}

	override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
		GL20.glStencilFuncSeparate(face, func, ref, mask)
	}

	override fun stencilMask(mask: Int) {
		GL11.glStencilMask(mask)
	}

	override fun stencilMaskSeparate(face: Int, mask: Int) {
		GL20.glStencilMaskSeparate(face, mask)
	}

	override fun stencilOp(fail: Int, zFail: Int, zPass: Int) {
		GL11.glStencilOp(fail, zFail, zPass)
	}

	override fun stencilOpSeparate(face: Int, fail: Int, zFail: Int, zPass: Int) {
		GL20.glStencilOpSeparate(face, fail, zFail, zPass)
	}

	override fun texImage2Db(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeReadBuffer<Byte>?) {
		val p: ByteBuffer? = if (pixels == null) null else pixels.native as ByteBuffer
		GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, type, p)
	}

	override fun texImage2Df(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeReadBuffer<Float>?) {
		val p: FloatBuffer? = if (pixels == null) null else pixels.native as FloatBuffer
		GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, type, p)
	}

	override fun texImage2D(target: Int, level: Int, internalFormat: Int, format: Int, type: Int, texture: Texture) {
		val jvmTexture = (texture as JvmTexture)
		jvmTexture.bytes!!.rewind()
		GL11.glTexImage2D(target, level, internalFormat, texture.width, texture.height, 0, format, type, texture.bytes)
	}

	override fun texParameterf(target: Int, pName: Int, param: Float) {
		GL11.glTexParameterf(target, pName, param)
	}

	override fun texParameteri(target: Int, pName: Int, param: Int) {
		GL11.glTexParameteri(target, pName, param)
	}

	override fun texSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, format: Int, type: Int, texture: Texture) {
//		GL11.glTexSubImage2D(target, level, offsetX, yOffset, format, type, (texture as JvmTexture).image)
	}

	override fun uniform1f(location: GlUniformLocationRef, x: Float) {
		GL20.glUniform1f((location as JvmGlUniformLocation).o, x)
	}

	override fun uniform1fv(location: GlUniformLocationRef, v: NativeReadBuffer<Float>) {
		GL20.glUniform1fv((location as JvmGlUniformLocation).o, v.native as FloatBuffer)
	}

	override fun uniform1i(location: GlUniformLocationRef, x: Int) {
		GL20.glUniform1i((location as JvmGlUniformLocation).o, x)
	}

	override fun uniform1iv(location: GlUniformLocationRef, v: NativeReadBuffer<Int>) {
		GL20.glUniform1iv((location as JvmGlUniformLocation).o, v.native as IntBuffer)
	}

	override fun uniform2f(location: GlUniformLocationRef, x: Float, y: Float) {
		GL20.glUniform2f((location as JvmGlUniformLocation).o, x, y)
	}

	override fun uniform2fv(location: GlUniformLocationRef, v: NativeReadBuffer<Float>) {
		GL20.glUniform2fv((location as JvmGlUniformLocation).o, v.native as FloatBuffer)
	}

	override fun uniform2i(location: GlUniformLocationRef, x: Int, y: Int) {
		GL20.glUniform2i((location as JvmGlUniformLocation).o, x, y)
	}

	override fun uniform2iv(location: GlUniformLocationRef, v: NativeReadBuffer<Int>) {
		GL20.glUniform2iv((location as JvmGlUniformLocation).o, v.native as IntBuffer)
	}

	override fun uniform3f(location: GlUniformLocationRef, x: Float, y: Float, z: Float) {
		GL20.glUniform3f((location as JvmGlUniformLocation).o, x, y, z)
	}

	override fun uniform3fv(location: GlUniformLocationRef, v: NativeReadBuffer<Float>) {
		GL20.glUniform3fv((location as JvmGlUniformLocation).o, v.native as FloatBuffer)
	}

	override fun uniform3i(location: GlUniformLocationRef, x: Int, y: Int, z: Int) {
		GL20.glUniform3i((location as JvmGlUniformLocation).o, x, y, z)
	}

	override fun uniform3iv(location: GlUniformLocationRef, v: NativeReadBuffer<Int>) {
		GL20.glUniform3iv((location as JvmGlUniformLocation).o, v.native as IntBuffer)
	}

	override fun uniform4f(location: GlUniformLocationRef, x: Float, y: Float, z: Float, w: Float) {
		GL20.glUniform4f((location as JvmGlUniformLocation).o, x, y, z, w)
	}

	override fun uniform4fv(location: GlUniformLocationRef, v: NativeReadBuffer<Float>) {
		GL20.glUniform4fv((location as JvmGlUniformLocation).o, v.native as FloatBuffer)
	}

	override fun uniform4i(location: GlUniformLocationRef, x: Int, y: Int, z: Int, w: Int) {
		GL20.glUniform4i((location as JvmGlUniformLocation).o, x, y, z, w)
	}

	override fun uniform4iv(location: GlUniformLocationRef, v: NativeReadBuffer<Int>) {
		GL20.glUniform4iv((location as JvmGlUniformLocation).o, v.native as IntBuffer)
	}

	override fun uniformMatrix2fv(location: GlUniformLocationRef, transpose: Boolean, value: NativeReadBuffer<Float>) {
		GL20.glUniformMatrix2fv((location as JvmGlUniformLocation).o, transpose, value.native as FloatBuffer)
	}

	override fun uniformMatrix3fv(location: GlUniformLocationRef, transpose: Boolean, value: NativeReadBuffer<Float>) {
		GL20.glUniformMatrix3fv((location as JvmGlUniformLocation).o, transpose, value.native as FloatBuffer)
	}

	override fun uniformMatrix4fv(location: GlUniformLocationRef, transpose: Boolean, value: NativeReadBuffer<Float>) {
		GL20.glUniformMatrix4fv((location as JvmGlUniformLocation).o, transpose, value.native as FloatBuffer)
	}

	override fun useProgram(program: GlProgramRef?) {
		if (program == null) GL20.glUseProgram(0)
		else GL20.glUseProgram((program as JvmGlProgram).o)
	}

	override fun validateProgram(program: GlProgramRef) {
		GL20.glValidateProgram((program as JvmGlProgram).o)
	}

	override fun vertexAttrib1f(index: Int, x: Float) {
		GL20.glVertexAttrib1f(index, x)
	}

	override fun vertexAttrib1fv(index: Int, values: NativeReadBuffer<Float>) {
		GL20.glVertexAttrib1fv(index, values.native as FloatBuffer)
	}

	override fun vertexAttrib2f(index: Int, x: Float, y: Float) {
		GL20.glVertexAttrib2f(index, x, y)
	}

	override fun vertexAttrib2fv(index: Int, values: NativeReadBuffer<Float>) {
		GL20.glVertexAttrib2fv(index, values.native as FloatBuffer)
	}

	override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float) {
		GL20.glVertexAttrib3f(index, x, y, z)
	}

	override fun vertexAttrib3fv(index: Int, values: NativeReadBuffer<Float>) {
		GL20.glVertexAttrib3fv(index, values.native as FloatBuffer)
	}

	override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) {
		GL20.glVertexAttrib4f(index, x, y, z, w)
	}

	override fun vertexAttrib4fv(index: Int, values: NativeReadBuffer<Float>) {
		GL20.glVertexAttrib4fv(index, values.native as FloatBuffer)
	}

	override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
		GL20.glVertexAttribPointer(index, size, type, normalized, stride, offset.toLong())
	}

	override fun viewport(x: Int, y: Int, width: Int, height: Int) {
		GL11.glViewport(x, y, width, height)
	}

	override fun getUniformb(program: GlProgramRef, location: GlUniformLocationRef): Boolean {
		return GL20.glGetUniformi((program as JvmGlProgram).o, (location as JvmGlUniformLocation).o) > 0
	}

	override fun getUniformi(program: GlProgramRef, location: GlUniformLocationRef): Int {
		return GL20.glGetUniformi((program as JvmGlProgram).o, (location as JvmGlUniformLocation).o)
	}

	override fun getUniformf(program: GlProgramRef, location: GlUniformLocationRef): Float {
		return GL20.glGetUniformf((program as JvmGlProgram).o, (location as JvmGlUniformLocation).o)
	}

	override fun getVertexAttribi(index: Int, pName: Int): Int {
		return GL20.glGetVertexAttribi(index, pName)
	}

	override fun getVertexAttribb(index: Int, pName: Int): Boolean {
		return GL20.glGetVertexAttribi(index, pName) > 0
	}

	override fun getTexParameter(target: Int, pName: Int): Int {
		return GL11.glGetTexParameteri(target, pName)
	}

	override fun getShaderParameterb(shader: GlShaderRef, pName: Int): Boolean {
		return GL20.glGetShaderi((shader as JvmGlShader).o, pName) > 0
	}

	override fun getShaderParameteri(shader: GlShaderRef, pName: Int): Int {
		return GL20.glGetShaderi((shader as JvmGlShader).o, pName)
	}

	override fun getRenderbufferParameter(target: Int, pName: Int): Int {
		return EXTFramebufferObject.glGetRenderbufferParameteriEXT(target, pName)
	}

	override fun getParameterb(pName: Int): Boolean {
		return GL11.glGetBoolean(pName)
	}

	override fun getParameterbv(pName: Int, out: BooleanArray): BooleanArray {
		val stack = stackGet()
		val stackPointer = stack.pointer
		try {
			val params = stack.calloc(out.size)
			nglGetBooleanv(pName, memAddress(params))
			for (i in 0..out.lastIndex) {
				out[i] = params[i].toInt() != 0
			}
		} finally {
			stack.pointer = stackPointer
		}
		return out
	}

	override fun getParameteri(pName: Int): Int {
		return GL11.glGetInteger(pName)
	}

	override fun getProgramParameterb(program: GlProgramRef, pName: Int): Boolean {
		return GL20.glGetProgrami((program as JvmGlProgram).o, pName) > 0
	}

	override fun getProgramParameteri(program: GlProgramRef, pName: Int): Int {
		return GL20.glGetProgrami((program as JvmGlProgram).o, pName)
	}

	override fun getBufferParameter(target: Int, pName: Int): Int {
		return GL15.glGetBufferParameteri(target, pName)
	}

	override fun getFramebufferAttachmentParameteri(target: Int, attachment: Int, pName: Int): Int {
		return EXTFramebufferObject.glGetFramebufferAttachmentParameteriEXT(target, attachment, pName)
	}

	override fun getSupportedExtensions(): List<String> {
		val extensionsStr = GL11.glGetString(GL11.GL_EXTENSIONS)
		return extensionsStr?.split(" ") ?: listOf()
	}
}

class JvmGlProgram(val o: Int) : GlProgramRef
class JvmGlShader(val o: Int) : GlShaderRef
class JvmGlBuffer(val o: Int) : GlBufferRef
class JvmGlFramebuffer(val o: Int) : GlFramebufferRef
class JvmGlRenderbuffer(val o: Int) : GlRenderbufferRef
class JvmGlTexture(val o: Int) : GlTextureRef
class JvmGlActiveInfo(
		override var name: String,
		override var size: Int,
		override var type: Int) : GlActiveInfoRef

class JvmGlUniformLocation(val o: Int) : GlUniformLocationRef
