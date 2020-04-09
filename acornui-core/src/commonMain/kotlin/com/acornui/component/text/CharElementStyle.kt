/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.component.text

import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo

interface CharElementStyleRo {
	
	/**
	 * If true, a line will be drawn at the baseline.
	 */
	val underlined: Boolean

	/**
	 * If true, a line will be drawn at half baseline.
	 */
	val strikeThrough: Boolean

	/**
	 * The thickness of the underline or strikethrough, in dp.
	 */
	val lineThickness: Float
	val selectedTextColorTint: ColorRo
	val selectedBackgroundColor: ColorRo
	val textColorTint: ColorRo
	val backgroundColor: ColorRo
	val scaleX: Float
	val scaleY: Float
	val scalingSnapAffordance: Float
}

/**
 * A [TextSpanElement] will decorate the span's characters all the same way. This class is used to store those
 * calculated properties.
 */
class CharElementStyle : CharElementStyleRo {
	override var underlined: Boolean = false
	override var strikeThrough: Boolean = false
	override var lineThickness: Float = 1f
	override val selectedTextColorTint = Color()
	override val selectedBackgroundColor = Color()
	override val textColorTint = Color()
	override val backgroundColor = Color()
	override var scaleX: Float = 1f
	override var scaleY: Float = 1f
	override var scalingSnapAffordance: Float = 0f

	fun set(charStyle: CharStyle) {
		underlined = charStyle.underlined
		strikeThrough = charStyle.strikeThrough
		lineThickness = charStyle.lineThickness
		selectedTextColorTint.set(charStyle.selectedColorTint)
		selectedBackgroundColor.set(charStyle.selectedBackgroundColor)
		textColorTint.set(charStyle.colorTint)
		backgroundColor.set(charStyle.backgroundColor)
		scaleX = charStyle.scaleX
		scaleY = charStyle.scaleY
		scalingSnapAffordance = charStyle.scalingSnapAffordance
	}
}
