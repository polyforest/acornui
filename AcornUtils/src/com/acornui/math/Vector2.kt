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
import com.acornui.serialization.*

/**
 * A read-only view into a Vector2
 */
interface Vector2Ro {

	val x: Float
	val y: Float

	operator fun component1(): Float = x
	operator fun component2(): Float = y

	fun len(): Float
	fun len2(): Float
	fun dot(v: Vector2Ro): Float
	fun dot(ox: Float, oy: Float): Float
	fun dst(v: Vector2Ro): Float
	/**
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the distance between this and the other vector
	 */
	fun dst(x: Float, y: Float): Float

	fun dst2(v: Vector2Ro): Float
	/**
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the squared distance between this and the other vector
	 */
	fun dst2(x: Float, y: Float): Float

	/**
	 * Returns the manhattan distance between this vector and the given vector.
	 */
	fun manhattanDst(v: Vector2Ro): Float

	/**
	 * @return the angle in radians of this vector (point) relative to the x-axis. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise)
	 */
	fun angleRad(): Float

	/**
	 * @return the angle in radians of this vector (point) relative to the given vector. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise.)
	 */
	fun angleRad(reference: Vector2Ro): Float

	/**
	 * Sets the angle of the vector in radians relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
	 * @param radians The angle in radians to set.
	 */
	fun setAngleRad(radians: Float): Vector2

	fun epsilonEquals(other: Vector2Ro?, epsilon: Float): Boolean
	/**
	 * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same.
	 */
	fun epsilonEquals(x: Float, y: Float, epsilon: Float): Boolean

	fun isUnit(): Boolean
	fun isUnit(margin: Float): Boolean
	fun isZero(): Boolean
	fun isZero(margin2: Float): Boolean
	fun isOnLine(other: Vector2Ro): Boolean
	fun isOnLine(other: Vector2Ro, epsilon2: Float): Boolean
	fun isCollinear(other: Vector2Ro, epsilon: Float): Boolean
	fun isCollinear(other: Vector2Ro): Boolean
	fun isCollinearOpposite(other: Vector2Ro, epsilon: Float): Boolean
	fun isCollinearOpposite(other: Vector2Ro): Boolean
	fun isPerpendicular(vector: Vector2Ro): Boolean
	fun isPerpendicular(vector: Vector2Ro, epsilon: Float): Boolean
	fun hasSameDirection(vector: Vector2Ro): Boolean
	fun hasOppositeDirection(vector: Vector2Ro): Boolean

	fun copy(): Vector2 {
		return Vector2(x, y)
	}
}

/**
 * Encapsulates a 2D vector. Allows chaining methods by returning a reference to itself
 * @author badlogicgames@gmail.com
 */
