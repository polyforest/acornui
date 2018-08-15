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

@file:Suppress("PropertyName", "MemberVisibilityCanBePrivate", "unused")

package com.acornui.core.graphics

import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.math.*
import com.acornui.observe.ModTagImpl
import com.acornui.observe.ModTagRo
import com.acornui.reflect.observable
import kotlin.properties.ReadWriteProperty

interface CameraRo {

	/**
	 * Incremented whenever something on this camera has changed.
	 */
	val modTag: ModTagRo

	/**
	 * The combined projection and view matrix.
	 */
	val combined: Matrix4Ro

	/**
	 * Creates a picking {@link Ray} from the coordinates given in global coordinates. The global coordinates origin
	 * is assumed to be in the top left corner, its y-axis pointing down, the x-axis  pointing to the right.
	 *
	 * @param viewportX the x coordinate of the bottom left corner of the viewport in glViewport coordinates.
	 * @param viewportY the y coordinate of the bottom left corner of the viewport in glViewport coordinates.
	 * @param viewportWidth the width of the viewport in pixels
	 * @param viewportHeight the height of the viewport in pixels
	 * @return The [out] parameter. The Ray will be in global coordinate space.
	 */
	fun getPickRay(canvasX: Float, canvasY: Float, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float, out: Ray): Ray {
		canvasToGlobal(out.origin.set(canvasX, canvasY, -1f), viewportX, viewportY, viewportWidth, viewportHeight)
		canvasToGlobal(out.direction.set(canvasX, canvasY, 0f), viewportX, viewportY, viewportWidth, viewportHeight)
		out.direction.sub(out.origin)
		out.update()
		return out
	}

