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

import com.acornui.collection.ArrayList
import com.acornui.collection.FloatList
import kotlin.math.abs
import kotlin.math.sqrt

interface Matrix4Ro {

	val values: List<Float>

	/**
	 * @return The determinant of this matrix
	 */
	fun det(): Float

	/**
	 * @return The determinant of the 3x3 upper left matrix
	 */
	fun det3x3(): Float

	/**
	 * The x translation.
	 */
	val translationX: Float

	/**
	 * The y translation.
	 */
	val translationY: Float

	/**
	 * The z translation.
	 */
	val translationZ: Float

	/**
	 * Sets the provided [out] Vector3 with the translation of this Matrix.
	 */
	fun getTranslation(out: Vector3): Vector3

	/**
	 * Gets the rotation of this matrix.
	 * @param out The {@link Quaternion} to receive the rotation
	 * @param normalizeAxes True to normalize the axes, necessary when the matrix might also include scaling.
	 * @return The provided {@link Quaternion} for chaining.
	 */
	fun getRotation(out: Quaternion, normalizeAxes: Boolean = false): Quaternion

	/**
	 * @return the squared scale factor on the X axis
	 */
	fun getScaleXSquared(): Float

	/**
	 * @return the squared scale factor on the Y axis
	 */
	fun getScaleYSquared(): Float

	/**
	 * @return the squared scale factor on the Z axis
	 */
	fun getScaleZSquared(): Float

	/**
	 * @return the scale factor on the X axis (non-negative)
	 */
	fun getScaleX(): Float

	/**
	 * @return the scale factor on the Y axis (non-negative)
	 */
	fun getScaleY(): Float

	/**
	 * @return the scale factor on the X axis (non-negative)
	 */
	fun getScaleZ(): Float

	/**
	 * @param scale The vector which will receive the (non-negative) scale components on each axis.
	 * @return The provided vector for chaining.
	 */
	fun getScale(scale: Vector3): Vector3

	/**
	 * Multiplies the vector with this matrix, performing a division by w.
	 *
	 * @param vec the vector.
	 * @return Returns the vec for chaining.
	 */
	fun prj(vec: Vector3): Vector3

	/**
	 * Multiplies the vector with this matrix, performing a division by w.
	 *
	 * @param vec the vector.
	 * @return Returns the vec for chaining.
	 */
	fun prj(vec: Vector2): Vector2

	/**
	 * Multiplies the vector with the top most 3x3 sub-matrix of this matrix.
	 * @return Returns the vec for chaining.
	 */
	fun rot(vec: Vector3): Vector3

	/**
	 * Multiplies the vector with the top most 3x3 sub-matrix of this matrix.
	 * @return Returns the vec for chaining.
	 */
	fun rot(vec: Vector2): Vector2

	/**
	 * Copies the 4x3 upper-left sub-matrix into float array. The destination array is supposed to be a column major matrix.
	 * @param out the destination matrix
	 * @return Returns the [out] parameter.
	 */
	fun extract4x3Matrix(out: MutableList<Float>): MutableList<Float>

	/**
	 * Copies this matrix. Note that the [values] list will be copied as well and the new Matrix will not back the
	 * same list.
	 */
	fun copy(): Matrix4 {
		return Matrix4(FloatList(values.toFloatArray()))
	}

}

/**
 * Encapsulates a <a href="http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column major</a> 4 by 4 matrix. Like
 * the {@link Vector3} class it allows the chaining of methods by returning a reference to itself. For example:
 *
 * <pre>
 * Matrix4 mat = new Matrix4().trn(position).mul(camera.combined);
 * </pre>
 *
 * @author badlogicgames@gmail.com
 */
