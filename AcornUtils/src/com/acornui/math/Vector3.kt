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

import com.acornui.collection.ClearableObjectPool
import com.acornui.collection.Clearable
import com.acornui.serialization.Reader
import com.acornui.serialization.Writer
import com.acornui.serialization.floatArray

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

/**
 * A read-only view into Vector3
 */
interface Vector3Ro {

	/**
	 * The x-component of this vector
	 **/
	val x: Float

	/**
	 * The y-component of this vector
	 **/
	val y: Float

	/**
	 * The z-component of this vector
	 **/
	val z: Float

	operator fun component1(): Float = x
	operator fun component2(): Float = y
	operator fun component3(): Float = z

	fun len(): Float
	fun len2(): Float
	fun dst(vector: Vector3Ro): Float

	/**
	 * @return the distance between this point and the given point
	 */
	fun dst(x: Float, y: Float, z: Float): Float

	fun dst2(point: Vector3Ro): Float
	/**
	 * Returns the squared distance between this point and the given point
	 * @param x The x-component of the other point
	 * @param y The y-component of the other point
	 * @param z The z-component of the other point
	 * @return The squared distance
	 */
	fun dst2(x: Float, y: Float, z: Float): Float

	fun dot(vector: Vector3Ro): Float
	/**
	 * Returns the dot product between this and the given vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return The dot product
	 */
	fun dot(x: Float, y: Float, z: Float): Float

	fun isUnit(margin: Float = 0.000000001f): Boolean
	fun isZero(): Boolean
	fun isZero(margin: Float): Boolean
	fun isOnLine(other: Vector3Ro, epsilon: Float): Boolean
	fun isOnLine(other: Vector3Ro): Boolean
	fun isCollinear(other: Vector3Ro, epsilon: Float): Boolean
	fun isCollinear(other: Vector3Ro): Boolean
	fun isCollinearOpposite(other: Vector3Ro, epsilon: Float): Boolean
	fun isCollinearOpposite(other: Vector3Ro): Boolean
	fun isPerpendicular(vector: Vector3Ro): Boolean
	fun isPerpendicular(vector: Vector3Ro, epsilon: Float): Boolean
	fun hasSameDirection(vector: Vector3Ro): Boolean
	fun hasOppositeDirection(vector: Vector3Ro): Boolean
	fun closeTo(other: Vector3Ro?, epsilon: Float = 0.0001f): Boolean
	/**
	 * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same.
	 */
	fun closeTo(x: Float, y: Float, z: Float, epsilon: Float = 0.0001f): Boolean

	fun copy(): Vector3 {
		return Vector3(x, y, z)
	}

}

/**
 * Encapsulates a 3D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com
 */
