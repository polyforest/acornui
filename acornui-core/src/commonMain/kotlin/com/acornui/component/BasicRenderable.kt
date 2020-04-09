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

import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Matrix4
import com.acornui.math.Matrix4Ro

/**
 * The minimal interface required for something that has vertices and can be rendered.
 *
 * [updateGlobalVertices] is called first, setting the size, applying transformation and color tinting.
 * Then rendering is done via [render].
 */
interface BasicRenderable {

	/**
	 * The x dpi scaling from dp to pixels.
	 */
	var scaleX: Float

	/**
	 * The y dpi scaling from dp to pixels.
	 */
	var scaleY: Float

	/**
	 * Sets the x and y dpi scaling.
	 */
	fun setScaling(scaleX: Float, scaleY: Float) {
		this.scaleX = scaleX
		this.scaleY = scaleY
	}
	
	/**
	 * This renderable's natural width, in dp.
	 */
	val naturalWidth: Float

	/**
	 * This renderable's natural height, in dp.
	 */
	val naturalHeight: Float

	/**
	 * Updates the world vertices based on the model transform and color tint.
	 * @param width The width of the renderable. Default is [naturalWidth].
	 * @param height The height  of the renderable. Default is [naturalHeight].
	 * @param transform The transformation matrix to go from local to global vertices.
	 * @param tint The color multiplier to go from local to global color tint.
	 */
	fun updateGlobalVertices(width: Float = naturalWidth, height: Float = naturalHeight, transform: Matrix4Ro = Matrix4.IDENTITY, tint: ColorRo = Color.WHITE)

	/**
	 * Renders this component.
	 */
	fun render()
}