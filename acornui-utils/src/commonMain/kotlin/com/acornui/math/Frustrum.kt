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

import com.acornui.collection.ArrayList

interface FrustumRo {

	/**
	 * The six clipping planes, near, far, left, right, top, bottom
	 */
	val planes: List<Plane>

	/**
	 * Eight points making up the near and far clipping "rectangles". order is counter clockwise, starting at bottom left
	 **/
	val planePoints: List<Vector3Ro>

	/**
	 * Returns whether the point is in the frustum.
	 *
	 * @param point The point
	 * @return Whether the point is in the frustum.
	 */
	fun pointInFrustum(point: Vector3Ro): Boolean

	/**
	 * Returns whether the point is in the frustum.
	 *
	 * @param x The X coordinate of the point
	 * @param y The Y coordinate of the point
	 * @param z The Z coordinate of the point
	 * @return Whether the point is in the frustum.
	 */
	fun pointInFrustum(x: Float, y: Float, z: Float): Boolean

	/**
	 * Returns whether the given sphere is in the frustum.
	 *
	 * @param center The center of the sphere
	 * @param radius The radius of the sphere
	 * @return Whether the sphere is in the frustum
	 */
	fun sphereInFrustum(center: Vector3Ro, radius: Float): Boolean

	/**
	 * Returns whether the given sphere is in the frustum.
	 *
	 * @param x The X coordinate of the center of the sphere
	 * @param y The Y coordinate of the center of the sphere
	 * @param z The Z coordinate of the center of the sphere
	 * @param radius The radius of the sphere
	 * @return Whether the sphere is in the frustum
	 */
	fun sphereInFrustum(x: Float, y: Float, z: Float, radius: Float): Boolean

	/**
	 * Returns whether the given sphere is in the frustum not checking whether it is behind the near and far clipping plane.
	 *
	 * @param center The center of the sphere
	 * @param radius The radius of the sphere
	 * @return Whether the sphere is in the frustum
	 */
	fun sphereInFrustumWithoutNearFar(center: Vector3, radius: Float): Boolean

	/**
	 * Returns whether the given sphere is in the frustum not checking whether it is behind the near and far clipping plane.
	 *
	 * @param x The X coordinate of the center of the sphere
	 * @param y The Y coordinate of the center of the sphere
	 * @param z The Z coordinate of the center of the sphere
	 * @param radius The radius of the sphere
	 * @return Whether the sphere is in the frustum
	 */
	fun sphereInFrustumWithoutNearFar(x: Float, y: Float, z: Float, radius: Float): Boolean

	/**
	 * Returns whether the given {@link BoundingBox} is in the frustum.
	 *
	 * @param bounds The bounding box
	 * @return Whether the bounding box is in the frustum
	 */
	fun boundsInFrustum(bounds: BoxRo): Boolean

	/**
	 * Returns whether the given bounding box is in the frustum.
	 * @return Whether the bounding box is in the frustum
	 */
	fun boundsInFrustum(center: Vector3Ro, dimensions: Vector3Ro): Boolean

	/**
	 * Returns whether the given bounding box is in the frustum.
	 * @return Whether the bounding box is in the frustum
	 */
	fun boundsInFrustum(x: Float, y: Float, z: Float, halfWidth: Float, halfHeight: Float, halfDepth: Float): Boolean
}

/**
 * A truncated rectangular pyramid. Used to define the viewable region and its projection onto the screen.
 * @see Camera#frustum
 */
