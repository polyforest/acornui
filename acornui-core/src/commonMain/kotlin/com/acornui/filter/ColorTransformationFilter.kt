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

import com.acornui.component.RenderContextRo
import com.acornui.core.di.Owned
import com.acornui.gl.core.useColorTransformation
import com.acornui.math.ColorTransformation

class ColorTransformationFilter(
		owner: Owned,

		/**
		 * The mutable color transformation to be applied.
		 */
		val colorTransformation: ColorTransformation
) : RenderFilterBase(owner) {

	override val shouldSkipFilter: Boolean
		get() = !enabled || colorTransformation.isIdentity

	override fun draw(renderContext: RenderContextRo) {
		glState.useColorTransformation(colorTransformation) {
			contents?.render(renderContext)
		}
	}

}

fun Owned.colorTransformationFilter(colorTransformation: ColorTransformation = ColorTransformation()): ColorTransformationFilter {
	return ColorTransformationFilter(this, colorTransformation)
}
