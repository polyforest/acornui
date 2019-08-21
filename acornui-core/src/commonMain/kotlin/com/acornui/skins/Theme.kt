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
import com.acornui.serialization.*
import kotlinx.serialization.Serializable

/**
 * The Theme is a set of common styling properties, used to build a skin.
 */
@Serializable
data class Theme(

		/**
		 * This will be set to the `backgroundColor` property in [com.acornui.config] window.
		 * @see com.acornui.WindowConfig.backgroundColor
		 * @see com.acornui.AppConfig
		 */
		val bgColor: ColorRo = Color(0xf1f2f3ff),
		val panelBgColor: ColorRo = Color(0xe7edf1ff),

		val fill: ColorRo = Color(0xf3f9faff),
		val fillOver: ColorRo = Color(0xffffffff),
		val fillDown: ColorRo = Color(0xe3e9eaff),
		val fillToggled: ColorRo = Color(0xedf1faff),
		val fillToggledOver: ColorRo = Color(0xfdffffff),
		val fillToggledDown: ColorRo = Color(0xdde1eaff),
		val fillDisabled: ColorRo = Color(0xccccccff),

		val stroke: ColorRo = Color(0x888888ff),
		val strokeOver: ColorRo = Color(0x989898ff),
		val strokeDown: ColorRo = Color(0x787878ff),
		val strokeToggled: ColorRo = Color(0x2287f9cc),
		val strokeToggledOver: ColorRo = Color(0x3297ffcc),
		val strokeToggledDown: ColorRo = Color(0x1277e9cc),
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

		val selectableText: Boolean = false,

		val errorColor: ColorRo = Color(0xcc3333ff),
		val warningColor: ColorRo = Color(0xff9933ff),
		val infoColor: ColorRo = Color(0x339933ff),

		val controlBarBgColor: ColorRo = Color(0xdae5f0ff),

		val evenRowBgColor: ColorRo = Color(0xffffffff),
		val oddRowBgColor: ColorRo = Color(0xe1e2e3ff),

		val highlightedEvenRowBgColor: ColorRo = Color(0xfeffd2ff),
		val highlightedOddRowBgColor: ColorRo = Color(0xfeffd2ff),

		val toggledEvenRowBgColor: ColorRo = Color(0xfcfd7cff),
		val toggledOddRowBgColor: ColorRo = Color(0xfcfd7cff),

		val buttonPad: PadRo = Pad(4f),
		val iconButtonGap: Float = 2f,

		val iconColor: ColorRo = Color(0.25f, 0.25f, 0.25f, 0.8f),

		val atlasPath: String = "assets/uiskin/uiskin.json"
)

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