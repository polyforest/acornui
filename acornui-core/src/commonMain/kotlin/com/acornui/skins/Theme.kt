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
import com.acornui.component.text.FontSize
import com.acornui.component.text.FontStyle
import com.acornui.component.text.FontWeight
import com.acornui.component.toDpis
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Pad
import com.acornui.math.PadRo
import kotlinx.serialization.Serializable

/**
 * The Theme is a set of common styling properties, used to build a skin.
 */
@Serializable
data class Theme(

		/**
		 * The window clear color.
		 * If null, this will default to the [com.acornui.WindowConfig.backgroundColor] property.
		 */
		val bgColor: ColorRo? = null,
		
		val panelBgColor: ColorRo = Color(0xe7edf1ff),

		val fill: ColorRo = Color(0xf3f9faff),
		val fillOver: ColorRo = Color(0xffffffff),
		val fillDown: ColorRo = Color(0xe3e9eaff),
		val fillToggled: ColorRo = Color(0xedf1faff),
		val fillToggledOver: ColorRo = Color(0xfdffffff),
		val fillToggledDown: ColorRo = Color(0xdde1eaff),
		val fillDisabled: ColorRo = Color(0xcccccc88),

		val stroke: ColorRo = Color(0x888888ff),
		val strokeOver: ColorRo = Color(0x989898ff),
		val strokeDown: ColorRo = Color(0x787878ff),
		val strokeToggled: ColorRo = Color(0x2287f9cc),
		val strokeToggledOver: ColorRo = Color(0x3297ffcc),
		val strokeToggledDown: ColorRo = Color(0x1277e9cc),
		val strokeDisabled: ColorRo = Color(0x99999988),

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

		val textDisabledColor: ColorRo = Color(0x666666ff),

		val selectableText: Boolean = true,

		val errorColor: ColorRo = Color(0xcc3333ff),
		val warningColor: ColorRo = Color(0xff9933ff),
		val infoColor: ColorRo = Color(0x339933ff),

		val controlBarBgColor: ColorRo = Color(0xdae5f0ff),

		val evenRowBgColor: ColorRo = Color(0xffffffff),
		val oddRowBgColor: ColorRo = Color(0xe1e2e3ff),

		val highlightedEvenRowBgColor: ColorRo = Color(0xefefc3ff),
		val highlightedOddRowBgColor: ColorRo = Color(0xdbdbb3ff),

		val toggledEvenRowBgColor: ColorRo = Color(0xf3f3b5ff),
		val toggledOddRowBgColor: ColorRo = Color(0xeff0a9ff),

		val buttonPad: PadRo = Pad(4f),
		val iconButtonGap: Float = 2f,

		val iconColor: ColorRo = Color(0.25f, 0.25f, 0.25f, 0.8f),
		val toggledIconColor: ColorRo = Color(0.5f, 0.5f, 0.25f, 0.8f),

		val atlasPaths: Map<Float, String> = "assets/uiskin/uiskin_{0}x.json".toDpis(1f, 2f),

		/**
		 * A map of font key to sizes in dp.
		 */
		val fontSizes: Map<String, Int> = mapOf(
				FontSize.EXTRA_SMALL to 10,
				FontSize.SMALL to 14,
				FontSize.REGULAR to 18,
				FontSize.LARGE to 22,
				FontSize.EXTRA_LARGE to 32
		),

		val bodyFont: ThemeFontVo = ThemeFontVo("Roboto", color = Color(0x333333ff)),
		val menuFont: ThemeFontVo = ThemeFontVo("Roboto", color = Color(0x333366ff)),
		val headingFont: ThemeFontVo = ThemeFontVo("Roboto", size = FontSize.LARGE, color = Color(0x333355ff)),
		val formLabelFont: ThemeFontVo = ThemeFontVo("Roboto", color = Color(0x27273aff))
) {

	@Deprecated("Use atlasPaths", ReplaceWith("atlasPaths"))
	val atlasPath: String
		get() = atlasPaths[1f]!!
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

@Serializable
data class ThemeFontVo(
		val family: String,
		val size: String = FontSize.REGULAR,
		val weight: String = FontWeight.REGULAR,
		val strongWeight: String = FontWeight.MEDIUM,
		val emphasisStyle: String = FontStyle.ITALIC,
		val style: String = FontStyle.NORMAL,
		val color: ColorRo
)