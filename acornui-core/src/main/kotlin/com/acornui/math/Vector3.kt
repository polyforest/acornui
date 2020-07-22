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

import kotlinx.serialization.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

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
 * A 3 component structure for representing either a position or a direction and magnitude.
 */
@Serializable(with = Vector3Serializer::class)
data class Vector3(

	/**
	 * The x-component of this vector
	 **/
	val x: Double = 0.0,

	/**
	 * The y-component of this vector
	 **/
	val y: Double = 0.0,

	/**
	 * The z-component of this vector
	 **/
	val z: Double = 0.0
) {

	/**
	 * Returns the euclidean length
	 */
	fun len(): Double =
		sqrt(x * x + y * y + z * z)

	/**
	 * Returns this vector scaled so that the length is equal to the provided value.
	 */
	fun len(value: Double): Vector3 =
		nor() * value

	/**
	 * Returns the squared length.
	 */
	fun len2(): Double =
		x * x + y * y + z * z

	fun dst(other: Vector3): Double =
		(other - this).len()

	/**
	 * The inverse of this vector.
	 * 1.0 / this
	 */
	fun inv(): Vector3 = 1.0 / this

	/**
	 * @return the distance between this point and the given point
	 */
	fun dst(x: Double, y: Double, z: Double): Double {
		val a = x - this.x
		val b = y - this.y
		val c = z - this.z
		return sqrt(a * a + b * b + c * c)
	}

	fun dst2(other: Vector3): Double =
		(other - this).len2()

	/**
	 * Returns the squared distance between this point and the given point
	 * @param x The x-component of the other point
	 * @param y The y-component of the other point
	 * @param z The z-component of the other point
	 * @return The squared distance
	 */
	fun dst2(x: Double, y: Double, z: Double): Double {
		val a = x - this.x
		val b = y - this.y
		val c = z - this.z
		return a * a + b * b + c * c
	}

	/**
	 * Returns this vector normalized.
	 * That is, the direction will be the same, but the [len] will be 1.0
	 * If the length of this vector is zero, zero will be returned instead of a divide by zero error.
	 */
	fun nor(): Vector3 {
		val len2 = this.len2()
		if (len2 == 0.0 || len2 == 1.0) return this
		return this * (1.0 / sqrt(len2))
	}

	fun dot(vector: Vector3): Double =
		x * vector.x + y * vector.y + z * vector.z

	/**
	 * Returns the dot product between this and the given vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return The dot product
	 */
	fun dot(x: Double, y: Double, z: Double): Double =
		this.x * x + this.y * y + this.z * z

	/**
	 * Sets this vector to the cross product between it and the other vector.
	 * @param vector The other vector
	 * @return This vector for chaining
	 */
	fun crs(vector: Vector3): Vector3 =
		Vector3(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x)

	/**
	 * Sets this vector to the cross product between it and the other vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return This vector for chaining
	 */
	fun crs(x: Double, y: Double, z: Double): Vector3 =
		Vector3(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x)

	/**
	 * Returns true if this vector is within [margin] squared of 1.0 length.
	 */
	fun isUnit(margin: Double = ROUNDING_ERROR): Boolean =
		abs(len2() - 1.0) < margin

	val isZero: Boolean
		get() = x == 0.0 && y == 0.0 && z == 0.0

	/**
	 * Returns true if this vector is zero within the given margin.
	 */
	fun isZero(margin: Double = ROUNDING_ERROR): Boolean =
		len2() < margin

	fun isOnLine(other: Vector3, epsilon: Double): Boolean =
		len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= epsilon

	fun isOnLine(other: Vector3): Boolean =
		len2(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x) <= ROUNDING_ERROR

	fun isCollinear(other: Vector3, epsilon: Double): Boolean =
		isOnLine(other, epsilon) && hasSameDirection(other)

	fun isCollinear(other: Vector3): Boolean =
		isOnLine(other) && hasSameDirection(other)

	fun isCollinearOpposite(other: Vector3, epsilon: Double): Boolean =
		isOnLine(other, epsilon) && hasOppositeDirection(other)

	fun isCollinearOpposite(other: Vector3): Boolean =
		isOnLine(other) && hasOppositeDirection(other)

	fun isPerpendicular(vector: Vector3): Boolean =
		com.acornui.math.isZero(dot(vector))

	fun isPerpendicular(vector: Vector3, epsilon: Double): Boolean =
		isZero(dot(vector), epsilon)

	fun hasSameDirection(vector: Vector3): Boolean =
		dot(vector) > 0

	fun hasOppositeDirection(vector: Vector3): Boolean =
		dot(vector) < 0

	fun lerp(target: Vector3, alpha: Double): Vector3 =
		this * (1.0 - alpha) + target * alpha

	fun interpolate(target: Vector3, alpha: Double, interpolator: Interpolation): Vector3 =
		lerp(target, interpolator.apply(0.0, 1.0, alpha))

	/**
	 * Returns this vector limited to a maximum length of [limit].
	 */
	fun limit(limit: Double): Vector3 =
		if (len2() > limit * limit) len(limit) else this

	/**
	 * Returns this vector clamped to the given min and max length range.
	 */
	fun clamp(min: Double, max: Double): Vector3 {
		val l2 = len2()
		if (l2 == 0.0) return this
		if (l2 > max * max) return nor() * max
		if (l2 < min * min) return nor() * min
		return this
	}

	fun closeTo(other: Vector3?, epsilon: Double): Boolean {
		if (other == null) return false
		if (abs(other.x - x) > epsilon) return false
		if (abs(other.y - y) > epsilon) return false
		if (abs(other.z - z) > epsilon) return false
		return true
	}

	/**
	 * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same.
	 */
	fun closeTo(x: Double, y: Double, z: Double, epsilon: Double): Boolean {
		if (abs(x - this.x) > epsilon) return false
		if (abs(y - this.y) > epsilon) return false
		if (abs(z - this.z) > epsilon) return false
		return true
	}

	/**
	 * Returns a new vector where the given values are added to each component.
	 */
	fun plus(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vector3 =
		vec3(this.x + x, this.y + y, this.z + z)

	/**
	 * Returns a new vector where the given values are subtracted from each component.
	 */
	fun minus(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vector3 =
		vec3(this.x - x, this.y - y, this.z - z)

	/**
	 * Returns a new vector where the given values are multiplied by each component.
	 */
	fun times(x: Double = 1.0, y: Double = 1.0, z: Double = 1.0): Vector3 =
		vec3(this.x * x, this.y * y, this.z * z)

	operator fun plus(other: Vector3): Vector3 =
		vec3(x + other.x, y + other.y, z + other.z)

	operator fun minus(other: Vector3): Vector3 =
		vec3(x - other.x, y - other.y, z - other.z)

	operator fun plus(other: Vector2): Vector3 =
		vec3(x + other.x, y + other.y, z)

	operator fun minus(other: Vector2): Vector3 =
		vec3(x - other.x, y - other.y, z)

	operator fun times(other: Vector3): Vector3 =
		vec3(x * other.x, y * other.y, z * other.z)

	operator fun times(other: Double): Vector3 =
		vec3(x * other, y * other, z * other)

	operator fun div(other: Double): Vector3 =
		vec3(x / other, y / other, z / other)

	/**
	 * Drops the z component and converts to a Vector2.
	 */
	fun toVec2(): Vector2 = vec2(x, y)

	companion object {
		val X: Vector3 = vec3(1.0, 0.0, 0.0)
		val Y: Vector3 = vec3(0.0, 1.0, 0.0)
		val Z: Vector3 = vec3(0.0, 0.0, 1.0)
		val NEG_X: Vector3 = vec3(-1.0, 0.0, 0.0)
		val NEG_Y: Vector3 = vec3(0.0, -1.0, 0.0)
		val NEG_Z: Vector3 = vec3(0.0, 0.0, -1.0)
		val ZERO: Vector3 = vec3(0.0, 0.0, 0.0)
		val ONE: Vector3 = vec3(1.0, 1.0, 1.0)

		/**
		 * @return The euclidean length
		 */
		fun len(x: Double, y: Double, z: Double): Double =
			sqrt(x * x + y * y + z * z)

		/**
		 * @return The squared euclidean length
		 */
		fun len2(x: Double, y: Double, z: Double): Double =
			x * x + y * y + z * z

		/**
		 * @return The euclidean distance between the two specified vectors
		 */
		fun dst(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
			val a = x2 - x1
			val b = y2 - y1
			val c = z2 - z1
			return sqrt(a * a + b * b + c * c)
		}

		/**
		 * @return the squared distance between the given points
		 */
		fun dst2(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
			val a = x2 - x1
			val b = y2 - y1
			val c = z2 - z1
			return a * a + b * b + c * c
		}

		/**
		 * @return The dot product between the two vectors
		 */
		fun dot(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double =
			x1 * x2 + y1 * y2 + z1 * z2

		/**
		 * Returns a vector where each component is random between -1.0 (inclusive) and 1.0 (exclusive)
		 */
		fun random(random: Random = Random): Vector3 = vec3(
			x = random.nextDouble() * 2.0 - 1.0,
			y = random.nextDouble() * 2.0 - 1.0,
			z = random.nextDouble() * 2.0 - 1.0
		)
	}
}

operator fun Double.times(other: Vector3): Vector3 =
	vec3(other.x * this, other.y * this, other.z * this)

operator fun Double.div(other: Vector3): Vector3 =
	vec3(this / other.x, this / other.y, this / other.z)

/**
 * Shorthand for Vector3.
 */
typealias vec3 = Vector3

@Serializer(forClass = Vector3::class)
object Vector3Serializer : KSerializer<Vector3> {

	override val descriptor: SerialDescriptor = SerialDescriptor("Vector3") {
		listDescriptor<Double>()
	}

	override fun serialize(encoder: Encoder, value: Vector3) {
		encoder.encodeSerializableValue(Double.serializer().list, listOf(value.x, value.y, value.z))
	}

	override fun deserialize(decoder: Decoder): Vector3 {
		val values = decoder.decodeSerializableValue(Double.serializer().list)
		return vec3(
			x = values[0],
			y = values[1],
			z = values[2]
		)
	}
}