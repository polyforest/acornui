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

package com.acornui.math

import com.acornui.recycle.ObjectPool
import kotlinx.serialization.*
import kotlinx.serialization.internal.FloatSerializer
import kotlinx.serialization.internal.StringDescriptor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A read-only view of [Matrix2]
 */
@Serializable(with = Matrix2Serializer::class)
interface Matrix2Ro {

	val values: FloatArray

	/**
	 * @return The determinant of this matrix
	 */
	fun det(): Float

	fun getScale(out: Vector2): Vector2

	fun getRotation(): Float

	/**
	 * Copies this matrix. Note that the [values] list will be copied as well and the new Matrix will not back the
	 * same list.
	 */
	fun copy(): Matrix2 {
		return Matrix2(values)
	}
}

/**
 * A 2x2 matrix; useful for 2D transforms without translation.
 */
@Serializable(with = Matrix2Serializer::class)
class Matrix2() : Matrix2Ro {

	override val values = floatArrayOf(
		1f, 0f,
		0f, 1f)

	/**
	 * Constructs this 2x2 matrix with column-major values.
	 */
	constructor (m00:Float, m10:Float, m01:Float, m11:Float) : this(floatArrayOf(m00, m10, m01, m11))
	
	constructor(values: FloatArray) : this() {
		set(values)
	}
	
	constructor(values: List<Float>) : this() {
		set(values)
	}

	operator fun set(index: Int, value: Float) {
		values[index] = value
	}


	/**
	 * Sets this matrix to the identity matrix
	 * @return This matrix for the purpose of chaining operations.
	 */
	fun idt(): Matrix2 {
		values[M00] = 1f
		values[M10] = 0f
		values[M01] = 0f
		values[M11] = 1f
		return this
	}

	/**
	 * Postmultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
	 *
	 * <pre>
	 * A.mul(B) results in A := AB
	 * </pre>
	 * @param matrix Matrix to multiply by.
	 * @return This matrix for the purpose of chaining operations together.
	 */
	fun mul(matrix: Matrix2Ro): Matrix2 {
		mul(values, matrix.values)
		return this
	}

	operator fun times(matrix: Matrix2Ro): Matrix2 {
		return copy().mul(matrix)
	}

	/**
	 * Premultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
	 *
	 * <pre>
	 * A.mulLeft(B) results in A := BA
	 * </pre>
	 * @param m The other Matrix to multiply by
	 * @return This matrix for the purpose of chaining operations.
	 */
	fun mulLeft(m: Matrix2Ro): Matrix2 {
		val v00 = m.values[M00] * values[M00] + m.values[M01] * values[M10]
		val v01 = m.values[M00] * values[M01] + m.values[M01] * values[M11]

		val v10 = m.values[M10] * values[M00] + m.values[M11] * values[M10]
		val v11 = m.values[M10] * values[M01] + m.values[M11] * values[M11]

		values[M00] = v00
		values[M10] = v10
		values[M01] = v01
		values[M11] = v11

		return this
	}

	fun prj(vec: Vector2): Vector2 {
		val mat = values
		val x = (vec.x * mat[M00] + vec.y * mat[M01])
		val y = (vec.x * mat[M10] + vec.y * mat[M11])
		vec.x = x
		vec.y = y
		return vec
	}

	override fun toString(): String {
		val values = values
		return """[${values[M00]}|${values[M01]}}]
[${values[M10]}|${values[M11]}}]"""
	}

	/**
	 * @return The determinant of this matrix
	 */
	override fun det(): Float {
		val values = values
		return values[M00] * values[M11] - values[M10] * values[M01]
	}

	/**
	 * Inverts this matrix given that the determinant is != 0.
	 * @return This matrix for the purpose of chaining operations.
	 * @throws Exception if the matrix is singular (not invertible)
	 */
	fun inv(): Matrix2 {
		val det = det()
		if (det == 0f) throw Exception("Can't invert a singular matrix")
		val values = values
		val invDet = 1f / det

		val a = values[M00]
		val b = values[M10]
		val c = values[M01]
		val d = values[M11]

		values[M00] = invDet * d
		values[M10] = invDet * -b
		values[M01] = invDet * -c
		values[M11] = invDet * a

		return this
	}

	/**
	 * Copies the values from the provided matrix to this matrix.
	 * @param mat The matrix to copy.
	 * @return This matrix for the purposes of chaining.
	 */
	fun set(mat: Matrix2Ro): Matrix2 {
		val v = mat.values
		for (i in 0..3) {
			values[i] = v[i]
		}
		return this
	}

	/**
	 * Sets this 3x3 matrix to the top left 3x3 corner of the provided 4x4 matrix.
	 * @param mat The matrix whose top left corner will be copied. This matrix will not be modified.
	 * @return This matrix for the purpose of chaining operations.
	 */
	fun set(mat: Matrix3Ro): Matrix2 {
		values[M00] = mat.values[Matrix3.M00]
		values[M10] = mat.values[Matrix3.M10]
		values[M01] = mat.values[Matrix3.M01]
		values[M11] = mat.values[Matrix3.M11]
		return this
	}

