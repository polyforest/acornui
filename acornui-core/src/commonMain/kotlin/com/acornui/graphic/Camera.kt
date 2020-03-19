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

@file:Suppress("PropertyName", "MemberVisibilityCanBePrivate", "unused")

package com.acornui.graphic

import com.acornui.component.CameraTransformableRo
import com.acornui.math.*
import com.acornui.observe.ModTagRo
import com.acornui.observe.modTag
import com.acornui.properties.afterChange
import com.acornui.signal.bind
import com.acornui.signal.or
import kotlin.properties.ReadWriteProperty

interface CameraRo : CameraTransformableRo {

	/**
	 * Incremented whenever something on this camera has changed.
	 */
	val modTag: ModTagRo

	/**
	 * The position of the camera
	 */
	val position: Vector3Ro

	/**
	 * The unit length direction vector of the camera.
	 */
	val direction: Vector3Ro

	/**
	 * The unit length up vector of the camera.
	 */
	val up: Vector3Ro

	/**
	 * The near clipping plane distance, must be positive.
	 */
	val near: Float

	/**
	 * The far clipping plane distance, must be positive.
	 */
	val far: Float

	/**
	 * The region of space that may appear on the screen.
	 */
	val frustum: FrustumRo
}

private val originTmp = Vector3()
private val directionTmp = Vector3()

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
	 * The near clipping plane distance, must be positive.
	 */
	override var near: Float

	/**
	 * The far clipping plane distance, must be positive.
	 */
	override var far: Float

	override var viewport: RectangleRo

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

	/**
	 * Moves and/or zooms the camera to fit the given 2d bounding box into the viewport, maintaining the current
	 * direction.
	 * @param scaling The scaling type to fit to the given width/height. This may not be a stretch type.
	 */
	fun moveToLookAtRect(x: Float, y: Float, width: Float, height: Float, scaling: Scaling = Scaling.FIT)
}

/**
 * Sets the [Camera.viewport].
 */
fun Camera.setViewport(width: Float, height: Float) {
	viewport = Rectangle(0f, 0f, width, height)
}

/**
 * Sets the [Camera.viewport].
 */
fun Camera.setViewport(x: Float, y: Float, width: Float, height: Float) {
	viewport = Rectangle(x, y, width, height)
}

fun Camera.moveToLookAtRect(region: RectangleRo, scaling: Scaling = Scaling.FIT) = moveToLookAtRect(region.x, region.y, region.width, region.height, scaling)
fun Camera.moveToLookAtRect(region: MinMaxRo, scaling: Scaling = Scaling.FIT) = moveToLookAtRect(region.xMin, region.yMin, region.width, region.height, scaling)

/**
 * If true, increasing clip space coordinates will be in the downward direction. 
 * 
 * Cameras by default will consider the top-left corner of the screen as 0,0.
 * Call this method to consider the bottom-left corner of the screen as 0,0.
 *
 * Typical conventions:
 * ```
 * UI Screen (Camera default)
 *   up:        Vector3.NEG_Y
 *   direction: Vector3.Z
 *
 * UI Frame Buffer
 *   up:        Vector3.Y
 *   direction: Vector3.NEG_Z
 *
 * Game Screen
 *   up:        Vector3.Y
 *   direction: Vector3.NEG_Z
 *
 * Game Frame Buffer (Camera default)
 *   up:        Vector3.NEG_Y
 *   direction: Vector3.Z
 *  ```
 */
fun Camera.yDown(value: Boolean) {
	if (value) {
		setUp(Vector3.NEG_Y)
		setDirection(Vector3.Z)
	} else {
		setUp(Vector3.Y)
		setDirection(Vector3.NEG_Z)
	}
}

abstract class CameraBase : Camera {

	protected val _viewProjection: Matrix4 = Matrix4()
	override val viewProjectionTransform: Matrix4Ro
		get() {
			validateViewProjection()
			return _viewProjection
		}

	protected val _modTag = modTag()

	override val modTag: ModTagRo = _modTag

	protected val _position = Vector3()

	override val position: Vector3Ro
		get() = _position


	override fun setPosition(x: Float, y: Float, z: Float) {
		_position.set(x, y, z)
		dirty()
	}

	protected val _direction = Vector3.Z.copy()

	override val direction: Vector3Ro
		get() = _direction

	protected val _up: Vector3 = Vector3.NEG_Y.copy()

	override val up: Vector3Ro
		get() = _up

	override fun setUp(x: Float, y: Float, z: Float) {
		_up.set(x, y, z)
		dirty()
	}

	protected val _projection: Matrix4 = Matrix4()

	override val projectionTransform: Matrix4Ro
		get() {
			validateViewProjection()
			return _projection
		}

	protected val _view: Matrix4 = Matrix4()

	override val viewTransform: Matrix4Ro
		get() {
			validateViewProjection()
			return _view
		}

	override var near by bindable(1f)

	/**
	 * The far clipping plane distance, must be positive.
	 */
	override var far by bindable(3000f)

	override var viewport: RectangleRo by bindable(Rectangle(0f, 0f, 1f, 1f))

	//------------------------------------
	// Set after update()
	//------------------------------------

	protected val _frustum: Frustum = Frustum()

	override val frustum: FrustumRo
		get() {
			validateFrustum()
			return _frustum
		}

	protected val _combinedInv: Matrix4 = Matrix4()

	override val viewProjectionTransformInv: Matrix4Ro
		get() {
			validateCombinedInv()
			return _combinedInv
		}

	//------------------------------------
	// Temp storage
	//------------------------------------

	private val tmpVec = Vector3()

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
	 * Rotates the direction and up vector of this camera by the given [Quaternion]. The direction and up vector will
	 * not be orthogonalized.
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

	private fun validateCombinedInv() {
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
		_combinedInv.set(_viewProjection).inv()
	}

	protected open fun updateFrustum() {
		_frustum.update(viewProjectionTransformInv)
	}

	protected fun <T> bindable(initial: T): ReadWriteProperty<Any?, T> = afterChange(initial) {
		dirty()
	}

}

fun Window.autoCenterCamera(camera: Camera) = (sizeChanged or scaleChanged).bind {
	centerCamera(camera)
}

/**
 * Centers the camera to this window.
 */
fun Window.centerCamera(camera: Camera) {
	if (width > 0 && height > 0) {
		val w = width
		val h = height
		camera.setViewport(w, h)
		camera.moveToLookAtRect(0f, 0f, w, h)
	}
}
