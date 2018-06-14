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

package com.acornui.core.graphics

import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.di.own
import com.acornui.math.Matrix4
import com.acornui.math.Vector2
import com.acornui.math.Vector3


/**
 * A camera with orthographic projection.
 *
 * @author mzechner
 */
class OrthographicCamera : CameraBase() {

	/**
	 * The zoom of the camera.
	 */
	var zoom: Float by bindable(1f)

	private val tmp: Vector3 = Vector3()
	private val tmp2: Vector2 = Vector2()

	init {
		near = -1f
	}

	override fun updateViewProjection() {
		_projection.setToOrtho(zoom * -viewportWidth / 2f, zoom * viewportWidth / 2f, zoom * -viewportHeight / 2f, zoom * viewportHeight / 2f, near, far)
		_view.setToLookAt(position, tmp.set(position).add(direction), up)
		_combined.set(_projection)
		_combined.mul(_view)
	}

	override fun moveToLookAtRect(x: Float, y: Float, width: Float, height: Float, scaling: Scaling) {
		scaling.apply(viewportWidth, viewportHeight, width, height, tmp2)
		val (newW, newH) = tmp2
		zoom = if (viewportWidth == 0f) 0f else newW / viewportWidth
		_position.set(x + newW * 0.5f, y + newH * 0.5f, 0f)
		dirty()
	}

}


fun Owned.orthographicCamera(autoCenter: Boolean = false, init: OrthographicCamera.() -> Unit = {}): OrthographicCamera {
	val p = OrthographicCamera()
	if (autoCenter) own(inject(Window).autoCenterCamera(p))
	p.init()
	return p
}

class FramebufferOrthographicCamera : CameraBase() {

	/**
	 * The zoom of the camera.
	 */
	var zoom: Float by bindable(1f)

	private val tmp: Vector3 = Vector3()
	private val tmp2: Vector2 = Vector2()

	init {
		near = -1f
	}

	override fun updateViewProjection() {
		_projection.setToOrtho(zoom * -viewportWidth / 2f, zoom * viewportWidth / 2f, zoom * viewportHeight / 2f, zoom * -viewportHeight / 2f, near, far)
		_projection.trn(-1f, -1f, 0f)
		_view.setToLookAt(position, tmp.set(position).add(direction), up)
		_combined.set(_projection)
		_combined.mul(_view)
	}

	override fun moveToLookAtRect(x: Float, y: Float, width: Float, height: Float, scaling: Scaling) {
		scaling.apply(viewportWidth, viewportHeight, width, height, tmp2)
		val (newW, newH) = tmp2
		zoom = if (viewportWidth == 0f) 0f else newW / viewportWidth
		_position.set(x + newW * 0.5f, y + newH * 0.5f, 0f)
		dirty()
	}
}