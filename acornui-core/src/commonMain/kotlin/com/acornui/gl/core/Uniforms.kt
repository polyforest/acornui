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

@file:Suppress("unused")

package com.acornui.gl.core

import com.acornui.graphic.CameraRo
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*

interface Uniforms {

	fun getUniformLocation(name: String): GlUniformLocationRef?

	fun getRequiredUniformLocation(name: String): GlUniformLocationRef {
		return checkNotNull(getUniformLocation(name)) { "Uniform not found $name" }
	}

	/**
	 * Return the uniform value at the given location for this program.
	 */
	fun getb(location: GlUniformLocationRef): Boolean

	fun getb(name: String): Boolean = getb(getRequiredUniformLocation(name))
	fun getbOptional(name: String): Boolean? {
		val loc = getUniformLocation(name) ?: return null
		return getb(loc)
	}

	/**
	 * Return the uniform value at the given location for this program.
	 */
	fun geti(location: GlUniformLocationRef): Int

	fun geti(name: String): Int = geti(getRequiredUniformLocation(name))
	fun getiOptional(name: String): Int? {
		val loc = getUniformLocation(name) ?: return null
		return geti(loc)
	}

	/**
	 * Return the uniform value at the given location for this program.
	 * If the uniform does not exist, an IllegalStateException will be thrown.
	 * If the uniform has never been set, [out] will be populated with 0.
	 */
	fun get(location: GlUniformLocationRef, out: IntArray): IntArray

	fun get(name: String, out: IntArray): IntArray = get(getRequiredUniformLocation(name), out)

	/**
	 * Return the uniform value at the location with the given name for this program.
	 * If the uniform does not exist, [out] will not be changed and null will be returned.
	 * If the uniform exists but has never been set, [out] will be populated with 0.
	 */
	fun getOptional(name: String, out: IntArray): IntArray? {
		val loc = getUniformLocation(name) ?: return null
		return get(loc, out)
	}

	fun getf(location: GlUniformLocationRef): Float
	fun getf(name: String): Float = getf(getRequiredUniformLocation(name))
	fun getfOptional(name: String): Float {
		val loc = getUniformLocation(name) ?: return 0f
		return getf(loc)
	}

	/**
	 * Return the uniform value at the given location for this program.
	 */
	fun get(location: GlUniformLocationRef, out: FloatArray): FloatArray

	fun get(name: String, out: FloatArray): FloatArray = get(getRequiredUniformLocation(name), out)

	/**
	 * Return the uniform value at the location with the given name for this program.
	 * If the uniform does not exist, [out] will not be changed and null will be returned.
	 * If the uniform exists but has never been set, [out] will be populated with 0f.
	 */
	fun getOptional(name: String, out: FloatArray): FloatArray? {
		val loc = getUniformLocation(name) ?: return out.also { it.fill(0f) }
		return get(loc, out)
	}

	fun get(location: GlUniformLocationRef, out: Matrix2): Matrix2
	fun get(name: String, out: Matrix2): Matrix2 = get(getRequiredUniformLocation(name), out)

	/**
	 * Return the uniform value at the location with the given name for this program.
	 * If the uniform does not exist, [out] will not be changed and null will be returned.
	 * If the uniform exists but has never been set, [out] will be set to the identity matrix.
	 */
	fun getOptional(name: String, out: Matrix2): Matrix2? {
		val loc = getUniformLocation(name) ?: return null
		return get(loc, out)
	}

	fun get(location: GlUniformLocationRef, out: Matrix3): Matrix3
	fun get(name: String, out: Matrix3): Matrix3 = get(getRequiredUniformLocation(name), out)

	/**
	 * Return the uniform value at the location with the given name for this program.
	 * If the uniform does not exist, [out] will not be changed and null will be returned.
	 * If the uniform exists but has never been set, [out] will be set to the identity matrix.
	 */
	fun getOptional(name: String, out: Matrix3): Matrix3? {
		val loc = getUniformLocation(name) ?: return null
		return get(loc, out)
	}

	fun get(location: GlUniformLocationRef, out: Matrix4): Matrix4
	fun get(name: String, out: Matrix4): Matrix4 = get(getRequiredUniformLocation(name), out)

	/**
	 * Return the uniform value at the location with the given name for this program.
	 * If the uniform does not exist, [out] will not be changed and null will be returned.
	 * If the uniform exists but has never been set, [out] will be set to the identity matrix.
	 */
	fun getOptional(name: String, out: Matrix4): Matrix4? {
		val loc = getUniformLocation(name) ?: return null
		return get(loc, out)
	}

