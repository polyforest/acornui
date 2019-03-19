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
import com.acornui.core.di.DKey
import com.acornui.core.di.Injector
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
class Theme {

	/**
	 * This will be set to the `backgroundColor` property in [com.acornui.core.config] window.
	 * @see com.acornui.core.WindowConfig.backgroundColor
	 * @see com.acornui.core.AppConfig
	 */
	var bgColor: ColorRo = Color(0xF1F2F3FF)
	var panelBgColor: ColorRo = Color(0xE7EDF1FF)

	private val brighten: ColorRo = Color(0x15151500)

	var fill: ColorRo = Color(0xF3F9FAFF)
	var fillDown: ColorRo = Color(0xE3E9EAFF)
	var fillHighlight: ColorRo = fill + brighten
	var fillDisabled: ColorRo = Color(0xCCCCCCFF)

	/**
	 * The shine color to overlay. (Set to clear for no shine.)
	 */
	var fillShine: ColorRo = Color(1f, 1f, 1f, 0.9f)
	var inputFill: ColorRo = Color(0.97f, 0.97f, 0.97f, 1f)

	var stroke: ColorRo = Color(0x888888FF)
	var strokeHighlight: ColorRo = stroke + brighten
	var strokeDisabled: ColorRo = Color(0x999999FF)

	var strokeToggled: ColorRo = Color(0x717276FF)
	var strokeToggledHighlight: ColorRo = strokeToggled + brighten

	var focusHighlightColor: ColorRo = Color(0x0235ACFF)

	var strokeThickness = 1f
	var borderRadius = 8f

	var textColor: ColorRo = Color(0x333333FF)
	var headingColor: ColorRo = Color(0x333333FF)
	var formLabelColor: ColorRo = Color(0x555555FF)

	var errorColor: ColorRo = Color(0xcc3333FF)
	var warningColor: ColorRo = Color(0xff9933FF)
	var infoColor: ColorRo = Color(0x339933FF)

	var controlBarBgColor: ColorRo = Color(0xDAE5F0FF)

	var evenRowBgColor: ColorRo = bgColor + Color(0x03030300)
	var oddRowBgColor: ColorRo = bgColor - Color(0x03030300)

	var highlightedEvenRowBgColor: ColorRo = Color(0xFEFFD2FF)
	var highlightedOddRowBgColor: ColorRo = Color(0xFEFFD2FF)

	var toggledEvenRowBgColor: ColorRo = Color(0xFCFD7CFF)
	var toggledOddRowBgColor: ColorRo = Color(0xFCFD7CFF)

	var buttonPad: PadRo = Pad(4f)
	var iconButtonGap = 2f

	var iconColor: ColorRo = Color(0.25f, 0.25f, 0.25f, 0.8f)

	var atlasPath = "assets/uiskin/uiskin.json"

	fun set(other: Theme) {
		bgColor = other.bgColor
		panelBgColor = other.panelBgColor

		fill = other.fill
		fillDown = other.fillDown
		fillHighlight = other.fillHighlight
		fillDisabled = other.fillDisabled
		fillShine = other.fillShine
		inputFill = other.inputFill

		stroke = other.stroke
		strokeHighlight = other.strokeHighlight
		strokeDisabled = other.strokeDisabled

		strokeToggled = other.strokeToggled
		strokeToggledHighlight = other.strokeToggledHighlight
		focusHighlightColor = other.focusHighlightColor

		strokeThickness = other.strokeThickness
		borderRadius = other.borderRadius

		textColor = other.textColor
		headingColor = other.headingColor
		formLabelColor = other.formLabelColor

		errorColor = other.errorColor
		warningColor = other.warningColor
		infoColor = other.infoColor

		controlBarBgColor = other.controlBarBgColor

		evenRowBgColor = other.evenRowBgColor
		oddRowBgColor = other.oddRowBgColor

		highlightedEvenRowBgColor = other.highlightedEvenRowBgColor
		highlightedOddRowBgColor = other.highlightedOddRowBgColor

		toggledEvenRowBgColor = other.toggledEvenRowBgColor
		toggledOddRowBgColor = other.toggledOddRowBgColor

		buttonPad = other.buttonPad
		iconButtonGap = other.iconButtonGap

		iconColor = other.iconColor

		atlasPath = other.atlasPath
	}

	companion object : DKey<Theme> {
		override fun factory(injector: Injector) = Theme()
	}
}

object ThemeSerializer : To<Theme>, From<Theme> {

