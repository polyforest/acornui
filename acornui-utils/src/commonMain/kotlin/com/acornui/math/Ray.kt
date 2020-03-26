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

/*
 * Derived from LibGDX
 * https://github.com/libgdx
 * Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
 */

package com.acornui.math

import com.acornui.closeTo
import com.acornui.math.MathUtils.FLOAT_ROUNDING_ERROR
import com.acornui.notCloseTo
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import kotlinx.serialization.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlin.math.sqrt

@Serializable(with = RaySerializer::class)
interface RayRo {

	/**
	 * The starting position of this ray.
	 */
	val origin: Vector3Ro

	/**
	 * The direction of this ray. This will always be normalized.
	 */
	val direction: Vector3Ro

	/**
	 * 1f / direction
	 */
	val directionInv: Vector3Ro

	/**
	 * Returns the endpoint given the distance. This is calculated as origin + distance * direction.
	 * @param distance The distance from the end point to the origin.
	 * @param out The vector to set to the result
	 * @return The out param
	 */
	fun getEndPoint(distance: Float, out: Vector3): Vector3 {
		return out.set(direction).scl(distance).add(origin)
	}

	/**
	 * Returns the endpoint when projecting towards the xy plane at [z].
	 * @throws Exception if direction.z is zero.
	 */
	fun getPointAtZ(z: Float, out: Vector2): Vector2 {
		if (direction.z == 0f) throw Exception("direction.z is zero")
		val d = (z - origin.z) / direction.z
		return out.set(direction.x, direction.y).scl(d).add(origin.x, origin.y)
	}

	/**
	 * http://math.stackexchange.com/questions/270767/find-intersection-of-two-3d-lines
	 * @author nbilyk
	 */
	fun intersectsRay(ray: RayRo, out: Vector3? = null): Boolean {
		if (this.origin == ray.origin) {
			out?.set(origin)
			return true
		}
		val cross = v3_4
		val perp1 = v3_5

		val d1 = direction
		val d2 = ray.direction

		cross.set(d1).crs(d2)

		val u = cross.dot(origin)
		val u2 = cross.dot(ray.origin)
		if (u.notCloseTo(u2)) {
			return false
		}

		perp1.set(d1).crs(cross)

		val v = perp1.dot(origin)
		if (v.closeTo(0f)) return false // Collinear

		if (out != null) {
			val t = (v - perp1.x * ray.origin.x - perp1.y * ray.origin.y - perp1.z * ray.origin.z) / (perp1.y * ray.direction.y + perp1.x * ray.direction.x + perp1.z * ray.direction.z)
			ray.getEndPoint(t, out)
		}
		return true
	}

	/**
	 * Intersects a [Ray] and a [Plane]. The intersection point is stored in [out] in the case an intersection is
	 * present.
	 * @param plane The plane
	 * @param out The vector the intersection point is written to (optional)
	 * @return True if an intersection is present.
	 */
	fun intersectsPlane(plane: PlaneRo, out: Vector3?): Boolean {
		val denom = direction.dot(plane.normal)
		return if (denom != 0f) {
			val t = -(origin.dot(plane.normal) + plane.d) / denom
			if (t < 0f) return false
			out?.set(origin)?.add(v3_0.set(direction).scl(t))
			true
		} else if (plane.testPoint(origin) === PlaneSide.ON_PLANE) {
			out?.set(origin)
			true
		} else
			false
	}

	/**
	 * Intersect a [Ray] and a triangle, returning the intersection point in intersection.
	 * @param v1 The first vertex of the triangle
	 * @param v2 The second vertex of the triangle
	 * @param v3 The third vertex of the triangle
	 * @param out The intersection point (optional)
	 * @return True in case an intersection is present.
	 */
	fun intersectsTriangle(v1: Vector3Ro, v2: Vector3Ro, v3: Vector3Ro, out: Vector3? = null): Boolean {
		plane.set(v1, v2, v3)
		if (!intersectsPlane(plane, v3_3)) return false

		v3_0.set(v3).sub(v1)
		v3_1.set(v2).sub(v1)
		v3_2.set(v3_3).sub(v1)

		val dot00 = v3_0.dot(v3_0)
		val dot01 = v3_0.dot(v3_1)
		val dot02 = v3_0.dot(v3_2)
		val dot11 = v3_1.dot(v3_1)
		val dot12 = v3_1.dot(v3_2)

		val denom = dot00 * dot11 - dot01 * dot01
		if (denom == 0f) return false

		val u = (dot11 * dot02 - dot01 * dot12) / denom
		val v = (dot00 * dot12 - dot01 * dot02) / denom

		return if (u >= -FLOAT_ROUNDING_ERROR && v >= -FLOAT_ROUNDING_ERROR && u + v <= 1f + FLOAT_ROUNDING_ERROR) {
			out?.set(v3_3)
			true
		} else {
			false
		}
	}