	fun put(location: GlUniformLocationRef, x: Float)
	fun put(name: String, x: Float) = put(getRequiredUniformLocation(name), x)
	fun putOptional(name: String, x: Float) = getUniformLocation(name)?.let { put(it, x) }

	fun put(location: GlUniformLocationRef, x: Float, y: Float)
	fun put(name: String, x: Float, y: Float) = put(getRequiredUniformLocation(name), x, y)
	fun putOptional(name: String, x: Float, y: Float) = getUniformLocation(name)?.let { put(it, x, y) }

	fun put(location: GlUniformLocationRef, x: Float, y: Float, z: Float)
	fun put(name: String, x: Float, y: Float, z: Float) = put(getRequiredUniformLocation(name), x, y, z)
	fun putOptional(name: String, x: Float, y: Float, z: Float) = getUniformLocation(name)?.let { put(it, x, y, z) }

	fun put(location: GlUniformLocationRef, x: Float, y: Float, z: Float, w: Float)
	fun put(name: String, x: Float, y: Float, z: Float, w: Float) = put(getRequiredUniformLocation(name), x, y, z, w)
	fun putOptional(name: String, x: Float, y: Float, z: Float, w: Float) = getUniformLocation(name)?.let { put(it, x, y, z, w) }

	fun put(location: GlUniformLocationRef, x: Int)
	fun put(name: String, x: Int) = put(getRequiredUniformLocation(name), x)
	fun putOptional(name: String, x: Int) = getUniformLocation(name)?.let { put(it, x) }

	fun put(location: GlUniformLocationRef, x: Int, y: Int)
	fun put(name: String, x: Int, y: Int) = put(getRequiredUniformLocation(name), x, y)
	fun putOptional(name: String, x: Int, y: Int) = getUniformLocation(name)?.let { put(it, x, y) }

	fun put(location: GlUniformLocationRef, x: Int, y: Int, z: Int)
	fun put(name: String, x: Int, y: Int, z: Int) = put(getRequiredUniformLocation(name), x, y, z)
	fun putOptional(name: String, x: Int, y: Int, z: Int) = getUniformLocation(name)?.let { put(it, x, y, z) }

	fun put(location: GlUniformLocationRef, x: Int, y: Int, z: Int, w: Int)
	fun put(name: String, x: Int, y: Int, z: Int, w: Int) = put(getRequiredUniformLocation(name), x, y, z, w)
	fun putOptional(name: String, x: Int, y: Int, z: Int, w: Int) = getUniformLocation(name)?.let { put(it, x, y, z, w) }

	fun put(location: GlUniformLocationRef, value: Matrix2Ro)
	fun put(name: String, value: Matrix2Ro) = put(getRequiredUniformLocation(name), value)
	fun putOptional(name: String, value: Matrix2Ro) = getUniformLocation(name)?.let { put(it, value) }

	fun put(location: GlUniformLocationRef, value: Matrix3Ro)
	fun put(name: String, value: Matrix3Ro) = put(getRequiredUniformLocation(name), value)
	fun putOptional(name: String, value: Matrix3Ro) = getUniformLocation(name)?.let { put(it, value) }

	fun put(location: GlUniformLocationRef, value: Matrix4Ro)
	fun put(name: String, value: Matrix4Ro) = put(getRequiredUniformLocation(name), value)
	fun putOptional(name: String, value: Matrix4Ro) = getUniformLocation(name)?.let { put(it, value) }
}

private val rgba = FloatArray(4)
fun Uniforms.getRgba(location: GlUniformLocationRef, color: Color): Color {
	get(location, rgba)
	color.set(rgba[0], rgba[1], rgba[2], rgba[3])
	return color
}

fun Uniforms.getRgba(name: String, color: Color): Color = getRgba(getRequiredUniformLocation(name), color)

private val rgb = FloatArray(3)
fun Uniforms.getRgb(location: GlUniformLocationRef, color: Color): Color {
	get(location, rgb)
	color.set(rgba[0], rgba[1], rgba[2], 1f)
	return color
}

fun Uniforms.getRgb(name: String, color: Color): Color = getRgb(getRequiredUniformLocation(name), color)

fun Uniforms.putRgba(location: GlUniformLocationRef, color: ColorRo) =
		put(location, color.r, color.g, color.b, color.a)

fun Uniforms.putRgba(name: String, color: ColorRo) =
		put(getRequiredUniformLocation(name), color.r, color.g, color.b, color.a)

fun Uniforms.putRgbaOptional(name: String, color: ColorRo) =
		getUniformLocation(name)?.let { put(it, color.r, color.g, color.b, color.a) }