	/**
	 * Sets the matrix to the given matrix as a float list. The float array must have at least 4 elements; the first
	 * 4 will be copied.
	 *
	 * @param v The matrix, in float form, that is to be copied. Remember that this matrix is in <a
	*           href="http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column major</a> order.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun set(v: List<Float>): Matrix2 {
		val values = values
		for (i in 0..3) {
			values[i] = v[i]
		}
		return this
	}
	
	/**
	 * Sets the matrix to the given matrix as a float array. The float array must have at least 4 elements; the first
	 * 4 will be copied.
	 *
	 * @param v The matrix, in float form, that is to be copied. Remember that this matrix is in <a
	*           href="http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column major</a> order.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun set(v: FloatArray): Matrix2 {
		v.copyInto(values, 0, 0, 4)
		return this
	}

	/**
	 * Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by
	 * OpenGL ES' 1.x glTranslate/glRotate/glScale.
	 * @param radians The angle in degrees
	 * @return This matrix for the purpose of chaining.
	 */
	fun rotate(radians: Float): Matrix2 {
		if (radians == 0f) return this
		val cos = cos(radians)
		val sin = sin(radians)

		tmp[M00] = cos
		tmp[M10] = sin

		tmp[M01] = -sin
		tmp[M11] = cos

		mul(values, tmp)
		return this
	}

	/**
	 * Transposes the matrix.
	 *
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun tra(): Matrix2 {
		val values = values
		val b = values[M10]
		val c = values[M01]
		values[M10] = c
		values[M01] = b
		return this
	}

	override fun getScale(out: Vector2): Vector2 {
		out.x = sqrt((values[M00] * values[M00] + values[M01] * values[M01]).toDouble()).toFloat()
		out.y = sqrt((values[M10] * values[M10] + values[M11] * values[M11]).toDouble()).toFloat()
		return out
	}

	override fun getRotation(): Float {
		return atan2(values[M10], values[M00])
	}

	/**
	 * Scale this matrix in both the x and y components by the scalar value.
	 * @param scale The single value that will be used to scale both the x and y components.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun scl(scale: Float): Matrix2 {
		values[M00] *= scale
		values[M11] *= scale
		return this
	}

	/**
	 * Scale this matrix's x,y components
	 * @param scaleX
	 * @param scaleY
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun scl(scaleX: Float, scaleY: Float): Matrix2 {
		values[M00] *= scaleX
		values[M11] *= scaleY
		return this
	}

	/**
	 * Scale this matrix using the x and y components of the vector but leave the rest of the matrix alone.
	 * @param scale The {@link Vector3} to use to scale this matrix.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun scl(scale: Vector2Ro): Matrix2 {
		values[M00] *= scale.x
		values[M11] *= scale.y
		return this
	}

	/**
	 * Scale this matrix using the x and y components of the vector but leave the rest of the matrix alone.
	 * @param scale The {@link Vector3} to use to scale this matrix. The z component will be ignored.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun scl(scale: Vector3Ro): Matrix2 {
		values[M00] *= scale.x
		values[M11] *= scale.y
		return this
	}

	/**
	 * Transposes the current matrix.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun transpose(): Matrix2 {
		val v01 = values[M10]
		val v10 = values[M01]
		values[M01] = v01
		values[M10] = v10
		return this
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as Matrix2Ro
		return values contentEquals (other.values)
	}

	override fun hashCode(): Int {
		return values.hashCode()
	}


	companion object {

		val IDENTITY: Matrix2Ro = Matrix2()

		const val M00: Int = 0
		const val M01: Int = 2
		const val M10: Int = 1
		const val M11: Int = 3

		private val tmp = floatArrayOf(0f, 0f, 0f, 0f)

		/**
		 * Multiplies matrix a with matrix b in the following manner:
		 *
		 * <pre>
		 * mul(A, B) => A := AB
		 * </pre>
		 * @param matA The float array representing the first matrix. Must have at least 4 elements.
		 * @param matB The float array representing the second matrix. Must have at least 4 elements.
		 */
		private fun mul(matA: FloatArray, matB: FloatArray) {
			val v00 = matA[M00] * matB[M00] + matA[M01] * matB[M10]
			val v01 = matA[M00] * matB[M01] + matA[M01] * matB[M11]

			val v10 = matA[M10] * matB[M00] + matA[M11] * matB[M10]
			val v11 = matA[M10] * matB[M01] + matA[M11] * matB[M11]

			matA[M00] = v00
			matA[M10] = v10
			matA[M01] = v01
			matA[M11] = v11
		}

		private val pool = ObjectPool { Matrix2() }

		fun obtain(): Matrix2 = pool.obtain()
		fun free(value: Matrix2) {
			value.idt()
			pool.free(value)
		}
	}
}


@Serializer(forClass = Matrix2::class)
object Matrix2Serializer : KSerializer<Matrix2> {

	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("Matrix2")

	override fun serialize(encoder: Encoder, obj: Matrix2) {
		encoder.encodeSerializableValue(FloatSerializer.list, obj.values.toList())
	}

	override fun deserialize(decoder: Decoder): Matrix2 {
		val values = decoder.decodeSerializableValue(FloatSerializer.list)
		return Matrix2(values)
	}
}
