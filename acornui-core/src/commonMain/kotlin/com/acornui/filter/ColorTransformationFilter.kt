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

package com.acornui.filter

import com.acornui.di.Owned
import com.acornui.gl.core.useColorTransformation
import com.acornui.graphic.ColorRo
import com.acornui.math.ColorTransformation
import com.acornui.math.Matrix4Ro
import com.acornui.math.RectangleRo

class ColorTransformationFilter(
		owner: Owned,

		/**
		 * The mutable color transformation to be applied.
		 */
		val colorTransformation: ColorTransformation
) : RenderFilterBase(owner) {

	private val colorTransformationWorld = ColorTransformation()

	override fun updateWorldVertices(region: RectangleRo, transform: Matrix4Ro, tint: ColorRo): RectangleRo {
		colorTransformationWorld.set(colorTransformation).mul(tint)
		return region
	}

	override fun render(inner: () -> Unit) {
		glState.uniforms.useColorTransformation(colorTransformationWorld) {
			inner()
		}
	}
}

fun Owned.colorTransformationFilter(colorTransformation: ColorTransformation = ColorTransformation()): ColorTransformationFilter {
	return ColorTransformationFilter(this, colorTransformation)
}
