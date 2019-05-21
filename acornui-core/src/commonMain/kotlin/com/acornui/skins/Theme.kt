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

package com.acornui.skins

import com.acornui.component.ButtonState
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.color
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.math.PadSerializer
import com.acornui.serialization.*

/**
 * The Theme is a set of common styling properties, used to build a skin.
 */
data class Theme(

		/**
		 * This will be set to the `backgroundColor` property in [com.acornui.core.config] window.
		 * @see com.acornui.core.WindowConfig.backgroundColor
		 * @see com.acornui.core.AppConfig
		 */
		val bgColor: ColorRo = Color(0xf1f2f3ff),
		val panelBgColor: ColorRo = Color(0xe7edf1ff),

		val brighten: ColorRo = Color(0x15151500),

		val fill: ColorRo = Color(0xf3f9faff),
		val fillOver: ColorRo = fill + brighten,
		val fillDown: ColorRo = fill - brighten,
		val fillToggled: ColorRo = Color(0xedf1faff),
		val fillToggledOver: ColorRo = fillToggled + brighten,
		val fillToggledDown: ColorRo = fillToggled - brighten,
		val fillDisabled: ColorRo = Color(0xccccccff),

		val stroke: ColorRo = Color(0x888888ff),
		val strokeOver: ColorRo = stroke + brighten,
		val strokeDown: ColorRo = stroke - brighten,
		val strokeToggled: ColorRo = Color(0x2287f9cc),
		val strokeToggledOver: ColorRo = strokeToggled + brighten,
		val strokeToggledDown: ColorRo = strokeToggled - brighten,
		val strokeDisabled: ColorRo = Color(0x999999ff),

		/**
		 * The shine color to overlay. (Set to clear for no shine.)
		 */
		val fillShine: ColorRo = Color(1f, 1f, 1f, 0.9f),
		val fillToggledShine: ColorRo = Color(1f, 1f, 1f, 0.9f),

		val focusHighlightColor: ColorRo = Color(0x0235acff),

		/**
		 * Text input, text area.
		 */
		val inputFill: ColorRo = Color(0.97f, 0.97f, 0.97f, 1f),

		val strokeThickness: Float = 1f,
		val borderRadius: Float = 8f,

		val textColor: ColorRo = Color(0x333333ff),
		val textDisabledColor: ColorRo = Color(0x666666ff),
		val headingColor: ColorRo = Color(0x333333ff),
		val formLabelColor: ColorRo = Color(0x555555ff),

		val errorColor: ColorRo = Color(0xcc3333ff),
		val warningColor: ColorRo = Color(0xff9933ff),
		val infoColor: ColorRo = Color(0x339933ff),

		val controlBarBgColor: ColorRo = Color(0xdae5f0ff),

		val evenRowBgColor: ColorRo = bgColor + Color(0x03030300),
		val oddRowBgColor: ColorRo = bgColor - Color(0x03030300),

		val highlightedEvenRowBgColor: ColorRo = Color(0xfeffd2ff),
		val highlightedOddRowBgColor: ColorRo = Color(0xfeffd2ff),

		val toggledEvenRowBgColor: ColorRo = Color(0xfcfd7cff),
		val toggledOddRowBgColor: ColorRo = Color(0xfcfd7cff),

		val buttonPad: PadRo = Pad(4f),
		val iconButtonGap: Float = 2f,

		val iconColor: ColorRo = Color(0.25f, 0.25f, 0.25f, 0.8f),

		val atlasPath: String = "assets/uiskin/uiskin.json"
)

object ThemeSerializer : To<Theme>, From<Theme> {

