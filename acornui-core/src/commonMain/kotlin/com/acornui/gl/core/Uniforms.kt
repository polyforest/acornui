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
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import kotlin.collections.HashMap
import kotlin.collections.MutableMap
import kotlin.collections.contentEquals
import kotlin.collections.copyInto
import kotlin.collections.fill
import kotlin.collections.getOrPut
import kotlin.collections.set

interface Uniforms {

	val changing: Signal<()->Unit>

	val isBound: Boolean
	fun bind()
	fun unbind()

	fun getUniformLocation(name: String): GlUniformLocationRef?

	fun getRequiredUniformLocation(name: String): GlUniformLocationRef {
		return checkNotNull(getUniformLocation(name)) { "Uniform not found $name" }
	}

	/**
	 * Return the uniform value at the given location for this program.
	 */
	fun getb(location: GlUniformLocationRef): Boolean = geti(location) > 0

	fun getb(name: String): Boolean = getb(getRequiredUniformLocation(name))

	/**
	 * Return the uniform value at the given location for this program.
	 */
	fun geti(location: GlUniformLocationRef): Int

	fun geti(name: String): Int = geti(getRequiredUniformLocation(name))

	fun get(location: GlUniformLocationRef, out: IntArray): IntArray

	/**
	 * Return the uniform value at the given location for this program.
	 * If the uniform does not exist, an IllegalStateException will be thrown.
	 * If the uniform has never been set, [out] will be populated with 0.
	 */
	fun get(name: String, out: IntArray): IntArray = get(getRequiredUniformLocation(name), out)

	fun getf(location: GlUniformLocationRef): Float

	fun getf(name: String): Float = getf(getRequiredUniformLocation(name))

	/**
	 * Return the uniform value at the given location for this program.
	 */
	fun get(location: GlUniformLocationRef, out: FloatArray): FloatArray

	/**
	 * Return the uniform value at the given location for this program.
	 * If the uniform does not exist, an IllegalStateException will be thrown.
	 * If the uniform has never been set, [out] will be populated with 0f.
	 */
	fun get(name: String, out: FloatArray): FloatArray = get(getRequiredUniformLocation(name), out)

	fun get(location: GlUniformLocationRef, out: Matrix2): Matrix2
	fun get(name: String, out: Matrix2): Matrix2 = get(getRequiredUniformLocation(name), out)

	fun get(location: GlUniformLocationRef, out: Matrix3): Matrix3
	fun get(name: String, out: Matrix3): Matrix3 = get(getRequiredUniformLocation(name), out)

	fun get(location: GlUniformLocationRef, out: Matrix4): Matrix4
	fun get(name: String, out: Matrix4): Matrix4 = get(getRequiredUniformLocation(name), out)

	fun put(location: GlUniformLocationRef, v: FloatArray)
	fun put(name: String, v: FloatArray) = put(getRequiredUniformLocation(name), v)
	fun putOptional(name: String, v: FloatArray) = getUniformLocation(name)?.let { put(it, v) }

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

	fun put(location: GlUniformLocationRef, v: IntArray)
	fun put(name: String, v: IntArray) = put(getRequiredUniformLocation(name), v)
	fun putOptional(name: String, v: IntArray) = getUniformLocation(name)?.let { put(it, v) }

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

class UniformsImpl(private val gl: Gl20, private val program: GlProgramRef) : Uniforms {

	private val _changing = Signal0()
	override val changing = _changing.asRo()

	private val uniformLocationCache = stringMapOf<GlUniformLocationRef?>()

	private val uniformsI = HashMap<GlUniformLocationRef, Int>()
	private val uniformsIv = HashMap<GlUniformLocationRef, IntArray>()

	private val uniformsF = HashMap<GlUniformLocationRef, Float>()
	private val uniformsFv = HashMap<GlUniformLocationRef, FloatArray>()

	private val uniformsMat2 = HashMap<GlUniformLocationRef, Matrix2>()
	private val uniformsMat3 = HashMap<GlUniformLocationRef, Matrix3>()
	private val uniformsMat4 = HashMap<GlUniformLocationRef, Matrix4>()

	private var _isBound: Boolean = false
	override val isBound: Boolean
		get() = _isBound

	override fun bind() {
		_isBound = true
	}

	override fun unbind() {
		_isBound = false
	}

	override fun getUniformLocation(name: String): GlUniformLocationRef? {
		if (!uniformLocationCache.containsKey(name)) {
			uniformLocationCache[name] = gl.getUniformLocation(program, name)
		}
		return uniformLocationCache[name]
	}

	override fun geti(location: GlUniformLocationRef): Int {
		return uniformsI[location] ?: 0
	}

	override fun get(location: GlUniformLocationRef, out: IntArray): IntArray {
		val v = uniformsIv[location] ?: return out.also { it.fill(0) }
		return v.copyInto(out)
	}

	override fun getf(location: GlUniformLocationRef): Float {
		return uniformsF[location] ?: 0f
	}

	override fun get(location: GlUniformLocationRef, out: FloatArray): FloatArray {
		val v = uniformsFv[location] ?: return out.also { it.fill(0f) }
		return v.copyInto(out)
	}

	override fun get(location: GlUniformLocationRef, out: Matrix2): Matrix2 {
		return out.set(uniformsMat2[location] ?: Matrix2.IDENTITY)
	}

	override fun get(location: GlUniformLocationRef, out: Matrix3): Matrix3 {
		return out.set(uniformsMat3[location] ?: Matrix3.IDENTITY)
	}