fun Uniforms.putRgb(location: GlUniformLocationRef, c: ColorRo) = put(location, c.r, c.g, c.b)
fun Uniforms.putRgb(name: String, c: ColorRo) = put(getRequiredUniformLocation(name), c.r, c.g, c.b)
fun Uniforms.putRgbOptional(name: String, c: ColorRo) = getUniformLocation(name)?.let {
	put(it, c.r, c.g, c.b)
}

fun Uniforms.put(location: GlUniformLocationRef, v: Vector3Ro) = put(location, v.x, v.y, v.z)
fun Uniforms.put(name: String, v: Vector3Ro) = put(getRequiredUniformLocation(name), v.x, v.y, v.z)
fun Uniforms.putOptional(name: String, v: Vector3Ro) = getUniformLocation(name)?.let {
	put(it, v.x, v.y, v.z)
}

fun Uniforms.put(location: GlUniformLocationRef, v: Vector2Ro) = put(location, v.x, v.y)
fun Uniforms.put(name: String, v: Vector2Ro) = put(getRequiredUniformLocation(name), v.x, v.y)
fun Uniforms.putOptional(name: String, v: Vector2Ro) = getUniformLocation(name)?.let {
	put(it, v.x, v.y)
}

class UniformsImpl(private val gl: CachedGl20) : Uniforms {

	private val program: GlProgramRef
		get() = gl.program ?: error("No shader program is bound.")

	override fun getUniformLocation(name: String): GlUniformLocationRef? {
		return gl.getUniformLocation(program, name)
	}

	override fun getb(location: GlUniformLocationRef): Boolean {
		return gl.getUniformb(program, location)
	}

	override fun geti(location: GlUniformLocationRef): Int {
		return gl.getUniformi(program, location)
	}

	override fun get(location: GlUniformLocationRef, out: IntArray): IntArray {
		return gl.getUniformiv(program, location, out)
	}

	override fun getf(location: GlUniformLocationRef): Float {
		return gl.getUniformf(program, location)
	}

	override fun get(location: GlUniformLocationRef, out: FloatArray): FloatArray {
		return gl.getUniformfv(program, location, out)
	}

	override fun get(location: GlUniformLocationRef, out: Matrix2): Matrix2 {
		return gl.getUniformfv(program, location, out)
	}

	override fun get(location: GlUniformLocationRef, out: Matrix3): Matrix3 {
		return gl.getUniformfv(program, location, out)
	}

	override fun get(location: GlUniformLocationRef, out: Matrix4): Matrix4 {
		return gl.getUniformfv(program, location, out)
	}

	override fun put(location: GlUniformLocationRef, x: Int) {
		gl.uniform1i(location, x)
	}

	override fun put(location: GlUniformLocationRef, x: Int, y: Int) {
		gl.uniform2i(location, x, y)
	}

	override fun put(location: GlUniformLocationRef, x: Int, y: Int, z: Int) {
		gl.uniform3i(location, x, y, z)
	}

	override fun put(location: GlUniformLocationRef, x: Int, y: Int, z: Int, w: Int) {
		gl.uniform4i(location, x, y, z, w)
	}

	override fun put(location: GlUniformLocationRef, x: Float) {
		gl.uniform1f(location, x)
	}

	override fun put(location: GlUniformLocationRef, x: Float, y: Float) {
		gl.uniform2f(location, x, y)
	}

	override fun put(location: GlUniformLocationRef, x: Float, y: Float, z: Float) {
		gl.uniform3f(location, x, y, z)
	}

	override fun put(location: GlUniformLocationRef, x: Float, y: Float, z: Float, w: Float) {
		gl.uniform4f(location, x, y, z, w)
	}

	override fun put(location: GlUniformLocationRef, value: Matrix2Ro) {
		gl.uniformMatrix2fv(location, value.values)
	}

	override fun put(location: GlUniformLocationRef, value: Matrix3Ro) {
		gl.uniformMatrix3fv(location, value.values)
	}

	override fun put(location: GlUniformLocationRef, value: Matrix4Ro) {
		gl.uniformMatrix4fv(location, value.values)
	}

}

fun Uniforms.getColorTransformation(out: ColorTransformation): ColorTransformation? {
	val useColorTransU = getUniformLocation(CommonShaderUniforms.U_USE_COLOR_TRANS) ?: return null
	if (!getb(useColorTransU)) return null
	val colorTrans = Matrix4.obtain()
	get(CommonShaderUniforms.U_COLOR_TRANS, colorTrans)
	out.matrix = colorTrans
	val colorOffset = Color.obtain()
	getRgba(CommonShaderUniforms.U_COLOR_OFFSET, colorOffset)
	out.offset = colorOffset
	Matrix4.free(colorTrans)
	Color.free(colorOffset)
	return out
}

