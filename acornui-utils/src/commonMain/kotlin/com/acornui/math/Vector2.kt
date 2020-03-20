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

import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import kotlinx.serialization.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlin.math.*
import kotlin.random.Random

/**
 * A read-only view into a Vector2
 */
@Serializable(with = Vector2Serializer::class)
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
	val angle: Float
		get() = atan2(y, x)

	/**
	 * @return the angle in radians of this vector (point) relative to the given vector. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise.)
	 */
	fun angle(reference: Vector2Ro): Float

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

	operator fun plus(other: Vector2Ro): Vector2 {
		return Vector2(x + other.x, y + other.y)
	}

	operator fun minus(other: Vector2Ro): Vector2 {
		return Vector2(x - other.x, y - other.y)
	}

	operator fun times(other: Vector2Ro): Vector2 {
		return Vector2(x * other.x, y * other.y)
	}

	operator fun times(other: Float): Vector2 {
		return Vector2(x * other, y * other)
	}

	fun copy(x: Float = this.x, y: Float = this.y): Vector2 {
		return Vector2(x, y)
	}

	/**
	 * Creates a Vector3 with the given z component.
	 */
	fun toVec3(z: Float = 0f): Vector3 = Vector3(x, y, z)
}

/**
 * Encapsulates a 2D vector. Allows chaining methods by returning a reference to itself
 * @author badlogicgames@gmail.com
 */
@Serializable(with = Vector2Serializer::class)
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
		return sqrt((x * x + y * y))
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
		if (len > 0.00001f) {
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

	override fun dst(v: Vector2Ro): Float {
		val xD = v.x - x
		val yD = v.y - y
		return sqrt(xD * xD + yD * yD)
	}

	/**
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the distance between this and the other vector
	 */
	override fun dst(x: Float, y: Float): Float {
		val xD = x - this.x
		val yD = y - this.y
		return sqrt((xD * xD + yD * yD))
	}

	override fun dst2(v: Vector2Ro): Float {
		val xD = v.x - x
		val yD = v.y - y
		return xD * xD + yD * yD
	}

	/**
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the squared distance between this and the other vector
	 */
	override fun dst2(x: Float, y: Float): Float {
		val xD = x - this.x
		val yD = y - this.y
		return xD * xD + yD * yD
	}

	/**
	 * Returns the manhattan distance between this vector and the given vector.
	 */
	override fun manhattanDst(v: Vector2Ro): Float {
		val xD = v.x - x
		val yD = v.y - y
		return abs(xD) + abs(yD)
	}

	fun limit(limit: Float): Vector2 {
		if (len2() > limit * limit) {
			nor()
			scl(limit)
		}
		return this
	}

	fun random(random: Random = Random): Vector2 {
		x = random.nextFloat() * 2f - 1f
		y = random.nextFloat() * 2f - 1f
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
	fun mul(mat: Matrix3Ro): Vector2 {
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
	 * @return the angle in radians of this vector (point) relative to the given vector. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise.)
	 */
	override fun angle(reference: Vector2Ro): Float {
		return atan2(crs(reference), dot(reference))
	}

	/**
	 * Sets the angle of the vector in radians relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
	 * @param radians The angle in radians to set.
	 */
	override fun setAngleRad(radians: Float): Vector2 {
		this.set(len(), 0f)
		this.rotate(radians)

		return this
	}

	@Deprecated("use rotate", ReplaceWith("rotate(radians)"))
	fun rotateRad(radians: Float): Vector2 = rotate(radians)

	/**
	 * Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
	 * @param radians the angle in radians
	 */
	fun rotate(radians: Float): Vector2 {
		val cos = cos(radians)
		val sin = sin(radians)

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
		if (abs(other.x - x) > epsilon) return false
		if (abs(other.y - y) > epsilon) return false
		return true
	}

	/**
	 * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same.
	 */
	override fun epsilonEquals(x: Float, y: Float, epsilon: Float): Boolean {
		if (abs(x - this.x) > epsilon) return false
		if (abs(y - this.y) > epsilon) return false
		return true
	}

	override fun isUnit(): Boolean {
		return isUnit(0.000000001f)
	}

	override fun isUnit(margin: Float): Boolean {
		return abs(len2() - 1f) < margin
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

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as Vector2Ro

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
		return "Vector2(x=$x, y=$y)"
	}

	companion object {

		val X: Vector2Ro = Vector2(1f, 0f)
		val Y: Vector2Ro = Vector2(0f, 1f)
		val ZERO: Vector2Ro = Vector2(0f, 0f)

		fun len(x: Float, y: Float): Float {
			return sqrt((x * x + y * y))
		}

		fun len2(x: Float, y: Float): Float {
			return x * x + y * y
		}

		fun dot(x1: Float, y1: Float, x2: Float, y2: Float): Float {
			return x1 * x2 + y1 * y2
		}

		fun dst(x1: Float, y1: Float, x2: Float, y2: Float): Float {
			val xD = x2 - x1
			val yD = y2 - y1
			return sqrt((xD * xD + yD * yD))
		}

		fun manhattanDst(x1: Float, y1: Float, x2: Float, y2: Float): Float {
			val xD = x2 - x1
			val yD = y2 - y1
			return abs(xD) + abs(yD)
		}

		fun dst2(x1: Float, y1: Float, x2: Float, y2: Float): Float {
			val xD = x2 - x1
			val yD = y2 - y1
			return xD * xD + yD * yD
		}

		private val pool = ClearableObjectPool { Vector2() }

		fun obtain(): Vector2 = pool.obtain()
		fun free(obj: Vector2) = pool.free(obj)
	}

}

@Serializer(forClass = Vector2::class)
object Vector2Serializer : KSerializer<Vector2> {

	override val descriptor: SerialDescriptor = SerialDescriptor("Vector2") {
		listDescriptor<Float>()
	}

	override fun serialize(encoder: Encoder, value: Vector2) {
		encoder.encodeSerializableValue(Float.serializer().list, listOf(value.x, value.y))
	}

	override fun deserialize(decoder: Decoder): Vector2 {
		val values = decoder.decodeSerializableValue(Float.serializer().list)
		return Vector2(
				x = values[0],
				y = values[1]
		)
	}
}