class Vector2(

		/**
		 * The x-component of this vector
		 **/
		override var x: Float = 0f,

		/**
		 * The y-component of this vector
		 **/
		override var y: Float = 0f
): Clearable, Vector2Ro {

	constructor(other: Vector2Ro) : this(other.x, other.y)

	override fun len(): Float {
		return MathUtils.sqrt((x * x + y * y))
	}

	override fun len2(): Float {
		return x * x + y * y
	}

	fun set(v: Vector2Ro): Vector2 {
		x = v.x
		y = v.y
		return this
	}

	/**
	 * Sets the components of this vector
	 * @param x The x-component
	 * @param y The y-component
	 * @return This vector for chaining
	 */
	fun set(x: Float, y: Float): Vector2 {
		this.x = x
		this.y = y
		return this
	}

	fun sub(v: Vector2Ro): Vector2 {
		x -= v.x
		y -= v.y
		return this
	}

	/**
	 * Substracts the other vector from this vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return This vector for chaining
	 */
	fun sub(x: Float, y: Float): Vector2 {
		this.x -= x
		this.y -= y
		return this
	}

	fun nor(): Vector2 {
		val len = len()
		if (len != 0f) {
			x /= len
			y /= len
		}
		return this
	}

	fun add(v: Vector2Ro): Vector2 {
		x += v.x
		y += v.y
		return this
	}

	/**
	 * Adds the given components to this vector
	 * @param x The x-component
	 * @param y The y-component
	 * @return This vector for chaining
	 */
	fun add(x: Float, y: Float): Vector2 {
		this.x += x
		this.y += y
		return this
	}

	override fun dot(v: Vector2Ro): Float {
		return x * v.x + y * v.y
	}

	override fun dot(ox: Float, oy: Float): Float {
		return x * ox + y * oy
	}

	fun scl(scalar: Float): Vector2 {
		x *= scalar
		y *= scalar
		return this
	}

	/**
	 * Multiplies this vector by a scalar
	 * @return This vector for chaining
	 */
	fun scl(x: Float, y: Float): Vector2 {
		this.x *= x
		this.y *= y
		return this
	}

	fun scl(v: Vector2Ro): Vector2 {
		this.x *= v.x
		this.y *= v.y
		return this
	}

	fun mulAdd(vec: Vector2Ro, scalar: Float): Vector2 {
		this.x += vec.x * scalar
		this.y += vec.y * scalar
		return this
	}

	fun mulAdd(vec: Vector2Ro, mulVec: Vector2Ro): Vector2 {
		this.x += vec.x * mulVec.x
		this.y += vec.y * mulVec.y
		return this
	}

	override fun dst(v: Vector2Ro): Float {
		val x_d = v.x - x
		val y_d = v.y - y
		return MathUtils.sqrt(x_d * x_d + y_d * y_d)
	}

	/**
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the distance between this and the other vector
	 */
	override fun dst(x: Float, y: Float): Float {
		val x_d = x - this.x
		val y_d = y - this.y
		return MathUtils.sqrt((x_d * x_d + y_d * y_d))
	}

	override fun dst2(v: Vector2Ro): Float {
		val x_d = v.x - x
		val y_d = v.y - y
		return x_d * x_d + y_d * y_d
	}

	/**
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the squared distance between this and the other vector
	 */
	override fun dst2(x: Float, y: Float): Float {
		val x_d = x - this.x
		val y_d = y - this.y
		return x_d * x_d + y_d * y_d
	}

	/**
	 * Returns the manhattan distance between this vector and the given vector.
	 */
	override fun manhattanDst(v: Vector2Ro): Float {
		val x_d = v.x - x
		val y_d = v.y - y
		return MathUtils.abs(x_d) + MathUtils.abs(y_d)
	}

	fun limit(limit: Float): Vector2 {
		if (len2() > limit * limit) {
			nor()
			scl(limit)
		}
		return this
	}

	fun random(): Vector2 {
		x = MathUtils.random() * 2f - 1f
		y = MathUtils.random() * 2f - 1f
		return this
	}

	fun clamp(min: Float, max: Float): Vector2 {
		val l2 = len2()
		if (l2 == 0f) return this
		if (l2 > max * max) return nor().scl(max)
		if (l2 < min * min) return nor().scl(min)
		return this
	}

	/**
	 * Left-multiplies this vector by the given matrix
	 * @param mat the matrix
	 * @return this vector
	 */
	fun mul(mat: Matrix3): Vector2 {
		val vals = mat.values
		val x2 = x * vals[0] + y * vals[3] + vals[6]
		val y2 = x * vals[1] + y * vals[4] + vals[7]
		this.x = x2
		this.y = y2
		return this
	}

	/**
	 * Calculates the 2D cross product between this and the given vector.
	 * @param v the other vector
	 * @return the cross product
	 */
	fun crs(v: Vector2Ro): Float {
		return this.x * v.y - this.y * v.x
	}

	/**
	 * Calculates the 2D cross product between this and the given vector.
	 * @param x the x-coordinate of the other vector
	 * @param y the y-coordinate of the other vector
	 * @return the cross product
	 */
	fun crs(x: Float, y: Float): Float {
		return this.x * y - this.y * x
	}

	/**
	 * @return the angle in radians of this vector (point) relative to the x-axis. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise)
	 */
	override fun angleRad(): Float {
		return MathUtils.atan2(y, x)
	}

	/**
	 * @return the angle in radians of this vector (point) relative to the given vector. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise.)
	 */
	override fun angleRad(reference: Vector2Ro): Float {
		return MathUtils.atan2(crs(reference), dot(reference))
	}

	/**
	 * Sets the angle of the vector in radians relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
	 * @param radians The angle in radians to set.
	 */
	override fun setAngleRad(radians: Float): Vector2 {
		this.set(len(), 0f)
		this.rotateRad(radians)

		return this
	}

	/**
	 * Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
	 * @param radians the angle in radians
	 */
	fun rotateRad(radians: Float): Vector2 {
		val cos = MathUtils.cos(radians)
		val sin = MathUtils.sin(radians)

		val newX = this.x * cos - this.y * sin
		val newY = this.x * sin + this.y * cos

		this.x = newX
		this.y = newY

		return this
	}

	fun lerp(target: Vector2Ro, alpha: Float): Vector2 {
		val invAlpha = 1.0f - alpha
		x = (x * invAlpha) + (target.x * alpha)
		y = (y * invAlpha) + (target.y * alpha)
		return this
	}

	fun lerp(x2: Float, y2: Float, alpha: Float): Vector2 {
		val invAlpha = 1.0f - alpha
		x = (x * invAlpha) + (x2 * alpha)
		y = (y * invAlpha) + (y2 * alpha)
		return this
	}

	fun interpolate(target: Vector2Ro, alpha: Float, interpolation: Interpolation): Vector2 {
		return lerp(target, interpolation.apply(alpha))
	}

	override fun epsilonEquals(other: Vector2Ro?, epsilon: Float): Boolean {
		if (other == null) return false
		if (MathUtils.abs(other.x - x) > epsilon) return false
		if (MathUtils.abs(other.y - y) > epsilon) return false
		return true
	}

	/**
	 * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same.
	 */
	override fun epsilonEquals(x: Float, y: Float, epsilon: Float): Boolean {
		if (MathUtils.abs(x - this.x) > epsilon) return false
		if (MathUtils.abs(y - this.y) > epsilon) return false
		return true
	}

	override fun isUnit(): Boolean {
		return isUnit(0.000000001f)
	}

	override fun isUnit(margin: Float): Boolean {
		return MathUtils.abs(len2() - 1f) < margin
	}

	override fun isZero(): Boolean {
		return x == 0f && y == 0f
	}

	override fun isZero(margin2: Float): Boolean {
		return len2() < margin2
	}

	override fun isOnLine(other: Vector2Ro): Boolean {
		return MathUtils.isZero(x * other.y - y * other.x)
	}

	override fun isOnLine(other: Vector2Ro, epsilon2: Float): Boolean {
		return MathUtils.isZero(x * other.y - y * other.x, epsilon2)
	}

	override fun isCollinear(other: Vector2Ro, epsilon: Float): Boolean {
		return isOnLine(other, epsilon) && dot(other) > 0f
	}

	override fun isCollinear(other: Vector2Ro): Boolean {
		return isOnLine(other) && dot(other) > 0f
	}

	override fun isCollinearOpposite(other: Vector2Ro, epsilon: Float): Boolean {
		return isOnLine(other, epsilon) && dot(other) < 0f
	}

	override fun isCollinearOpposite(other: Vector2Ro): Boolean {
		return isOnLine(other) && dot(other) < 0f
	}

	override fun isPerpendicular(vector: Vector2Ro): Boolean {
		return MathUtils.isZero(dot(vector))
	}

	override fun isPerpendicular(vector: Vector2Ro, epsilon: Float): Boolean {
		return MathUtils.isZero(dot(vector), epsilon)
	}

	override fun hasSameDirection(vector: Vector2Ro): Boolean {
		return dot(vector) > 0
	}

	override fun hasOppositeDirection(vector: Vector2Ro): Boolean {
		return dot(vector) < 0
	}

	fun ext(x: Float, y: Float) {
		if (x > this.x) this.x = x
		if (y > this.y) this.y = y
	}

	override fun clear() {
		this.x = 0f
		this.y = 0f
	}

	@Deprecated("Use Vector2.free", ReplaceWith("Vector2.free(this)"))
	fun free() {
		pool.free(this)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Vector2) return false
		if (x != other.x) return false
		if (y != other.y) return false
		return true
	}

	override fun hashCode(): Int {
		var result = x.hashCode()
		result = 31 * result + y.hashCode()
		return result
	}

	override fun toString(): String {
		return "[x=$x, y=$y]"
	}

	companion object {

		val X: Vector2Ro = Vector2(1f, 0f)
		val Y: Vector2Ro = Vector2(0f, 1f)
		val ZERO: Vector2Ro = Vector2(0f, 0f)

		fun len(x: Float, y: Float): Float {
			return MathUtils.sqrt((x * x + y * y))
		}

		fun len2(x: Float, y: Float): Float {
			return x * x + y * y
		}

		fun dot(x1: Float, y1: Float, x2: Float, y2: Float): Float {
			return x1 * x2 + y1 * y2
		}

		fun dst(x1: Float, y1: Float, x2: Float, y2: Float): Float {
			val x_d = x2 - x1
			val y_d = y2 - y1
			return MathUtils.sqrt((x_d * x_d + y_d * y_d))
		}

		fun dst2(x1: Float, y1: Float, x2: Float, y2: Float): Float {
			val x_d = x2 - x1
			val y_d = y2 - y1
			return x_d * x_d + y_d * y_d
		}

		private val pool = ClearableObjectPool { Vector2() }

		fun obtain(): Vector2 = pool.obtain()
		fun free(obj: Vector2) = pool.free(obj)
	}

}

object Vector2Serializer : From<Vector2?>, To<Vector2Ro?> {

	override fun read(reader: Reader): Vector2? {
		return reader.vector2()
	}

	override fun Vector2Ro?.write(writer: Writer) {
		writer.vector2(this)
	}
}

fun Writer.vector2(v: Vector2Ro?) {
	if (v == null) writeNull()
	else floatArray(floatArrayOf(v.x, v.y))
}

fun Writer.vector2(name: String, v: Vector2Ro) = property(name).vector2(v)

fun Reader.vector2(): Vector2? {
	val f = floatArray() ?: return null
	return Vector2(f[0], f[1])
}

fun Reader.vector2(name: String): Vector2? = get(name)?.vector2()
