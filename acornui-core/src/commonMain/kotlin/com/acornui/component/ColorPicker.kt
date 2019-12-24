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

import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.hGroup
import com.acornui.component.style.*
import com.acornui.component.text.*
import com.acornui.cursor.StandardCursors
import com.acornui.cursor.cursor
import com.acornui.di.Owned
import com.acornui.di.own
import com.acornui.focus.blurred
import com.acornui.focus.focus
import com.acornui.focus.focused
import com.acornui.focus.isFocusedSelf
import com.acornui.graphic.*
import com.acornui.input.interaction.click
import com.acornui.input.interaction.dragAttachment
import com.acornui.input.interaction.isEnterOrReturn
import com.acornui.input.keyDown
import com.acornui.math.*
import com.acornui.popup.lift
import com.acornui.reflect.afterChange
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import com.acornui.toInt
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class ColorPicker(owner: Owned) : ContainerImpl(owner) {

	val style = bind(ColorPickerStyle())

	private var background: UiComponent? = null
	private var colorSwatch: UiComponent? = null
	private val colorPalette = ColorPalette(this)

	private val colorPaletteLift = lift {
		+colorPalette
		onClosed = this@ColorPicker::close
	}

	val changed: Signal<() -> Unit>
		get() = colorPalette.changed

	var color: ColorRo
		get() = colorPalette.color
		set(value) {
			val v = value.copy().clamp()
			colorPalette.color = v
			colorSwatch?.colorTint = v
		}

	var value: HsvRo
		get() = colorPalette.value
		set(value) {
			colorPalette.value = value
			colorSwatch?.colorTint = value.toRgb(tmpColor).copy().clamp()
		}

	fun userChange(value: ColorRo) {
		colorPalette.userChange(value)
		colorSwatch?.colorTint = value.copy().clamp()
	}

	fun userChange(value: HsvRo) {
		colorPalette.userChange(value)
		colorSwatch?.colorTint = value.toRgb(tmpColor).copy().clamp()
	}

	/**
	 * If true, there will be a slider input for the color's value component in the HSV color.
	 */
	var showValuePicker: Boolean
		get() = colorPalette.showValuePicker
		set(value) {
			colorPalette.showValuePicker = value
		}

	/**
	 * If true (default), there will be a slider input for the color's alpha.
	 */
	var showAlphaPicker: Boolean
		get() = colorPalette.showAlphaPicker
		set(value) {
			colorPalette.showAlphaPicker = value
		}


	private val tmpColor = Color()

	init {
		focusEnabled = true
		styleTags.add(ColorPicker)

		click().add {
			toggleOpen()
		}

		colorPalette.changed.add {
			colorSwatch?.colorTint = value.toRgb(tmpColor).copy()
		}

		watch(style) {
			background?.dispose()
			background = addChild(0, it.background(this))
			colorSwatch?.dispose()
			colorSwatch = addChild(it.colorSwatch(this)).apply {
				colorTint = color
				interactivityMode = InteractivityMode.NONE
			}

		}

		blurred().add(::close)
	}

	private var isOpen by afterChange(false) {
		if (it) {
			addChild(colorPaletteLift)
		} else {
			removeChild(colorPaletteLift)
		}
	}


	fun open() {
		isOpen = true
	}

	fun close() {
		isOpen = false
	}

	fun toggleOpen() {
		isOpen = !isOpen
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val background = background ?: return
		val colorSwatch = colorSwatch ?: return
		val s = style
		val padding = s.padding
		colorSwatch.setSize(
				padding.reduceWidth(explicitWidth) ?: s.defaultSwatchWidth,
				padding.reduceHeight(explicitHeight) ?: s.defaultSwatchHeight
		)
		val measuredW = padding.expandWidth(colorSwatch.width)
		val measuredH = padding.expandHeight(colorSwatch.height)
		background.setSize(measuredW, measuredH)
		colorSwatch.moveTo(0.5f * (background.width - colorSwatch.width), 0.5f * (background.height - colorSwatch.height))
		out.set(background.width, background.height, colorSwatch.bottom)

		colorPaletteLift.moveTo(0f, measuredH)
	}

	override fun dispose() {
		super.dispose()
		close()
	}

	companion object : StyleTag {
		val COLOR_SWATCH_STYLE = styleTag()
	}
}