	override fun read(reader: Reader): Theme {
		return Theme(
				bgColor = reader.color("bgColor")!!,
				borderRadius = reader.float("borderRadius")!!,
				brighten = reader.color("brighten")!!,
				buttonPad = reader.obj("buttonPad", PadSerializer)!!,
				controlBarBgColor = reader.color("controlBarBgColor")!!,
				errorColor = reader.color("errorColor")!!,
				evenRowBgColor = reader.color("evenRowBgColor")!!,
				fill = reader.color("fill")!!,
				fillDisabled = reader.color("fillDisabled")!!,
				fillDown = reader.color("fillDown")!!,
				fillOver = reader.color("fillOver")!!,
				fillShine = reader.color("fillShine")!!,
				fillToggled = reader.color("fillToggled")!!,
				fillToggledDown = reader.color("fillToggledDown")!!,
				fillToggledOver = reader.color("fillToggledOver")!!,
				fillToggledShine = reader.color("fillToggledShine")!!,
				focusHighlightColor = reader.color("focusHighlightColor")!!,
				formLabelColor = reader.color("formLabelColor")!!,
				headingColor = reader.color("headingColor")!!,
				highlightedEvenRowBgColor = reader.color("highlightedEvenRowBgColor")!!,
				highlightedOddRowBgColor = reader.color("highlightedOddRowBgColor")!!,
				iconButtonGap = reader.float("iconButtonGap")!!,
				iconColor = reader.color("iconColor")!!,
				infoColor = reader.color("infoColor")!!,
				inputFill = reader.color("inputFill")!!,
				oddRowBgColor = reader.color("oddRowBgColor")!!,
				panelBgColor = reader.color("panelBgColor")!!,
				stroke = reader.color("stroke")!!,
				strokeDisabled = reader.color("strokeDisabled")!!,
				strokeDown = reader.color("strokeDown")!!,
				strokeOver = reader.color("strokeOver")!!,
				strokeThickness = reader.float("strokeThickness")!!,
				strokeToggled = reader.color("strokeToggled")!!,
				strokeToggledDown = reader.color("strokeToggledDown")!!,
				strokeToggledOver = reader.color("strokeToggledOver")!!,
				textColor = reader.color("textColor")!!,
				textDisabledColor = reader.color("textDisabledColor")!!,
				toggledEvenRowBgColor = reader.color("toggledEvenRowBgColor")!!,
				toggledOddRowBgColor = reader.color("toggledOddRowBgColor")!!,
				warningColor = reader.color("warningColor")!!
		)
	}

	override fun Theme.write(writer: Writer) {
		writer.color("bgColor", bgColor)
		writer.float("borderRadius", borderRadius)
		writer.color("brighten", brighten)
		writer.obj("buttonPad", buttonPad, PadSerializer)
		writer.color("controlBarBgColor", controlBarBgColor)
		writer.color("errorColor", errorColor)
		writer.color("evenRowBgColor", evenRowBgColor)
		writer.color("fill", fill)
		writer.color("fillDisabled", fillDisabled)
		writer.color("fillDown", fillDown)
		writer.color("fillOver", fillOver)
		writer.color("fillShine", fillShine)
		writer.color("fillToggled", fillToggled)
		writer.color("fillToggledDown", fillToggledDown)
		writer.color("fillToggledOver", fillToggledOver)
		writer.color("fillToggledShine", fillToggledShine)
		writer.color("focusHighlightColor", focusHighlightColor)
		writer.color("formLabelColor", formLabelColor)
		writer.color("headingColor", headingColor)
		writer.color("highlightedEvenRowBgColor", highlightedEvenRowBgColor)
		writer.color("highlightedOddRowBgColor", highlightedOddRowBgColor)
		writer.float("iconButtonGap", iconButtonGap)
		writer.color("iconColor", iconColor)
		writer.color("infoColor", infoColor)
		writer.color("inputFill", inputFill)
		writer.color("oddRowBgColor", oddRowBgColor)
		writer.color("panelBgColor", panelBgColor)
		writer.color("stroke", stroke)
		writer.color("strokeDisabled", strokeDisabled)
		writer.color("strokeDown", strokeDown)
		writer.color("strokeOver", strokeOver)
		writer.float("strokeThickness", strokeThickness)
		writer.color("strokeToggled", strokeToggled)
		writer.color("strokeToggledDown", strokeToggledDown)
		writer.color("strokeToggledOver", strokeToggledOver)
		writer.color("textColor", textColor)
		writer.color("textDisabledColor", textDisabledColor)
		writer.color("toggledEvenRowBgColor", toggledEvenRowBgColor)
		writer.color("toggledOddRowBgColor", toggledOddRowBgColor)
		writer.color("warningColor", warningColor)
	}
}

fun Theme.getButtonFillColor(buttonState: ButtonState): ColorRo {
	return when (buttonState) {
		ButtonState.UP,
		ButtonState.INDETERMINATE_UP -> fill
		ButtonState.TOGGLED_UP -> fillToggled

		ButtonState.DOWN,
		ButtonState.INDETERMINATE_DOWN -> fillDown
		ButtonState.TOGGLED_DOWN -> fillToggledDown

		ButtonState.OVER,
		ButtonState.INDETERMINATE_OVER -> fillOver
		ButtonState.TOGGLED_OVER -> fillToggledOver

		ButtonState.DISABLED -> fillDisabled
	}
}

fun Theme.getButtonStrokeColor(buttonState: ButtonState): ColorRo {
	return when (buttonState) {
		ButtonState.UP,
		ButtonState.INDETERMINATE_UP -> stroke
		ButtonState.TOGGLED_UP -> strokeToggled

		ButtonState.DOWN,
		ButtonState.INDETERMINATE_DOWN -> strokeDown
		ButtonState.TOGGLED_DOWN -> strokeToggledDown

		ButtonState.OVER,
		ButtonState.INDETERMINATE_OVER -> strokeOver
		ButtonState.TOGGLED_OVER -> strokeToggledOver

		ButtonState.DISABLED -> strokeDisabled
	}
}