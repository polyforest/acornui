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

package com.acornui.component

import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMaxRo

interface BasicDrawable {

	/**
	 * The natural width of this drawable, in points.
	 */
	val naturalWidth: Float

	/**
	 * The natural height of this drawable, in points.
	 */
	val naturalHeight: Float

	/**
	 * The region (in local coordinates) that will be drawn with [render].
	 */
	val drawRegion: MinMaxRo

	/**
	 * Updates this BasicDrawable's local vertices.
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
	fun updateVertices(width: Float = naturalWidth, height: Float = naturalHeight, x: Float = 0f, y: Float = 0f, z: Float = 0f, rotation: Float = 0f, originX: Float = 0f, originY: Float = 0f)

	fun render(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo)
}

fun BasicDrawable.render(renderContext: RenderContextRo) {
	render(renderContext.clipRegion, renderContext.modelTransform, renderContext.colorTint)
}