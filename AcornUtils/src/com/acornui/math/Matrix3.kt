/*
 * Derived from LibGDX by Nicholas Bilyk
 * https://github.com/libgdx
 * Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
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

import com.acornui.collection.copy
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

interface Matrix3Ro {

	val values: List<Float>

	/**
	 * @return The determinant of this matrix
	 */
	fun det(): Float

	fun getTranslation(out: Vector2): Vector2

	fun getScale(out: Vector2): Vector2

	fun getRotation(): Float

	/**
	 * Copies this matrix. Note that the [values] list will be copied as well and the new Matrix will not back the
	 * same list.
	 */
	fun copy(): Matrix3 {
		return Matrix3(values.copy())
	}
}

/**
 * A 3x3 <a href="http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column major</a> matrix; useful for 2D
 * transforms.
 *
 * @author mzechner
 */
data class Matrix3(

		override val values: MutableList<Float> = arrayListOf(
				1f, 0f, 0f,
				0f, 1f, 0f,
				0f, 0f, 1f)
) : Matrix3Ro {

	constructor (m00:Float, m10:Float, m20:Float, m01:Float, m11:Float, m21:Float, m02:Float, m12:Float, m22:Float) : this(arrayListOf(m00, m10, m20, m01, m11, m21, m02, m12, m22))

	/**
	 * Sets this matrix to the identity matrix
	 * @return This matrix for the purpose of chaining operations.
	 */
	fun idt(): Matrix3 {
		values[M00] = 1f
		values[M10] = 0f
		values[M20] = 0f
		values[M01] = 0f
		values[M11] = 1f
		values[M21] = 0f
		values[M02] = 0f
		values[M12] = 0f
		values[M22] = 1f
		return this
	}

	/**
	 * Postmultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
	 *
	 * <pre>
	 * A.mul(B) results in A := AB
	 * </pre>
	 * @param m Matrix to multiply by.
	 * @return This matrix for the purpose of chaining operations together.
	 */
	fun mul(matrix: Matrix3Ro): Matrix3 {
		mul(values, matrix.values)
		return this
	}

	operator fun times(matrix: Matrix3Ro): Matrix3 {
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
	fun mulLeft(m: Matrix3Ro): Matrix3 {
		val v00 = m.values[M00] * values[M00] + m.values[M01] * values[M10] + m.values[M02] * values[M20]
		val v01 = m.values[M00] * values[M01] + m.values[M01] * values[M11] + m.values[M02] * values[M21]
		val v02 = m.values[M00] * values[M02] + m.values[M01] * values[M12] + m.values[M02] * values[M22]

		val v10 = m.values[M10] * values[M00] + m.values[M11] * values[M10] + m.values[M12] * values[M20]
		val v11 = m.values[M10] * values[M01] + m.values[M11] * values[M11] + m.values[M12] * values[M21]
		val v12 = m.values[M10] * values[M02] + m.values[M11] * values[M12] + m.values[M12] * values[M22]

		val v20 = m.values[M20] * values[M00] + m.values[M21] * values[M10] + m.values[M22] * values[M20]
		val v21 = m.values[M20] * values[M01] + m.values[M21] * values[M11] + m.values[M22] * values[M21]
		val v22 = m.values[M20] * values[M02] + m.values[M21] * values[M12] + m.values[M22] * values[M22]

		values[M00] = v00
		values[M10] = v10
		values[M20] = v20
		values[M01] = v01
		values[M11] = v11
		values[M21] = v21
		values[M02] = v02
		values[M12] = v12
		values[M22] = v22

		return this
	}

	fun prj(vec: Vector2): Vector2 {
		val mat = values
		val x = (vec.x * mat[0] + vec.y * mat[3] + mat[6])
		val y = (vec.x * mat[1] + vec.y * mat[4] + mat[7])
		vec.x = x
		vec.y = y
		return vec
	}

	override fun toString(): String {
		return "[" + values[0] + "|" + values[3] + "|" + values[6] + "]\n" + "[" + values[1] + "|" + values[4] + "|" + values[7] + "]\n" + "[" + values[2] + "|" + values[5] + "|" + values[8] + "]"
	}

	/**
	 * @return The determinant of this matrix
	 */
	override fun det(): Float {
		return values[M00] * values[M11] * values[M22] + values[M01] * values[M12] * values[M20] + values[M02] * values[M10] * values[M21] - values[M00] * values[M12] * values[M21] - values[M01] * values[M10] * values[M22] - values[M02] * values[M11] * values[M20]
	}

	/**
	 * Inverts this matrix given that the determinant is != 0.
	 * @return This matrix for the purpose of chaining operations.
	 * @throws Exception if the matrix is singular (not invertible)
	 */
	fun inv(): Matrix3 {
		val det = det()
		if (det == 0f) throw Exception("Can't invert a singular matrix")

		val invDet = 1f / det

		tmp[M00] = values[M11] * values[M22] - values[M21] * values[M12]
		tmp[M10] = values[M20] * values[M12] - values[M10] * values[M22]
		tmp[M20] = values[M10] * values[M21] - values[M20] * values[M11]
		tmp[M01] = values[M21] * values[M02] - values[M01] * values[M22]
		tmp[M11] = values[M00] * values[M22] - values[M20] * values[M02]
		tmp[M21] = values[M20] * values[M01] - values[M00] * values[M21]
		tmp[M02] = values[M01] * values[M12] - values[M11] * values[M02]
		tmp[M12] = values[M10] * values[M02] - values[M00] * values[M12]
		tmp[M22] = values[M00] * values[M11] - values[M10] * values[M01]

		values[M00] = invDet * tmp[M00]
		values[M10] = invDet * tmp[M10]
		values[M20] = invDet * tmp[M20]
		values[M01] = invDet * tmp[M01]
		values[M11] = invDet * tmp[M11]
		values[M21] = invDet * tmp[M21]
		values[M02] = invDet * tmp[M02]
		values[M12] = invDet * tmp[M12]
		values[M22] = invDet * tmp[M22]

		return this
	}

	/**
	 * Copies the values from the provided matrix to this matrix.
	 * @param mat The matrix to copy.
	 * @return This matrix for the purposes of chaining.
	 */
	fun set(mat: Matrix3Ro): Matrix3 {
		for (i in 0..9-1) {
			values[i] = mat.values[i]
		}
		return this
	}

	/**
	 * Sets this 3x3 matrix to the top left 3x3 corner of the provided 4x4 matrix.
	 * @param mat The matrix whose top left corner will be copied. This matrix will not be modified.
	 * @return This matrix for the purpose of chaining operations.
	 */
	fun set(mat: Matrix4Ro): Matrix3 {
		values[M00] = mat.values[0]
		values[M10] = mat.values[1]
		values[M20] = mat.values[2]
		values[M01] = mat.values[4]
		values[M11] = mat.values[5]
		values[M21] = mat.values[6]
		values[M02] = mat.values[8]
		values[M12] = mat.values[9]
		values[M22] = mat.values[10]
		return this
	}

	/**
	 * Sets the matrix to the given matrix as a float array. The float array must have at least 9 elements; the first
	 * 9 will be copied.
	 *
	 * @param values The matrix, in float form, that is to be copied. Remember that this matrix is in <a
	*           href="http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column major</a> order.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun set(values: List<Float>): Matrix3 {
		for (i in 0..9 - 1) {
			this.values[i] = values[i]
		}
		return this
	}

	/**
	 * Sets this matrix to have the given translation.
	 */
	fun setTranslation(x: Float, y: Float): Matrix3 {
		values[6] = x
		values[7] = y
		return this
	}

	/**
	 * Adds a translational component to the matrix in the 3rd column. The other columns are untouched.
	 * @param x The x-component of the translation vector.
	 * @param y The y-component of the translation vector.
	 * @return This matrix for the purpose of chaining.
	 */
	fun trn(x: Float, y: Float): Matrix3 {
		values[6] += x
		values[7] += y
		return this
	}

	/**
	 * Adds a translational component to the matrix in the 3rd column. The other columns are untouched.
	 * @param vector The translation vector.
	 * @return This matrix for the purpose of chaining.
	 */
	fun trn(vector: Vector2): Matrix3 {
		values[6] += vector.x
		values[7] += vector.y
		return this
	}

	/**
	 * Adds a translational component to the matrix in the 3rd column. The other columns are untouched.
	 * @param vector The translation vector. (The z-component of the vector is ignored because this is a 3x3 matrix)
	 * @return This matrix for the purpose of chaining.
	 */
	fun trn(vector: Vector3): Matrix3 {
		values[M02] += vector.x
		values[M12] += vector.y
		return this
	}

	/**
	 * Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by
	 * OpenGL ES' 1.x glTranslate/glRotate/glScale.
	 * @param radians The angle in degrees
	 * @return This matrix for the purpose of chaining.
	 */
	fun rotate(radians: Float): Matrix3 {
		if (radians == 0f) return this
		val cos = cos(radians)
		val sin = sin(radians)

		tmp[M00] = cos
		tmp[M10] = sin
		tmp[M20] = 0f

		tmp[M01] = -sin
		tmp[M11] = cos
		tmp[M21] = 0f

		tmp[M02] = 0f
		tmp[M12] = 0f
		tmp[M22] = 1f
		mul(values, tmp)
		return this
	}

	override fun getTranslation(out: Vector2): Vector2 {
		out.x = values[M02]
		out.y = values[M12]
		return out
	}

	override fun getScale(out: Vector2): Vector2 {
		out.x = Math.sqrt((values[M00] * values[M00] + values[M01] * values[M01]).toDouble()).toFloat()
		out.y = Math.sqrt((values[M10] * values[M10] + values[M11] * values[M11]).toDouble()).toFloat()
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
	fun scl(scale: Float): Matrix3 {
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
	fun scl(scaleX: Float, scaleY: Float): Matrix3 {
		values[M00] *= scaleX
		values[M11] *= scaleY
		return this
	}

	/**
	 * Scale this matrix using the x and y components of the vector but leave the rest of the matrix alone.
	 * @param scale The {@link Vector3} to use to scale this matrix.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun scl(scale: Vector2Ro): Matrix3 {
		values[M00] *= scale.x
		values[M11] *= scale.y
		return this
	}

	/**
	 * Scale this matrix using the x and y components of the vector but leave the rest of the matrix alone.
	 * @param scale The {@link Vector3} to use to scale this matrix. The z component will be ignored.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun scl(scale: Vector3Ro): Matrix3 {
		values[M00] *= scale.x
		values[M11] *= scale.y
		return this
	}

	/**
	 * Transposes the current matrix.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun transpose(): Matrix3 {
		// Where MXY you do not have to change MXX
		val v01 = values[M10]
		val v02 = values[M20]
		val v10 = values[M01]
		val v12 = values[M21]
		val v20 = values[M02]
		val v21 = values[M12]
		values[M01] = v01
		values[M02] = v02
		values[M10] = v10
		values[M12] = v12
		values[M20] = v20
		values[M21] = v21
		return this
	}

	companion object {

		private val tmp = arrayListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)

		val IDENTITY: Matrix3Ro = Matrix3()

		const val M00: Int = 0
		const val M01: Int = 3
		const val M02: Int = 6
		const val M10: Int = 1
		const val M11: Int = 4
		const val M12: Int = 7
		const val M20: Int = 2
		const val M21: Int = 5
		const val M22: Int = 8

		/**
		 * Multiplies matrix a with matrix b in the following manner:
		 *
		 * <pre>
		 * mul(A, B) => A := AB
		 * </pre>
		 * @param matA The float array representing the first matrix. Must have at least 9 elements.
		 * @param matB The float array representing the second matrix. Must have at least 9 elements.
		 */
		private fun mul(matA: MutableList<Float>, matB: List<Float>) {
			val v00 = matA[0] * matB[0] + matA[3] * matB[1] + matA[6] * matB[2]
			val v01 = matA[0] * matB[3] + matA[3] * matB[4] + matA[6] * matB[5]
			val v02 = matA[0] * matB[6] + matA[3] * matB[7] + matA[6] * matB[8]

			val v10 = matA[1] * matB[0] + matA[4] * matB[1] + matA[7] * matB[2]
			val v11 = matA[1] * matB[3] + matA[4] * matB[4] + matA[7] * matB[5]
			val v12 = matA[1] * matB[6] + matA[4] * matB[7] + matA[7] * matB[8]

			val v20 = matA[2] * matB[0] + matA[5] * matB[1] + matA[8] * matB[2]
			val v21 = matA[2] * matB[3] + matA[5] * matB[4] + matA[8] * matB[5]
			val v22 = matA[2] * matB[6] + matA[5] * matB[7] + matA[8] * matB[8]

			matA[0] = v00
			matA[1] = v10
			matA[2] = v20
			matA[3] = v01
			matA[4] = v11
			matA[5] = v21
			matA[6] = v02
			matA[7] = v12
			matA[8] = v22
		}
	}

}
