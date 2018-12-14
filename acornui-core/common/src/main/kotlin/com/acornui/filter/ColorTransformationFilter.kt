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

import com.acornui.component.UiComponentRo
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.gl.core.GlState
import com.acornui.math.ColorTransformation
import com.acornui.math.ColorTransformationRo
import com.acornui.math.MinMaxRo

class ColorTransformationFilter(
		override val injector: Injector,

		/**
		 * The mutable color transformation to be applied.
		 */
		val colorTransformation: ColorTransformation
) : RenderFilter, Scoped {

	override var enabled = true

	private val glState = inject(GlState)
	private val combined = ColorTransformation()
	private var previous: ColorTransformationRo? = null

	override fun begin(viewport: MinMaxRo, target: UiComponentRo) {
		previous = glState.colorTransformation
		val previous = previous
		if (previous == null) {
			glState.colorTransformation = colorTransformation
		} else {
			combined.set(previous).mul(colorTransformation)
			glState.colorTransformation = combined
		}
	}

	override fun end() {
		glState.colorTransformation = previous
	}
}

fun Scoped.colorTransformationFilter(colorTransformation: ColorTransformation = ColorTransformation()): ColorTransformationFilter {
	return ColorTransformationFilter(injector, colorTransformation)
}