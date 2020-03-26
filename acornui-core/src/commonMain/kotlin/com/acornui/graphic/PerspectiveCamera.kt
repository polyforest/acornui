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
import com.acornui.math.MathUtils
import com.acornui.math.Vector2
import com.acornui.math.vec2
import com.acornui.math.vec3
import kotlin.math.abs
import kotlin.math.tan

// TODO: yDown, toggle

/**
 * @author nbilyk
 */
open class PerspectiveCamera : CameraBase() {

	/**
	 * The field of view of the height, in radians
	 **/
	var fieldOfView: Float by bindable(67f * MathUtils.degRad)

	private val tmp = vec3()
	private val tmp2: Vector2 = vec2()

	protected open fun updateProjection() {
		_projection.setToProjection(abs(near), abs(far), fieldOfView, viewport.width / viewport.height)
	}

	protected open fun updateView() {
		_view.setToLookAt(position, tmp.set(position).add(direction), up)
	}

	override fun updateViewProjection() {
		updateProjection()
		updateView()
		_viewProjection.set(_projection)
		_viewProjection.mul(_view)
	}

	override fun moveToLookAtRect(x: Float, y: Float, width: Float, height: Float, scaling: Scaling) {
		scaling.apply(viewport.width, viewport.height, width, height, tmp2)
		val (newW, newH) = tmp2
		val distance = (newH * 0.5f) / tan(fieldOfView * 0.5f)
		moveToLookAtPoint(x + newW * 0.5f, y + newH * 0.5f, 0f, distance)
	}
}

fun Context.perspectiveCamera(autoCenter: Boolean = false, init: PerspectiveCamera.() -> Unit = {}): PerspectiveCamera {
	val p = PerspectiveCamera()
	p.init()
	if (autoCenter) own(inject(Window).autoCenterCamera(p))
	return p
}
