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

interface BasicRenderable {

	val naturalWidth: Float

	val naturalHeight: Float

	/**
	 * Updates the world vertices based on the model transform and color tint.
	 */
	fun updateWorldVertices(width: Float = naturalWidth, height: Float = naturalHeight, transform: Matrix4Ro = Matrix4.IDENTITY, tint: ColorRo = Color.WHITE)

	/**
	 * Renders this component.
	 */
	fun render()
}