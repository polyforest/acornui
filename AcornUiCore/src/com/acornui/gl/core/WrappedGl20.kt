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

import com.acornui.core.graphics.Texture
import com.acornui.graphics.ColorRo
import com.acornui.io.NativeReadBuffer

/**
 * A class that wraps all Gl20 methods with a before() and after()
 * May be used for such things as debugging or profiling.
 * @author nbilyk
 */
open class WrappedGl20(protected val wrapped: Gl20, private val before: () -> Unit, private val after: () -> Unit) : Gl20 {

	override fun activeTexture(texture: Int) {
		before()
		wrapped.activeTexture(texture)
		after()
	}

	override fun attachShader(program: GlProgramRef, shader: GlShaderRef) {
		before()
		wrapped.attachShader(program, shader)
		after()
	}

	override fun bindAttribLocation(program: GlProgramRef, index: Int, name: String) {
		before()
		wrapped.bindAttribLocation(program, index, name)
		after()
	}

	override fun bindBuffer(target: Int, buffer: GlBufferRef?) {
		before()
		wrapped.bindBuffer(target, buffer)
		after()
	}

	override fun bindFramebuffer(target: Int, framebuffer: GlFramebufferRef?) {
		before()
		wrapped.bindFramebuffer(target, framebuffer)
		after()
	}

	override fun bindRenderbuffer(target: Int, renderbuffer: GlRenderbufferRef?) {
		before()
		wrapped.bindRenderbuffer(target, renderbuffer)
		after()
	}

	override fun bindTexture(target: Int, texture: GlTextureRef?) {
		before()
		wrapped.bindTexture(target, texture)
		after()
	}

	override fun blendColor(red: Float, green: Float, blue: Float, alpha: Float) {
		before()
		wrapped.blendColor(red, green, blue, alpha)
		after()
	}

	override fun blendEquation(mode: Int) {
		before()
		wrapped.blendEquation(mode)
		after()
	}