data class Frustum(

		override val planes: MutableList<Plane> = ArrayList(6, { Plane(vec3(), 0f) })
) : FrustumRo {


	override val planePoints = ArrayList(8, { vec3() })

	/**
	 * Updates the clipping plane's based on the given inverse combined projection and view matrix, e.g. from an
	 * {@link OrthographicCamera} or {@link PerspectiveCamera}.
	 * @param inverseProjectionView the inverse of the combined projection and view matrices.
	 */
	fun update(inverseProjectionView: Matrix4Ro) {
		for (i in 0..8 - 1) {
			inverseProjectionView.prj(planePoints[i].set(clipSpacePlanePoints[i]))
		}
		planes[0].set(planePoints[1], planePoints[0], planePoints[2])
		planes[1].set(planePoints[4], planePoints[5], planePoints[7])
		planes[2].set(planePoints[0], planePoints[4], planePoints[3])
		planes[3].set(planePoints[5], planePoints[1], planePoints[6])
		planes[4].set(planePoints[2], planePoints[3], planePoints[6])
		planes[5].set(planePoints[4], planePoints[0], planePoints[1])
	}

	override fun pointInFrustum(point: Vector3Ro): Boolean {
		for (i in planes.indices) {
			val result = planes[i].testPoint(point)
			if (result == PlaneSide.BACK) return false
		}
		return true
	}

	override fun pointInFrustum(x: Float, y: Float, z: Float): Boolean {
		for (i in planes.indices) {
			val result = planes[i].testPoint(x, y, z)
			if (result == PlaneSide.BACK) return false
		}
		return true
	}

	override fun sphereInFrustum(center: Vector3Ro, radius: Float): Boolean {
		for (i in 0..6 - 1)
			if ((planes[i].normal.x * center.x + planes[i].normal.y * center.y + planes[i].normal.z * center.z) < (-radius - planes[i].d))
				return false
		return true
	}

	override fun sphereInFrustum(x: Float, y: Float, z: Float, radius: Float): Boolean {
		for (i in 0..6 - 1)
			if ((planes[i].normal.x * x + planes[i].normal.y * y + planes[i].normal.z * z) < (-radius - planes[i].d)) return false
		return true
	}

	override fun sphereInFrustumWithoutNearFar(center: Vector3, radius: Float): Boolean {
		for (i in 2..6 - 1)
			if ((planes[i].normal.x * center.x + planes[i].normal.y * center.y + planes[i].normal.z * center.z) < (-radius - planes[i].d))
				return false
		return true
	}

	override fun sphereInFrustumWithoutNearFar(x: Float, y: Float, z: Float, radius: Float): Boolean {
		for (i in 2..6 - 1)
			if ((planes[i].normal.x * x + planes[i].normal.y * y + planes[i].normal.z * z) < (-radius - planes[i].d)) return false
		return true
	}

	private val corners by lazy { ArrayList(8, { vec3() }) }

	override fun boundsInFrustum(bounds: BoxRo): Boolean {
		val corners = bounds.getCorners(corners)
		val len = corners.size

		for (i in 0..planes.lastIndex) {
			var out = 0

			for (j in 0..len - 1)
				if (planes[i].testPoint(corners[j]) == PlaneSide.BACK) out++

			if (out == 8) return false
		}

		return true
	}

	override fun boundsInFrustum(center: Vector3Ro, dimensions: Vector3Ro): Boolean {
		return boundsInFrustum(center.x, center.y, center.z, dimensions.x / 2, dimensions.y / 2, dimensions.z / 2)
	}

	override fun boundsInFrustum(x: Float, y: Float, z: Float, halfWidth: Float, halfHeight: Float, halfDepth: Float): Boolean {
		for (i in 0..planes.lastIndex) {
			val plane = planes[i];
			if (plane.testPoint(x + halfWidth, y + halfHeight, z + halfDepth) != PlaneSide.BACK) continue
			if (plane.testPoint(x + halfWidth, y + halfHeight, z - halfDepth) != PlaneSide.BACK) continue
			if (plane.testPoint(x + halfWidth, y - halfHeight, z + halfDepth) != PlaneSide.BACK) continue
			if (plane.testPoint(x + halfWidth, y - halfHeight, z - halfDepth) != PlaneSide.BACK) continue
			if (plane.testPoint(x - halfWidth, y + halfHeight, z + halfDepth) != PlaneSide.BACK) continue
			if (plane.testPoint(x - halfWidth, y + halfHeight, z - halfDepth) != PlaneSide.BACK) continue
			if (plane.testPoint(x - halfWidth, y - halfHeight, z + halfDepth) != PlaneSide.BACK) continue
			if (plane.testPoint(x - halfWidth, y - halfHeight, z - halfDepth) != PlaneSide.BACK) continue
			return false
		}

		return true
	}

	companion object {

		val clipSpacePlanePoints: List<Vector3Ro> = listOf(vec3(-1f, -1f, -1f), vec3(1f, -1f, -1f), vec3(1f, 1f, -1f), vec3(-1f, 1f, -1f), // near clip
				vec3(-1f, -1f, 1f), vec3(1f, -1f, 1f), vec3(1f, 1f, 1f), vec3(-1f, 1f, 1f)) // far clip

	}
}
