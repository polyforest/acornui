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

package com.acornui.graphic

import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.math.Vector2
import com.acornui.math.Vector3
import com.acornui.math.vec2
import com.acornui.math.vec3


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

	private val tmp: Vector3 = vec3()
	private val tmp2: Vector2 = vec2()

	init {
		near = -1f
	}

	override fun updateViewProjection() {
		val viewport = viewport
		_projection.setToOrtho(zoom * -viewport.width * 0.5f, zoom * viewport.width * 0.5f, zoom * -viewport.height * 0.5f, zoom * viewport.height * 0.5f, near, far)
		_view.setToLookAt(position, tmp.set(position).add(direction), up)
		_viewProjection.set(_projection).mul(_view)
	}

	override fun moveToLookAtRect(x: Float, y: Float, width: Float, height: Float, scaling: Scaling) {
		val viewport = viewport
		scaling.apply(viewport.width, viewport.height, width, height, tmp2)
		val (newW, newH) = tmp2
		zoom = if (viewport.width == 0f) 0f else newW / viewport.width
		_position.set(x + newW * 0.5f, y + newH * 0.5f, 0f)
		dirty()
	}

}


fun Context.orthographicCamera(autoCenter: Boolean = false, init: OrthographicCamera.() -> Unit = {}): OrthographicCamera {
	val p = OrthographicCamera()
	if (autoCenter) own(inject(Window).autoCenterCamera(p))
	p.init()
	return p
}