	/**
	 * Projects the {@link Vector3} given in global space to screen coordinates. The screen coordinate system has its
	 * origin in the top left, with the y-axis pointing downwards and the x-axis pointing to the right.
	 *
	 * @param viewportX the coordinate of the top left corner of the viewport in glViewport coordinates.
	 * @param viewportY the coordinate of the top left corner of the viewport in glViewport coordinates.
	 * @param viewportWidth the width of the viewport in pixels
	 * @param viewportHeight the height of the viewport in pixels
	 */
	fun project(globalCoords: Vector3, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Vector3 {
		combined.prj(globalCoords)
		globalCoords.x = viewportWidth * (globalCoords.x + 1f) * 0.5f + viewportX
		globalCoords.y = viewportHeight * (-globalCoords.y + 1f) * 0.5f + viewportY
		globalCoords.z = (globalCoords.z + 1f) * 0.5f
		return globalCoords
	}

	/**
	 * The position of the camera
	 */
	val position: Vector3Ro

	/**
	 * The unit length direction vector of the camera
	 */
	val direction: Vector3Ro

	/**
	 * The unit length up vector of the camera
	 */
	val up: Vector3Ro

	/**
	 * The projection matrix.
	 */
	val projection: Matrix4Ro

	/**
	 * The view matrix.
	 */
	val view: Matrix4Ro

	/**
	 * The near clipping plane distance, has to be positive
	 */
	val near: Float

	/**
	 * The far clipping plane distance, has to be positive
	 */
	val far: Float

	/**
	 * The viewport width
	 */
	var viewportWidth: Float

	/**
	 * The viewport height
	 */
	var viewportHeight: Float

	val aspect: Float
		get() = viewportWidth / viewportHeight

	/**
	 * The frustum
	 */
	val frustum: FrustumRo

	/**
	 * The inverse combined projection and view matrix
	 */
	val invCombined: Matrix4Ro

	/**
	 * Translates a point given in screen coordinates to global space. It's the same as GLU gluUnProject, but
	 * does not rely on OpenGL. The x- and y-coordinate of vec are assumed to be in screen coordinates (origin is the
	 * top left corner, y pointing down, x pointing to the right) as reported by the canvas coordinates in
	 * input events. A z-coordinate of 0 will return a point on the near plane, a z-coordinate
	 * of 1 will return a point on the far plane. This method allows you to specify the viewport position and
	 * dimensions in the coordinate system expected by {@link GL20#glViewport(int, int, int, int)}, with the origin in
	 * the top left corner of the screen.
	 * @param canvasCoords the point in canvas coordinates (origin top left). This will be mutated.
	 * @param viewportX the coordinate of the bottom left corner of the viewport in glViewport coordinates.
	 * @param viewportY the coordinate of the bottom left corner of the viewport in glViewport coordinates.
	 * @param viewportWidth the width of the viewport in pixels
	 * @param viewportHeight the height of the viewport in pixels
	 */
	fun canvasToGlobal(canvasCoords: Vector3, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Vector3

}

interface Camera : CameraRo {

	fun setPosition(value: Vector3Ro) = setPosition(value.x, value.y, value.z)
	fun setPosition(x: Float = position.x, y: Float = position.y, z: Float = position.z)

	/**
	 * Sets this camera's direction.
	 * @param keepUpOrthonormal If true, setting the direction also sets up, ensuring that the up vector and direction
	 * vector remain orthonormal.
	 */
	fun setDirection(x: Float = direction.x, y: Float = direction.y, z: Float = direction.z, keepUpOrthonormal: Boolean = true)
	fun setDirection(value: Vector3Ro, keepUpOrthonormal: Boolean = true) = setDirection(value.x, value.y, value.z, keepUpOrthonormal)

	fun setUp(x: Float = up.x, y: Float = up.y, z: Float = up.z)
	fun setUp(value: Vector3Ro) = setUp(value.x, value.y, value.z)

	/**
	 * The near clipping plane distance, has to be positive
	 */
	override var near: Float

	/**
	 * The far clipping plane distance, has to be positive
	 */
	override var far: Float

	fun setViewport(width: Float, height: Float) {
		viewportWidth = maxOf(1f, width)
		viewportHeight = maxOf(1f, height)
	}

	fun pointToLookAt(target: Vector3Ro) = pointToLookAt(target.x, target.y, target.z)

	/**
	 * Recalculates the direction of the camera to look at the point (x, y, z). This function assumes the up vector is
	 * normalized.
	 *
	 * @param x the x-coordinate of the point to look at
	 * @param y the x-coordinate of the point to look at
	 * @param z the x-coordinate of the point to look at
	 */
	fun pointToLookAt(x: Float, y: Float, z: Float)

	fun moveToLookAtPoint(x: Float, y: Float, z: Float, distance: Float = 1.0f)

	fun moveToLookAtRect(rect: RectangleRo, scaling: Scaling = Scaling.FIT) = moveToLookAtRect(rect.x, rect.y, rect.width, rect.height, scaling)

	/**
	 * Moves and/or zooms the camera to fit the given 2d bounding box into the viewport, maintaining the current
	 * direction.
	 * @param scaling The scaling type to fit to the given width/height. This may not be a stretch type.
	 */
	fun moveToLookAtRect(x: Float, y: Float, width: Float, height: Float, scaling: Scaling = Scaling.FIT)

	companion object : DKey<Camera>
}

abstract class CameraBase : Camera {

	protected val _combined: Matrix4 = Matrix4()
	override val combined: Matrix4Ro
		get() {
			validateViewProjection()
			return _combined
		}

	protected val _modTag = ModTagImpl()

	/**
	 * Incremented whenever something on this camera has changed.
	 */
	override val modTag: ModTagRo
		get() = _modTag

	protected val _position = Vector3()

	/**
	 * The position of the camera
	 */
	override val position: Vector3Ro
		get() = _position


	override fun setPosition(x: Float, y: Float, z: Float) {
		_position.set(x, y, z)
		dirty()
	}

	protected val _direction = Vector3.Z.copy()

	/**
	 * The unit length direction vector of the camera
	 */
	override val direction: Vector3Ro
		get() = _direction

	protected val _up: Vector3 = Vector3.NEG_Y.copy()

	/**
	 * The unit length up vector of the camera
	 */
	override val up: Vector3Ro
		get() = _up

	override fun setUp(x: Float, y: Float, z: Float) {
		_up.set(x, y, z)
		dirty()
	}

	protected val _projection: Matrix4 = Matrix4()

	/**
	 * The projection matrix
	 */
	override val projection: Matrix4Ro
		get() {
			validateViewProjection()
			return _projection
		}

	protected val _view: Matrix4 = Matrix4()

	/**
	 * The view matrix
	 */
	override val view: Matrix4Ro
		get() {
			validateViewProjection()
			return _view
		}

	/**
	 * The near clipping plane distance, has to be positive
	 */
	override var near by bindable(1f)

	/**
	 * The far clipping plane distance, has to be positive
	 */
	override var far by bindable(3000f)

	/**
	 * The viewport width
	 */
	override var viewportWidth by bindable(1f)

	/**
	 * The viewport height
	 */
	override var viewportHeight by bindable(1f)

	//------------------------------------
	// Set after update()
	//------------------------------------

	protected val _frustum: Frustum = Frustum()

	/**
	 * The frustum
	 */
	override val frustum: FrustumRo
		get() {
			validateFrustum()
			return _frustum
		}

	protected val _invCombined: Matrix4 = Matrix4()

	/**
	 * The inverse combined projection and view matrix
	 */
	override val invCombined: Matrix4Ro
		get() {
			validateInvCombined()
			return _invCombined
		}

	//------------------------------------
	// Temp storage
	//------------------------------------

	private val tmpVec = Vector3()

	/**
	 * Sets the camera's direction, keeping the up vector orthonormal.
	 */
	override fun setDirection(x: Float, y: Float, z: Float, keepUpOrthonormal: Boolean) {
		if (x == 0f && y == 0f && z == 0f) return
		if (keepUpOrthonormal) {
			tmpVec.set(x, y, z)
			val dot = tmpVec.dot(_up)
			if (MathUtils.isZero(dot - 1)) {
				// Collinear
				_up.set(direction).scl(-1f)
			} else if (MathUtils.isZero(dot + 1)) {
				// Collinear opposite
				_up.set(direction)
			}
			_direction.set(tmpVec)
			normalizeUp()
		} else {
			_direction.set(x, y, z)
		}
		dirty()
	}

	/**
	 * Recalculates the direction of the camera to look at the point (x, y, z).
	 */
	override fun pointToLookAt(x: Float, y: Float, z: Float) {
		setDirection(x - position.x, y - position.y, z - position.z)
	}

	/**
	 * Normalizes the up vector by first calculating the right vector via a cross product between direction and up, and then
	 * recalculating the up vector via a cross product between right and direction.
	 */
	protected fun normalizeUp() {
		tmpVec.set(direction).crs(up).nor()
		_up.set(tmpVec).crs(direction).nor()
	}

	/**
	 * Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and
	 * up vector will not be orthogonalized.
	 *
	 * @param radians the angle in radians
	 * @param axisX the x-component of the axis
	 * @param axisY the y-component of the axis
	 * @param axisZ the z-component of the axis
	 */
	fun rotate(radians: Float, axisX: Float, axisY: Float, axisZ: Float) {
		_direction.rotate(radians, axisX, axisY, axisZ)
		_up.rotate(radians, axisX, axisY, axisZ)
		dirty()
	}

	/**
	 * Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and up vector
	 * will not be orthogonalized.
	 *
	 * @param axis the axis to rotate around
	 * @param radians the angle
	 */
	fun rotate(axis: Vector3Ro, radians: Float) {
		_direction.rotate(radians, axis)
		_up.rotate(radians, axis)
		dirty()
	}

	/**
	 * Rotates the camera by the given angle around the direction vector. The direction and up vector will not be orthogonalized.
	 * @param radians
	 */
	fun rotate(radians: Float) {
		rotate(direction, radians)
	}

	/**
	 * Rotates the direction and up vector of this camera by the given rotation matrix. The direction and up vector will not be
	 * orthogonalized.
	 *
	 * @param transform The rotation matrix
	 */
	fun rotate(transform: Matrix4Ro) {
		_direction.rot(transform)
		_up.rot(transform)
		dirty()
	}

	/**
	 * Rotates the direction and up vector of this camera by the given {@link Quaternion}. The direction and up vector will not be
	 * orthogonalized.
	 *
	 * @param quat The quaternion
	 */
	fun rotate(quat: QuaternionRo) {
		quat.transform(_direction)
		quat.transform(_up)
		dirty()
	}

	/**
	 * Rotates the direction and up vector of this camera by the given angle around the given axis, with the axis attached to given
	 * point. The direction and up vector will not be orthogonalized.
	 *
	 * @param point the point to attach the axis to
	 * @param axis the axis to rotate around
	 * @param radians the angle in radians
	 */
	fun rotateAround(point: Vector3Ro, axis: Vector3Ro, radians: Float) {
		tmpVec.set(point)
		tmpVec.sub(position)
		translate(tmpVec)
		rotate(axis, radians)
		tmpVec.rotate(radians, axis)
		translate(-tmpVec.x, -tmpVec.y, -tmpVec.z)
	}

	/**
	 * Transform the position, direction and up vector by the given matrix
	 *
	 * @param transform The transform matrix
	 */
	fun transform(transform: Matrix4Ro) {
		_position.mul(transform)
		rotate(transform)
	}

	/**
	 * Moves the camera by the given amount on each axis.
	 * @param x the displacement on the x-axis
	 * @param y the displacement on the y-axis
	 * @param z the displacement on the z-axis
	 */
	fun translate(x: Float, y: Float, z: Float = 0f) {
		_position.add(x, y, z)
		dirty()
	}

	/**
	 * Moves the camera by the given vector.
	 * @param vec the displacement vector
	 */
	fun translate(vec: Vector3Ro) {
		_position.add(vec)
		dirty()
	}

	/**
	 * Moves the camera by the given vector.
	 * @param vec the displacement vector
	 */
	fun translate(vec: Vector2Ro) {
		translate(vec.x, vec.y)
	}

	override fun canvasToGlobal(canvasCoords: Vector3, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Vector3 {
		canvasCoords.x = 2f * (canvasCoords.x - viewportX) / viewportWidth - 1f
		canvasCoords.y = -2f * (canvasCoords.y - viewportY) / viewportHeight + 1f
		canvasCoords.z = 2f * canvasCoords.z - 1f
		invCombined.prj(canvasCoords)
		return canvasCoords
	}

	/**
	 * Moves the position to the point where the camera is looking at the provided coordinates at a given distance.
	 */
	override fun moveToLookAtPoint(x: Float, y: Float, z: Float, distance: Float) {
		tmpVec.set(direction).scl(distance) // Assumes direction is normalized
		_position.set(x, y, z).sub(tmpVec)
		dirty()
	}

	private var viewProjectionIsValid: Boolean = false
	private var invCombinedIsValid: Boolean = false
	private var frustumIsValid: Boolean = false

	protected fun dirty() {
		viewProjectionIsValid = false
		invCombinedIsValid = false
		frustumIsValid = false
		_modTag.increment()
	}

	private fun validateViewProjection() {
		if (viewProjectionIsValid) return
		viewProjectionIsValid = true
		updateViewProjection()
	}

	private fun validateInvCombined() {
		if (invCombinedIsValid) return
		invCombinedIsValid = true
		validateViewProjection()
		updateInvCombined()
	}

	private fun validateFrustum() {
		if (frustumIsValid) return
		frustumIsValid = true
		updateFrustum()
	}

	protected abstract fun updateViewProjection()

	protected open fun updateInvCombined() {
		_invCombined.set(_combined)
		_invCombined.inv()
	}

	protected open fun updateFrustum() {
		_frustum.update(invCombined)
	}

	protected fun <T> bindable(initial: T): ReadWriteProperty<Any?, T> = observable(initial) {
		dirty()
	}

}

fun Window.autoCenterCamera(camera: Camera): Disposable {
	val windowResizedHandler = {
		_: Float, _: Float, _: Boolean ->
		centerCamera(camera)
	}
	sizeChanged.add(windowResizedHandler)
	centerCamera(camera)
	return object : Disposable {
		override fun dispose() {
			sizeChanged.remove(windowResizedHandler)
		}
	}
}

/**
 * Centers the camera to this window.
 */
fun Window.centerCamera(camera: Camera) {
	if (width > 0f && height > 0f && (camera.viewportWidth != width || camera.viewportHeight != height)) {
		camera.setViewport(width, height)
		camera.moveToLookAtRect(0f, 0f, width, height)
	}
}