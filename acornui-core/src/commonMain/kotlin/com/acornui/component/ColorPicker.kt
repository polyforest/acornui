package com.acornui.component

import com.acornui.component.layout.algorithm.HorizontalLayoutContainer
import com.acornui.component.style.*
import com.acornui.component.text.textInput
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.focus.blurred
import com.acornui.core.input.interaction.click
import com.acornui.core.input.interaction.dragAttachment
import com.acornui.core.popup.lift
import com.acornui.core.toInt
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.Hsv
import com.acornui.graphic.HsvRo
import com.acornui.math.*
import com.acornui.reflect.observable
import com.acornui.signal.Signal
import com.acornui.signal.Signal0

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
			val v = value.copy()
			colorPalette.color = v
			colorSwatch?.colorTint = v
		}

	var value: HsvRo
		get() = colorPalette.value
		set(value) {
			colorPalette.value = value
			colorSwatch?.colorTint = value.toRgb(tmpColor).copy()
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

	private var isOpen by observable(false) {
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
		val w = explicitWidth ?: padding.expandWidth2(s.defaultSwatchWidth)
		val h = explicitHeight ?: padding.expandHeight2(s.defaultSwatchHeight)

		colorSwatch.setSize(padding.reduceWidth(w), padding.reduceHeight(h))

		background.setSize(maxOf(padding.expandWidth2(colorSwatch.width), w), maxOf(padding.expandHeight2(colorSwatch.height), h))
		out.set(background.width, background.height)
		colorSwatch.moveTo(0.5f * (out.width - colorSwatch.width), 0.5f * (out.height - colorSwatch.height))

		colorPaletteLift.moveTo(0f, h)
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

	var padding: PadRo by prop(Pad(4f))
	var background by prop(noSkin)
	var defaultSwatchWidth by prop(21f)
	var defaultSwatchHeight by prop(21f)
	var colorSwatch by prop(noSkin)

	companion object : StyleType<ColorPickerStyle>
}

fun Owned.colorPicker(init: ComponentInit<ColorPicker> = {}): ColorPicker {
	val c = ColorPicker(this)
	c.init()
	return c
}

class ColorPalette(owner: Owned) : ContainerImpl(owner) {

	private val _changed = own(Signal0())
	val changed = _changed.asRo()

	val style = bind(ColorPaletteStyle())

	/**
	 * If true, there will be a slider input for the color's value component in the HSV color.
	 */
	var showValuePicker by observable(true) {
		valueRect.visible = it
		valueIndicator?.visible = it
		invalidateLayout()
	}

	/**
	 * If true, there will be a slider input for the color's alpha.
	 */
	var showAlphaPicker by observable(true) {
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
			tmpHSV.set(_value)
			tmpHSV.h = 360f * MathUtils.clamp(tmpVec.x / width, 0f, 1f)
			tmpHSV.s = 1f - MathUtils.clamp(tmpVec.y / height, 0f, 1f)

			userChange(tmpHSV)
		}
	})

	private fun userChange(value: Hsv) {
		val oldValue = _value
		if (oldValue == value) return
		this.value = value
		_changed.dispatch()
	}

	private val valueRect = addChild(rect {
		dragAttachment(0f).drag.add {
			canvasToLocal(tmpVec.set(it.position))
			val p = MathUtils.clamp(tmpVec.y / height, 0f, 1f)

			tmpHSV.set(_value)
			tmpHSV.v = 1f - p
			userChange(tmpHSV)
		}
	})

	private val alphaGrid = addChild(repeatingTexture("assets/uiskin/AlphaCheckerboard.png"))

	private val alphaRect = addChild(rect {
		dragAttachment(0f).drag.add {
			canvasToLocal(tmpVec.set(it.position))
			val p = MathUtils.clamp(tmpVec.y / height, 0f, 1f)

			tmpHSV.set(_value)
			tmpHSV.a = 1f - p
			userChange(tmpHSV)
		}
	})

	private var _value = Color.WHITE.toHsv(Hsv())

	var color: ColorRo
		get() = _value.toRgb(Color())
		set(value) {
			this.value = value.toHsv(Hsv())
		}

	var value: HsvRo
		get() = _value
		set(value) {
			val oldValue = _value
			if (oldValue == value) return
			_value = value.copy()
			invalidate(COLORS)
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

		validation.addNode(COLORS, ValidationFlags.STYLES, ValidationFlags.LAYOUT, this::updateColors)
	}

	private fun updateColors() {
		tmpHSV.set(_value)
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
		valueRect.setSize(s.sliderWidth, sliderHeight)
		valueRect.moveTo(hueRect.right + s.gap, padding.top)

		alphaGrid.setSize(s.sliderWidth, sliderHeight)
		alphaGrid.moveTo(valueRect.right + s.gap, padding.top)
		alphaRect.setSize(s.sliderWidth, sliderHeight)
		alphaRect.moveTo(valueRect.right + s.gap, padding.top)

		hueSaturationIndicator!!.moveTo(saturationRect.x + _value.h / 360f * saturationRect.width - hueSaturationIndicator!!.width * 0.5f, saturationRect.y + (1f - _value.s) * saturationRect.height - hueSaturationIndicator!!.height * 0.5f)
		valueIndicator!!.moveTo(valueRect.x - valueIndicator!!.width * 0.5f, (1f - _value.v) * sliderHeight + padding.top - valueIndicator!!.height * 0.5f)
		alphaIndicator!!.moveTo(alphaRect.x - alphaIndicator!!.width * 0.5f, (1f - _value.a) * sliderHeight + padding.top - alphaIndicator!!.height * 0.5f)

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

	var padding by prop(Pad(5f))
	var sliderWidth by prop(16f)
	var defaultPaletteWidth by prop(200f)
	var defaultPaletteHeight by prop(100f)
	var gap by prop(5f)
	var background by prop(noSkin)
	var hueSaturationIndicator by prop(noSkin)
	var sliderArrow by prop(noSkin)

	companion object : StyleType<ColorPaletteStyle>
}

// TODO
/**
 * A Color picker with a text input for a hexdecimal color representation.
 */
open class ColorPickerWithText(owner: Owned) : HorizontalLayoutContainer(owner) {

	val textInput = +textInput {
		changed.add {

		}
	}

	private val color = Color()

	val colorPicker = +colorPicker {
		changed.add {
			textInput.text = value.toRgb(color.copy()).toRgbString()
		}
	}
}

fun Owned.colorPickerWithText(init: ComponentInit<ColorPicker> = {}): ColorPicker {
	val c = ColorPicker(this)
	c.init()
	return c
}