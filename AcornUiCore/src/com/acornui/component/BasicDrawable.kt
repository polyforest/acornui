/*
 * Copyright 2018 Nicholas Bilyk
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

import com.acornui.gl.core.GlState
import com.acornui.graphics.ColorRo
import com.acornui.math.Matrix4Ro

interface BasicDrawable {

	val naturalWidth: Float
	val naturalHeight: Float

	fun updateWorldVertices(worldTransform: Matrix4Ro, width: Float, height: Float, x: Float = 0f, y: Float = 0f, z: Float = 0f, rotation: Float = 0f, originX: Float = 0f, originY: Float = 0f)

	/**
	 * Updates this BasicDrawable's local vertices.
	 * If this is used directly, [updateWorldVertices] should not be used, and the [GlState.setCamera] method should
	 * be supplied with the world transformation matrix.
	 *
	 * @param width The width of the sprite.
	 * @param height The height of the sprite.
	 * @param x translation
	 * @param y translation
	 * @param z translation
	 * @param rotation The rotation around the Z axis in radians. If y-axis is pointing down, this will be clockwise.
	 * @param originX The x point of the rectangle that will be 0,0
	 * @param originY The y point of the rectangle that will be 0,0
	 */
	fun updateVertices(width: Float, height: Float, x: Float = 0f, y: Float = 0f, z: Float = 0f, rotation: Float = 0f, originX: Float = 0f, originY: Float = 0f)

	/**
	 * Draws this component.
	 * Remember to set the camera on the [GlState] object before drawing.
	 * If [updateVertices] was used (and therefore no world transformation), that world transform matrix must be
	 * supplied to [GlState.setCamera] first.
	 */
	fun draw(glState: GlState, colorTint: ColorRo)
}