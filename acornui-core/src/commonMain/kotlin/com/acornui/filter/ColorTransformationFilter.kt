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

package com.acornui.filter

import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.gl.core.GlState
import com.acornui.math.ColorTransformation
import com.acornui.math.MinMaxRo

class ColorTransformationFilter(
		owner: Owned,

		/**
		 * The mutable color transformation to be applied.
		 */
		val colorTransformation: ColorTransformation
) : RenderFilterBase(owner) {

	private val glState = inject(GlState)
	private val combined = ColorTransformation()

	override fun render(clip: MinMaxRo) {
		if (!enabled) return renderContents(clip)
		val previous = glState.colorTransformation
		if (previous == null) {
			glState.colorTransformation = colorTransformation
		} else {
			combined.set(previous).mul(colorTransformation)
			glState.colorTransformation = combined
		}
		renderContents(clip)
		glState.colorTransformation = previous
	}

}

fun Owned.colorTransformationFilter(colorTransformation: ColorTransformation = ColorTransformation()): ColorTransformationFilter {
	return ColorTransformationFilter(this, colorTransformation)
}