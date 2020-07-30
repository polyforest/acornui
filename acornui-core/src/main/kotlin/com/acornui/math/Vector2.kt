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

import com.acornui.number.closeTo
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.*
import kotlin.random.Random

/**
 * A 2 component structure for representing either a position or a direction and magnitude.
 */
@Serializable(with = Vector2Serializer::class)
data class Vector2(

		/**
		 * The x-component of this vector
		 **/
		val x: Double = 0.0,

		/**
		 * The y-component of this vector
		 **/
		val y: Double = 0.0
) {

	constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

	/**
	 * The length of this vector.
	 */
	fun len(): Double =
		sqrt((x * x + y * y))

	/**
	 * Returns this vector scaled so that the length is equal to the provided value.
	 */
	fun len(value: Double): Vector2 =
		nor() * value

	/**
	 * The squared length of this vector.
	 */
	fun len2(): Double =
		x * x + y * y

	fun nor(): Vector2 {
		val len = len()
		return if (len > ROUNDING_ERROR) {
			this / len
		} else
			this
	}

	fun dot(v: Vector2): Double =
		x * v.x + y * v.y

	fun dot(ox: Double, oy: Double): Double =
		x * ox + y * oy

	fun dst(v: Vector2): Double {
		val xD = v.x - x
		val yD = v.y - y
		return sqrt(xD * xD + yD * yD)
	}

	/**
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the distance between this and the other vector
	 */
	fun dst(x: Double, y: Double): Double {
		val xD = x - this.x
		val yD = y - this.y
		return sqrt((xD * xD + yD * yD))
	}

	fun dst2(v: Vector2): Double {
		val xD = v.x - x
		val yD = v.y - y
		return xD * xD + yD * yD
	}

	/**
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @return the squared distance between this and the other vector
	 */
	fun dst2(x: Double, y: Double): Double {
		val xD = x - this.x
		val yD = y - this.y
		return xD * xD + yD * yD
	}

	/**
	 * Returns the manhattan distance between this vector and the given vector.
	 */
	fun manhattanDst(v: Vector2): Double {
		val xD = v.x - x
		val yD = v.y - y
		return abs(xD) + abs(yD)
	}

	/**
	 * Returns this vector limited to a maximum length of [limit].
	 */
	fun limit(limit: Double): Vector2 =
		if (len2() > limit * limit) len(limit) else this

	fun clamp(min: Double, max: Double): Vector2 {
		val l2 = len2()
		if (l2 == 0.0) return this
		if (l2 > max * max) return len(max)
		if (l2 < min * min) return len(min)
		return this
	}

	/**
	 * @return the angle in radians of this vector (point) relative to the x-axis. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise)
	 */
	val angle: Double
		get() = atan2(y, x)

	/**
	 * Calculates the 2D cross product between this and the given vector.
	 * @param v the other vector
	 * @return the cross product
	 */
	fun crs(v: Vector2): Double {
		return this.x * v.y - this.y * v.x
	}

	/**
	 * Calculates the 2D cross product between this and the given vector.
	 * @param x the x-coordinate of the other vector
	 * @param y the y-coordinate of the other vector
	 * @return the cross product
	 */
	fun crs(x: Double, y: Double): Double {
		return this.x * y - this.y * x
	}

	/**
	 * @return the angle in radians of this vector (point) relative to the given vector. Angles are towards the positive y-axis.
	 *         (typically counter-clockwise.)
	 */
	fun angle(reference: Vector2): Double =
		atan2(crs(reference), dot(reference))

	/**
	 * Sets the angle of the vector in radians relative to the x-axis, towards the positive y-axis
	 * (typically counter-clockwise).
	 * @param radians The angle in radians to set.
	 */
	fun setAngle(radians: Double): Vector2 {
		val len = len()
		return vec2(len * cos(radians), len * sin(radians))
	}

	/**
	 * Returns a new vector where the given values are added to each component.
	 */
	fun plus(x: Double = 0.0, y: Double = 0.0): Vector2 =
		vec2(this.x + x, this.y + y)

	/**
	 * Returns a new vector where the given values are subtracted from each component.
	 */
	fun minus(x: Double = 0.0, y: Double = 0.0): Vector2 =
		vec2(this.x - x, this.y - y)

	/**
	 * Returns a new vector where the given values are multiplied by each component.
	 */
	fun times(x: Double = 1.0, y: Double = 1.0): Vector2 =
		vec2(this.x * x, this.y * y)

	operator fun plus(other: Vector2): Vector2 {
		return vec2(x + other.x, y + other.y)
	}

	operator fun minus(other: Vector2): Vector2 {
		return vec2(x - other.x, y - other.y)
	}

	operator fun plus(other: Vector3): Vector3 {
		return vec3(x + other.x, y + other.y, other.z)
	}

	operator fun minus(other: Vector3): Vector3 {
		return vec3(x - other.x, y - other.y, -other.z)
	}

	operator fun times(other: Vector2): Vector2 {
		return vec2(x * other.x, y * other.y)
	}

	operator fun div(other: Vector2): Vector2 {
		return vec2(x / other.x, y / other.y)
	}

	operator fun times(value: Double): Vector2 {
		if (value.closeTo(1.0, ROUNDING_ERROR)) return this
		return vec2(x * value, y * value)
	}

	operator fun div(value: Double): Vector2 {
		if (value.closeTo(1.0, ROUNDING_ERROR)) return this
		val inv = 1.0 / value
		return vec2(x * inv, y * inv)
	}

	/**
	 * The inverse of this vector.
	 * 1.0 / this
	 */
	fun inv(): Vector2 = 1.0 / this

	/**
	 * Creates a Vector3 with the given z component.
	 */
	fun toVec3(z: Double = 0.0): Vector3 = vec3(x, y, z)

	/**
	 * Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
	 * @param radians the angle in radians
	 */
	fun rot(radians: Double): Vector2 {
		val cos = cos(radians)
		val sin = sin(radians)
		return vec2(x * cos - y * sin, x * sin + y * cos)
	}

	/**
	 * Returns a vector linearly interpolated between this and [target].
	 * @param target The target vector to interpolate this value towards.
	 * @param alpha The percent to interpolate in the direction of [target].
	 * A value of 0.0 will return this vector, a value of 1.0 will return [target].
	 */
	fun lerp(target: Vector2, alpha: Double): Vector2 {
		val invAlpha = 1.0 - alpha
		return this * invAlpha + target * alpha
	}

	fun lerp(x2: Double, y2: Double, alpha: Double): Vector2 =
		lerp(vec2(x2, y2), alpha)

	fun interpolate(target: Vector2, alpha: Double, interpolation: Interpolation): Vector2 =
		lerp(target, interpolation.apply(alpha))

	/**
	 * Returns true if this vector equals [other] where each component is within the given epsilon margin for fuzzy
	 * equality.
	 */
	fun epsilonEquals(other: Vector2?, epsilon: Double): Boolean {
		if (other == null) return false
		return epsilonEquals(other.x, other.y, epsilon)
	}

	/**
	 * Returns true if this vector equals each component within the given epsilon margin for fuzzy equality.
	 */
	fun epsilonEquals(x: Double, y: Double, epsilon: Double): Boolean {
		if (abs(x - this.x) > epsilon) return false
		if (abs(y - this.y) > epsilon) return false
		return true
	}

	/**
	 * Returns true if the squared length is within [margin2] of 1.0
	 */
	fun isUnit(margin2: Double = ROUNDING_ERROR): Boolean =
		abs(len2() - 1.0) < margin2

	/**
	 * Returns true if x and y are exactly zero.
	 */
	val isZero: Boolean
		get() = x == 0.0 && y == 0.0

	/**
	 * Returns true if the squared length is within [margin2] of 0.0
	 */
	fun isZero(margin2: Double = ROUNDING_ERROR): Boolean {
		return len2() < margin2
	}

	fun isOnLine(other: Vector2, epsilon2: Double = ROUNDING_ERROR): Boolean =
		isZero(x * other.y - y * other.x, epsilon2)

	fun isCollinear(other: Vector2, epsilon: Double): Boolean =
		isOnLine(other, epsilon) && dot(other) > 0.0

	fun isCollinear(other: Vector2): Boolean =
		isOnLine(other) && dot(other) > 0.0

	fun isCollinearOpposite(other: Vector2, epsilon: Double): Boolean =
		isOnLine(other, epsilon) && dot(other) < 0.0

	fun isCollinearOpposite(other: Vector2): Boolean =
		isOnLine(other) && dot(other) < 0.0

	fun isPerpendicular(vector: Vector2): Boolean =
		com.acornui.math.isZero(dot(vector))

	fun isPerpendicular(vector: Vector2, epsilon: Double): Boolean =
		isZero(dot(vector), epsilon)

	fun hasSameDirection(vector: Vector2): Boolean =
		dot(vector) > 0

	fun hasOppositeDirection(vector: Vector2): Boolean =
		dot(vector) < 0

	companion object {

		val X: Vector2 = vec2(1.0, 0.0)
		val Y: Vector2 = vec2(0.0, 1.0)
		val ZERO: Vector2 = vec2(0.0, 0.0)

		fun len(x: Double, y: Double): Double {
			return sqrt((x * x + y * y))
		}

		fun len2(x: Double, y: Double): Double {
			return x * x + y * y
		}

		fun dot(x1: Double, y1: Double, x2: Double, y2: Double): Double {
			return x1 * x2 + y1 * y2
		}

		fun dst(x1: Double, y1: Double, x2: Double, y2: Double): Double {
			val xD = x2 - x1
			val yD = y2 - y1
			return sqrt((xD * xD + yD * yD))
		}

		fun manhattanDst(x1: Double, y1: Double, x2: Double, y2: Double): Double {
			val xD = x2 - x1
			val yD = y2 - y1
			return abs(xD) + abs(yD)
		}

		fun dst2(x1: Double, y1: Double, x2: Double, y2: Double): Double {
			val xD = x2 - x1
			val yD = y2 - y1
			return xD * xD + yD * yD
		}

		/**
		 * Returns a vector where each component is random between -1.0 (inclusive) and 1.0 (exclusive)
		 */
		fun random(random: Random = Random): Vector2 = vec2(
			x = random.nextDouble() * 2.0 - 1.0,
			y = random.nextDouble() * 2.0 - 1.0
		)
	}
}

/**
 * Shorthand for Vector2.
 */
typealias vec2 = Vector2

operator fun Double.times(other: Vector2): Vector2 =
	vec2(other.x * this, other.y * this)

operator fun Double.div(other: Vector2): Vector2 =
	vec2(this / other.x, this / other.y)


@Serializer(forClass = Vector2::class)
object Vector2Serializer : KSerializer<Vector2> {

	override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Vector2") {
		listSerialDescriptor<Double>()
	}

	override fun serialize(encoder: Encoder, value: Vector2) {
		encoder.encodeSerializableValue(ListSerializer(Double.serializer()), listOf(value.x, value.y))
	}

	override fun deserialize(decoder: Decoder): Vector2 {
		val values = decoder.decodeSerializableValue(ListSerializer(Double.serializer()))
		return vec2(x = values[0],
				y = values[1]
		)
	}
}