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

import com.acornui.collection.*
import com.acornui.graphic.Texture
import com.acornui.graphic.Window
import com.acornui.io.NativeReadBuffer

/**
 * A wrapper to [Gl20] that caches properties for faster access.
 * Example:
 * gl.activeTexture(Gl20.TEXTURE0)
 * gl.getParameter(Gl20.ACTIVE_TEXTURE) // No need to query GPU, returns Gl20.TEXTURE0 from ram.
 */
class Gl20CachedImpl(
		override val wrapped: Gl20,
		private val window: Window
) : CachedGl20 {

	override var changeCount: Int = 0
		private set(value) {
			field = value
//			window.makeCurrent()
			batch.flush()
		}

	private val enabled = HashMap<Int, Boolean>()

	private val parametersB = HashMap<Int, Boolean>()
	private val parametersBv = HashMap<Int, BooleanArray>()

	private val parametersI = HashMap<Int, Int>()
	private val parametersIv = HashMap<Int, IntArray>()

	private val parametersF = HashMap<Int, Float>()
	private val parametersFv = HashMap<Int, FloatArray>()

	private val programs = HashMap<GlProgramRef, ShaderProgramCachedProperties>()

	override var program: GlProgramRef? = null
		private set

	override var framebuffer: GlFramebufferRef? = null
		private set

	override var renderbuffer: GlRenderbufferRef? = null
		private set

	private val programCache: ShaderProgramCachedProperties
		get() = programs[program] ?: error("No active shader program")

	private val supportedExtensionsCache by lazy {
		wrapped.getSupportedExtensions()
	}

	override val uniforms: Uniforms = UniformsImpl(this)

	override fun activeTexture(texture: Int) {
		parametersI.cached(Gl20.ACTIVE_TEXTURE, texture) {
			changeCount++
			wrapped.activeTexture(texture)
		}
	}

	override fun attachShader(program: GlProgramRef, shader: GlShaderRef) {
		changeCount++
		wrapped.attachShader(program, shader)
	}

	override fun bindAttribLocation(program: GlProgramRef, index: Int, name: String) {
		changeCount++
		wrapped.bindAttribLocation(program, index, name)
	}

	override fun bindBuffer(target: Int, buffer: GlBufferRef?) {
		changeCount++
		wrapped.bindBuffer(target, buffer)
	}

	override fun bindFramebuffer(framebuffer: GlFramebufferRef?) {
		if (this.framebuffer == framebuffer) return
		changeCount++
		this.framebuffer = framebuffer
		wrapped.bindFramebuffer(framebuffer)
	}

	override fun bindRenderbuffer(renderbuffer: GlRenderbufferRef?) {
		if (this.renderbuffer == renderbuffer) return
		changeCount++
		this.renderbuffer = renderbuffer
		wrapped.bindRenderbuffer(renderbuffer)
	}

	override fun bindTexture(target: Int, texture: GlTextureRef?) {
		changeCount++
		wrapped.bindTexture(target, texture)
	}

	override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float) {
		changeCount++
		wrapped.blendColor(red, green, blue, alpha)
	}

	override fun blendEquationSeparate(modeRgb: Int, modeAlpha: Int) {
		if (parametersI[Gl20.BLEND_EQUATION_RGB] == modeRgb && parametersI[Gl20.BLEND_EQUATION_ALPHA] == modeAlpha) return
		changeCount++
		parametersI[Gl20.BLEND_EQUATION_RGB] = modeRgb
		parametersI[Gl20.BLEND_EQUATION_ALPHA] = modeAlpha
		wrapped.blendEquationSeparate(modeRgb, modeAlpha)
	}

	override fun blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) {
		if (parametersI[Gl20.BLEND_SRC_RGB] == srcRgb
				&& parametersI[Gl20.BLEND_DST_RGB] == dstRgb
				&& parametersI[Gl20.BLEND_SRC_ALPHA] == srcAlpha
				&& parametersI[Gl20.BLEND_DST_ALPHA] == dstAlpha
		) return
		changeCount++
		parametersI[Gl20.BLEND_SRC_RGB] = srcRgb
		parametersI[Gl20.BLEND_DST_RGB] = dstRgb
		parametersI[Gl20.BLEND_SRC_ALPHA] = srcAlpha
		parametersI[Gl20.BLEND_DST_ALPHA] = dstAlpha
		wrapped.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
	}

	override fun bufferData(target: Int, size: Int, usage: Int) {
		wrapped.bufferData(target, size, usage)
	}

	override fun bufferDatabv(target: Int, data: NativeReadBuffer<Byte>, usage: Int) {
		wrapped.bufferDatabv(target, data, usage)
	}

	override fun bufferDatafv(target: Int, data: NativeReadBuffer<Float>, usage: Int) {
		wrapped.bufferDatafv(target, data, usage)
	}

	override fun bufferDatasv(target: Int, data: NativeReadBuffer<Short>, usage: Int) {
		wrapped.bufferDatasv(target, data, usage)
	}

	override fun bufferSubDatafv(target: Int, offset: Int, data: NativeReadBuffer<Float>) {
		wrapped.bufferSubDatafv(target, offset, data)
	}

	override fun bufferSubDatasv(target: Int, offset: Int, data: NativeReadBuffer<Short>) {
		wrapped.bufferSubDatasv(target, offset, data)
	}

	override fun checkFramebufferStatus(target: Int): Int {
		return wrapped.checkFramebufferStatus(target)
	}

	override fun clear(mask: Int) {
		changeCount++
		wrapped.clear(mask)
	}

	override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) {
		parametersFv.cached4(Gl20.COLOR_CLEAR_VALUE, red, green, blue, alpha) {
			changeCount++
			wrapped.clearColor(red, green, blue, alpha)
		}
	}

	override fun clearDepth(depth: Float) {
		parametersF.cached(Gl20.DEPTH_CLEAR_VALUE, depth) {
			changeCount++
			wrapped.clearDepth(depth)
		}
	}

	override fun clearStencil(s: Int) {
		parametersI.cached(Gl20.STENCIL_CLEAR_VALUE, s) {
			changeCount++
			wrapped.clearStencil(s)
		}
	}

	override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
		parametersBv.cached4(Gl20.COLOR_WRITEMASK, red, green, blue, alpha) {
			changeCount++
			wrapped.colorMask(red, green, blue, alpha)
		}
	}

	override fun compileShader(shader: GlShaderRef) {
		changeCount++
		wrapped.compileShader(shader)
	}

	override fun copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) {
		changeCount++
		wrapped.copyTexImage2D(target, level, internalFormat, x, y, width, height, border)
	}

	override fun copyTexSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, x: Int, y: Int, width: Int, height: Int) {
		changeCount++
		wrapped.copyTexSubImage2D(target, level, xOffset, yOffset, x, y, width, height)
	}

	override fun createBuffer(): GlBufferRef {
		return wrapped.createBuffer()
	}

	override fun createFramebuffer(): GlFramebufferRef {
		return wrapped.createFramebuffer()
	}

	override fun createProgram(): GlProgramRef {
		val p = wrapped.createProgram()
		programs[p] = ShaderProgramCachedProperties()
		return p
	}

	override fun createRenderbuffer(): GlRenderbufferRef {
		return wrapped.createRenderbuffer()
	}

	override fun createShader(type: Int): GlShaderRef {
		return wrapped.createShader(type)
	}

	override fun createTexture(): GlTextureRef {
		return wrapped.createTexture()
	}

	override fun cullFace(mode: Int) {
		parametersI.cached(Gl20.CULL_FACE_MODE, mode) {
			changeCount++
			wrapped.cullFace(mode)
		}
	}

	override fun deleteBuffer(buffer: GlBufferRef) {
		wrapped.deleteBuffer(buffer)
	}

	override fun deleteFramebuffer(framebuffer: GlFramebufferRef) {
		wrapped.deleteFramebuffer(framebuffer)
	}

	override fun deleteProgram(program: GlProgramRef) {
		wrapped.deleteProgram(program)
		programs.remove(program)
	}

	override fun deleteRenderbuffer(renderbuffer: GlRenderbufferRef) {
		wrapped.deleteRenderbuffer(renderbuffer)
	}

	override fun deleteShader(shader: GlShaderRef) {
		wrapped.deleteShader(shader)
	}

	override fun deleteTexture(texture: GlTextureRef) {
		wrapped.deleteTexture(texture)
	}

	override fun depthFunc(func: Int) {
		parametersI.cached(Gl20.DEPTH_FUNC, func) {
			changeCount++
			wrapped.depthFunc(func)
		}
	}

	override fun depthMask(flag: Boolean) {
		parametersB.cached(Gl20.DEPTH_WRITEMASK, flag) {
			changeCount++
			wrapped.depthMask(flag)
		}
	}

	override fun depthRange(zNear: Float, zFar: Float) {
		changeCount++
		wrapped.depthRange(zNear, zFar)
	}

	override fun detachShader(program: GlProgramRef, shader: GlShaderRef) {
		changeCount++
		wrapped.detachShader(program, shader)
	}

	override fun disable(cap: Int) {
		changeCount++
		enabled[cap] = false
		wrapped.disable(cap)
	}

	override fun disableVertexAttribArray(index: Int) {
		changeCount++
		wrapped.disableVertexAttribArray(index)
	}

	override fun drawArrays(mode: Int, first: Int, count: Int) {
		changeCount++
		wrapped.drawArrays(mode, first, count)
	}

	override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) {
		changeCount++
		wrapped.drawElements(mode, count, type, offset)
	}

	override fun enable(cap: Int) {
		changeCount++
		enabled[cap] = true
		wrapped.enable(cap)
	}

	override fun enableVertexAttribArray(index: Int) {
		changeCount++
		wrapped.enableVertexAttribArray(index)
	}

	override fun finish() {
		changeCount++
		wrapped.finish()
	}

	override fun flush() {
		changeCount++
		wrapped.flush()
	}

	override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbufferTarget: Int, renderbuffer: GlRenderbufferRef) {
		changeCount++
		wrapped.framebufferRenderbuffer(target, attachment, renderbufferTarget, renderbuffer)
	}

	override fun framebufferTexture2D(target: Int, attachment: Int, textureTarget: Int, texture: GlTextureRef, level: Int) {
		changeCount++
		wrapped.framebufferTexture2D(target, attachment, textureTarget, texture, level)
	}

	override fun frontFace(mode: Int) {
		changeCount++
		wrapped.frontFace(mode)
	}

	override fun generateMipmap(target: Int) {
		changeCount++
		wrapped.generateMipmap(target)
	}

	override fun getActiveAttrib(program: GlProgramRef, index: Int): GlActiveInfoRef {
		return wrapped.getActiveAttrib(program, index)
	}

	override fun getActiveUniform(program: GlProgramRef, index: Int): GlActiveInfoRef {
		return wrapped.getActiveUniform(program, index)
	}

	override fun getAttachedShaders(program: GlProgramRef): Array<GlShaderRef> {
		return wrapped.getAttachedShaders(program)
	}

	override fun getAttribLocation(program: GlProgramRef, name: String): Int {
		return wrapped.getAttribLocation(program, name)
	}

	override fun getError(): Int {
		return wrapped.getError()
	}

	override fun getProgramInfoLog(program: GlProgramRef): String? {
		return wrapped.getProgramInfoLog(program)
	}

	override fun getShaderInfoLog(shader: GlShaderRef): String? {
		return wrapped.getShaderInfoLog(shader)
	}

	override fun getUniformLocation(program: GlProgramRef, name: String): GlUniformLocationRef? {
		return programs[program]!!.uniformLocationCache.getOrPut(name) {
			wrapped.getUniformLocation(program, name)
		}
	}

	override fun hint(target: Int, mode: Int) {
		changeCount++
		wrapped.hint(target, mode)
	}

	override fun isBuffer(buffer: GlBufferRef): Boolean {
		return wrapped.isBuffer(buffer)
	}

	override fun isEnabled(cap: Int): Boolean {
		return enabled.getOrPut(cap) { wrapped.isEnabled(cap) }
	}

	override fun isFramebuffer(framebuffer: GlFramebufferRef): Boolean {
		return wrapped.isFramebuffer(framebuffer)
	}

	override fun isProgram(program: GlProgramRef): Boolean {
		return wrapped.isProgram(program)
	}

	override fun isRenderbuffer(renderbuffer: GlRenderbufferRef): Boolean {
		return wrapped.isRenderbuffer(renderbuffer)
	}

	override fun isShader(shader: GlShaderRef): Boolean {
		return wrapped.isShader(shader)
	}

	override fun isTexture(texture: GlTextureRef): Boolean {
		return wrapped.isTexture(texture)
	}

	override fun lineWidth(width: Float) {
		changeCount++
		wrapped.lineWidth(width)
	}

	override fun linkProgram(program: GlProgramRef) {
		changeCount++
		wrapped.linkProgram(program)
	}

	override fun pixelStorei(pName: Int, param: Int) {
		changeCount++
		wrapped.pixelStorei(pName, param)
	}

	override fun polygonOffset(factor: Float, units: Float) {
		changeCount++
		wrapped.polygonOffset(factor, units)
	}

	override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: NativeReadBuffer<Byte>) {
		changeCount++
		wrapped.readPixels(x, y, width, height, format, type, pixels)
	}

	override fun renderbufferStorage(target: Int, internalFormat: Int, width: Int, height: Int) {
		changeCount++
		wrapped.renderbufferStorage(target, internalFormat, width, height)
	}

	override fun sampleCoverage(value: Float, invert: Boolean) {
		changeCount++
		wrapped.sampleCoverage(value, invert)
	}

	override fun scissor(x: Int, y: Int, width: Int, height: Int) {
		parametersIv.cached4(Gl20.SCISSOR_BOX, x, y, width, height) {
			changeCount++
			wrapped.scissor(x, y, width, height)
		}
	}

	override fun shaderSource(shader: GlShaderRef, source: String) {
		wrapped.shaderSource(shader, source)
	}

	override fun stencilFunc(func: Int, ref: Int, mask: Int) {
		changeCount++
		wrapped.stencilFunc(func, ref, mask)
	}

	override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
		changeCount++
		wrapped.stencilFuncSeparate(face, func, ref, mask)
	}

	override fun stencilMask(mask: Int) {
		changeCount++
		wrapped.stencilMask(mask)
	}

	override fun stencilMaskSeparate(face: Int, mask: Int) {
		changeCount++
		wrapped.stencilMaskSeparate(face, mask)
	}

	override fun stencilOp(fail: Int, zFail: Int, zPass: Int) {
		changeCount++
		wrapped.stencilOp(fail, zFail, zPass)
	}

	override fun stencilOpSeparate(face: Int, fail: Int, zFail: Int, zPass: Int) {
		changeCount++
		wrapped.stencilOpSeparate(face, fail, zFail, zPass)
	}

	override fun texImage2Db(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeReadBuffer<Byte>?) {
		changeCount++
		wrapped.texImage2Db(target, level, internalFormat, width, height, border, format, type, pixels)
	}

	override fun texImage2Df(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeReadBuffer<Float>?) {
		changeCount++
		wrapped.texImage2Df(target, level, internalFormat, width, height, border, format, type, pixels)
	}

	override fun texImage2D(target: Int, level: Int, internalFormat: Int, format: Int, type: Int, texture: Texture) {
		changeCount++
		wrapped.texImage2D(target, level, internalFormat, format, type, texture)
	}

	override fun texParameterf(target: Int, pName: Int, param: Float) {
		changeCount++
		wrapped.texParameterf(target, pName, param)
	}

	override fun texParameteri(target: Int, pName: Int, param: Int) {
		changeCount++
		wrapped.texParameteri(target, pName, param)
	}

	override fun texSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, format: Int, type: Int, texture: Texture) {
		changeCount++
		wrapped.texSubImage2D(target, level, xOffset, yOffset, format, type, texture)
	}

	override fun uniform1f(location: GlUniformLocationRef, x: Float) {
		programCache.uniformsF.cached(location, x) {
			changeCount++
			wrapped.uniform1f(location, x)
		}
	}

	override fun uniform1fv(location: GlUniformLocationRef, v: FloatArray) {
		programCache.uniformsFv.cached(location, v) {
			changeCount++
			wrapped.uniform1fv(location, v)
		}
	}

	override fun uniform1i(location: GlUniformLocationRef, x: Int) {
		programCache.uniformsI.cached(location, x) {
			changeCount++
			wrapped.uniform1i(location, x)
		}
	}

	override fun uniform1iv(location: GlUniformLocationRef, v: IntArray) {
		programCache.uniformsIv.cached(location, v) {
			changeCount++
			wrapped.uniform1iv(location, v)
		}
	}

	override fun uniform2f(location: GlUniformLocationRef, x: Float, y: Float) {
		programCache.uniformsFv.cached2(location, x, y) {
			changeCount++
			wrapped.uniform2f(location, x, y)
		}
	}

	override fun uniform2fv(location: GlUniformLocationRef, v: FloatArray) {
		programCache.uniformsFv.cached(location, v) {
			changeCount++
			wrapped.uniform2fv(location, v)
		}
	}

	override fun uniform2i(location: GlUniformLocationRef, x: Int, y: Int) {
		programCache.uniformsIv.cached2(location, x, y) {
			changeCount++
			wrapped.uniform2i(location, x, y)
		}
	}

	override fun uniform2iv(location: GlUniformLocationRef, v: IntArray) {
		programCache.uniformsIv.cached(location, v) {
			changeCount++
			wrapped.uniform2iv(location, v)
		}
	}

	override fun uniform3f(location: GlUniformLocationRef, x: Float, y: Float, z: Float) {
		programCache.uniformsFv.cached3(location, x, y, z) {
			changeCount++
			wrapped.uniform3f(location, x, y, z)
		}
	}

	override fun uniform3fv(location: GlUniformLocationRef, v: FloatArray) {
		programCache.uniformsFv.cached(location, v) {
			changeCount++
			wrapped.uniform3fv(location, v)
		}
	}

	override fun uniform3i(location: GlUniformLocationRef, x: Int, y: Int, z: Int) {
		programCache.uniformsIv.cached3(location, x, y, z) {
			changeCount++
			wrapped.uniform3i(location, x, y, z)
		}
	}

	override fun uniform3iv(location: GlUniformLocationRef, v: IntArray) {
		programCache.uniformsIv.cached(location, v) {
			changeCount++
			wrapped.uniform3iv(location, v)
		}
	}

	override fun uniform4f(location: GlUniformLocationRef, x: Float, y: Float, z: Float, w: Float) {
		programCache.uniformsFv.cached4(location, x, y, z, w) {
			changeCount++
			wrapped.uniform4f(location, x, y, z, w)
		}
	}

	override fun uniform4fv(location: GlUniformLocationRef, v: FloatArray) {
		programCache.uniformsFv.cached(location, v) {
			changeCount++
			wrapped.uniform4fv(location, v)
		}
	}

	override fun uniform4i(location: GlUniformLocationRef, x: Int, y: Int, z: Int, w: Int) {
		programCache.uniformsIv.cached4(location, x, y, z, w) {
			changeCount++
			wrapped.uniform4i(location, x, y, z, w)
		}
	}

	override fun uniform4iv(location: GlUniformLocationRef, v: IntArray) {
		programCache.uniformsIv.cached(location, v) {
			changeCount++
			wrapped.uniform4iv(location, v)
		}
	}

	override fun uniformMatrix2fv(location: GlUniformLocationRef, value: FloatArray) {
		programCache.uniformsFv.cached(location, value) {
			changeCount++
			wrapped.uniformMatrix2fv(location, value)
		}
	}

	override fun uniformMatrix3fv(location: GlUniformLocationRef, value: FloatArray) {
		programCache.uniformsFv.cached(location, value) {
			changeCount++
			wrapped.uniformMatrix3fv(location, value)
		}
	}

	override fun uniformMatrix4fv(location: GlUniformLocationRef, value: FloatArray) {
		programCache.uniformsFv.cached(location, value) {
			changeCount++
			wrapped.uniformMatrix4fv(location, value)
		}
	}

	override fun useProgram(program: GlProgramRef?) {
		if (this.program != program) {
			changeCount++
			this.program = program
			wrapped.useProgram(program)
		}
	}

	override fun validateProgram(program: GlProgramRef) {
		wrapped.validateProgram(program)
	}

	override fun vertexAttrib1f(index: Int, x: Float) {
		changeCount++
		wrapped.vertexAttrib1f(index, x)
	}

	override fun vertexAttrib1fv(index: Int, values: FloatArray) {
		changeCount++
		wrapped.vertexAttrib1fv(index, values)
	}

	override fun vertexAttrib2f(index: Int, x: Float, y: Float) {
		changeCount++
		wrapped.vertexAttrib2f(index, x, y)
	}

	override fun vertexAttrib2fv(index: Int, values: FloatArray) {
		changeCount++
		wrapped.vertexAttrib2fv(index, values)
	}

	override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float) {
		changeCount++
		wrapped.vertexAttrib3f(index, x, y, z)
	}

	override fun vertexAttrib3fv(index: Int, values: FloatArray) {
		changeCount++
		wrapped.vertexAttrib3fv(index, values)
	}

	override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) {
		changeCount++
		wrapped.vertexAttrib4f(index, x, y, z, w)
	}

	override fun vertexAttrib4fv(index: Int, values: FloatArray) {
		changeCount++
		wrapped.vertexAttrib4fv(index, values)
	}

	override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
		changeCount++
		wrapped.vertexAttribPointer(index, size, type, normalized, stride, offset)
	}

	override fun viewport(x: Int, y: Int, width: Int, height: Int) {
		parametersIv.cached4(Gl20.VIEWPORT, x, y, width, height) {
			changeCount++
			wrapped.viewport(x, y, width, height)
		}
	}

	override fun getUniformb(program: GlProgramRef, location: GlUniformLocationRef): Boolean {
		return programs[program]!!.uniformsB.getOrPut(location) {
			wrapped.getUniformb(program, location)
		}
	}

	override fun getUniformi(program: GlProgramRef, location: GlUniformLocationRef): Int {
		return programs[program]!!.uniformsI.getOrPut(location) {
			wrapped.getUniformi(program, location)
		}
	}

	override fun getUniformiv(program: GlProgramRef, location: GlUniformLocationRef, out: IntArray): IntArray {
		programs[program]!!.uniformsIv.getOrPut(location) {
			wrapped.getUniformiv(program, location, out)
		}.copyInto(out)
		return out
	}

	override fun getUniformf(program: GlProgramRef, location: GlUniformLocationRef): Float {
		return programs[program]!!.uniformsF.getOrPut(location) {
			wrapped.getUniformf(program, location)
		}
	}

	override fun getUniformfv(program: GlProgramRef, location: GlUniformLocationRef, out: FloatArray): FloatArray {
		programs[program]!!.uniformsFv.getOrPut(location) {
			wrapped.getUniformfv(program, location, out)
		}.copyInto(out)
		return out
	}

	override fun getVertexAttribi(index: Int, pName: Int): Int {
		return wrapped.getVertexAttribi(index, pName)
	}

	override fun getVertexAttribb(index: Int, pName: Int): Boolean {
		return wrapped.getVertexAttribb(index, pName)
	}

	override fun getTexParameter(target: Int, pName: Int): Int {
		return wrapped.getTexParameter(target, pName)
	}

	override fun getShaderParameterb(shader: GlShaderRef, pName: Int): Boolean {
		return wrapped.getShaderParameterb(shader, pName)
	}

	override fun getShaderParameteri(shader: GlShaderRef, pName: Int): Int {
		return wrapped.getShaderParameteri(shader, pName)
	}

	override fun getRenderbufferParameter(target: Int, pName: Int): Int {
		return wrapped.getRenderbufferParameter(target, pName)
	}

	override fun getParameterb(pName: Int): Boolean {
		return parametersB[pName] ?: wrapped.getParameterb(pName)
	}

	override fun getParameterbv(pName: Int, out: BooleanArray): BooleanArray {
		if (parametersBv.containsKey(pName))
			parametersBv[pName]!!.copyInto(out) else wrapped.getParameterbv(pName, out)
		return out
	}

	override fun getParameteri(pName: Int): Int {
		return parametersI[pName] ?: wrapped.getParameteri(pName)
	}

	override fun getParameteriv(pName: Int, out: IntArray): IntArray {
		if (parametersIv.containsKey(pName))
			parametersIv[pName]!!.copyInto(out) else wrapped.getParameteriv(pName, out)
		return out
	}

	override fun getParameterf(pName: Int): Float {
		return parametersF[pName] ?: wrapped.getParameterf(pName)
	}

	override fun getParameterfv(pName: Int, out: FloatArray): FloatArray {
		if (parametersFv.containsKey(pName))
			parametersFv[pName]!!.copyInto(out) else wrapped.getParameterfv(pName, out)
		return out
	}

	override fun getProgramParameterb(program: GlProgramRef, pName: Int): Boolean {
		return wrapped.getProgramParameterb(program, pName)
	}

	override fun getProgramParameteri(program: GlProgramRef, pName: Int): Int {
		return wrapped.getProgramParameteri(program, pName)
	}

	override fun getBufferParameter(target: Int, pName: Int): Int {
		return wrapped.getBufferParameter(target, pName)
	}

	override fun getFramebufferAttachmentParameteri(target: Int, attachment: Int, pName: Int): Int {
		return wrapped.getFramebufferAttachmentParameteri(target, attachment, pName)
	}

	override fun getSupportedExtensions(): List<String> = supportedExtensionsCache

	override var batch: ShaderBatch = ShaderBatchImpl(wrapped)
		set(value) {
			field.flush()
			field = value
		}

}

class ShaderProgramCachedProperties {

	val uniformLocationCache = stringMapOf<GlUniformLocationRef?>()

	val uniformsB = HashMap<GlUniformLocationRef, Boolean>()

	val uniformsI = HashMap<GlUniformLocationRef, Int>()
	val uniformsIv = HashMap<GlUniformLocationRef, IntArray>()

	val uniformsF = HashMap<GlUniformLocationRef, Float>()
	val uniformsFv = HashMap<GlUniformLocationRef, FloatArray>()
}