fun Uniforms.setColorTransformation(value: ColorTransformationRo?) {
	if (value == null) {
		putOptional(CommonShaderUniforms.U_USE_COLOR_TRANS, 0)
	} else {
		putOptional(CommonShaderUniforms.U_USE_COLOR_TRANS, 1)
		putOptional(CommonShaderUniforms.U_COLOR_TRANS, value.matrix)
		putRgbaOptional(CommonShaderUniforms.U_COLOR_OFFSET, value.offset)
	}
}

/**
 * Sets the color transformation to the current color transformation multiplied by [value], calls [inner], then resets
 * the color transformation back to its previous value.
 */
fun Uniforms.mulColorTransformation(value: ColorTransformationRo?, inner: () -> Unit) {
	val cT = ColorTransformation.obtain()
	val previous = getColorTransformation(cT)
	val combined = if (previous == null || value == null) value else previous.mul(value)
	setColorTransformation(combined)
	inner()
	setColorTransformation(previous)
	ColorTransformation.free(cT)
}

fun Uniforms.getCamera(viewProjectionOut: Matrix4, viewTransformOut: Matrix4, modelTransformOut: Matrix4) {
	val uProjTrans = getUniformLocation(CommonShaderUniforms.U_PROJ_TRANS)
	if (uProjTrans == null) {
		viewProjectionOut.idt()
	} else {
		get(uProjTrans, viewProjectionOut)
	}
	val uViewTrans = getUniformLocation(CommonShaderUniforms.U_VIEW_TRANS)
	if (uViewTrans == null) {
		viewTransformOut.idt()
	} else {
		get(uViewTrans, viewTransformOut)
	}
	val uModelTrans = getUniformLocation(CommonShaderUniforms.U_MODEL_TRANS)
	if (uModelTrans == null) {
		modelTransformOut.idt()
	} else {
		get(uModelTrans, modelTransformOut)
	}
}

private val mvp = Matrix4()
private val tmpMat = Matrix3()

/**
 * Sets the model, view, and projection matrices.
 * This will set the gl uniforms `u_modelTrans` (if exists), `u_viewTrans` (if exists), and `u_projTrans`
 * The shader should have the following optional uniforms:
 * `u_projTrans` - Either MVP or VP if u_modelTrans is present.
 * `u_modelTrans` - M
 * `u_viewTrans` - V
 * `u_normalTrans`
 */
fun Uniforms.setCamera(viewProjection: Matrix4Ro, viewTransform: Matrix4Ro, modelTransform: Matrix4Ro) {
	val hasModel = getUniformLocation(CommonShaderUniforms.U_MODEL_TRANS) != null
	if (hasModel) {
		putOptional(CommonShaderUniforms.U_PROJ_TRANS, viewProjection)
		putOptional(CommonShaderUniforms.U_VIEW_TRANS, viewTransform)
		putOptional(CommonShaderUniforms.U_MODEL_TRANS, modelTransform)
		getUniformLocation(CommonShaderUniforms.U_NORMAL_TRANS)?.let {
			tmpMat.set(modelTransform).setTranslation(0f, 0f).inv().tra()
			put(it, tmpMat)
		}
	} else {
		val v = if (modelTransform.isIdentity) viewProjection else mvp.set(viewProjection).mul(modelTransform)
		putOptional(CommonShaderUniforms.U_PROJ_TRANS, v)
	}
}

fun Uniforms.setCamera(camera: CameraRo, model: Matrix4Ro = Matrix4.IDENTITY) = setCamera(camera.viewProjectionTransform, camera.viewTransform, model)

/**
 * Temporarily uses a camera, resetting the uniforms when [inner] has completed.
 */
fun Uniforms.useCamera(viewProjection: Matrix4Ro, viewTransform: Matrix4Ro, modelTransform: Matrix4Ro, inner: () -> Unit) {
	val previousViewProjection = Matrix4()
	val previousViewTransform = Matrix4()
	val previousModelTransform = Matrix4()
	getCamera(previousViewProjection, previousViewTransform, previousModelTransform)
	setCamera(viewProjection, viewTransform, modelTransform)
	inner()
	setCamera(previousViewProjection, previousViewTransform, previousModelTransform)
}

/**
 * Temporarily uses a camera, resetting the uniforms when [inner] has completed.
 */
fun Uniforms.useCamera(camera: CameraRo, model: Matrix4Ro = Matrix4.IDENTITY, inner: () -> Unit) {
	useCamera(camera.viewProjectionTransform, camera.viewTransform, model, inner)
}