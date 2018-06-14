/*
 * Copyright 2015 Nicholas Bilyk
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
import com.acornui.math.MathUtils
import com.acornui.math.Vector2
import com.acornui.math.Vector3
import kotlin.math.abs
import kotlin.math.tan

/**
 * @author nbilyk
 */
open class PerspectiveCamera : CameraBase() {

	/**
	 * The field of view of the height, in radians
	 **/
	var fieldOfView: Float by bindable(67f * MathUtils.degRad)

	private val tmp = Vector3()
	private val tmp2: Vector2 = Vector2()

	override fun updateViewProjection() {
		val aspect = viewportWidth / viewportHeight
		_projection.setToProjection(abs(near), abs(far), fieldOfView, aspect)
		_view.setToLookAt(position, tmp.set(position).add(direction), up)
		_combined.set(_projection)
		_combined.mul(_view)
	}

	override fun moveToLookAtRect(x: Float, y: Float, width: Float, height: Float, scaling: Scaling) {
		scaling.apply(viewportWidth, viewportHeight, width, height, tmp2)
		val (newW, newH) = tmp2
		val distance = (newH * 0.5f) / tan(fieldOfView * 0.5f)
		moveToLookAtPoint(x + newW * 0.5f, y + newH * 0.5f, 0f, distance)
	}
}

fun Owned.perspectiveCamera(autoCenter: Boolean = false, init: PerspectiveCamera.() -> Unit = {}): PerspectiveCamera {
	val p = PerspectiveCamera()
	p.init()
	if (autoCenter) own(inject(Window).autoCenterCamera(p))
	return p
}
