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
	 * Sets the provided position Vector3 with the translation of this Matrix
	 */
	fun getTranslation(out: Vector3): Vector3

	/**
	 * Gets the rotation of this matrix.
	 * @param out The {@link Quaternion} to receive the rotation
	 * @param normalizeAxes True to normalize the axes, necessary when the matrix might also include scaling.
	 * @return The provided {@link Quaternion} for chaining.
	 */
	fun getRotation(out: Quaternion, normalizeAxes: Boolean): Quaternion

	/**
	 * Gets the rotation of this matrix.
	 * @param out The {@link Quaternion} to receive the rotation
	 * @return The provided {@link Quaternion} for chaining.
	 */
	fun getRotation(out: Quaternion): Quaternion

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
	 * @param dst the destination matrix
	 */
	fun extract4x3Matrix(dst: MutableList<Float>)

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
data class Matrix4(

		override val values: MutableList<Float> = arrayListOf(
				1f, 0f, 0f, 0f,
				0f, 1f, 0f, 0f,
				0f, 0f, 1f, 0f,
				0f, 0f, 0f, 1f)


) : Matrix4Ro {

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

		values[0] = (1f - (yy + zz))
		values[4] = (xy - wz)
		values[8] = (xz + wy)
		values[12] = translationX

		values[1] = (xy + wz)
		values[5] = (1f - (xx + zz))
		values[9] = (yz - wx)
		values[13] = translationY

		values[2] = (xz - wy)
		values[6] = (yz + wx)
		values[10] = (1f - (xx + yy))
		values[14] = translationZ

		values[3] = 0f
		values[7] = 0f
		values[11] = 0f
		values[15] = 1f
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

		values[0] = scaleX * (1f - (yy + zz))
		values[4] = scaleY * (xy - wz)
		values[8] = scaleZ * (xz + wy)
		values[12] = translationX

		values[1] = scaleX * (xy + wz)
		values[5] = scaleY * (1f - (xx + zz))
		values[9] = scaleZ * (yz - wx)
		values[13] = translationY

		values[2] = scaleX * (xz - wy)
		values[6] = scaleY * (yz + wx)
		values[10] = scaleZ * (1f - (xx + yy))
		values[14] = translationZ

		values[3] = 0f
		values[7] = 0f
		values[11] = 0f
		values[15] = 1f
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
		values[0] = xAxis.x
		values[4] = xAxis.y
		values[8] = xAxis.z
		values[1] = yAxis.x
		values[5] = yAxis.y
		values[9] = yAxis.z
		values[2] = zAxis.x
		values[6] = zAxis.y
		values[10] = zAxis.z
		values[12] = pos.x
		values[13] = pos.y
		values[14] = pos.z
		values[3] = 0f
		values[7] = 0f
		values[11] = 0f
		values[15] = 1f
		return this
	}

	/**
	 * Adds a translational component to the matrix in the 4th column. The other columns are untouched.
	 *
	 * @param vector The translation vector to add to the current matrix. (This vector is not modified)
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun trn(vector: Vector3Ro): Matrix4 {
		values[12] += vector.x
		values[13] += vector.y
		values[14] += vector.z
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
		values[12] += x
		values[13] += y
		values[14] += z
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
		tmp[0] = values[0]
		tmp[4] = values[1]
		tmp[8] = values[2]
		tmp[12] = values[3]
		tmp[1] = values[4]
		tmp[5] = values[5]
		tmp[9] = values[6]
		tmp[13] = values[7]
		tmp[2] = values[8]
		tmp[6] = values[9]
		tmp[10] = values[10]
		tmp[14] = values[11]
		tmp[3] = values[12]
		tmp[7] = values[13]
		tmp[11] = values[14]
		tmp[15] = values[15]
		return set(tmp)
	}

	/**
	 * Sets the matrix to an identity matrix.
	 *
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun idt(): Matrix4 {
		values[0] = 1f
		values[4] = 0f
		values[8] = 0f
		values[12] = 0f
		values[1] = 0f
		values[5] = 1f
		values[9] = 0f
		values[13] = 0f
		values[2] = 0f
		values[6] = 0f
		values[10] = 1f
		values[14] = 0f
		values[3] = 0f
		values[7] = 0f
		values[11] = 0f
		values[15] = 1f
		return this
	}

	/**
	 * Inverts the matrix. Stores the result in this matrix.
	 *
	 * @return This matrix for the purpose of chaining methods together.
	 * @throws RuntimeException if the matrix is singular (not invertible)
	 */
	fun inv(): Matrix4 {
		val l_det = det()
		if (l_det == 0f) throw RuntimeException("non-invertible matrix")
		val inv_det = 1f / l_det
		tmp[0] = values[9] * values[14] * values[7] - values[13] * values[10] * values[7] + values[13] * values[6] * values[11] - values[5] * values[14] * values[11] - values[9] * values[6] * values[15] + values[5] * values[10] * values[15]
		tmp[4] = values[12] * values[10] * values[7] - values[8] * values[14] * values[7] - values[12] * values[6] * values[11] + values[4] * values[14] * values[11] + values[8] * values[6] * values[15] - values[4] * values[10] * values[15]
		tmp[8] = values[8] * values[13] * values[7] - values[12] * values[9] * values[7] + values[12] * values[5] * values[11] - values[4] * values[13] * values[11] - values[8] * values[5] * values[15] + values[4] * values[9] * values[15]
		tmp[12] = values[12] * values[9] * values[6] - values[8] * values[13] * values[6] - values[12] * values[5] * values[10] + values[4] * values[13] * values[10] + values[8] * values[5] * values[14] - values[4] * values[9] * values[14]
		tmp[1] = values[13] * values[10] * values[3] - values[9] * values[14] * values[3] - values[13] * values[2] * values[11] + values[1] * values[14] * values[11] + values[9] * values[2] * values[15] - values[1] * values[10] * values[15]
		tmp[5] = values[8] * values[14] * values[3] - values[12] * values[10] * values[3] + values[12] * values[2] * values[11] - values[0] * values[14] * values[11] - values[8] * values[2] * values[15] + values[0] * values[10] * values[15]
		tmp[9] = values[12] * values[9] * values[3] - values[8] * values[13] * values[3] - values[12] * values[1] * values[11] + values[0] * values[13] * values[11] + values[8] * values[1] * values[15] - values[0] * values[9] * values[15]
		tmp[13] = values[8] * values[13] * values[2] - values[12] * values[9] * values[2] + values[12] * values[1] * values[10] - values[0] * values[13] * values[10] - values[8] * values[1] * values[14] + values[0] * values[9] * values[14]
		tmp[2] = values[5] * values[14] * values[3] - values[13] * values[6] * values[3] + values[13] * values[2] * values[7] - values[1] * values[14] * values[7] - values[5] * values[2] * values[15] + values[1] * values[6] * values[15]
		tmp[6] = values[12] * values[6] * values[3] - values[4] * values[14] * values[3] - values[12] * values[2] * values[7] + values[0] * values[14] * values[7] + values[4] * values[2] * values[15] - values[0] * values[6] * values[15]
		tmp[10] = values[4] * values[13] * values[3] - values[12] * values[5] * values[3] + values[12] * values[1] * values[7] - values[0] * values[13] * values[7] - values[4] * values[1] * values[15] + values[0] * values[5] * values[15]
		tmp[14] = values[12] * values[5] * values[2] - values[4] * values[13] * values[2] - values[12] * values[1] * values[6] + values[0] * values[13] * values[6] + values[4] * values[1] * values[14] - values[0] * values[5] * values[14]
		tmp[3] = values[9] * values[6] * values[3] - values[5] * values[10] * values[3] - values[9] * values[2] * values[7] + values[1] * values[10] * values[7] + values[5] * values[2] * values[11] - values[1] * values[6] * values[11]
		tmp[7] = values[4] * values[10] * values[3] - values[8] * values[6] * values[3] + values[8] * values[2] * values[7] - values[0] * values[10] * values[7] - values[4] * values[2] * values[11] + values[0] * values[6] * values[11]
		tmp[11] = values[8] * values[5] * values[3] - values[4] * values[9] * values[3] - values[8] * values[1] * values[7] + values[0] * values[9] * values[7] + values[4] * values[1] * values[11] - values[0] * values[5] * values[11]
		tmp[15] = values[4] * values[9] * values[2] - values[8] * values[5] * values[2] + values[8] * values[1] * values[6] - values[0] * values[9] * values[6] - values[4] * values[1] * values[10] + values[0] * values[5] * values[10]
		values[0] = tmp[0] * inv_det
		values[4] = tmp[4] * inv_det
		values[8] = tmp[8] * inv_det
		values[12] = tmp[12] * inv_det
		values[1] = tmp[1] * inv_det
		values[5] = tmp[5] * inv_det
		values[9] = tmp[9] * inv_det
		values[13] = tmp[13] * inv_det
		values[2] = tmp[2] * inv_det
		values[6] = tmp[6] * inv_det
		values[10] = tmp[10] * inv_det
		values[14] = tmp[14] * inv_det
		values[3] = tmp[3] * inv_det
		values[7] = tmp[7] * inv_det
		values[11] = tmp[11] * inv_det
		values[15] = tmp[15] * inv_det
		return this
	}

	/**
	 * @return The determinant of this matrix
	 */
	override fun det(): Float {
		return values[3] * values[6] * values[9] * values[12] - values[2] * values[7] * values[9] * values[12] - values[3] * values[5] * values[10] * values[12] + values[1] * values[7] * values[10] * values[12] + values[2] * values[5] * values[11] * values[12] - values[1] * values[6] * values[11] * values[12] - values[3] * values[6] * values[8] * values[13] + values[2] * values[7] * values[8] * values[13] + values[3] * values[4] * values[10] * values[13] - values[0] * values[7] * values[10] * values[13] - values[2] * values[4] * values[11] * values[13] + values[0] * values[6] * values[11] * values[13] + values[3] * values[5] * values[8] * values[14] - values[1] * values[7] * values[8] * values[14] - values[3] * values[4] * values[9] * values[14] + values[0] * values[7] * values[9] * values[14] + values[1] * values[4] * values[11] * values[14] - values[0] * values[5] * values[11] * values[14] - values[2] * values[5] * values[8] * values[15] + values[1] * values[6] * values[8] * values[15] + values[2] * values[4] * values[9] * values[15] - values[0] * values[6] * values[9] * values[15] - values[1] * values[4] * values[10] * values[15] + values[0] * values[5] * values[10] * values[15]
	}

	/**
	 * @return The determinant of the 3x3 upper left matrix
	 */
	override fun det3x3(): Float {
		return values[0] * values[5] * values[10] + values[4] * values[9] * values[2] + values[8] * values[1] * values[6] - values[0] * values[9] * values[6] - values[4] * values[1] * values[10] - values[8] * values[5] * values[2]
	}

	/**
	 * Sets the 4th column to the translation vector.
	 *
	 * @param vector The translation vector
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun setTranslation(vector: Vector3Ro): Matrix4 {
		values[12] = vector.x
		values[13] = vector.y
		values[14] = vector.z
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
		values[12] = x
		values[13] = y
		values[14] = z
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
		values[0] = l_vex.x
		values[4] = l_vex.y
		values[8] = l_vex.z
		values[1] = l_vey.x
		values[5] = l_vey.y
		values[9] = l_vey.z
		values[2] = -l_vez.x
		values[6] = -l_vez.y
		values[10] = -l_vez.z
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
		return "[" + values[0] + "|" + values[4] + "|" + values[8] + "|" + values[12] + "]\n" + "[" + values[1] + "|" + values[5] + "|" + values[9] + "|" + values[13] + "]\n" + "[" + values[2] + "|" + values[6] + "|" + values[10] + "|" + values[14] + "]\n" + "[" + values[3] + "|" + values[7] + "|" + values[11] + "|" + values[15] + "]\n"
	}

	/**
	 * Linearly interpolates between this matrix and the given matrix mixing by alpha
	 * @param matrix the matrix
	 * @param alpha the alpha value in the range [0,1]
	 * @return This matrix for the purpose of chaining methods together.
	 */
	fun lerp(matrix: Matrix3Ro, alpha: Float): Matrix4 {
		for (i in 0..16 - 1)
			this.values[i] = this.values[i] * (1 - alpha) + matrix.values[i] * alpha
		return this
	}

	/**
	 * Sets this matrix to the given 3x3 matrix. The third column of this matrix is set to (0,0,1,0).
	 * @param mat the matrix
	 */
	fun set(mat: Matrix3Ro): Matrix4 {
		values[0] = mat.values[0]
		values[1] = mat.values[1]
		values[2] = mat.values[2]
		values[3] = 0f
		values[4] = mat.values[3]
		values[5] = mat.values[4]
		values[6] = mat.values[5]
		values[7] = 0f
		values[8] = 0f
		values[9] = 0f
		values[10] = 1f
		values[11] = 0f
		values[12] = mat.values[6]
		values[13] = mat.values[7]
		values[14] = 0f
		values[15] = mat.values[8]
		return this
	}

	fun scl(scale: Vector3Ro): Matrix4 {
		values[0] *= scale.x
		values[5] *= scale.y
		values[10] *= scale.z
		return this
	}

	fun scl(x: Float, y: Float, z: Float): Matrix4 {
		values[0] *= x
		values[5] *= y
		values[10] *= z
		return this
	}

	fun scl(scale: Float): Matrix4 {
		values[0] *= scale
		values[5] *= scale
		values[10] *= scale
		return this
	}

	/**
	 * Sets the provided position Vector3 with the translation of this Matrix
	 */
	override fun getTranslation(out: Vector3): Vector3 {
		out.x = values[12]
		out.y = values[13]
		out.z = values[14]
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
	 * Gets the rotation of this matrix.
	 * @param out The {@link Quaternion} to receive the rotation
	 * @return The provided {@link Quaternion} for chaining.
	 */
	override fun getRotation(out: Quaternion): Quaternion {
		return out.setFromMatrix(this)
	}

	/**
	 * @return the squared scale factor on the X axis
	 */
	override fun getScaleXSquared(): Float {
		return values[0] * values[0] + values[4] * values[4] + values[8] * values[8]
	}

	/**
	 * @return the squared scale factor on the Y axis
	 */
	override fun getScaleYSquared(): Float {
		return values[1] * values[1] + values[5] * values[5] + values[9] * values[9]
	}

	/**
	 * @return the squared scale factor on the Z axis
	 */
	override fun getScaleZSquared(): Float {
		return values[2] * values[2] + values[6] * values[6] + values[10] * values[10]
	}

	/**
	 * @return the scale factor on the X axis (non-negative)
	 */
	override fun getScaleX(): Float {
		return if ((MathUtils.isZero(values[4]) && MathUtils.isZero(values[8])))
			MathUtils.abs(values[0])
		else
			MathUtils.sqrt(getScaleXSquared())
	}

	/**
	 * @return the scale factor on the Y axis (non-negative)
	 */
	override fun getScaleY(): Float {
		return if ((MathUtils.isZero(values[1]) && MathUtils.isZero(values[9])))
			MathUtils.abs(values[5])
		else
			MathUtils.sqrt(getScaleYSquared())
	}

	/**
	 * @return the scale factor on the X axis (non-negative)
	 */
	override fun getScaleZ(): Float {
		return if ((MathUtils.isZero(values[2]) && MathUtils.isZero(values[6])))
			MathUtils.abs(values[10])
		else
			MathUtils.sqrt(getScaleZSquared())
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
		values[12] = 0f
		values[13] = 0f
		values[14] = 0f
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
		val v03 = matA[0] * x + matA[4] * y + matA[8] * z + matA[12]
		val v13 = matA[1] * x + matA[5] * y + matA[9] * z + matA[13]
		val v23 = matA[2] * x + matA[6] * y + matA[10] * z + matA[14]
		val v33 = matA[3] * x + matA[7] * y + matA[11] * z + matA[15]
		matA[12] = v03
		matA[13] = v13
		matA[14] = v23
		matA[15] = v33
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
		rotation.toMatrix(tmp)
		mul(values, tmp)
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
	 * @param dst the destination matrix
	 */
	override fun extract4x3Matrix(dst: MutableList<Float>) {
		dst[0] = values[0]
		dst[1] = values[1]
		dst[2] = values[2]
		dst[3] = values[4]
		dst[4] = values[5]
		dst[5] = values[6]
		dst[6] = values[8]
		dst[7] = values[9]
		dst[8] = values[10]
		dst[9] = values[12]
		dst[10] = values[13]
		dst[11] = values[14]
	}

	/**
	 * Multiplies the vector with this matrix, performing a division by w.
	 *
	 * @param vec the vector.
	 * @return Returns the vec for chaining.
	 */
	override fun prj(vec: Vector3): Vector3 {
		val mat = values
		val inv_w = 1.0f / (vec.x * mat[3] + vec.y * mat[7] + vec.z * mat[11] + mat[15])
		val x = (vec.x * mat[0] + vec.y * mat[4] + vec.z * mat[8] + mat[12]) * inv_w
		val y = (vec.x * mat[1] + vec.y * mat[5] + vec.z * mat[9] + mat[13]) * inv_w
		val z = (vec.x * mat[2] + vec.y * mat[6] + vec.z * mat[10] + mat[14]) * inv_w
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
		val inv_w = 1.0f / (vec.x * mat[3] + vec.y * mat[7] + mat[15])
		val x = (vec.x * mat[0] + vec.y * mat[4] + mat[12]) * inv_w
		val y = (vec.x * mat[1] + vec.y * mat[5] + mat[13]) * inv_w
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
		val x = vec.x * mat[0] + vec.y * mat[4] + vec.z * mat[8]
		val y = vec.x * mat[1] + vec.y * mat[5] + vec.z * mat[9]
		val z = vec.x * mat[2] + vec.y * mat[6] + vec.z * mat[10]
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
		val x = vec.x * mat[0] + vec.y * mat[4]
		val y = vec.x * mat[1] + vec.y * mat[5]
		vec.x = x
		vec.y = y
		return vec
	}

	companion object {

		private val tmp = ArrayList(16, { 0f })

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

		private fun mul(matA: MutableList<Float>, matB: List<Float>) {
			val v00 = matA[0] * matB[0] + matA[4] * matB[1] + matA[8] * matB[2] + matA[12] * matB[3]
			val v01 = matA[0] * matB[4] + matA[4] * matB[5] + matA[8] * matB[6] + matA[12] * matB[7]
			val v02 = matA[0] * matB[8] + matA[4] * matB[9] + matA[8] * matB[10] + matA[12] * matB[11]
			val v03 = matA[0] * matB[12] + matA[4] * matB[13] + matA[8] * matB[14] + matA[12] * matB[15]

			val v10 = matA[1] * matB[0] + matA[5] * matB[1] + matA[9] * matB[2] + matA[13] * matB[3]
			val v11 = matA[1] * matB[4] + matA[5] * matB[5] + matA[9] * matB[6] + matA[13] * matB[7]
			val v12 = matA[1] * matB[8] + matA[5] * matB[9] + matA[9] * matB[10] + matA[13] * matB[11]
			val v13 = matA[1] * matB[12] + matA[5] * matB[13] + matA[9] * matB[14] + matA[13] * matB[15]

			val v20 = matA[2] * matB[0] + matA[6] * matB[1] + matA[10] * matB[2] + matA[14] * matB[3]
			val v21 = matA[2] * matB[4] + matA[6] * matB[5] + matA[10] * matB[6] + matA[14] * matB[7]
			val v22 = matA[2] * matB[8] + matA[6] * matB[9] + matA[10] * matB[10] + matA[14] * matB[11]
			val v23 = matA[2] * matB[12] + matA[6] * matB[13] + matA[10] * matB[14] + matA[14] * matB[15]

			val v30 = matA[3] * matB[0] + matA[7] * matB[1] + matA[11] * matB[2] + matA[15] * matB[3]
			val v31 = matA[3] * matB[4] + matA[7] * matB[5] + matA[11] * matB[6] + matA[15] * matB[7]
			val v32 = matA[3] * matB[8] + matA[7] * matB[9] + matA[11] * matB[10] + matA[15] * matB[11]
			val v33 = matA[3] * matB[12] + matA[7] * matB[13] + matA[11] * matB[14] + matA[15] * matB[15]

			matA[0] = v00
			matA[4] = v01
			matA[8] = v02
			matA[12] = v03

			matA[1] = v10
			matA[5] = v11
			matA[9] = v12
			matA[13] = v13

			matA[2] = v20
			matA[6] = v21
			matA[10] = v22
			matA[14] = v23

			matA[3] = v30
			matA[7] = v31
			matA[11] = v32
			matA[15] = v33
		}

	}

	// TODO: support shearXY, shearZY, etc
	/**
	 * Postmultiplies this matrix by a shear matrix.
	 * @param shearXZ The shear in x direction.
	 * @param shearYZ The shear in y direction.
	 * @return This matrix for the purpose of chaining.
	 */
	fun shear(shearXZ: Float = 0f, shearYZ: Float = 0f, shearXY: Float = 0f, shearZY: Float = 0f, shearZX: Float = 0f, shearYX: Float = 0f): Matrix4 {
		var tmp0 = values[0] + shearYZ * values[4]
		var tmp1 = values[4] + shearXZ * values[0]
		values[0] = tmp0
		values[4] = tmp1

		tmp0 = values[1] + shearYZ * values[5]
		tmp1 = values[5] + shearXZ * values[1]
		values[1] = tmp0
		values[5] = tmp1

		return this
	}
}

fun matrix4(init: Matrix4.() -> Unit): Matrix4 {
	val m = Matrix4()
	m.init()
	return m
}