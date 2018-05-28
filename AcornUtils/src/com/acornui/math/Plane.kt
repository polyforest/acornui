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

/**
 * The read-only interface to [Plane]
 */
interface PlaneRo {

	val normal: Vector3Ro
	val d: Float

	/**
	 * Calculates the shortest signed distance between the plane and the given point.
	 *
	 * @param vec The point
	 * @return the shortest signed distance between the plane and the point
	 */
	fun distance(vec: Vector3Ro): Float {
		return normal.dot(vec) + d
	}

	/**
	 * Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the
	 * side the plane normal points to.
	 *
	 * @param point The point
	 * @return The side the point lies relative to the plane
	 */
	fun testPoint(point: Vector3Ro): PlaneSide {
		val dist = normal.dot(point) + d

		return if (dist == 0f)
			PlaneSide.ON_PLANE
		else if (dist < 0)
			PlaneSide.BACK
		else
			PlaneSide.FRONT
	}

	/**
	 * Returns on which side the given point lies relative to the plane and its normal. PlaneSide.Front refers to the side the
	 * plane normal points to.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @return The side the point lies relative to the plane
	 */
	fun testPoint(x: Float, y: Float, z: Float): PlaneSide {
		val dist = normal.dot(x, y, z) + d

		return if (dist == 0f)
			PlaneSide.ON_PLANE
		else if (dist < 0)
			PlaneSide.BACK
		else
			PlaneSide.FRONT
	}

	/**
	 * Returns whether the plane is facing the direction vector. Think of the direction vector as the direction a camera looks in.
	 * This method will return true if the front side of the plane determined by its normal faces the camera.
	 *
	 * @param direction the direction
	 * @return whether the plane is front facing
	 */
	fun isFrontFacing(direction: Vector3Ro): Boolean {
		val dot = normal.dot(direction)
		return dot <= 0
	}

	/**
	 * Calculates the intersection of this Plane with the provided Ray.
	 *
	 * @param out If the ray intersects this Plane, the out vector will be set with the intersecting point.
	 * @return Returns true if the ray intersects this plane, false if the Ray is parallel to the plane, or if
	 * the intersection is behind the Ray.
	 *
	 * @author nbilyk
	 */
	fun intersects(r: Ray, out: Vector3? = null): Boolean {
		val m = r.direction.dot(normal)
		if (m == 0f) return false // Ray is parallel to the plane
		val t = -(r.origin.dot(normal) + d) / m
		return if (t >= 0) {
			if (out != null) out.set(r.direction).scl(t).add(r.origin)
			true
		} else {
			// If t < 0, the plane is behind the ray.
			false
		}
	}

	/**
	 * Projects the given point onto this Plane.
	 */
	fun prj(out: Vector3): Vector3 {
		val t = normal.dot(out) + d
		out.set(t * -normal.x + out.x, t * -normal.y + out.y, t * -normal.z + out.z)
		return out
	}

	fun copy(normal: Vector3Ro = this.normal, d: Float = this.d): Plane {
		return Plane(normal.copy(), d)
	}
}

/**
 * A plane defined via a unit length normal and the distance from the origin, as you learned in your math class.
 *
 * @author badlogicgames@gmail.com
 */
class Plane(
		override val normal: Vector3 = Vector3(),
		override var d: Float = 0f
) : Clearable, PlaneRo {


	/**
	 * Sets the plane normal and distance to the origin based on the three given points which are considered to be on the plane.
	 * The normal is calculated via a cross product between (point1-point2)x(point2-point3)
	 *
	 * @param point1
	 * @param point2
	 * @param point3
	 */
	fun set(point1: Vector3Ro, point2: Vector3Ro, point3: Vector3Ro) {
		normal.set(point1).sub(point2).crs(point2.x - point3.x, point2.y - point3.y, point2.z - point3.z).nor()
		d = -point1.dot(normal)
	}

	/**
	 * Sets the plane normal and distance
	 *
	 * @param nx normal x-component
	 * @param ny normal y-component
	 * @param nz normal z-component
	 * @param d distance to origin
	 */
	fun set(nx: Float, ny: Float, nz: Float, d: Float) {
		normal.set(nx, ny, nz)
		this.d = d
	}

	/**
	 * Sets the plane to the given point and normal.
	 *
	 * @param point the point on the plane
	 * @param normal the normal of the plane
	 */
	fun set(point: Vector3Ro, normal: Vector3) {
		this.normal.set(normal)
		d = -point.dot(normal)
	}

	fun set(pointX: Float, pointY: Float, pointZ: Float, norX: Float, norY: Float, norZ: Float) {
		this.normal.set(norX, norY, norZ)
		d = -(pointX * norX + pointY * norY + pointZ * norZ)
	}

	/**
	 * Sets this plane from the given plane
	 *
	 * @param plane the plane
	 */
	fun set(plane: PlaneRo) {
		this.normal.set(plane.normal)
		this.d = plane.d
	}

	fun free() {
		pool.free(this)
	}

	override fun clear() {
		normal.clear()
		d = 0f
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		other as PlaneRo
		if (normal != other.normal) return false
		if (d != other.d) return false
		return true
	}

	override fun hashCode(): Int {
		var result = normal.hashCode()
		result = 31 * result + d.hashCode()
		return result
	}


	companion object {
		private val pool = ClearableObjectPool { Plane() }

		fun obtain(): Plane {
			return pool.obtain()
		}
	}

}


/**
 * Enum specifying on which side a point lies respective to the plane and it's normal. {@link PlaneSide#Front} is the side to
 * which the normal points.
 *
 * @author mzechner
 */
enum class PlaneSide {
	ON_PLANE,
	BACK,
	FRONT
}