class Vector3 (

		/**
		 * The x-component of this vector
		 **/
		override var x: Float = 0f,

		/**
		 * The y-component of this vector
		 **/
		override var y: Float = 0f,

		/**
		 * The z-component of this vector
		 **/
		override var z: Float = 0f
) : Clearable, Vector3Ro {

	constructor(vector: Vector2Ro, z: Float): this(vector.x, vector.y, z)

	/**
	 * Sets the vector to the given components
	 *
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @return this vector for chaining
	 */
	fun set(x: Float, y: Float, z: Float): Vector3 {
		this.x = x
		this.y = y
		this.z = z
		return this
	}

	fun set(vector: Vector3Ro): Vector3 {
		return this.set(vector.x, vector.y, vector.z)
	}

	/**
	 * Sets the components from the array. The array must have at least 3 elements
	 *
	 * @param values The array
	 * @return this vector for chaining
	 */
	fun set(values: FloatArray): Vector3 {
		return this.set(values[0], values[1], values[2])
	}

	/**
	 * Sets the components of the given vector and z-component
	 *
	 * @param vector The vector
	 * @param z The z-component
	 * @return This vector for chaining
	 */
	fun set(vector: Vector2Ro, z: Float = 0f): Vector3 {
		return this.set(vector.x, vector.y, z)
	}

	fun add(vector: Vector3Ro): Vector3 {
		return this.add(vector.x, vector.y, vector.z)
	}

	/**
	 * Adds the given vector to this component
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining.
	 */
	fun add(x: Float, y: Float, z: Float): Vector3 {
		return this.set(this.x + x, this.y + y, this.z + z)
	}

	/**
	 * Adds the given value to all three components of the vector.
	 *
	 * @param values The value
	 * @return This vector for chaining
	 */
	fun add(values: Float): Vector3 {
		return this.set(this.x + values, this.y + values, this.z + values)
	}

	fun sub(a_vec: Vector3Ro): Vector3 {
		return this.sub(a_vec.x, a_vec.y, a_vec.z)
	}

	/**
	 * Subtracts the other vector from this vector.
	 *
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining
	 */
	fun sub(x: Float, y: Float, z: Float): Vector3 {
		return this.set(this.x - x, this.y - y, this.z - z)
	}

	/**
	 * Subtracts the given value from all components of this vector
	 *
	 * @param value The value
	 * @return This vector for chaining

	 */
	fun sub(value: Float): Vector3 {
		return this.set(this.x - value, this.y - value, this.z - value)
	}

	fun scl(scalar: Float): Vector3 {
		return this.set(this.x * scalar, this.y * scalar, this.z * scalar)
	}

	fun scl(other: Vector3Ro): Vector3 {
		return this.set(x * other.x, y * other.y, z * other.z)
	}

	/**
	 * Scales this vector by the given values
	 * @param vx X value
	 * @param vy Y value
	 * @param vz Z value
	 * @return This vector for chaining
	 */
	fun scl(vx: Float, vy: Float, vz: Float): Vector3 {
		return this.set(this.x * vx, this.y * vy, this.z * vz)
	}

	fun mulAdd(vec: Vector3Ro, scalar: Float): Vector3 {
		this.x += vec.x * scalar
		this.y += vec.y * scalar
		this.z += vec.z * scalar
		return this
	}

	fun mulAdd(vec: Vector3Ro, mulVec: Vector3Ro): Vector3 {
		this.x += vec.x * mulVec.x
		this.y += vec.y * mulVec.y
		this.z += vec.z * mulVec.z
		return this
	}

	override fun len(): Float {
		return MathUtils.sqrt(x * x + y * y + z * z)
	}

	/**
	 * Scales this vector so that the length is equal to the provided value.
	 */
	fun len(value: Float): Vector3 {
		return nor().scl(value)
	}

	override fun len2(): Float {
		return x * x + y * y + z * z
	}

	/**
	 * @param vector The other vector
	 * @return Wether this and the other vector are equal
	 */
	fun idt(vector: Vector3Ro): Boolean {
		return x == vector.x && y == vector.y && z == vector.z
	}

	override fun dst(vector: Vector3Ro): Float {
		val a = vector.x - x
		val b = vector.y - y
		val c = vector.z - z
		return MathUtils.sqrt(a * a + b * b + c * c)
	}

	/**
	 * @return the distance between this point and the given point
	 */
	override fun dst(x: Float, y: Float, z: Float): Float {
		val a = x - this.x
		val b = y - this.y
		val c = z - this.z
		return MathUtils.sqrt(a * a + b * b + c * c)
	}

	override fun dst2(point: Vector3Ro): Float {
		val a = point.x - x
		val b = point.y - y
		val c = point.z - z
		return a * a + b * b + c * c
	}

	/**
	 * Returns the squared distance between this point and the given point
	 * @param x The x-component of the other point
	 * @param y The y-component of the other point
	 * @param z The z-component of the other point
	 * @return The squared distance
	 */
	override fun dst2(x: Float, y: Float, z: Float): Float {
		val a = x - this.x
		val b = y - this.y
		val c = z - this.z
		return a * a + b * b + c * c
	}

	fun nor(): Vector3 {
		val len2 = this.len2()
		if (len2 == 0f || len2 == 1f) return this
		return this.scl(1f / MathUtils.sqrt(len2))
	}

	override fun dot(vector: Vector3Ro): Float {
		return x * vector.x + y * vector.y + z * vector.z
	}

	/**
	 * Returns the dot product between this and the given vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return The dot product
	 */
	override fun dot(x: Float, y: Float, z: Float): Float {
		return this.x * x + this.y * y + this.z * z
	}

	/**
	 * Sets this vector to the cross product between it and the other vector.
	 * @param vector The other vector
	 * @return This vector for chaining
	 */
	fun crs(vector: Vector3Ro): Vector3 {
		return this.set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x)
	}

	/**
	 * Sets this vector to the cross product between it and the other vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining
	 */
	fun crs(x: Float, y: Float, z: Float): Vector3 {
		return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x)
	}

	/**
	 * Left-multiplies the vector by the given 4x3 column major matrix. The matrix should be composed by a 3x3 matrix representing
	 * rotation and scale plus a 1x3 matrix representing the translation.
	 * @param matrix The matrix
	 * @return This vector for chaining
	 */
	fun mul4x3(matrix: List<Float>): Vector3 {
		return set(x * matrix[0] + y * matrix[3] + z * matrix[6] + matrix[9], x * matrix[1] + y * matrix[4] + z * matrix[7] + matrix[10], x * matrix[2] + y * matrix[5] + z * matrix[8] + matrix[11])
	}

	/**
	 * Left-multiplies the vector by the given matrix, assuming the fourth (w) component of the vector is 1.
	 * @param matrix The matrix
	 * @return This vector for chaining
	 */
	fun mul(matrix: Matrix4Ro): Vector3 {
		val l_mat = matrix.values
		return set(
				x * l_mat[0] + y * l_mat[4] + z * l_mat[8] + l_mat[12],
				x * l_mat[1] + y * l_mat[5] + z * l_mat[9] + l_mat[13],
				x * l_mat[2] + y * l_mat[6] + z * l_mat[10] + l_mat[14])
	}

	/**
	 * Multiplies the vector by the transpose of the given matrix, assuming the fourth (w) component of the vector is 1.
	 * @param matrix The matrix
	 * @return This vector for chaining
	 */
	fun traMul(matrix: Matrix4Ro): Vector3 {
		val l_mat = matrix.values
		return this.set(x * l_mat[0] + y * l_mat[1] + z * l_mat[2] + l_mat[3], x * l_mat[4] + y * l_mat[5] + z * l_mat[6] + l_mat[7], x * l_mat[8] + y * l_mat[9] + z * l_mat[10] + l_mat[11])
	}

	/**
	 * Left-multiplies the vector by the given matrix.
	 * @param matrix The matrix
	 * @return This vector for chaining
	 */
	fun mul(matrix: Matrix3Ro): Vector3 {
		val l_mat = matrix.values
		return set(x * l_mat[Matrix3.M00] + y * l_mat[Matrix3.M01] + z * l_mat[Matrix3.M02], x * l_mat[Matrix3.M10] + y * l_mat[Matrix3.M11] + z * l_mat[Matrix3.M12], x * l_mat[Matrix3.M20] + y * l_mat[Matrix3.M21] + z * l_mat[Matrix3.M22])
	}

	/**
	 * Multiplies the vector by the transpose of the given matrix.
	 * @param matrix The matrix
	 * @return This vector for chaining
	 */
	fun traMul(matrix: Matrix3Ro): Vector3 {
		val l_mat = matrix.values
		return set(x * l_mat[Matrix3.M00] + y * l_mat[Matrix3.M10] + z * l_mat[Matrix3.M20], x * l_mat[Matrix3.M01] + y * l_mat[Matrix3.M11] + z * l_mat[Matrix3.M21], x * l_mat[Matrix3.M02] + y * l_mat[Matrix3.M12] + z * l_mat[Matrix3.M22])
	}

	/**
	 * Multiplies the vector by the given {@link Quaternion}.
	 * @return This vector for chaining
	 */
	fun mul(quat: QuaternionRo): Vector3 {
		return quat.transform(this)
	}

	/**
	 * Multiplies this vector by the first three columns of the matrix, essentially only applying rotation and scaling.
	 *
	 * @param matrix The matrix
	 * @return This vector for chaining
	 */
	fun rot(matrix: Matrix4Ro): Vector3 {
		val l_mat = matrix.values
		return this.set(x * l_mat[0] + y * l_mat[4] + z * l_mat[8], x * l_mat[1] + y * l_mat[5] + z * l_mat[9], x * l_mat[2] + y * l_mat[6] + z * l_mat[10])
	}

	/**
	 * Multiplies this vector by the transpose of the first three columns of the matrix. Note: only works for translation and
	 * rotation, does not work for scaling. For those, use {@link #rot(Matrix4)} with {@link Matrix4#inv()}.
	 * @param matrix The transformation matrix
	 * @return The vector for chaining
	 */
	fun unrotate(matrix: Matrix4Ro): Vector3 {
		val l_mat = matrix.values
		return this.set(x * l_mat[0] + y * l_mat[1] + z * l_mat[2], x * l_mat[4] + y * l_mat[5] + z * l_mat[6], x * l_mat[8] + y * l_mat[9] + z * l_mat[10])
	}

	/**
	 * Translates this vector in the direction opposite to the translation of the matrix and the multiplies this vector by the
	 * transpose of the first three columns of the matrix. Note: only works for translation and rotation, does not work for
	 * scaling. For those, use {@link #mul(Matrix4)} with {@link Matrix4#inv()}.
	 * @param matrix The transformation matrix
	 * @return The vector for chaining
	 */
	fun untransform(matrix: Matrix4Ro): Vector3 {
		val l_mat = matrix.values
		x -= l_mat[12]
		y -= l_mat[12]
		z -= l_mat[12]
		return this.set(x * l_mat[0] + y * l_mat[1] + z * l_mat[2], x * l_mat[4] + y * l_mat[5] + z * l_mat[6], x * l_mat[8] + y * l_mat[9] + z * l_mat[10])
	}

	/**
	 * Rotates this vector by the given angle in radians around the given axis.
	 *
	 * @param radians the angle in radians
	 * @param axisX the x-component of the axis
	 * @param axisY the y-component of the axis
	 * @param axisZ the z-component of the axis
	 * @return This vector for chaining
	 */
	fun rotate(radians: Float, axisX: Float, axisY: Float, axisZ: Float): Vector3 {
		return this.mul(tmpMat.idt().rotate(axisX, axisY, axisZ, radians))
	}

	/**
	 * Rotates this vector by the given angle in radians around the given axis.
	 *
	 * @param radians the angle in radians
	 * @param axis the axis
	 * @return This vector for chaining
	 */
	fun rotate(radians: Float, axis: Vector3Ro): Vector3 {
		tmpMat.idt().rotate(axis, radians)
		return this.mul(tmpMat)
	}

	override fun isUnit(margin: Float): Boolean {
		return MathUtils.abs(len2() - 1f) < margin
	}

	override fun isZero(): Boolean {
		return x == 0f && y == 0f && z == 0f
	}

	override fun isZero(margin: Float): Boolean {
		return len2() < margin
	}

	override fun isOnLine(other: Vector3Ro, epsilon: Float): Boolean {
		return len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= epsilon
	}

	override fun isOnLine(other: Vector3Ro): Boolean {
		return len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= MathUtils.FLOAT_ROUNDING_ERROR
	}

	override fun isCollinear(other: Vector3Ro, epsilon: Float): Boolean {
		return isOnLine(other, epsilon) && hasSameDirection(other)
	}

	override fun isCollinear(other: Vector3Ro): Boolean {
		return isOnLine(other) && hasSameDirection(other)
	}

	override fun isCollinearOpposite(other: Vector3Ro, epsilon: Float): Boolean {
		return isOnLine(other, epsilon) && hasOppositeDirection(other)
	}

	override fun isCollinearOpposite(other: Vector3Ro): Boolean {
		return isOnLine(other) && hasOppositeDirection(other)
	}

	override fun isPerpendicular(vector: Vector3Ro): Boolean {
		return MathUtils.isZero(dot(vector))
	}

	override fun isPerpendicular(vector: Vector3Ro, epsilon: Float): Boolean {
		return MathUtils.isZero(dot(vector), epsilon)
	}

	override fun hasSameDirection(vector: Vector3Ro): Boolean {
		return dot(vector) > 0
	}

	override fun hasOppositeDirection(vector: Vector3Ro): Boolean {
		return dot(vector) < 0
	}

	fun lerp(target: Vector3Ro, alpha: Float): Vector3 {
		scl(1.0f - alpha)
		add(target.x * alpha, target.y * alpha, target.z * alpha)
		return this
	}

	fun interpolate(target: Vector3Ro, alpha: Float, interpolator: Interpolation): Vector3 {
		return lerp(target, interpolator.apply(0f, 1f, alpha))
	}

	/**
	 * Spherically interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is
	 * stored in this vector.
	 *
	 * @param target The target vector
	 * @param alpha The interpolation coefficient
	 * @return This vector for chaining.
	 */
	fun slerp(target: Vector3Ro, alpha: Float): Vector3 {
		val dot = dot(target)
		// If the inputs are too close for comfort, simply linearly interpolate.
		if (dot > 0.9995f || dot < -0.9995f) return lerp(target, alpha)

		// theta0 = angle between input vectors
		val theta0 = MathUtils.acos(dot)
		// theta = angle between this vector and result
		val theta = theta0 * alpha

		val st = MathUtils.sin(theta)
		val tx = target.x - x * dot
		val ty = target.y - y * dot
		val tz = target.z - z * dot
		val l2 = tx * tx + ty * ty + tz * tz
		val dl = st * (if ((l2 < 0.0001f)) 1f else 1f / MathUtils.sqrt(l2))

		return scl(MathUtils.cos(theta)).add(tx * dl, ty * dl, tz * dl).nor()
	}

	fun limit(limit: Float): Vector3 {
		if (len2() > limit * limit) nor().scl(limit)
		return this
	}

	fun random(): Vector3 {
		x = MathUtils.random() * 2f - 1f
		y = MathUtils.random() * 2f - 1f
		z = MathUtils.random() * 2f - 1f
		return this
	}

	fun clamp(min: Float, max: Float): Vector3 {
		val l2 = len2()
		if (l2 == 0f) return this
		if (l2 > max * max) return nor().scl(max)
		if (l2 < min * min) return nor().scl(min)
		return this
	}

	override fun closeTo(other: Vector3Ro?, epsilon: Float): Boolean {
		if (other == null) return false
		if (MathUtils.abs(other.x - x) > epsilon) return false
		if (MathUtils.abs(other.y - y) > epsilon) return false
		if (MathUtils.abs(other.z - z) > epsilon) return false
		return true
	}

	/**
	 * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same.
	 */
	override fun closeTo(x: Float, y: Float, z: Float, epsilon: Float): Boolean {
		if (MathUtils.abs(x - this.x) > epsilon) return false
		if (MathUtils.abs(y - this.y) > epsilon) return false
		if (MathUtils.abs(z - this.z) > epsilon) return false
		return true
	}

	override fun clear() {
		x = 0f
		y = 0f
		z = 0f
	}

	fun free() {
		pool.free(this)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Vector3) return false
		if (x != other.x) return false
		if (y != other.y) return false
		if (z != other.z) return false
		return true
	}

	override fun hashCode(): Int {
		var result = x.hashCode()
		result = 31 * result + y.hashCode()
		result = 31 * result + z.hashCode()
		return result
	}

	override fun toString(): String {
		return "[x=$x, y=$y, z=$z]"
	}

	companion object {
		val X: Vector3Ro = Vector3(1f, 0f, 0f)
		val Y: Vector3Ro = Vector3(0f, 1f, 0f)
		val Z: Vector3Ro = Vector3(0f, 0f, 1f)
		val NEG_X: Vector3Ro = Vector3(-1f, 0f, 0f)
		val NEG_Y: Vector3Ro = Vector3(0f, -1f, 0f)
		val NEG_Z: Vector3Ro = Vector3(0f, 0f, -1f)
		val ZERO: Vector3Ro = Vector3(0f, 0f, 0f)
		val ONE: Vector3Ro = Vector3(1f, 1f, 1f)

		private val tmpMat = Matrix4()

		/**
		 * @return The euclidian length
		 */
		fun len(x: Float, y: Float, z: Float): Float {
			return MathUtils.sqrt(x * x + y * y + z * z)
		}

		/**
		 * @return The squared euclidian length
		 */
		fun len2(x: Float, y: Float, z: Float): Float {
			return x * x + y * y + z * z
		}

		/**
		 * @return The euclidian distance between the two specified vectors
		 */
		fun dst(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
			val a = x2 - x1
			val b = y2 - y1
			val c = z2 - z1
			return MathUtils.sqrt(a * a + b * b + c * c)
		}

		/**
		 * @return the squared distance between the given points
		 */
		fun dst2(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
			val a = x2 - x1
			val b = y2 - y1
			val c = z2 - z1
			return a * a + b * b + c * c
		}

		/**
		 * @return The dot product between the two vectors
		 */
		fun dot(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
			return x1 * x2 + y1 * y2 + z1 * z2
		}

		private val pool = ClearableObjectPool { Vector3() }

		fun obtain(): Vector3 {
			return pool.obtain()
		}
	}

}

fun Writer.vector3(v: Vector3Ro) {
	floatArray(floatArrayOf(v.x, v.y, v.z))
}

fun Writer.vector3(name: String, v: Vector3Ro) = property(name).vector3(v)

fun Reader.vector3(): Vector3? {
	val f = floatArray() ?: return null
	return Vector3(f[0], f[1], f[2])
}

fun Reader.vector3(name: String): Vector3? = get(name)?.vector3()