class ColorPickerStyle : StyleBase() {

	override val type = Companion

	var padding: PadRo by prop(Pad(2f))
	var background by prop(noSkin)
	var defaultSwatchWidth by prop(14f)
	var defaultSwatchHeight by prop(14f)
	var colorSwatch by prop(noSkin)

	companion object : StyleType<ColorPickerStyle>
}

inline fun Owned.colorPicker(init: ComponentInit<ColorPicker> = {}): ColorPicker  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = ColorPicker(this)
	c.init()
	return c
}

class ColorPalette(owner: Owned) : ContainerImpl(owner) {

	private val _changed = own(Signal0())
	val changed = _changed.asRo()

	private val handleWidth = 7f

	val style = bind(ColorPaletteStyle())

	/**
	 * If true, there will be a slider input for the color's value component in the HSV color.
	 */
	var showValuePicker by afterChange(true) {
		valueRect.visible = it
		valueIndicator?.visible = it
		invalidateLayout()
	}

	/**
	 * If true, there will be a slider input for the color's alpha.
	 */
	var showAlphaPicker by afterChange(true) {
		alphaRect.visible = it
		alphaGrid.visible = it
		alphaIndicator?.visible = it
		invalidateLayout()
	}

	private var background: UiComponent? = null
	private var hueSaturationIndicator: UiComponent? = null
	private var valueIndicator: UiComponent? = null
	private var alphaIndicator: UiComponent? = null

	private val hueRect = addChild(rect {
		includeInLayout = false
		style.linearGradient = LinearGradient(GradientDirection.RIGHT,
				Color(1f, 0f, 0f, 1f),
				Color(1f, 1f, 0f, 1f),
				Color(0f, 1f, 0f, 1f),
				Color(0f, 1f, 1f, 1f),
				Color(0f, 0f, 1f, 1f),
				Color(1f, 0f, 1f, 1f),
				Color(1f, 0f, 0f, 1f)
		)
	})

	private val saturationRect = addChild(rect {
		includeInLayout = false
		style.linearGradient = LinearGradient(GradientDirection.BOTTOM,
				Color(1f, 1f, 1f, 0f),
				Color(1f, 1f, 1f, 1f)
		)
		cursor(StandardCursors.CROSSHAIR)

		dragAttachment(0f).drag.add {
			canvasToLocal(tmpVec.set(it.position))
			tmpHSV.set(value)
			tmpHSV.h = 360f * MathUtils.clamp(tmpVec.x / width, 0f, 1f)
			tmpHSV.s = 1f - MathUtils.clamp(tmpVec.y / height, 0f, 1f)

			userChange(tmpHSV)
		}
	})

	/**
	 * Sets the value and triggers a changed signal.
	 */
	fun userChange(value: HsvRo) {
		val previous = this.value
		if (previous == value) return
		this.value = value
		_changed.dispatch()
	}

	/**
	 * Sets the color and triggers a changed signal.
	 */
	fun userChange(value: ColorRo) {
		val previous = color
		if (previous == value) return
		this.color = value
		_changed.dispatch()
	}



	private val valueRect = addChild(rect {
		style.margin = Pad(0f, 0f, 0f, handleWidth)
		dragAttachment(0f).drag.add {
			canvasToLocal(tmpVec.set(it.position))
			val p = MathUtils.clamp(tmpVec.y / height, 0f, 1f)

			tmpHSV.set(value)
			tmpHSV.v = 1f - p
			userChange(tmpHSV)
		}
	})

	private val alphaGrid = addChild(repeatingTexture("assets/uiskin/AlphaCheckerboard.png"))

	private val alphaRect = addChild(rect {
		style.margin = Pad(0f, 0f, 0f, handleWidth)
		dragAttachment(0f).drag.add {
			canvasToLocal(tmpVec.set(it.position))
			val p = MathUtils.clamp(tmpVec.y / height, 0f, 1f)

			tmpHSV.set(value)
			tmpHSV.a = 1f - p
			userChange(tmpHSV)
		}
	})

