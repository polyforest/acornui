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

interface ColorTransformableRo {

	/**
	 * The color tint of this component.
	 */
	val colorTint: ColorRo

	/**
	 * The color multiplier of this component and all ancestor color tints multiplied together.
	 */
	val concatenatedColorTint: ColorRo

}

/**
 * A trait for objects that can have a color transformation applied.
 * The final pixel color value for the default shader is [colorTint * pixel]
 *
 * Note: Dom applications do not support color transformation.
 *
 * @author nbilyk
 */
interface ColorTransformable : ColorTransformableRo {

	/**
	 * The color tint of this component.
	 */
	override var colorTint: ColorRo

	fun colorTint(r: Float, g: Float, b: Float, a: Float)

}

val ColorTransformableRo.alpha: Float
	get() = colorTint.a

/**
 * A utility method for setting and retrieving the alpha tint.
 */
var ColorTransformable.alpha: Float
	get() = colorTint.a
	set(value) {
		val t = colorTint
		if (t.a == value) return
		colorTint(t.r, t.g, t.b, value)
	}
