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

package com.acornui.graphic.lighting

import com.acornui.graphic.PerspectiveCamera
import com.acornui.graphic.Window
import com.acornui.math.MathUtils
import com.acornui.math.Vector3

/**
 * @author nbilyk
 */
class PointLightCamera(window: Window, resolution: Float) {

	val camera = PerspectiveCamera()

	init {
		camera.fieldOfView = 90f * MathUtils.degRad
		camera.viewportWidth = resolution
		camera.viewportHeight = resolution
	}

	fun update(pointLight: PointLight, direction: Int) {
		if (pointLight.radius < 0.0001f) return
		camera.setPosition(pointLight.position)
		camera.setDirection(CUBEMAP_DIRECTIONS[direction], keepUpOrthonormal = false)
		camera.setUp(CUBEMAP_UP[direction])
	}

	companion object {
		// positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ
		private val CUBEMAP_DIRECTIONS = arrayOf(Vector3(1f, 0f, 0f), Vector3(-1f, 0f, 0f), Vector3(0f, 1f, 0f), Vector3(0f, -1f, 0f), Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f))
		private val CUBEMAP_UP = arrayOf(Vector3(0f, -1f, 0f), Vector3(0f, -1f, 0f), Vector3(0f, 0f, 1f), Vector3(0f, 0f, -1f), Vector3(0f, -1f, 0f), Vector3(0f, -1f, 0f))
	}
}