	/**
	 * Intersects a [Ray] and a sphere, returning the intersection point in intersection.
	 * The direction component must be normalized before calling this method
	 *
	 * @param center The center of the sphere
	 * @param radius The radius of the sphere
	 * @param intersection The intersection point (optional, can be null)
	 * @return Whether an intersection is present.
	 */
	fun intersectSphere(center: Vector3Ro, radius: Float, intersection: Vector3? = null): Boolean {
		val len = direction.dot(center.x - origin.x, center.y - origin.y, center.z - origin.z)
		if (len < 0f) return false // Behind the ray
		val dst2 = center.dst2(origin.x + direction.x * len, origin.y + direction.y * len,
				origin.z + direction.z * len)
		val r2 = radius * radius
		if (dst2 > r2) return false
		intersection?.set(direction)?.scl(len - sqrt(r2 - dst2))?.add(origin)
		return true
	}

	fun copy(origin: Vector3Ro = this.origin, direction: Vector3Ro = this.direction): Ray {
		return Ray(origin, direction)
	}

	companion object {
		// Temporary values.
		private val plane = Plane(vec3(), 0f)
		private val v3_0 = vec3()
		private val v3_1 = vec3()
		private val v3_2 = vec3()
		private val v3_3 = vec3()
		private val v3_4 = vec3()
		private val v3_5 = vec3()
	}

}

/**
 * Encapsulates a ray having a starting position and a unit length direction.
 */
@Serializable(with = RaySerializer::class)
class Ray() : Clearable, RayRo {

	constructor(origin: Vector3Ro, direction: Vector3Ro) : this() {
		set(origin, direction)
	}

	private val _origin = vec3()
	override val origin: Vector3Ro
		get() = _origin

	private val _direction = vec3(1f, 0f, 0f)
	override val direction: Vector3Ro
		get() {
			validate()
			return _direction
		}

	private val _directionInv = vec3(1f, 0f, 0f)
	override val directionInv: Vector3Ro
		get() {
			validate()
			return _directionInv
		}

	@Transient private var isValid = true

	/**
	 * Normalizes the direction and calculates [directionInv]
	 */
	private fun validate(): Ray {
		if (isValid) return this
		isValid = true
		_direction.nor()
		_directionInv.set(1f / _direction.x, 1f / _direction.y, 1f / _direction.z)
		return this
	}

	/**
	 * Multiplies the ray by the given matrix. Use this to transform a ray into another coordinate system.
	 *
	 * @param matrix The matrix
	 * @return This ray for chaining.
	 */
	fun mul(matrix: Matrix4Ro): Ray {
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
	fun set(origin: Vector3Ro, direction: Vector3Ro): Ray {
		_origin.set(origin)
		_direction.set(direction)
		isValid = false
		return this
	}

	/**
	 * Sets the origin.
	 */
	fun setOrigin(value: Vector3Ro): Ray = setOrigin(value.x, value.y, value.z)

	/**
	 * Sets the origin.
	 */
	fun setOrigin(x: Float, y: Float, z: Float): Ray {
		_origin.set(x, y, z)
		return this
	}

	/**
	 * Sets the direction.
	 * The provided value does not need to be normalized; normalization will happen on the next request.
	 */
	fun setDirection(value: Vector3Ro): Ray = setDirection(value.x, value.y, value.z)

	/**
	 * Sets the direction.
	 * The provided value does not need to be normalized; normalization will happen on the next request.
	 */
	fun setDirection(x: Float, y: Float, z: Float): Ray {
		_direction.set(x, y, z)
		isValid = false
		return this
	}

	/**
	 * Sets this ray from the given starting position and direction.
	 *
	 * @param x The x-component of the starting position
	 * @param y The y-component of the starting position
	 * @param z The z-component of the starting position
	 * @param dX The x-component of the direction
	 * @param dY The y-component of the direction
	 * @param dZ The z-component of the direction
	 * @return this ray for chaining
	 */
	fun set(x: Float, y: Float, z: Float, dX: Float, dY: Float, dZ: Float): Ray {
		_origin.set(x, y, z)
		_direction.set(dX, dY, dZ)
		isValid = false
		return this
	}

	/**
	 * Sets the starting position and direction from the given ray
	 *
	 * @param ray The ray
	 * @return This ray for chaining
	 */
	fun set(ray: RayRo): Ray {
		_origin.set(ray.origin)
		_direction.set(ray.direction)
		_directionInv.set(ray.directionInv)
		isValid = true
		return this
	}

	/**
	 * Clears this ray, setting the origin to [Vector3.ZERO] and its direction to [Vector3.X]
	 */
	override fun clear() {
		_origin.clear()
		_direction.set(Vector3.X)
		_directionInv.set(Vector3.X)
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

		private val tmpVec = vec3()

		private val pool = ClearableObjectPool { Ray() }

		fun obtain(): Ray = pool.obtain()
		fun free(obj: Ray) = pool.free(obj)
	}
}

@Serializer(forClass = Ray::class)
object RaySerializer : KSerializer<Ray> {

	override val descriptor: SerialDescriptor = SerialDescriptor("Ray") {
		listDescriptor<Float>()
	}

	override fun serialize(encoder: Encoder, value: Ray) {
		val origin = value.origin
		val direction = value.direction
		encoder.encodeSerializableValue(Float.serializer().list, listOf(origin.x, origin.y, origin.z, direction.x, direction.y, direction.z))
	}

	override fun deserialize(decoder: Decoder): Ray {
		val values = decoder.decodeSerializableValue(Float.serializer().list)
		return Ray().set(values[0], values[1], values[2], values[3], values[4], values[5])
	}
}