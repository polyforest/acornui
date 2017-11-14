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
	 * Converts a local coordinate to screen coordinates.
	 */
	fun localToWindow(localCoord: Vector3): Vector3

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
	 */
	var cameraOverride: CameraRo?
}