	override fun read(reader: Reader): Theme {
		val o = Theme()
		o.atlasPath = reader.string("atlasPath")!!
		o.bgColor = reader.color("bgColor")!!
		o.borderRadius = reader.float("borderRadius")!!
		o.buttonPad = reader.obj("buttonPad", PadSerializer)!!
		o.controlBarBgColor = reader.color("controlBarBgColor")!!
		o.errorColor = reader.color("errorColor")!!
		o.evenRowBgColor = reader.color("evenRowBgColor")!!
		o.fill = reader.color("fill")!!
		o.fillDown = reader.color("fillDown")!!
		o.fillDisabled = reader.color("fillDisabled")!!
		o.fillHighlight = reader.color("fillHighlight")!!
		o.fillShine = reader.color("fillShine")!!
		o.formLabelColor = reader.color("formLabelColor")!!
		o.headingColor = reader.color("headingColor")!!
		o.highlightedEvenRowBgColor = reader.color("highlightedEvenRowBgColor")!!
		o.highlightedOddRowBgColor = reader.color("highlightedOddRowBgColor")!!
		o.iconButtonGap = reader.float("iconButtonGap")!!
		o.iconColor = reader.color("iconColor") ?: Color.DARK_GRAY
		o.infoColor = reader.color("infoColor")!!
		o.inputFill = reader.color("inputFill")!!
		o.oddRowBgColor = reader.color("oddRowBgColor")!!
		o.panelBgColor = reader.color("panelBgColor")!!
		o.stroke = reader.color("stroke")!!
		o.strokeDisabled = reader.color("strokeDisabled")!!
		o.strokeHighlight = reader.color("strokeHighlight")!!
		o.strokeThickness = reader.float("strokeThickness")!!
		o.strokeToggled = reader.color("strokeToggled")!!
		o.strokeToggledHighlight = reader.color("strokeToggledHighlight")!!
		o.focusHighlightColor = reader.color("focusHighlightColor")!!
		o.textColor = reader.color("textColor")!!
		o.toggledEvenRowBgColor = reader.color("toggledEvenRowBgColor")!!
		o.toggledOddRowBgColor = reader.color("toggledOddRowBgColor")!!
		o.warningColor = reader.color("warningColor")!!
		return o
	}

	override fun Theme.write(writer: Writer) {
		writer.string("atlasPath", atlasPath)
		writer.color("bgColor", bgColor)
		writer.float("borderRadius", borderRadius)
		writer.obj("buttonPad", buttonPad, PadSerializer)
		writer.color("controlBarBgColor", controlBarBgColor)
		writer.color("evenRowBgColor", evenRowBgColor)
		writer.color("errorColor", errorColor)
		writer.color("fill", fill)
		writer.color("fillDown", fillDown)
		writer.color("fillDisabled", fillDisabled)
		writer.color("fillHighlight", fillHighlight)
		writer.color("fillShine", fillShine)
		writer.color("formLabelColor", formLabelColor)
		writer.float("iconButtonGap", iconButtonGap)
		writer.color("iconColor", iconColor)
		writer.color("infoColor", infoColor)
		writer.color("headingColor", headingColor)
		writer.color("highlightedEvenRowBgColor", highlightedEvenRowBgColor)
		writer.color("highlightedOddRowBgColor", highlightedOddRowBgColor)
		writer.color("inputFill", inputFill)
		writer.color("oddRowBgColor", oddRowBgColor)
		writer.color("panelBgColor", panelBgColor)
		writer.color("stroke", stroke)
		writer.color("strokeDisabled", strokeDisabled)
		writer.color("strokeHighlight", strokeHighlight)
		writer.float("strokeThickness", strokeThickness)
		writer.color("strokeToggled", strokeToggled)
		writer.color("strokeToggledHighlight", strokeToggledHighlight)
		writer.color("focusHighlightColor", focusHighlightColor)
		writer.color("textColor", textColor)
		writer.color("toggledEvenRowBgColor", toggledEvenRowBgColor)
		writer.color("toggledOddRowBgColor", toggledOddRowBgColor)
		writer.color("warningColor", warningColor)
	}
}

fun Theme.getButtonFillColor(buttonState: ButtonState): ColorRo {
	return when (buttonState) {
		ButtonState.UP, ButtonState.TOGGLED_UP -> fill
		ButtonState.DOWN, ButtonState.TOGGLED_DOWN -> fillDown

		ButtonState.OVER,
		ButtonState.TOGGLED_OVER -> fillHighlight

		ButtonState.DISABLED -> fillDisabled
	}
}

fun Theme.getButtonStrokeColor(buttonState: ButtonState): ColorRo {
	return when (buttonState) {

		ButtonState.UP,
		ButtonState.DOWN -> stroke

		ButtonState.OVER -> strokeHighlight

		ButtonState.TOGGLED_UP,
		ButtonState.TOGGLED_DOWN -> strokeToggled

		ButtonState.TOGGLED_OVER -> strokeToggledHighlight

		ButtonState.DISABLED -> strokeDisabled
	}
}