	override fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int) {
		before()
		wrapped.blendEquationSeparate(modeRGB, modeAlpha)
		after()
	}

	override fun blendFunc(sfactor: Int, dfactor: Int) {
		before()
		wrapped.blendFunc(sfactor, dfactor)
		after()
	}

	override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
		before()
		wrapped.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
		after()
	}

	override fun bufferData(target: Int, size: Int, usage: Int) {
		before()
		wrapped.bufferData(target, size, usage)
		after()
	}

	override fun bufferDatabv(target: Int, data: NativeReadBuffer<Byte>, usage: Int) {
		before()
		wrapped.bufferDatabv(target, data, usage)
		after()
	}

	override fun bufferDatafv(target: Int, data: NativeReadBuffer<Float>, usage: Int) {
		before()
		wrapped.bufferDatafv(target, data, usage)
		after()
	}

	override fun bufferDatasv(target: Int, data: NativeReadBuffer<Short>, usage: Int) {
		before()
		wrapped.bufferDatasv(target, data, usage)
		after()
	}

	override fun bufferSubDatafv(target: Int, offset: Int, data: NativeReadBuffer<Float>) {
		before()
		wrapped.bufferSubDatafv(target, offset, data)
		after()
	}

	override fun bufferSubDatasv(target: Int, offset: Int, data: NativeReadBuffer<Short>) {
		before()
		wrapped.bufferSubDatasv(target, offset, data)
		after()
	}

	override fun checkFramebufferStatus(target: Int): Int {
		before()
		val ret = wrapped.checkFramebufferStatus(target)
		after()
		return ret
	}

	override fun clear(mask: Int) {
		before()
		wrapped.clear(mask)
		after()
	}

	override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) {
		before()
		wrapped.clearColor(red, green, blue, alpha)
		after()
	}

	override fun clearDepth(depth: Float) {
		before()
		wrapped.clearDepth(depth)
		after()
	}

	override fun clearStencil(s: Int) {
		before()
		wrapped.clearStencil(s)
		after()
	}

	override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
		before()
		wrapped.colorMask(red, green, blue, alpha)
		after()
	}

	override fun compileShader(shader: GlShaderRef) {
		before()
		wrapped.compileShader(shader)
		after()
	}

	override fun copyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) {
		before()
		wrapped.copyTexImage2D(target, level, internalFormat, x, y, width, height, border)
		after()
	}

	override fun copyTexSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, x: Int, y: Int, width: Int, height: Int) {
		before()
		wrapped.copyTexSubImage2D(target, level, xOffset, yOffset, x, y, width, height)
		after()
	}

	override fun createBuffer(): GlBufferRef {
		before()
		val ret = wrapped.createBuffer()
		after()
		return ret
	}

	override fun createFramebuffer(): GlFramebufferRef {
		before()
		val ret = wrapped.createFramebuffer()
		after()
		return ret
	}

	override fun createProgram(): GlProgramRef {
		before()
		val ret = wrapped.createProgram()
		after()
		return ret
	}

	override fun createRenderbuffer(): GlRenderbufferRef {
		before()
		val ret = wrapped.createRenderbuffer()
		after()
		return ret
	}

	override fun createShader(type: Int): GlShaderRef {
		before()
		val ret = wrapped.createShader(type)
		after()
		return ret
	}

	override fun createTexture(): GlTextureRef {
		before()
		val ret = wrapped.createTexture()
		after()
		return ret
	}

	override fun cullFace(mode: Int) {
		before()
		wrapped.cullFace(mode)
		after()
	}

	override fun deleteBuffer(buffer: GlBufferRef) {
		before()
		wrapped.deleteBuffer(buffer)
		after()
	}

	override fun deleteFramebuffer(framebuffer: GlFramebufferRef) {
		before()
		wrapped.deleteFramebuffer(framebuffer)
		after()
	}

	override fun deleteProgram(program: GlProgramRef) {
		before()
		wrapped.deleteProgram(program)
		after()
	}

	override fun deleteRenderbuffer(renderbuffer: GlRenderbufferRef) {
		before()
		wrapped.deleteRenderbuffer(renderbuffer)
		after()
	}

	override fun deleteShader(shader: GlShaderRef) {
		before()
		wrapped.deleteShader(shader)
		after()
	}

	override fun deleteTexture(texture: GlTextureRef) {
		before()
		wrapped.deleteTexture(texture)
		after()
	}

	override fun depthFunc(func: Int) {
		before()
		wrapped.depthFunc(func)
		after()
	}

	override fun depthMask(flag: Boolean) {
		before()
		wrapped.depthMask(flag)
		after()
	}

	override fun depthRange(zNear: Float, zFar: Float) {
		before()
		wrapped.depthRange(zNear, zFar)
		after()
	}

	override fun detachShader(program: GlProgramRef, shader: GlShaderRef) {
		before()
		wrapped.detachShader(program, shader)
		after()
	}

	override fun disable(cap: Int) {
		before()
		wrapped.disable(cap)
		after()
	}

	override fun disableVertexAttribArray(index: Int) {
		before()
		wrapped.disableVertexAttribArray(index)
		after()
	}

	override fun drawArrays(mode: Int, first: Int, count: Int) {
		before()
		wrapped.drawArrays(mode, first, count)
		after()
	}

	override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) {
		before()
		wrapped.drawElements(mode, count, type, offset)
		after()
	}

	override fun enable(cap: Int) {
		before()
		wrapped.enable(cap)
		after()
	}

	override fun enableVertexAttribArray(index: Int) {
		before()
		wrapped.enableVertexAttribArray(index)
		after()
	}

	override fun finish() {
		before()
		wrapped.finish()
		after()
	}

	override fun flush() {
		before()
		wrapped.flush()
		after()
	}

	override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbufferTarget: Int, renderbuffer: GlRenderbufferRef) {
		before()
		wrapped.framebufferRenderbuffer(target, attachment, renderbufferTarget, renderbuffer)
		after()
	}

	override fun framebufferTexture2D(target: Int, attachment: Int, textureTarget: Int, texture: GlTextureRef, level: Int) {
		before()
		wrapped.framebufferTexture2D(target, attachment, textureTarget, texture, level)
		after()
	}

	override fun frontFace(mode: Int) {
		before()
		wrapped.frontFace(mode)
		after()
	}

	override fun generateMipmap(target: Int) {
		before()
		wrapped.generateMipmap(target)
		after()
	}

	override fun getActiveAttrib(program: GlProgramRef, index: Int): GlActiveInfoRef {
		before()
		val ret = wrapped.getActiveAttrib(program, index)
		after()
		return ret
	}

	override fun getActiveUniform(program: GlProgramRef, index: Int): GlActiveInfoRef {
		before()
		val ret = wrapped.getActiveUniform(program, index)
		after()
		return ret
	}

	override fun getAttachedShaders(program: GlProgramRef): Array<GlShaderRef> {
		before()
		val ret = wrapped.getAttachedShaders(program)
		after()
		return ret
	}

	override fun getAttribLocation(program: GlProgramRef, name: String): Int {
		before()
		val ret = wrapped.getAttribLocation(program, name)
		after()
		return ret
	}

	override fun getError(): Int {
		before()
		val ret = wrapped.getError()
		after()
		return ret
	}

	override fun getProgramInfoLog(program: GlProgramRef): String? {
		before()
		val ret = wrapped.getProgramInfoLog(program)
		after()
		return ret
	}

	override fun getShaderInfoLog(shader: GlShaderRef): String? {
		before()
		val ret = wrapped.getShaderInfoLog(shader)
		after()
		return ret
	}

	override fun getUniformLocation(program: GlProgramRef, name: String): GlUniformLocationRef? {
		before()
		val ret = wrapped.getUniformLocation(program, name)
		after()
		return ret
	}

	override fun hint(target: Int, mode: Int) {
		before()
		wrapped.hint(target, mode)
		after()
	}

	override fun isBuffer(buffer: GlBufferRef): Boolean {
		before()
		val ret = wrapped.isBuffer(buffer)
		after()
		return ret
	}

	override fun isEnabled(cap: Int): Boolean {
		before()
		val ret = wrapped.isEnabled(cap)
		after()
		return ret
	}

	override fun isFramebuffer(framebuffer: GlFramebufferRef): Boolean {
		before()
		val ret = wrapped.isFramebuffer(framebuffer)
		after()
		return ret
	}

	override fun isProgram(program: GlProgramRef): Boolean {
		before()
		val ret = wrapped.isProgram(program)
		after()
		return ret
	}

	override fun isRenderbuffer(renderbuffer: GlRenderbufferRef): Boolean {
		before()
		val ret = wrapped.isRenderbuffer(renderbuffer)
		after()
		return ret
	}

	override fun isShader(shader: GlShaderRef): Boolean {
		before()
		val ret = wrapped.isShader(shader)
		after()
		return ret
	}

	override fun isTexture(texture: GlTextureRef): Boolean {
		before()
		val ret = wrapped.isTexture(texture)
		after()
		return ret
	}

	override fun lineWidth(width: Float) {
		before()
		wrapped.lineWidth(width)
		after()
	}

	override fun linkProgram(program: GlProgramRef) {
		before()
		wrapped.linkProgram(program)
		after()
	}

	override fun pixelStorei(pName: Int, param: Int) {
		before()
		wrapped.pixelStorei(pName, param)
		after()
	}

	override fun polygonOffset(factor: Float, units: Float) {
		before()
		wrapped.polygonOffset(factor, units)
		after()
	}

	override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: NativeReadBuffer<Byte>) {
		before()
		wrapped.readPixels(x, y, width, height, format, type, pixels)
		after()
	}

	override fun renderbufferStorage(target: Int, internalFormat: Int, width: Int, height: Int) {
		before()
		wrapped.renderbufferStorage(target, internalFormat, width, height)
		after()
	}

	override fun sampleCoverage(value: Float, invert: Boolean) {
		before()
		wrapped.sampleCoverage(value, invert)
		after()
	}

	override fun scissor(x: Int, y: Int, width: Int, height: Int) {
		before()
		wrapped.scissor(x, y, width, height)
		after()
	}

	override fun shaderSource(shader: GlShaderRef, source: String) {
		before()
		wrapped.shaderSource(shader, source)
		after()
	}

	override fun stencilFunc(func: Int, ref: Int, mask: Int) {
		before()
		wrapped.stencilFunc(func, ref, mask)
		after()
	}

	override fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int) {
		before()
		wrapped.stencilFuncSeparate(face, func, ref, mask)
		after()
	}

	override fun stencilMask(mask: Int) {
		before()
		wrapped.stencilMask(mask)
		after()
	}

	override fun stencilMaskSeparate(face: Int, mask: Int) {
		before()
		wrapped.stencilMaskSeparate(face, mask)
		after()
	}

	override fun stencilOp(fail: Int, zfail: Int, zpass: Int) {
		before()
		wrapped.stencilOp(fail, zfail, zpass)
		after()
	}

	override fun stencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int) {
		before()
		wrapped.stencilOpSeparate(face, fail, zfail, zpass)
		after()
	}

	override fun texImage2Db(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeReadBuffer<Byte>?) {
		before()
		wrapped.texImage2Db(target, level, internalFormat, width, height, border, format, type, pixels)
		after()
	}

	override fun texImage2Df(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: NativeReadBuffer<Float>?) {
		before()
		wrapped.texImage2Df(target, level, internalFormat, width, height, border, format, type, pixels)
		after()
	}

	override fun texImage2D(target: Int, level: Int, internalFormat: Int, format: Int, type: Int, texture: Texture) {
		before()
		wrapped.texImage2D(target, level, internalFormat, format, type, texture)
		after()
	}

	override fun texParameterf(target: Int, pName: Int, param: Float) {
		before()
		wrapped.texParameterf(target, pName, param)
		after()
	}

	override fun texParameteri(target: Int, pName: Int, param: Int) {
		before()
		wrapped.texParameteri(target, pName, param)
		after()
	}

	override fun texSubImage2D(target: Int, level: Int, xOffset: Int, yOffset: Int, format: Int, type: Int, texture: Texture) {
		before()
		wrapped.texSubImage2D(target, level, xOffset, yOffset, format, type, texture)
		after()
	}

	override fun uniform1f(location: GlUniformLocationRef, x: Float) {
		before()
		wrapped.uniform1f(location, x)
		after()
	}

	override fun uniform1fv(location: GlUniformLocationRef, v: NativeReadBuffer<Float>) {
		before()
		wrapped.uniform1fv(location, v)
		after()
	}

	override fun uniform1i(location: GlUniformLocationRef, x: Int) {
		before()
		wrapped.uniform1i(location, x)
		after()
	}

	override fun uniform1iv(location: GlUniformLocationRef, v: NativeReadBuffer<Int>) {
		before()
		wrapped.uniform1iv(location, v)
		after()
	}

	override fun uniform2f(location: GlUniformLocationRef, x: Float, y: Float) {
		before()
		wrapped.uniform2f(location, x, y)
		after()
	}

	override fun uniform2fv(location: GlUniformLocationRef, v: NativeReadBuffer<Float>) {
		before()
		wrapped.uniform2fv(location, v)
		after()
	}

	override fun uniform2i(location: GlUniformLocationRef, x: Int, y: Int) {
		before()
		wrapped.uniform2i(location, x, y)
		after()
	}

	override fun uniform2iv(location: GlUniformLocationRef, v: NativeReadBuffer<Int>) {
		before()
		wrapped.uniform2iv(location, v)
		after()
	}

	override fun uniform3f(location: GlUniformLocationRef, x: Float, y: Float, z: Float) {
		before()
		wrapped.uniform3f(location, x, y, z)
		after()
	}

	override fun uniform3fv(location: GlUniformLocationRef, v: NativeReadBuffer<Float>) {
		before()
		wrapped.uniform3fv(location, v)
		after()
	}

	override fun uniform3i(location: GlUniformLocationRef, x: Int, y: Int, z: Int) {
		before()
		wrapped.uniform3i(location, x, y, z)
		after()
	}

	override fun uniform3iv(location: GlUniformLocationRef, v: NativeReadBuffer<Int>) {
		before()
		wrapped.uniform3iv(location, v)
		after()
	}

	override fun uniform4f(location: GlUniformLocationRef, x: Float, y: Float, z: Float, w: Float) {
		before()
		wrapped.uniform4f(location, x, y, z, w)
		after()
	}

	override fun uniform4fv(location: GlUniformLocationRef, v: NativeReadBuffer<Float>) {
		before()
		wrapped.uniform4fv(location, v)
		after()
	}

	override fun uniform4i(location: GlUniformLocationRef, x: Int, y: Int, z: Int, w: Int) {
		before()
		wrapped.uniform4i(location, x, y, z, w)
		after()
	}

	override fun uniform4iv(location: GlUniformLocationRef, v: NativeReadBuffer<Int>) {
		before()
		wrapped.uniform4iv(location, v)
		after()
	}

	override fun uniformMatrix2fv(location: GlUniformLocationRef, transpose: Boolean, value: NativeReadBuffer<Float>) {
		before()
		wrapped.uniformMatrix2fv(location, transpose, value)
		after()
	}

	override fun uniformMatrix3fv(location: GlUniformLocationRef, transpose: Boolean, value: NativeReadBuffer<Float>) {
		before()
		wrapped.uniformMatrix3fv(location, transpose, value)
		after()
	}

	override fun uniformMatrix4fv(location: GlUniformLocationRef, transpose: Boolean, value: NativeReadBuffer<Float>) {
		before()
		wrapped.uniformMatrix4fv(location, transpose, value)
		after()
	}

	override fun useProgram(program: GlProgramRef?) {
		before()
		wrapped.useProgram(program)
		after()
	}

	override fun validateProgram(program: GlProgramRef) {
		before()
		wrapped.validateProgram(program)
		after()
	}

	override fun vertexAttrib1f(index: Int, x: Float) {
		before()
		wrapped.vertexAttrib1f(index, x)
		after()
	}

	override fun vertexAttrib1fv(index: Int, values: NativeReadBuffer<Float>) {
		before()
		wrapped.vertexAttrib1fv(index, values)
		after()
	}

	override fun vertexAttrib2f(index: Int, x: Float, y: Float) {
		before()
		wrapped.vertexAttrib2f(index, x, y)
		after()
	}

	override fun vertexAttrib2fv(index: Int, values: NativeReadBuffer<Float>) {
		before()
		wrapped.vertexAttrib2fv(index, values)
		after()
	}

	override fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float) {
		before()
		wrapped.vertexAttrib3f(index, x, y, z)
		after()
	}

	override fun vertexAttrib3fv(index: Int, values: NativeReadBuffer<Float>) {
		before()
		wrapped.vertexAttrib3fv(index, values)
		after()
	}

	override fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) {
		before()
		wrapped.vertexAttrib4f(index, x, y, z, w)
		after()
	}

	override fun vertexAttrib4fv(index: Int, values: NativeReadBuffer<Float>) {
		before()
		wrapped.vertexAttrib4fv(index, values)
		after()
	}

	override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
		before()
		wrapped.vertexAttribPointer(index, size, type, normalized, stride, offset)
		after()
	}

	override fun viewport(x: Int, y: Int, width: Int, height: Int) {
		before()
		wrapped.viewport(x, y, width, height)
		after()
	}

	override fun getUniformb(program: GlProgramRef, location: GlUniformLocationRef): Boolean {
		before()
		val ret = wrapped.getUniformb(program, location)
		after()
		return ret
	}

	override fun getUniformi(program: GlProgramRef, location: GlUniformLocationRef): Int {
		before()
		val ret = wrapped.getUniformi(program, location)
		after()
		return ret
	}

	override fun getUniformf(program: GlProgramRef, location: GlUniformLocationRef): Float {
		before()
		val ret = wrapped.getUniformf(program, location)
		after()
		return ret
	}

	override fun getVertexAttribi(index: Int, pName: Int): Int {
		before()
		val ret = wrapped.getVertexAttribi(index, pName)
		after()
		return ret
	}

	override fun getVertexAttribb(index: Int, pName: Int): Boolean {
		before()
		val ret = wrapped.getVertexAttribb(index, pName)
		after()
		return ret
	}

	override fun getTexParameter(target: Int, pName: Int): Int {
		before()
		val ret = wrapped.getTexParameter(target, pName)
		after()
		return ret
	}

	override fun getShaderParameterb(shader: GlShaderRef, pName: Int): Boolean {
		before()
		val ret = wrapped.getShaderParameterb(shader, pName)
		after()
		return ret
	}

	override fun getShaderParameteri(shader: GlShaderRef, pName: Int): Int {
		before()
		val ret = wrapped.getShaderParameteri(shader, pName)
		after()
		return ret
	}

	override fun getRenderbufferParameter(target: Int, pName: Int): Int {
		before()
		val ret = wrapped.getRenderbufferParameter(target, pName)
		after()
		return ret
	}

	override fun getParameterb(pName: Int): Boolean {
		before()
		val ret = wrapped.getParameterb(pName)
		after()
		return ret
	}

	override fun getParameteri(pName: Int): Int {
		before()
		val ret = wrapped.getParameteri(pName)
		after()
		return ret
	}

	override fun getProgramParameterb(program: GlProgramRef, pName: Int): Boolean {
		before()
		val ret = wrapped.getProgramParameterb(program, pName)
		after()
		return ret
	}

	override fun getProgramParameteri(program: GlProgramRef, pName: Int): Int {
		before()
		val ret = wrapped.getProgramParameteri(program, pName)
		after()
		return ret
	}

	override fun getBufferParameter(target: Int, pName: Int): Int {
		before()
		val ret = wrapped.getBufferParameter(target, pName)
		after()
		return ret
	}

	override fun getFramebufferAttachmentParameteri(target: Int, attachment: Int, pName: Int): Int {
		before()
		val ret = wrapped.getFramebufferAttachmentParameteri(target, attachment, pName)
		after()
		return ret
	}

	override fun clearColor(color: ColorRo) {
		before()
		wrapped.clearColor(color)
		after()
	}

	override fun getSupportedExtensions(): List<String> {
		before()
		val ret = wrapped.getSupportedExtensions()
		after()
		return ret
	}
}