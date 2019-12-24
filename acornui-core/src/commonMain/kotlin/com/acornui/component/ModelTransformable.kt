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

package com.acornui.component

import com.acornui.math.*

/**
 * Coordinate space transformation for local to global coordinates.
 */
interface ModelTransformableRo {

	/**
	 * The transformation of local to global coordinates.
	 */
	val transformGlobal: Matrix4Ro

	/**
	 * The transformation of global to local coordinates.
	 */
	val transformGlobalInv: Matrix4Ro
}

/**
 * Converts a coordinate from local coordinate space to global coordinate space.
 * This will modify the provided coord parameter.
 * @param localCoord The coordinate local to this Transformable. This will be mutated to become a global coordinate.
 * @return Returns the coord
 */
fun ModelTransformableRo.localToGlobal(localCoord: Vector3): Vector3 {
	transformGlobal.prj(localCoord)
	return localCoord
}

/**
 * Converts a coordinate from global coordinate space to local coordinate space.
 * This will modify the provided coord parameter.
 * @param globalCoord The coordinate in global space. This will be mutated to become a local coordinate.
 * @return Returns the coord
 */
fun ModelTransformableRo.globalToLocal(globalCoord: Vector3): Vector3 {
	transformGlobalInv.prj(globalCoord)
	return globalCoord
}

/**
 * Converts a ray from local coordinate space to global coordinate space.
 * This will modify the provided ray parameter.
 * @param ray The ray local to this Transformable. This will be mutated to become a global ray.
 * @return Returns the ray
 */
fun ModelTransformableRo.localToGlobal(ray: Ray): Ray {
	ray.mul(transformGlobal)
	return ray
}

/**
 * Converts a ray from global coordinate space to local coordinate space.
 * This will modify the provided ray parameter.
 *
 * Note: This is a heavy operation as it performs a Matrix4 inversion.
 *
 * @param ray The ray in global space. This will be mutated to become a local coordinate.
 * @return Returns the ray
 */
fun ModelTransformableRo.globalToLocal(ray: Ray): Ray {
	ray.mul(transformGlobalInv)
	return ray
}

/**
 * Converts a bounding rectangle from local to global coordinates.
 * @param minMax These bounds will be mutated into the projected global coordinates, and set to the
 * bounding region of those four points.
 * @return Returns the mutated [minMax] parameter.
 */
fun ModelTransformableRo.localToGlobal(minMax: MinMax): MinMax {
	val tmp1 =  Vector3.obtain().set(minMax.xMin, minMax.yMin, 0f)
	val tmp2 =  Vector3.obtain().set(minMax.xMax, minMax.yMax, 0f)
	val tmp =  Vector3.obtain()
	minMax.clear()
	localToGlobal(tmp.set(tmp1))
	minMax.ext(tmp.x, tmp.y)
	localToGlobal(tmp.set(tmp2.x, tmp1.y, 0f))
	minMax.ext(tmp.x, tmp.y)
	localToGlobal(tmp.set(tmp2))
	minMax.ext(tmp.x, tmp.y)
	localToGlobal(tmp.set(tmp1.x, tmp2.y, 0f))
	minMax.ext(tmp.x, tmp.y)
	Vector3.free(tmp1)
	Vector3.free(tmp2)
	Vector3.free(tmp)
	return minMax
}

/**
 * Converts a bounding rectangle from global to local coordinates.
 */
fun ModelTransformableRo.globalToLocal(minMax: MinMax): MinMax {
	val tmp1 =  Vector3.obtain().set(minMax.xMin, minMax.yMin, 0f)
	val tmp2 =  Vector3.obtain().set(minMax.xMax, minMax.yMax, 0f)
	val tmp =  Vector3.obtain()
	minMax.clear()
	globalToLocal(tmp.set(tmp1))
	minMax.ext(tmp.x, tmp.y)
	globalToLocal(tmp.set(tmp2.x, tmp1.y, 0f))
	minMax.ext(tmp.x, tmp.y)
	globalToLocal(tmp.set(tmp2))
	minMax.ext(tmp.x, tmp.y)
	globalToLocal(tmp.set(tmp1.x, tmp2.y, 0f))
	minMax.ext(tmp.x, tmp.y)
	Vector3.free(tmp1)
	Vector3.free(tmp2)
	Vector3.free(tmp)
	return minMax
}

/**
 * Calculates the intersection coordinates of the provided Ray (in local coordinate space) and this layout
 * element's plane.
 * @return Returns true if the provided Ray intersects with this plane, or false if the Ray is parallel.
 */
fun rayToPlane(ray: RayRo, out: Vector2): Boolean {
	if (ray.direction.z == 0f) return false
	val m = -ray.origin.z * ray.directionInv.z
	out.x = ray.origin.x + m * ray.direction.x
	out.y = ray.origin.y + m * ray.direction.y
	return true
}

/**
 * Converts a coordinate from this Transformable's coordinate space to the target coordinate space.
 */
fun ModelTransformableRo.convertCoord(coord: Vector3, targetCoordSpace: ModelTransformableRo): Vector3 = targetCoordSpace.globalToLocal(localToGlobal(coord))