	override fun get(location: GlUniformLocationRef, out: Matrix4): Matrix4 {
		return out.set(uniformsMat4[location] ?: Matrix4.IDENTITY)
	}

	override fun put(location: GlUniformLocationRef, v: IntArray) {
		checkBound()
		require(v.size in 1..4)
		val existing = uniformsIv.getOrPut(location) { IntArray(v.size) }
		if (!existing.contentEquals(v)) {
			_changing.dispatch()
			v.copyInto(existing)
			when (v.size) {
				1 -> gl.uniform1iv(location, v)
				2 -> gl.uniform2iv(location, v)
				3 -> gl.uniform3iv(location, v)
				4 -> gl.uniform4iv(location, v)
			}
		}
	}

	override fun put(location: GlUniformLocationRef, x: Int) {
		uniformsI.change(location, x) {
			gl.uniform1i(location, x)
		}
	}

	override fun put(location: GlUniformLocationRef, x: Int, y: Int) {
		checkBound()
		val existing = uniformsIv.getOrPut(location) { IntArray(2) }
		if (existing[0] != x || existing[1] != y) {
			_changing.dispatch()
			existing[0] = x
			existing[1] = y
			gl.uniform2i(location, x, y)
		}
	}

	override fun put(location: GlUniformLocationRef, x: Int, y: Int, z: Int) {
		checkBound()
		val existing = uniformsIv.getOrPut(location) { IntArray(3) }
		if (existing[0] != x || existing[1] != y || existing[2] != z) {
			_changing.dispatch()
			existing[0] = x
			existing[1] = y
			existing[2] = z
			gl.uniform3i(location, x, y, z)
		}
	}

	override fun put(location: GlUniformLocationRef, x: Int, y: Int, z: Int, w: Int) {
		checkBound()
		val existing = uniformsIv.getOrPut(location) { IntArray(4) }
		if (existing[0] != x || existing[1] != y || existing[2] != z || existing[3] != w) {
			_changing.dispatch()
			existing[0] = x
			existing[1] = y
			existing[2] = z
			existing[3] = w
			gl.uniform4i(location, x, y, z, w)
		}
	}

	override fun put(location: GlUniformLocationRef, v: FloatArray) {
		checkBound()
		require(v.size in 1..4)
		val existing = uniformsFv.getOrPut(location) { FloatArray(v.size) }
		if (!existing.contentEquals(v)) {
			_changing.dispatch()
			v.copyInto(existing)
			when (v.size) {
				1 -> gl.uniform1fv(location, v)
				2 -> gl.uniform2fv(location, v)
				3 -> gl.uniform3fv(location, v)
				4 -> gl.uniform4fv(location, v)
			}
		}
	}

	override fun put(location: GlUniformLocationRef, x: Float) {
		uniformsF.change(location, x) {
			gl.uniform1f(location, x)
		}
	}

	override fun put(location: GlUniformLocationRef, x: Float, y: Float) {
		checkBound()
		val existing = uniformsFv.getOrPut(location) { FloatArray(2) }
		if (existing[0] != x || existing[1] != y) {
			_changing.dispatch()
			existing[0] = x
			existing[1] = y
			gl.uniform2f(location, x, y)
		}
	}

	override fun put(location: GlUniformLocationRef, x: Float, y: Float, z: Float) {
		checkBound()
		val existing = uniformsFv.getOrPut(location) { FloatArray(3) }
		if (existing[0] != x || existing[1] != y || existing[2] != z) {
			_changing.dispatch()
			existing[0] = x
			existing[1] = y
			existing[2] = z
			gl.uniform3f(location, x, y, z)
		}
	}

	override fun put(location: GlUniformLocationRef, x: Float, y: Float, z: Float, w: Float) {
		checkBound()
		val existing = uniformsFv.getOrPut(location) { FloatArray(4) }
		if (existing[0] != x || existing[1] != y || existing[2] != z || existing[3] != w) {
			_changing.dispatch()
			existing[0] = x
			existing[1] = y
			existing[2] = z
			existing[3] = w
			gl.uniform4f(location, x, y, z, w)
		}
	}

	override fun put(location: GlUniformLocationRef, value: Matrix2Ro) {
		checkBound()
		val existing = uniformsMat2.getOrPut(location) { Matrix2() }
		if (existing != value) {
			_changing.dispatch()
			existing.set(value)
			gl.uniformMatrix2fv(location, false, value.values)
		}
	}

	override fun put(location: GlUniformLocationRef, value: Matrix3Ro) {
		checkBound()
		val existing = uniformsMat3.getOrPut(location) { Matrix3() }
		if (existing != value) {
			_changing.dispatch()
			existing.set(value)
			gl.uniformMatrix3fv(location, false, value.values)
		}
	}

	override fun put(location: GlUniformLocationRef, value: Matrix4Ro) {
		checkBound()
		val existing = uniformsMat4.getOrPut(location) { Matrix4() }
		if (existing != value) {
			_changing.dispatch()
			existing.set(value)
			gl.uniformMatrix4fv(location, false, value.values)
		}
	}

	private fun checkBound() {
		check(isBound) { "Shader must be bound" }
	}

	private inline fun <T> MutableMap<GlUniformLocationRef, T>.change(location: GlUniformLocationRef, newValue: T, onChanged: () -> Unit) {
		checkBound()
		if (this[location] != newValue) {
			_changing.dispatch()
			this[location] = newValue
			onChanged()
		}
	}
}