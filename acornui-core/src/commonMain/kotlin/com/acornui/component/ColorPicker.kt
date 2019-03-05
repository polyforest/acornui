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
	private val colorSwatch: Rect
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
			colorSwatch.style.backgroundColor = v
		}

	var value: HsvRo
		get() = colorPalette.value
		set(value) {
			colorPalette.value = value
			colorSwatch.style.backgroundColor = value.toRgb(tmpColor).copy()
		}

	private val tmpColor = Color()

	init {
		focusEnabled = true
		styleTags.add(ColorPicker)

		click().add {
			toggleOpen()
		}

		colorSwatch = addChild(rect {
			styleTags.add(COLOR_SWATCH_STYLE)
			interactivityMode = InteractivityMode.NONE
			style.backgroundColor = color
		})

		colorPalette.changed.add {
			colorSwatch.style.backgroundColor = value.toRgb(tmpColor).copy()
		}

		watch(style) {
			background?.dispose()
			background = addChild(0, it.background(this))
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


	fun open() { isOpen = true }
	fun close() { isOpen = false }
	fun toggleOpen() { isOpen = !isOpen }

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val s = style
		val padding = s.padding
		val w = explicitWidth ?: s.defaultSwatchWidth+padding.left+padding.right
		val h = explicitHeight ?: s.defaultSwatchHeight+padding.top+padding.bottom

		colorSwatch.setSize(padding.reduceWidth(w), padding.reduceHeight(h))
		colorSwatch.moveTo(padding.left, padding.top)

		background!!.setSize(w, h)
		out.set(w, h)

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
	var defaultSwatchWidth by prop(20f)
	var defaultSwatchHeight by prop(20f)

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

	var showAlphaPicker by observable(false) {
		alphaRect.visible = it
		alphaValueIndicator?.visible = it
		invalidateLayout()
	}

	private var background: UiComponent? = null
	private var hueSaturationIndicator: UiComponent? = null
	private var saturationValueIndicator: UiComponent? = null
	private var alphaValueIndicator: UiComponent? = null

	val hueRect = addChild(rect {
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

	val saturationRect = addChild(rect {
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

	val valueRect = addChild(rect {
		includeInLayout = false
		dragAttachment(0f).drag.add {
			canvasToLocal(tmpVec.set(it.position))
			val p = MathUtils.clamp(tmpVec.y / height, 0f, 1f)

			tmpHSV.set(_value)
			tmpHSV.v = 1f - p
			userChange(tmpHSV)
		}
	})

	val alphaRect = addChild(rect {
		includeInLayout = false
		visible = false
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
			saturationValueIndicator?.dispose()
			alphaValueIndicator?.dispose()

			background = addChild(0, it.background(this))

			hueSaturationIndicator = addChild(it.hueSaturationIndicator(this))
			hueSaturationIndicator!!.interactivityMode = InteractivityMode.NONE

			saturationValueIndicator = addChild(it.valueIndicator(this))
			saturationValueIndicator!!.interactivityMode = InteractivityMode.NONE

			alphaValueIndicator = addChild(it.valueIndicator(this))
			alphaValueIndicator!!.interactivityMode = InteractivityMode.NONE
			alphaValueIndicator?.visible = showAlphaPicker
		}

		validation.addNode(COLORS, ValidationFlags.STYLES, ValidationFlags.LAYOUT, this::validateColors)
	}

	private fun validateColors() {
		tmpHSV.set(_value)
		tmpHSV.v = 1f
		valueRect.style.linearGradient = LinearGradient(GradientDirection.BOTTOM,
				tmpHSV.toRgb(tmpColor).copy(),
				Color(0f, 0f, 0f, 1f)
		)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val s = style
		val padding = s.padding

		val w = explicitWidth ?: s.defaultPaletteWidth+s.brightnessWidth+s.gap+padding.left+padding.right
		val h = explicitHeight ?: s.defaultPaletteHeight+padding.top+padding.bottom

		hueRect.setSize(w - padding.right - padding.left - s.gap - s.brightnessWidth, h - padding.top - padding.bottom)
		hueRect.moveTo(padding.left, padding.top)
		saturationRect.setSize(hueRect.width, hueRect.height)
		saturationRect.moveTo(hueRect.x, hueRect.y)

		val satHeight = h - padding.top - padding.bottom
		valueRect.setSize(s.brightnessWidth, satHeight)
		valueRect.moveTo(padding.left + hueRect.width + s.gap, padding.top)

		hueSaturationIndicator!!.moveTo(saturationRect.x + _value.h / 360f * saturationRect.width - hueSaturationIndicator!!.width * 0.5f, saturationRect.y + (1f - _value.s) * saturationRect.height - hueSaturationIndicator!!.height * 0.5f)

		saturationValueIndicator!!.moveTo(valueRect.x - saturationValueIndicator!!.width * 0.5f, (1f - _value.v) * satHeight + padding.top - saturationValueIndicator!!.height * 0.5f)

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
	var brightnessWidth by prop(15f)
	var defaultPaletteWidth by prop(200f)
	var defaultPaletteHeight by prop(100f)
	var gap by prop(5f)
	var background by prop(noSkin)
	var hueSaturationIndicator by prop(noSkin)
	var valueIndicator by prop(noSkin)

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