class Matrix4(

		override val values: FloatList = FloatList(floatArrayOf(
				1f, 0f, 0f, 0f,
				0f, 1f, 0f, 0f,
				0f, 0f, 1f, 0f,
				0f, 0f, 0f, 1f))


) : Matrix4Ro {

	constructor(values: FloatArray) : this(FloatList(values))

	/**
	 * Sets this matrix to the given matrix.
	 *
	 * @param matrix The matrix that is to be copied. (The given matrix is not modified)
	 * @return This matrix for the purpose of chaining methods together.

	 */
	fun set(matrix: Matrix4Ro): Matrix4 {
		return this.set(matrix.values)
	}

	/**
	 * Sets the matrix to the given matrix as a float array. The float array must have at least 16 elements; the first 16 will be
	 * copied.
	 *
	 * @param values The matrix, in float form, that is to be copied. Remember that this matrix is in <a
	 *           href="http://en.wikipedia.org/wiki/Row-major_order">column major</a> order.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun set(values: List<Float>): Matrix4 {
		for (i in 0..16 - 1) {
			this.values[i] = values[i]
		}
		return this
	}

	/**
	 * Sets the matrix to a rotation matrix representing the quaternion.
	 *
	 * @param quaternion The quaternion that is to be used to set this matrix.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun set(quaternion: QuaternionRo): Matrix4 {
		return set(quaternion.x, quaternion.y, quaternion.z, quaternion.w)
	}

	/**
	 * Sets the matrix to a rotation matrix representing the quaternion.
	 *
	 * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
	 * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
	 * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
	 * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun set(quaternionX: Float, quaternionY: Float, quaternionZ: Float, quaternionW: Float): Matrix4 {
		return set(0f, 0f, 0f, quaternionX, quaternionY, quaternionZ, quaternionW)
	}

	/**
	 * Set this matrix to the specified translation and rotation.
	 * @param position The translation
	 * @param orientation The rotation, must be normalized
	 * @return This matrix for chaining
	 */
	fun set(position: Vector3Ro, orientation: QuaternionRo): Matrix4 {
		return set(position.x, position.y, position.z, orientation.x, orientation.y, orientation.z, orientation.w)
	}

	/**
	 * Sets the matrix to a rotation matrix representing the translation and quaternion.
	 *
	 * @param translationX The X component of the translation that is to be used to set this matrix.
	 * @param translationY The Y component of the translation that is to be used to set this matrix.
	 * @param translationZ The Z component of the translation that is to be used to set this matrix.
	 * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
	 * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
	 * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
	 * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun set(translationX: Float, translationY: Float, translationZ: Float, quaternionX: Float, quaternionY: Float, quaternionZ: Float, quaternionW: Float): Matrix4 {
		val xs = quaternionX * 2f
		val ys = quaternionY * 2f
		val zs = quaternionZ * 2f
		val wx = quaternionW * xs
		val wy = quaternionW * ys
		val wz = quaternionW * zs
		val xx = quaternionX * xs
		val xy = quaternionX * ys
		val xz = quaternionX * zs
		val yy = quaternionY * ys
		val yz = quaternionY * zs
		val zz = quaternionZ * zs

		values[M00] = (1f - (yy + zz))
		values[M01] = (xy - wz)
		values[M02] = (xz + wy)
		values[M03] = translationX

		values[M10] = (xy + wz)
		values[M11] = (1f - (xx + zz))
		values[M12] = (yz - wx)
		values[M13] = translationY

		values[M20] = (xz - wy)
		values[M21] = (yz + wx)
		values[M22] = (1f - (xx + yy))
		values[M23] = translationZ

		values[M30] = 0f
		values[M31] = 0f
		values[M32] = 0f
		values[M33] = 1f
		return this
	}

	/**
	 * Set this matrix to the specified translation, rotation and scale.
	 * @param position The translation
	 * @param orientation The rotation, must be normalized
	 * @param scale The scale
	 * @return This matrix for chaining
	 */
	fun set(position: Vector3Ro, orientation: Quaternion, scale: Vector3Ro): Matrix4 {
		return set(position.x, position.y, position.z, orientation.x, orientation.y, orientation.z, orientation.w, scale.x, scale.y, scale.z)
	}

	/**
	 * Sets the matrix to a rotation matrix representing the translation and quaternion.
	 *
	 * @param translationX The X component of the translation that is to be used to set this matrix.
	 * @param translationY The Y component of the translation that is to be used to set this matrix.
	 * @param translationZ The Z component of the translation that is to be used to set this matrix.
	 * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
	 * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
	 * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
	 * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
	 * @param scaleX The X component of the scaling that is to be used to set this matrix.
	 * @param scaleY The Y component of the scaling that is to be used to set this matrix.
	 * @param scaleZ The Z component of the scaling that is to be used to set this matrix.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun set(translationX: Float, translationY: Float, translationZ: Float, quaternionX: Float, quaternionY: Float, quaternionZ: Float, quaternionW: Float, scaleX: Float, scaleY: Float, scaleZ: Float): Matrix4 {
		val xs = quaternionX * 2f
		val ys = quaternionY * 2f
		val zs = quaternionZ * 2f
		val wx = quaternionW * xs
		val wy = quaternionW * ys
		val wz = quaternionW * zs
		val xx = quaternionX * xs
		val xy = quaternionX * ys
		val xz = quaternionX * zs
		val yy = quaternionY * ys
		val yz = quaternionY * zs
		val zz = quaternionZ * zs

		values[M00] = scaleX * (1f - (yy + zz))
		values[M01] = scaleY * (xy - wz)
		values[M02] = scaleZ * (xz + wy)
		values[M03] = translationX

		values[M10] = scaleX * (xy + wz)
		values[M11] = scaleY * (1f - (xx + zz))
		values[M12] = scaleZ * (yz - wx)
		values[M13] = translationY

		values[M20] = scaleX * (xz - wy)
		values[M21] = scaleY * (yz + wx)
		values[M22] = scaleZ * (1f - (xx + yy))
		values[M23] = translationZ

		values[M30] = 0f
		values[M31] = 0f
		values[M32] = 0f
		values[M33] = 1f
		return this
	}

	/**
	 * Sets the four columns of the matrix which correspond to the x-, y- and z-axis of the vector space this matrix creates as
	 * well as the 4th column representing the translation of any point that is multiplied by this matrix.
	 *
	 * @param xAxis The x-axis.
	 * @param yAxis The y-axis.
	 * @param zAxis The z-axis.
	 * @param pos The translation vector.
	 */
	fun set(xAxis: Vector3Ro, yAxis: Vector3Ro, zAxis: Vector3Ro, pos: Vector3Ro): Matrix4 {
		values[M00] = xAxis.x
		values[M01] = xAxis.y
		values[M02] = xAxis.z
		values[M10] = yAxis.x
		values[M11] = yAxis.y
		values[M12] = yAxis.z
		values[M20] = zAxis.x
		values[M21] = zAxis.y
		values[M22] = zAxis.z
		values[M03] = pos.x
		values[M13] = pos.y
		values[M23] = pos.z
		values[M30] = 0f
		values[M31] = 0f
		values[M32] = 0f
		values[M33] = 1f
		return this
	}

	/**
	 * Adds a translational component to the matrix in the 4th column. The other columns are untouched.
	 *
	 * @param vector The translation vector to add to the current matrix. (This vector is not modified)
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun trn(vector: Vector3Ro): Matrix4 {
		values[M03] += vector.x
		values[M13] += vector.y
		values[M23] += vector.z
		return this
	}

	/**
	 * Adds a translational component to the matrix in the 4th column. The other columns are untouched.
	 *
	 * @param x The x-component of the translation vector.
	 * @param y The y-component of the translation vector.
	 * @param z The z-component of the translation vector.
	 * @return This matrix for the purpose of chaining methods together.

	 */
	fun trn(x: Float, y: Float, z: Float): Matrix4 {
		values[M03] += x
		values[M13] += y
		values[M23] += z
		return this
	}

	/**
	 * Postmultiplies this matrix with the given matrix, storing the result in this matrix. For example:
	 *
	 * <pre>
	 * A.mul(B) results in A := AB.
	 * </pre>
	 *
	 * @param matrix The other matrix to multiply by.
	 * @return This matrix for the purpose of chaining operations together.

	 */
	fun mul(matrix: Matrix4Ro): Matrix4 {
		mul(values, matrix.values)
		return this
	}

	/**
	 * Premultiplies this matrix with the given matrix, storing the result in this matrix. For example:
	 *
	 * <pre>
	 * A.mulLeft(B) results in A := BA.
	 * </pre>
	 *
	 * @param matrix The other matrix to multiply by.
	 * @return This matrix for the purpose of chaining operations together.
	 */
	fun mulLeft(matrix: Matrix4Ro): Matrix4 {
		tmpMat.set(matrix)
		mul(tmpMat.values, this.values)
		return set(tmpMat)
	}

	/**
	 * Transposes the matrix.
	 *
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun tra(): Matrix4 {
		tmp[M00] = values[M00]
		tmp[M01] = values[M10]
		tmp[M02] = values[M20]
		tmp[M03] = values[M30]
		tmp[M10] = values[M01]
		tmp[M11] = values[M11]
		tmp[M12] = values[M21]
		tmp[M13] = values[M31]
		tmp[M20] = values[M02]
		tmp[M21] = values[M12]
		tmp[M22] = values[M22]
		tmp[M23] = values[M32]
		tmp[M30] = values[M03]
		tmp[M31] = values[M13]
		tmp[M32] = values[M23]
		tmp[M33] = values[M33]
		return set(tmp)
	}

	/**
	 * Sets the matrix to an identity matrix.
	 *
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun idt(): Matrix4 {
		values[M00] = 1f
		values[M01] = 0f
		values[M02] = 0f
		values[M03] = 0f
		values[M10] = 0f
		values[M11] = 1f
		values[M12] = 0f
		values[M13] = 0f
		values[M20] = 0f
		values[M21] = 0f
		values[M22] = 1f
		values[M23] = 0f
		values[M30] = 0f
		values[M31] = 0f
		values[M32] = 0f
		values[M33] = 1f
		return this
	}

	/**
	 * Inverts the matrix. Stores the result in this matrix.
	 *
	 * @return This matrix for the purpose of chaining methods together.
	 * @throws RuntimeException if the matrix is singular (not invertible)
	 */
	fun inv(): Matrix4 {
		val lDet = det()
		if (lDet == 0f) throw RuntimeException("non-invertible matrix")
		val invDet = 1f / lDet
		tmp[M00] = values[M12] * values[M23] * values[M31] - values[M13] * values[M22] * values[M31] + values[M13] * values[M21] * values[M32] - values[M11] * values[M23] * values[M32] - values[M12] * values[M21] * values[M33] + values[M11] * values[M22] * values[M33]
		tmp[M01] = values[M03] * values[M22] * values[M31] - values[M02] * values[M23] * values[M31] - values[M03] * values[M21] * values[M32] + values[M01] * values[M23] * values[M32] + values[M02] * values[M21] * values[M33] - values[M01] * values[M22] * values[M33]
		tmp[M02] = values[M02] * values[M13] * values[M31] - values[M03] * values[M12] * values[M31] + values[M03] * values[M11] * values[M32] - values[M01] * values[M13] * values[M32] - values[M02] * values[M11] * values[M33] + values[M01] * values[M12] * values[M33]
		tmp[M03] = values[M03] * values[M12] * values[M21] - values[M02] * values[M13] * values[M21] - values[M03] * values[M11] * values[M22] + values[M01] * values[M13] * values[M22] + values[M02] * values[M11] * values[M23] - values[M01] * values[M12] * values[M23]
		tmp[M10] = values[M13] * values[M22] * values[M30] - values[M12] * values[M23] * values[M30] - values[M13] * values[M20] * values[M32] + values[M10] * values[M23] * values[M32] + values[M12] * values[M20] * values[M33] - values[M10] * values[M22] * values[M33]
		tmp[M11] = values[M02] * values[M23] * values[M30] - values[M03] * values[M22] * values[M30] + values[M03] * values[M20] * values[M32] - values[M00] * values[M23] * values[M32] - values[M02] * values[M20] * values[M33] + values[M00] * values[M22] * values[M33]
		tmp[M12] = values[M03] * values[M12] * values[M30] - values[M02] * values[M13] * values[M30] - values[M03] * values[M10] * values[M32] + values[M00] * values[M13] * values[M32] + values[M02] * values[M10] * values[M33] - values[M00] * values[M12] * values[M33]
		tmp[M13] = values[M02] * values[M13] * values[M20] - values[M03] * values[M12] * values[M20] + values[M03] * values[M10] * values[M22] - values[M00] * values[M13] * values[M22] - values[M02] * values[M10] * values[M23] + values[M00] * values[M12] * values[M23]
		tmp[M20] = values[M11] * values[M23] * values[M30] - values[M13] * values[M21] * values[M30] + values[M13] * values[M20] * values[M31] - values[M10] * values[M23] * values[M31] - values[M11] * values[M20] * values[M33] + values[M10] * values[M21] * values[M33]
		tmp[M21] = values[M03] * values[M21] * values[M30] - values[M01] * values[M23] * values[M30] - values[M03] * values[M20] * values[M31] + values[M00] * values[M23] * values[M31] + values[M01] * values[M20] * values[M33] - values[M00] * values[M21] * values[M33]
		tmp[M22] = values[M01] * values[M13] * values[M30] - values[M03] * values[M11] * values[M30] + values[M03] * values[M10] * values[M31] - values[M00] * values[M13] * values[M31] - values[M01] * values[M10] * values[M33] + values[M00] * values[M11] * values[M33]
		tmp[M23] = values[M03] * values[M11] * values[M20] - values[M01] * values[M13] * values[M20] - values[M03] * values[M10] * values[M21] + values[M00] * values[M13] * values[M21] + values[M01] * values[M10] * values[M23] - values[M00] * values[M11] * values[M23]
		tmp[M30] = values[M12] * values[M21] * values[M30] - values[M11] * values[M22] * values[M30] - values[M12] * values[M20] * values[M31] + values[M10] * values[M22] * values[M31] + values[M11] * values[M20] * values[M32] - values[M10] * values[M21] * values[M32]
		tmp[M31] = values[M01] * values[M22] * values[M30] - values[M02] * values[M21] * values[M30] + values[M02] * values[M20] * values[M31] - values[M00] * values[M22] * values[M31] - values[M01] * values[M20] * values[M32] + values[M00] * values[M21] * values[M32]
		tmp[M32] = values[M02] * values[M11] * values[M30] - values[M01] * values[M12] * values[M30] - values[M02] * values[M10] * values[M31] + values[M00] * values[M12] * values[M31] + values[M01] * values[M10] * values[M32] - values[M00] * values[M11] * values[M32]
		tmp[M33] = values[M01] * values[M12] * values[M20] - values[M02] * values[M11] * values[M20] + values[M02] * values[M10] * values[M21] - values[M00] * values[M12] * values[M21] - values[M01] * values[M10] * values[M22] + values[M00] * values[M11] * values[M22]
		values[M00] = tmp[M00] * invDet
		values[M01] = tmp[M01] * invDet
		values[M02] = tmp[M02] * invDet
		values[M03] = tmp[M03] * invDet
		values[M10] = tmp[M10] * invDet
		values[M11] = tmp[M11] * invDet
		values[M12] = tmp[M12] * invDet
		values[M13] = tmp[M13] * invDet
		values[M20] = tmp[M20] * invDet
		values[M21] = tmp[M21] * invDet
		values[M22] = tmp[M22] * invDet
		values[M23] = tmp[M23] * invDet
		values[M30] = tmp[M30] * invDet
		values[M31] = tmp[M31] * invDet
		values[M32] = tmp[M32] * invDet
		values[M33] = tmp[M33] * invDet
		return this
	}

	/**
	 * @return The determinant of this matrix
	 */
	override fun det(): Float {
		return values[M30] * values[M21] * values[M12] * values[M03] - values[M20] * values[M31] * values[M12] * values[M03] - values[M30] * values[M11] * values[M22] * values[M03] + values[M10] * values[M31] * values[M22] * values[M03] + values[M20] * values[M11] * values[M32] * values[M03] - values[M10] * values[M21] * values[M32] * values[M03] - values[M30] * values[M21] * values[M02] * values[M13] + values[M20] * values[M31] * values[M02] * values[M13] + values[M30] * values[M01] * values[M22] * values[M13] - values[M00] * values[M31] * values[M22] * values[M13] - values[M20] * values[M01] * values[M32] * values[M13] + values[M00] * values[M21] * values[M32] * values[M13] + values[M30] * values[M11] * values[M02] * values[M23] - values[M10] * values[M31] * values[M02] * values[M23] - values[M30] * values[M01] * values[M12] * values[M23] + values[M00] * values[M31] * values[M12] * values[M23] + values[M10] * values[M01] * values[M32] * values[M23] - values[M00] * values[M11] * values[M32] * values[M23] - values[M20] * values[M11] * values[M02] * values[M33] + values[M10] * values[M21] * values[M02] * values[M33] + values[M20] * values[M01] * values[M12] * values[M33] - values[M00] * values[M21] * values[M12] * values[M33] - values[M10] * values[M01] * values[M22] * values[M33] + values[M00] * values[M11] * values[M22] * values[M33]
	}

	/**
	 * @return The determinant of the 3x3 upper left matrix
	 */
	override fun det3x3(): Float {
		return values[M00] * values[M11] * values[M22] + values[M01] * values[M12] * values[M20] + values[M02] * values[M10] * values[M21] - values[M00] * values[M12] * values[M21] - values[M01] * values[M10] * values[M22] - values[M02] * values[M11] * values[M20]
	}

	/**
	 * Sets the 4th column to the translation vector.
	 *
	 * @param vector The translation vector
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun setTranslation(vector: Vector3Ro): Matrix4 {
		values[M03] = vector.x
		values[M13] = vector.y
		values[M23] = vector.z
		return this
	}

	/**
	 * Sets the 4th column to the translation vector.
	 *
	 * @param x The X coordinate of the translation vector
	 * @param y The Y coordinate of the translation vector
	 * @param z The Z coordinate of the translation vector
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun setTranslation(x: Float, y: Float, z: Float): Matrix4 {
		values[M03] = x
		values[M13] = y
		values[M23] = z
		return this
	}

	/**
	 * Sets this matrix to a rotation matrix from the given euler angles.
	 * @param yaw the yaw in radians
	 * @param pitch the pitch in radians
	 * @param roll the roll in radians
	 * @return This matrix
	 */
	fun setFromEulerAnglesRad(yaw: Float, pitch: Float, roll: Float): Matrix4 {
		quat.setEulerAnglesRad(yaw, pitch, roll)
		return set(quat)
	}


	/**
	 *
	 * Sets the matrix to a look at matrix with a direction and an up vector. Multiply with a translation matrix to get a camera
	 * model view matrix.
	 *
	 * @param direction The direction vector
	 * @param up The up vector
	 * @return This matrix for the purpose of chaining methods together.

	 */
	fun setToLookAt(direction: Vector3Ro, up: Vector3Ro): Matrix4 {
		l_vez.set(direction).nor()
		l_vex.set(direction).nor()
		l_vex.crs(up).nor()
		l_vey.set(l_vex).crs(l_vez).nor()
		idt()
		values[M00] = l_vex.x
		values[M01] = l_vex.y
		values[M02] = l_vex.z
		values[M10] = l_vey.x
		values[M11] = l_vey.y
		values[M12] = l_vey.z
		values[M20] = -l_vez.x
		values[M21] = -l_vez.y
		values[M22] = -l_vez.z
		return this
	}

	/**
	 * Sets this matrix to a look at matrix with the given position, target and up vector.
	 *
	 * @param position the position
	 * @param target the target
	 * @param up the up vector
	 * @return This matrix
	 */
	fun setToLookAt(position: Vector3Ro, target: Vector3Ro, up: Vector3Ro): Matrix4 {
		tmpVec.set(target).sub(position)
		setToLookAt(tmpVec, up)
		translate(-position.x, -position.y, -position.z)

		return this
	}

	fun setToGlobal(position: Vector3Ro, forward: Vector3Ro, up: Vector3Ro): Matrix4 {
		tmpForward.set(forward).nor()
		right.set(tmpForward).crs(up).nor()
		tmpUp.set(right).crs(tmpForward).nor()

		this.set(right, tmpUp, tmpForward.scl(-1f), position)
		return this
	}

	override fun toString(): String {
		return "[" + values[M00] + "|" + values[M01] + "|" + values[M02] + "|" + values[M03] + "]\n" + "[" + values[M10] + "|" + values[M11] + "|" + values[M12] + "|" + values[M13] + "]\n" + "[" + values[M20] + "|" + values[M21] + "|" + values[M22] + "|" + values[M23] + "]\n" + "[" + values[M30] + "|" + values[M31] + "|" + values[M32] + "|" + values[M33] + "]\n"
	}

	/**
	 * Sets this matrix to the given 3x3 matrix. The third column of this matrix is set to (0,0,1,0).
	 * @param mat the matrix
	 */
	fun set(mat: Matrix3Ro): Matrix4 {
		values[M00] = mat.values[M00]
		values[M10] = mat.values[M10]
		values[M20] = mat.values[M20]
		values[M30] = 0f
		values[M01] = mat.values[M30]
		values[M11] = mat.values[M01]
		values[M21] = mat.values[M11]
		values[M31] = 0f
		values[M02] = 0f
		values[M12] = 0f
		values[M22] = 1f
		values[M32] = 0f
		values[M03] = mat.values[M21]
		values[M13] = mat.values[M31]
		values[M23] = 0f
		values[M33] = mat.values[M02]
		return this
	}

	fun scl(scale: Vector3Ro): Matrix4 {
		values[M00] *= scale.x
		values[M11] *= scale.y
		values[M22] *= scale.z
		return this
	}

	fun scl(x: Float, y: Float, z: Float): Matrix4 {
		values[M00] *= x
		values[M11] *= y
		values[M22] *= z
		return this
	}

	fun scl(scale: Float): Matrix4 {
		values[M00] *= scale
		values[M11] *= scale
		values[M22] *= scale
		return this
	}

	override val translationX: Float
		get() = values[M03]

	override val translationY: Float
		get() = values[M13]

	override val translationZ: Float
		get() = values[M23]

	/**
	 * Sets the provided position Vector3 with the translation of this Matrix
	 */
	override fun getTranslation(out: Vector3): Vector3 {
		out.x = values[M03]
		out.y = values[M13]
		out.z = values[M23]
		return out
	}

	/**
	 * Gets the rotation of this matrix.
	 * @param out The {@link Quaternion} to receive the rotation
	 * @param normalizeAxes True to normalize the axes, necessary when the matrix might also include scaling.
	 * @return The provided {@link Quaternion} for chaining.
	 */
	override fun getRotation(out: Quaternion, normalizeAxes: Boolean): Quaternion {
		return out.setFromMatrix(this, normalizeAxes)
	}

	/**
	 * @return the squared scale factor on the X axis
	 */
	override fun getScaleXSquared(): Float {
		return values[M00] * values[M00] + values[M01] * values[M01] + values[M02] * values[M02]
	}

	/**
	 * @return the squared scale factor on the Y axis
	 */
	override fun getScaleYSquared(): Float {
		return values[M10] * values[M10] + values[M11] * values[M11] + values[M12] * values[M12]
	}

	/**
	 * @return the squared scale factor on the Z axis
	 */
	override fun getScaleZSquared(): Float {
		return values[M20] * values[M20] + values[M21] * values[M21] + values[M22] * values[M22]
	}

	/**
	 * @return the scale factor on the X axis (non-negative)
	 */
	override fun getScaleX(): Float {
		return if ((MathUtils.isZero(values[M01]) && MathUtils.isZero(values[M02])))
			abs(values[M00])
		else
			sqrt(getScaleXSquared())
	}

	/**
	 * @return the scale factor on the Y axis (non-negative)
	 */
	override fun getScaleY(): Float {
		return if ((MathUtils.isZero(values[M10]) && MathUtils.isZero(values[M12])))
			abs(values[M11])
		else
			sqrt(getScaleYSquared())
	}

	/**
	 * @return the scale factor on the X axis (non-negative)
	 */
	override fun getScaleZ(): Float {
		return if ((MathUtils.isZero(values[M20]) && MathUtils.isZero(values[M21])))
			abs(values[M22])
		else
			sqrt(getScaleZSquared())
	}

	/**
	 * @param scale The vector which will receive the (non-negative) scale components on each axis.
	 * @return The provided vector for chaining.
	 */
	override fun getScale(scale: Vector3): Vector3 {
		return scale.set(getScaleX(), getScaleY(), getScaleZ())
	}

	/**
	 * removes the translational part and transposes the matrix.
	 */
	fun toNormalMatrix(): Matrix4 {
		values[M03] = 0f
		values[M13] = 0f
		values[M23] = 0f
		return inv().tra()
	}

	/**
	 * Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES'
	 * glTranslate/glRotate/glScale
	 * @param translation
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun translate(translation: Vector3Ro): Matrix4 {
		return translate(translation.x, translation.y, translation.z)
	}

	/**
	 * Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale.
	 * @param x Translation in the x-axis.
	 * @param y Translation in the y-axis.
	 * @param z Translation in the z-axis.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun translate(x: Float = 0f, y: Float = 0f, z: Float = 0f): Matrix4 {
		val matA = values
		val v03 = matA[M00] * x + matA[M01] * y + matA[M02] * z + matA[M03]
		val v13 = matA[M10] * x + matA[M11] * y + matA[M12] * z + matA[M13]
		val v23 = matA[M20] * x + matA[M21] * y + matA[M22] * z + matA[M23]
		val v33 = matA[M30] * x + matA[M31] * y + matA[M32] * z + matA[M33]
		matA[M03] = v03
		matA[M13] = v13
		matA[M23] = v23
		matA[M33] = v33
		return this
	}

	/**
	 * Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale.
	 *
	 * @param axis The vector axis to rotate around.
	 * @param radians The angle in radians.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun rotate(axis: Vector3Ro, radians: Float): Matrix4 {
		if (radians == 0f) return this
		quat.setFromAxis(axis, radians)
		return rotate(quat)
	}

	/**
	 * Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale
	 * @param axisX The x-axis component of the vector to rotate around.
	 * @param axisY The y-axis component of the vector to rotate around.
	 * @param axisZ The z-axis component of the vector to rotate around.
	 * @param radians The angle in radians
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun rotate(axisX: Float, axisY: Float, axisZ: Float, radians: Float): Matrix4 {
		if (radians == 0f) return this
		quat.setFromAxis(axisX, axisY, axisZ, radians)
		return rotate(quat)
	}

	/**
	 * Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x
	 * glTranslate/glRotate/glScale.
	 *
	 * @param rotation
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun rotate(rotation: QuaternionRo): Matrix4 {
		tmpMat.set(rotation)
		mul(values, tmpMat.values)
		return this
	}

	/**
	 * Postmultiplies this matrix by the rotation between two vectors.
	 * @param v1 The base vector
	 * @param v2 The target vector
	 * @return This matrix for the purpose of chaining methods together
	 */
	fun rotate(v1: Vector3Ro, v2: Vector3Ro): Matrix4 {
		return rotate(quat.setFromCross(v1, v2))
	}

	/**
	 * Postmultiplies this matrix with a scale matrix.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun scale(scale: Vector3Ro): Matrix4 = scale(scale.x, scale.y, scale.z)

	/**
	 * Postmultiplies this matrix with a scale matrix.
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun scale(scaleX: Float, scaleY: Float, scaleZ: Float): Matrix4 {
		if (scaleX == 1f && scaleY == 1f && scaleZ == 1f) return this
		tmpMat.idt()
		tmpMat.scl(scaleX, scaleY, scaleZ)
		return mul(tmpMat)
	}

	/**
	 * Copies the 4x3 upper-left sub-matrix into float array. The destination array is supposed to be a column major matrix.
	 * @param out the destination matrix
	 */
	override fun extract4x3Matrix(out: MutableList<Float>): MutableList<Float> {
		out[M00] = values[M00]
		out[M10] = values[M10]
		out[M20] = values[M20]
		out[M30] = values[M01]
		out[M01] = values[M11]
		out[M11] = values[M21]
		out[M21] = values[M02]
		out[M31] = values[M12]
		out[M02] = values[M22]
		out[M12] = values[M03]
		out[M22] = values[M13]
		out[M32] = values[M23]
		return out
	}

	/**
	 * Multiplies the vector with this matrix, performing a division by w.
	 *
	 * @param vec the vector.
	 * @return Returns the vec for chaining.
	 */
	override fun prj(vec: Vector3): Vector3 {
		val mat = values
		val inv_w = 1.0f / (vec.x * mat[M30] + vec.y * mat[M31] + vec.z * mat[M32] + mat[M33])
		val x = (vec.x * mat[M00] + vec.y * mat[M01] + vec.z * mat[M02] + mat[M03]) * inv_w
		val y = (vec.x * mat[M10] + vec.y * mat[M11] + vec.z * mat[M12] + mat[M13]) * inv_w
		val z = (vec.x * mat[M20] + vec.y * mat[M21] + vec.z * mat[M22] + mat[M23]) * inv_w
		vec.x = x
		vec.y = y
		vec.z = z
		return vec
	}

	/**
	 * Multiplies the vector with this matrix, performing a division by w.
	 *
	 * @param vec the vector.
	 * @return Returns the vec for chaining.
	 */
	override fun prj(vec: Vector2): Vector2 {
		val mat = values
		val invW = 1.0f / (vec.x * mat[M30] + vec.y * mat[M31] + mat[M33])
		val x = (vec.x * mat[M00] + vec.y * mat[M01] + mat[M03]) * invW
		val y = (vec.x * mat[M10] + vec.y * mat[M11] + mat[M13]) * invW
		vec.x = x
		vec.y = y
		return vec
	}

	/**
	 * Multiplies the vector with the top most 3x3 sub-matrix of this matrix.
	 * @return Returns the vec for chaining.
	 */
	override fun rot(vec: Vector3): Vector3 {
		val mat = values
		val x = vec.x * mat[M00] + vec.y * mat[M01] + vec.z * mat[M02]
		val y = vec.x * mat[M10] + vec.y * mat[M11] + vec.z * mat[M12]
		val z = vec.x * mat[M20] + vec.y * mat[M21] + vec.z * mat[M22]
		vec.x = x
		vec.y = y
		vec.z = z
		return vec
	}


	/**
	 * Multiplies the vector with the top most 3x3 sub-matrix of this matrix.
	 * @return Returns the vec for chaining.
	 */
	override fun rot(vec: Vector2): Vector2 {
		val mat = values
		val x = vec.x * mat[M00] + vec.y * mat[M01]
		val y = vec.x * mat[M10] + vec.y * mat[M11]
		vec.x = x
		vec.y = y
		return vec
	}



	companion object {

		private val tmp = FloatList(16)

		// These values are kept for reference, but were too big of a performance problem on the JS side. Use the values directly.

		/**
		 * XX: Typically the unrotated X component for scaling, also the cosine of the angle when rotated on the Y and/or Z axis. On
		 * Vector3 multiplication this value is multiplied with the source X component and added to the target X component.
		 */
		const val M00: Int = 0

		/**
		 * XY: Typically the negative sine of the angle when rotated on the Z axis. On Vector3 multiplication this value is multiplied
		 * with the source Y component and added to the target X component.
		 */
		const val M01: Int = 4

		/**
		 * XZ: Typically the sine of the angle when rotated on the Y axis. On Vector3 multiplication this value is multiplied with the
		 * source Z component and added to the target X component.
		 */
		const val M02: Int = 8

		/**
		 * XW: Typically the translation of the X component. On Vector3 multiplication this value is added to the target X component.
		 */
		const val M03: Int = 12

		/**
		 * YX: Typically the sine of the angle when rotated on the Z axis. On Vector3 multiplication this value is multiplied with the
		 * source X component and added to the target Y component.
		 */
		const val M10: Int = 1

		/**
		 * YY: Typically the unrotated Y component for scaling, also the cosine of the angle when rotated on the X and/or Z axis. On
		 * Vector3 multiplication this value is multiplied with the source Y component and added to the target Y component.
		 */
		const val M11: Int = 5

		/**
		 * YZ: Typically the negative sine of the angle when rotated on the X axis. On Vector3 multiplication this value is multiplied
		 * with the source Z component and added to the target Y component.
		 */
		const val M12: Int = 9

		/**
		 * YW: Typically the translation of the Y component. On Vector3 multiplication this value is added to the target Y component.
		 */
		const val M13: Int = 13

		/**
		 * ZX: Typically the negative sine of the angle when rotated on the Y axis. On Vector3 multiplication this value is multiplied
		 * with the source X component and added to the target Z component.
		 */
		const val M20: Int = 2

		/**
		 * ZY: Typical the sine of the angle when rotated on the X axis. On Vector3 multiplication this value is multiplied with the
		 * source Y component and added to the target Z component.
		 */
		const val M21: Int = 6

		/**
		 * ZZ: Typically the unrotated Z component for scaling, also the cosine of the angle when rotated on the X and/or Y axis. On
		 * Vector3 multiplication this value is multiplied with the source Z component and added to the target Z component.
		 */
		const val M22: Int = 10

		/**
		 * ZW: Typically the translation of the Z component. On Vector3 multiplication this value is added to the target Z component.
		 */
		const val M23: Int = 14

		/**
		 * WX: Typically the value zero. On Vector3 multiplication this value is ignored.
		 */
		const val M30: Int = 3

		/**
		 * WY: Typically the value zero. On Vector3 multiplication this value is ignored.
		 */
		const val M31: Int = 7

		/**
		 * WZ: Typically the value zero. On Vector3 multiplication this value is ignored.
		 */
		const val M32: Int = 11

		/**
		 * WW: Typically the value one. On Vector3 multiplication this value is ignored.
		 */
		const val M33: Int = 15

		val IDENTITY: Matrix4Ro = Matrix4()

		private val quat: Quaternion = Quaternion()

		private val l_vez = Vector3()
		private val l_vex = Vector3()
		private val l_vey = Vector3()

		private val tmpVec = Vector3()
		private val tmpMat = Matrix4()

		private val right = Vector3()
		private val tmpForward = Vector3()
		private val tmpUp = Vector3()

		private fun mul(matA: FloatList, matB: List<Float>) {
			val v00 = matA[M00] * matB[M00] + matA[M01] * matB[M10] + matA[M02] * matB[M20] + matA[M03] * matB[M30]
			val v01 = matA[M00] * matB[M01] + matA[M01] * matB[M11] + matA[M02] * matB[M21] + matA[M03] * matB[M31]
			val v02 = matA[M00] * matB[M02] + matA[M01] * matB[M12] + matA[M02] * matB[M22] + matA[M03] * matB[M32]
			val v03 = matA[M00] * matB[M03] + matA[M01] * matB[M13] + matA[M02] * matB[M23] + matA[M03] * matB[M33]

			val v10 = matA[M10] * matB[M00] + matA[M11] * matB[M10] + matA[M12] * matB[M20] + matA[M13] * matB[M30]
			val v11 = matA[M10] * matB[M01] + matA[M11] * matB[M11] + matA[M12] * matB[M21] + matA[M13] * matB[M31]
			val v12 = matA[M10] * matB[M02] + matA[M11] * matB[M12] + matA[M12] * matB[M22] + matA[M13] * matB[M32]
			val v13 = matA[M10] * matB[M03] + matA[M11] * matB[M13] + matA[M12] * matB[M23] + matA[M13] * matB[M33]

			val v20 = matA[M20] * matB[M00] + matA[M21] * matB[M10] + matA[M22] * matB[M20] + matA[M23] * matB[M30]
			val v21 = matA[M20] * matB[M01] + matA[M21] * matB[M11] + matA[M22] * matB[M21] + matA[M23] * matB[M31]
			val v22 = matA[M20] * matB[M02] + matA[M21] * matB[M12] + matA[M22] * matB[M22] + matA[M23] * matB[M32]
			val v23 = matA[M20] * matB[M03] + matA[M21] * matB[M13] + matA[M22] * matB[M23] + matA[M23] * matB[M33]

			val v30 = matA[M30] * matB[M00] + matA[M31] * matB[M10] + matA[M32] * matB[M20] + matA[M33] * matB[M30]
			val v31 = matA[M30] * matB[M01] + matA[M31] * matB[M11] + matA[M32] * matB[M21] + matA[M33] * matB[M31]
			val v32 = matA[M30] * matB[M02] + matA[M31] * matB[M12] + matA[M32] * matB[M22] + matA[M33] * matB[M32]
			val v33 = matA[M30] * matB[M03] + matA[M31] * matB[M13] + matA[M32] * matB[M23] + matA[M33] * matB[M33]

			matA[M00] = v00
			matA[M01] = v01
			matA[M02] = v02
			matA[M03] = v03

			matA[M10] = v10
			matA[M11] = v11
			matA[M12] = v12
			matA[M13] = v13

			matA[M20] = v20
			matA[M21] = v21
			matA[M22] = v22
			matA[M23] = v23

			matA[M30] = v30
			matA[M31] = v31
			matA[M32] = v32
			matA[M33] = v33
		}
	}

	// TODO: support shearing on Y and Z axes

	/**
	 * Postmultiplies this matrix by a shear matrix on the z axis.
	 * @param shearXZ The shear in x direction.
	 * @param shearYZ The shear in y direction.
	 * @return This matrix for the purpose of chaining.
	 */
	fun shearZ(shearXZ: Float = 0f, shearYZ: Float = 0f): Matrix4 {
		var tmp0 = values[M00] + shearYZ * values[M01]
		var tmp1 = values[M01] + shearXZ * values[M00]
		values[M00] = tmp0
		values[M01] = tmp1

		tmp0 = values[M10] + shearYZ * values[M11]
		tmp1 = values[M11] + shearXZ * values[M10]
		values[M10] = tmp0
		values[M11] = tmp1

		return this
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as Matrix4Ro
		if (values != other.values) return false
		return true
	}

	override fun hashCode(): Int {
		return values.hashCode()
	}
}