	private var _color: ColorRo = Color.WHITE
	var color: ColorRo
		get() = _color
		set(value) {
			if (_color != value) {
				_color = value.copy()
				_value = value.toHsv()
				invalidate(COLORS)
			}
		}

	private var _value: HsvRo = Color.WHITE.toHsv(Hsv())
	var value: HsvRo
		get() = _value
		set(value) {
			if (_value != value) {
				_value = value.copy()
				_color = value.toRgb()
				invalidate(COLORS)
			}
		}

	init {
		focusEnabled = true
		styleTags.add(ColorPalette)
		watch(style) {
			background?.dispose()
			hueSaturationIndicator?.dispose()
			valueIndicator?.dispose()
			alphaIndicator?.dispose()

			background = addChild(0, it.background(this))

			hueSaturationIndicator = addChild(it.hueSaturationIndicator(this))
			hueSaturationIndicator!!.interactivityMode = InteractivityMode.NONE

			valueIndicator = addChild(it.sliderArrow(this))
			valueIndicator!!.interactivityMode = InteractivityMode.NONE
			valueIndicator?.visible = showValuePicker

			alphaIndicator = addChild(it.sliderArrow(this))
			alphaIndicator!!.interactivityMode = InteractivityMode.NONE
			alphaIndicator?.visible = showAlphaPicker
		}

		validation.addNode(COLORS, ValidationFlags.STYLES, ValidationFlags.LAYOUT, ::updateColors)
	}

	private fun updateColors() {
		tmpHSV.set(value)
		tmpHSV.v = 1f
		tmpHSV.a = 1f
		valueRect.style.linearGradient = LinearGradient(GradientDirection.BOTTOM,
				tmpHSV.toRgb(tmpColor).copy(),
				Color(0f, 0f, 0f, 1f)
		)
		alphaRect.style.linearGradient = LinearGradient(GradientDirection.BOTTOM,
				tmpHSV.toRgb(),
				Color(0f, 0f, 0f, 0f)
		)

	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val s = style
		val padding = s.padding

		val numSliders = showValuePicker.toInt() + showAlphaPicker.toInt()
		val w = explicitWidth ?: s.defaultPaletteWidth + numSliders * (s.sliderWidth + s.gap) + padding.left + padding.right
		val h = explicitHeight ?: s.defaultPaletteHeight + padding.top + padding.bottom

		hueRect.setSize(w - padding.right - padding.left - numSliders * (s.sliderWidth + s.gap), h - padding.top - padding.bottom)
		hueRect.moveTo(padding.left, padding.top)
		saturationRect.setSize(hueRect.width, hueRect.height)
		saturationRect.moveTo(hueRect.x, hueRect.y)

		val sliderHeight = h - padding.top - padding.bottom
		valueRect.setSize(s.sliderWidth + handleWidth, sliderHeight)
		valueRect.moveTo(hueRect.right + s.gap - handleWidth, padding.top)

		alphaGrid.setSize(s.sliderWidth, sliderHeight)
		alphaGrid.moveTo(valueRect.right + s.gap, padding.top)
		alphaRect.setSize(s.sliderWidth + handleWidth, sliderHeight)
		alphaRect.moveTo(valueRect.right + s.gap - handleWidth, padding.top)

		hueSaturationIndicator!!.moveTo(saturationRect.x + value.h / 360f * saturationRect.width - hueSaturationIndicator!!.width * 0.5f, saturationRect.y + (1f - value.s) * saturationRect.height - hueSaturationIndicator!!.height * 0.5f)
		valueIndicator!!.moveTo(valueRect.x + handleWidth - valueIndicator!!.width * 0.5f, (1f - value.v) * sliderHeight + padding.top - valueIndicator!!.height * 0.5f)
		alphaIndicator!!.moveTo(alphaRect.x + handleWidth - alphaIndicator!!.width * 0.5f, (1f - value.a) * sliderHeight + padding.top - alphaIndicator!!.height * 0.5f)

		val bg = background!!
		bg.setSize(w, h)
		out.set(bg.width, bg.height)
	}

