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

import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.gl.core.GlState
import com.acornui.gl.core.useColorTransformation
import com.acornui.graphic.ColorRo
import com.acornui.math.ColorTransformation
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMaxRo

class ColorTransformationFilter(
		owner: Owned,

		/**
		 * The mutable color transformation to be applied.
		 */
		val colorTransformation: ColorTransformation
) : RenderFilterBase(owner) {

	private val glState = inject(GlState)

	override val shouldSkipFilter: Boolean
		get() = !enabled || colorTransformation.isIdentity

	override fun draw(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo) {
		glState.useColorTransformation(colorTransformation) {
			contents?.render()
		}
	}

}

fun Owned.colorTransformationFilter(colorTransformation: ColorTransformation = ColorTransformation()): ColorTransformationFilter {
	return ColorTransformationFilter(this, colorTransformation)
}
