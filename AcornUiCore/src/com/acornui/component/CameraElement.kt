/*
 * Copyright 2017 Nicholas Bilyk
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

import com.acornui.component.layout.Transformable
import com.acornui.component.layout.TransformableRo
import com.acornui.core.graphics.CameraRo
import com.acornui.math.MinMax
import com.acornui.math.Vector2
import com.acornui.math.Vector3

interface CameraElementRo : TransformableRo {

	/**
	 * Converts a 2d coordinate relative to the window into local coordinate space.
	 *
	 * Note: This is a heavy operation as it performs a Matrix4 inversion.
	 */
	fun windowToLocal(windowCoord: Vector2): Vector2

	/**
	 * Converts a local coordinate to window coordinates.
	 */
	fun localToWindow(localCoord: Vector3): Vector3

	/**
	 * Converts a bounding rectangle from local to window coordinates.
	 */
	fun localToWindow(minMax: MinMax): MinMax {
		val tmp1 =  Vector3.obtain().set(minMax.xMin, minMax.yMin, 0f)
		val tmp2 =  Vector3.obtain().set(minMax.xMax, minMax.yMax, 0f)
		val tmp =  Vector3.obtain()
		minMax.inf()
		localToWindow(tmp.set(tmp1))
		minMax.ext(tmp.x, tmp.y)
		localToWindow(tmp.set(tmp2.x, tmp1.y, 0f))
		minMax.ext(tmp.x, tmp.y)
		localToWindow(tmp.set(tmp2))
		minMax.ext(tmp.x, tmp.y)
		localToWindow(tmp.set(tmp1.x, tmp2.y, 0f))
		minMax.ext(tmp.x, tmp.y)
		Vector3.free(tmp1)
		Vector3.free(tmp2)
		Vector3.free(tmp)
		return minMax
	}

	/**
	 * Returns the camera to be used for this component.
	 * camera.
	 */
	val camera: CameraRo
}

/**
 * A transformable element that is viewed via a Camera.
 */
interface CameraElement : CameraElementRo, Transformable {

	/**
	 * Overrides the camera to be used for this component (and its children).
	 * Set to null to switch back to the inherited camera.
	 *
	 * Note that this does NOT request a render (so it is safe to call within a draw call).
	 * If this is set outside of a render, window.requestRender should be invoked.
	 */
	var cameraOverride: CameraRo?
}