	companion object : StyleTag {
		private val tmpVec = Vector2()
		private val tmpHSV = Hsv()
		private val tmpColor = Color()

		private const val COLORS = 1 shl 16
	}

}

class ColorPaletteStyle : StyleBase() {

	override val type = Companion

	var padding by prop(Pad(7f))
	var sliderWidth by prop(16f)
	var defaultPaletteWidth by prop(200f)
	var defaultPaletteHeight by prop(100f)
	var gap by prop(5f)
	var background by prop(noSkin)
	var hueSaturationIndicator by prop(noSkin)
	var sliderArrow by prop(noSkin)

	companion object : StyleType<ColorPaletteStyle>
}

/**
 * A Color picker with a text input for a hexdecimal color representation.
 */
open class ColorPickerWithText(owner: Owned) : ContainerImpl(owner) {

	val changed: Signal<() -> Unit>
		get() = colorPicker.changed

	var color: ColorRo
		get() = colorPicker.color
		set(value) {
			colorPicker.color = value
			updateText()
		}

	var value: HsvRo
		get() = colorPicker.value
		set(value) {
			colorPicker.value = value
			updateText()
		}

	/**
	 * If true, there will be a slider input for the color's value component in the HSV color.
	 */
	var showValuePicker: Boolean
		get() = colorPicker.showValuePicker
		set(value) {
			colorPicker.showValuePicker = value
		}

	/**
	 * If true (default), there will be a slider input for the color's alpha.
	 */
	var showAlphaPicker: Boolean
		get() = colorPicker.showAlphaPicker
		set(value) {
			colorPicker.showAlphaPicker = value
		}

	private val textInput: TextInputImpl = textInput {
		restrictPattern = RestrictPatterns.COLOR
		visible = false
		changed.add {
			val c = text.toColorOrNull()
			if (c != null) {
				colorPicker.userChange(c)
				updateText()
				closeTextEditor()
			}
		}
	}

	private val text = text("") {
		focusEnabled = true
		selectable = false
		cursor(StandardCursors.HAND)
	}

	private val colorPicker: ColorPicker = colorPicker {
		changed.add {
			updateText()
		}
	}

	private val hGroup = addChild(hGroup {
		style.verticalAlign = VAlign.BASELINE
		+colorPicker
		+textInput
		+text
	})

	init {
		styleTags.add(Companion)
		colorPicker.focusEnabled = false
		colorPicker.focusEnabledChildren = false

		keyDown().add {
			if (it.isEnterOrReturn && isFocusedSelf) {
				it.handled = true
				openTextEditor()
			}
		}
		text.focused().add {
			openTextEditor()
		}
		textInput.blurred().add {
			closeTextEditor()
		}
	}

	fun openTextEditor() {
		if (textInput.visible) return
		textInput.visible = true
		text.visible = false
		textInput.focus()
	}

	fun closeTextEditor() {
		if (!textInput.visible) return
		textInput.visible = false
		text.visible = true
//		if (textInput.isFocusedSelf)
//			this@ColorPickerWithText.focusSelf()
	}

	fun open() = colorPicker.open()

	fun close() = colorPicker.close()

	fun toggleOpen() = colorPicker.toggleOpen()

	private fun updateText() {
		val str = "#" + color.toRgbaString()
		textInput.text = str
		text.text = str
	}

	private val pad = Pad()

	override fun updateStyles() {
		super.updateStyles()
		textInput.validate(ValidationFlags.STYLES)
		val textInputStyle = textInput.textInputStyle
		text.flowStyle.padding = pad.set(top = textInputStyle.margin.top + textInputStyle.padding.top, right = 0f, bottom = textInputStyle.margin.bottom + textInputStyle.padding.bottom, left = 0f)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		hGroup.setSize(explicitWidth, explicitHeight)
		out.set(hGroup.bounds)
	}

	companion object : StyleTag
}

inline fun Owned.colorPickerWithText(init: ComponentInit<ColorPickerWithText> = {}): ColorPickerWithText  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = ColorPickerWithText(this)
	c.init()
	return c
}
