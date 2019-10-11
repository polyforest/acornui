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
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.FloatSerializer
import kotlinx.serialization.internal.StringDescriptor


@Serializable(with = Ray2Serializer::class)
interface Ray2Ro {

	/**
	 * The starting position of this ray.
	 */
	val origin: Vector2Ro

	/**
	 * The direction of this ray. This will always be normalized.
	 */
	val direction: Vector2Ro

	/**
	 * 1f / direction
	 */
	val directionInv: Vector2Ro

	/**
	 * Returns the endpoint given the distance. This is calculated as origin + distance * direction.
	 * @param distance The distance from the end point to the origin.
	 * @param out The vector to set to the result
	 * @return The out param
	 */
	fun getEndPoint(distance: Float, out: Vector2): Vector2 {
		return out.set(direction).scl(distance).add(origin)
	}

	fun intersectsRay(ray: Ray2Ro, out: Vector2): Boolean {
		return Ray2.intersects(origin, direction, ray.origin, ray.direction, out)
	}

	fun copy(origin: Vector2Ro = this.origin, direction: Vector2Ro = this.direction): Ray2 {
		return Ray2(origin.copy(), direction.copy())
	}
}

/**
 * A 2d ray.
 */
@Serializable(with = Ray2Serializer::class)
class Ray2() : Clearable, Ray2Ro {

	constructor(origin: Vector2Ro, direction: Vector2Ro) : this() {
		set(origin, direction)
	}

	private val _origin = Vector2()
	override val origin: Vector2Ro
		get() = _origin

	private val _direction = Vector2(1f, 0f)
	override val direction: Vector2Ro
		get() {
			validate()
			return _direction
		}

	private val _directionInv = Vector2(1f, 0f)
	override val directionInv: Vector2Ro
		get() {
			validate()
			return _directionInv
		}

	@Transient
	private var isValid = true

	/**
	 * Normalizes the direction and calculates [directionInv]
	 */
	private fun validate(): Ray2 {
		if (isValid) return this
		isValid = true
		_direction.nor()
		_directionInv.set(1f / _direction.x, 1f / _direction.y)
		return this
	}

	/**
	 * Multiplies the ray by the given matrix. Use this to transform a ray into another coordinate system.
	 *
	 * @param matrix The matrix
	 * @return This ray for chaining.
	 */
	fun mul(matrix: Matrix3Ro): Ray2 {
		tmpVec.set(origin).add(direction)
		tmpVec.mul(matrix)
		_origin.mul(matrix)
		_direction.set(tmpVec.sub(origin))
		isValid = false
		return this
	}

	/**
	 * Sets the starting position and the direction of this ray.
	 *
	 * @param origin The starting position
	 * @param direction The direction
	 * @return this ray for chaining
	 */
	fun set(origin: Vector2Ro, direction: Vector2Ro): Ray2 {
		_origin.set(origin)
		_direction.set(direction)
		isValid = false
		return this
	}

	/**
	 * Sets the origin.
	 */
	fun setOrigin(value: Vector2Ro): Ray2 = setOrigin(value.x, value.y)

	/**
	 * Sets the origin.
	 */
	fun setOrigin(x: Float, y: Float): Ray2 {
		_origin.set(x, y)
		return this
	}

	/**
	 * Sets the direction.
	 * The provided value does not need to be normalized; normalization will happen on the next request.
	 */
	fun setDirection(value: Vector2Ro): Ray2 = setDirection(value.x, value.y)

	/**
	 * Sets the direction.
	 * The provided value does not need to be normalized; normalization will happen on the next request.
	 */
	fun setDirection(x: Float, y: Float): Ray2 {
		_direction.set(x, y)
		isValid = false
		return this
	}

	/**
	 * Sets this ray from the given starting position and direction.
	 *
	 * @param x The x-component of the starting position
	 * @param y The y-component of the starting position
	 * @param dX The x-component of the direction
	 * @param dY The y-component of the direction
	 * @return this ray for chaining
	 */
	fun set(x: Float, y: Float, dX: Float, dY: Float): Ray2 {
		_origin.set(x, y)
		_direction.set(dX, dY)
		isValid = false
		return this
	}

	/**
	 * Sets the starting position and direction from the given ray
	 *
	 * @param ray The ray
	 * @return This ray for chaining
	 */
	fun set(ray: Ray2Ro): Ray2 {
		_origin.set(ray.origin)
		_direction.set(ray.direction)
		_directionInv.set(ray.directionInv)
		isValid = true
		return this
	}

	/**
	 * Clears this ray, setting the origin to [Vector2.ZERO] and its direction to [Vector2.X]
	 */
	override fun clear() {
		_origin.clear()
		_direction.set(Vector2.X)
		_directionInv.set(Vector2.X)
		isValid = true
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as RayRo
		if (origin != other.origin) return false
		if (direction != other.direction) return false
		return true
	}

	override fun hashCode(): Int {
		var result = origin.hashCode()
		result = 31 * result + direction.hashCode()
		return result
	}

	companion object {

		private val tmpVec = Vector2()

		private val pool = ClearableObjectPool { Ray2() }

		fun obtain(): Ray2 = pool.obtain()
		fun free(obj: Ray2) = pool.free(obj)

		/**
		 * Intersect two 2D Rays and return the scalar parameter of the first ray at the intersection point.
		 *
		 * For more information, check: http://stackoverflow.com/a/565282/1091440
		 *
		 * @param start1 Where the first ray start
		 * @param direction1 The direction the first ray is pointing
		 * @param start2 Where the second ray start
		 * @param direction2 The direction the second ray is pointing
		 * @return scalar parameter on the first ray describing the point where the intersection happens. May be negative.
		 * In the case the rays are collinear, PositiveInfinity will be returned.
		 */
		fun intersects(start1: Vector2Ro, direction1: Vector2Ro, start2: Vector2Ro, direction2: Vector2Ro): Float {
			val diffX = start2.x - start1.x
			val diffY = start2.y - start1.y
			val d1xd2 = direction1.x * direction2.y - direction1.y * direction2.x
			if (d1xd2 == 0f) {
				return Float.POSITIVE_INFINITY // collinear
			}
			val d2sx = direction2.x / d1xd2
			val d2sy = direction2.y / d1xd2
			return diffX * d2sy - diffY * d2sx
		}

		/**
		 * Intersect two 2D Rays, returning true if the rays intersect (are not collinear), and sets the [out]
		 * intersection point.
		 */
		fun intersects(start1: Vector2Ro, direction1: Vector2Ro, start2: Vector2Ro, direction2: Vector2Ro, out: Vector2): Boolean {
			val f = intersects(start1, direction1, start2, direction2)
			return if (f < Float.POSITIVE_INFINITY) {
				out.set(direction1).scl(f).add(start1)
				true
			} else {
				out.set(start1)
				false
			}
		}

	}
}


@Serializer(forClass = Ray2::class)
object Ray2Serializer : KSerializer<Ray2> {

	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("Ray2")

	override fun serialize(encoder: Encoder, obj: Ray2) {
		val origin = obj.origin
		val direction = obj.direction
		encoder.encodeSerializableValue(ArrayListSerializer(FloatSerializer), listOf(origin.x, origin.y, direction.x, direction.y))
	}

	override fun deserialize(decoder: Decoder): Ray2 {
		val values = decoder.decodeSerializableValue(ArrayListSerializer(FloatSerializer))
		return Ray2().set(values[0], values[1], values[2], values[3])
	}
}