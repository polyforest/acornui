package com.acornui.component

import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.algorithm.BasicLayoutData
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.styleTag
import com.acornui.component.text.textInput
import com.acornui.core.di.Owned
import com.acornui.core.input.Ascii
import com.acornui.core.input.interaction.KeyInteractionRo
import com.acornui.core.input.interaction.enableDownRepeat
import com.acornui.core.input.keyDown
import com.acornui.core.input.mouseDown
import com.acornui.core.text.StringFormatter
import com.acornui.core.text.ToStringFormatter
import com.acornui.math.Bounds
import com.acornui.math.MathUtils
import com.acornui.signal.Cancel
import com.acornui.signal.Signal3
import com.acornui.signal.Signal4

class NumericStepper(owner: Owned) : ElementContainerImpl<UiComponent>(owner) {

	val style = bind(NumericStepperStyle())

	val userChanged = Signal4<NumericStepper, Float, Float, Cancel>()
	val changed = Signal3<NumericStepper, Float, Float>()

	private var _formatter: StringFormatter<Float> = ToStringFormatter
	var formatter: StringFormatter<Float>
		get() = _formatter
		set(value) {
			_formatter = value
			textInput.text = value.format(this.value)
		}

	var step: Float = 1f

	private var _max: Float = Float.POSITIVE_INFINITY

	/**
	 * The maximum value this stepper will allow.
	 */
	var max: Float
		get() = _max
		set(newMax) {
			if (newMax == _max) return
			_max = newMax
			if (_value > _max) {
				this.value = _max
			}
			textInput.maxLength = newMax.toString().length
		}

	private var _min: Float = Float.NEGATIVE_INFINITY

	/**
	 * The minimum value this stepper will allow.
	 */
	var min: Float
		get() = _min
		set(newMin) {
			if (newMin == _min) return
			_min = newMin
			if (_value < _min) {
				this.value = _min
			}
		}

	val textInput = +textInput {
		changed.add {
			val newValue = text.toFloatOrNull() ?: 0f
			userChange(newValue)
		}
	}

	val stepUpButton = +button {
		styleTags.add(STEP_UP_STYLE)
		mouseDown().add {
			userChange(value + step)
		}
		enableDownRepeat()
	}

	val stepDownButton = +button {
		styleTags.add(STEP_DOWN_STYLE)
		mouseDown().add {
			userChange(value - step)
		}
		enableDownRepeat()
	}

	private var _value: Float = 0f

	var value: Float
		get() = _value
		set(value) {
			val oldValue = _value
			val newValue = MathUtils.clamp(value, _min, _max)
			if (oldValue == newValue) return
			_value = newValue
			changed.dispatch(this, oldValue, newValue)
			invalidate(ValidationFlags.PROPERTIES)
		}

	private val cancel: Cancel = Cancel()

	private val keyDownHandler = {
		e: KeyInteractionRo ->
		if (e.keyCode == Ascii.UP) {
			userChange(value + step)
		} else if (e.keyCode == Ascii.DOWN) {
			userChange(value - step)
		}
	}

	init {
		styleTags.add(NumericStepper)
		keyDown().add(keyDownHandler)
	}

	/**
	 * Sets the value, but first gives an opportunity to observers to cancel via the [userChanged] signal.
	 */
	fun userChange(value: Float) {
		val oldValue = _value
		val newValue = MathUtils.clamp(value, _min, _max)
		if (oldValue == newValue) return
		userChanged.dispatch(this, oldValue, newValue, cancel.reset())
		if (!cancel.canceled()) {
			this.value = newValue
		}
	}

	override fun updateProperties() {
		textInput.text = formatter.format(value)
		stepUpButton.disabled = value >= _max
		stepDownButton.disabled = value <= _min
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		out.height.min = maxOf(textInput.minHeight ?: 0f, ((stepUpButton.height) + style.vGap + (stepDownButton.height)))
		out.width.min = (textInput.minWidth ?: 0f) + style.hGap + maxOf((stepUpButton.width), (stepDownButton.width))
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val hGap = style.hGap
		val vGap = style.vGap
		val stepperWidths = maxOf(stepUpButton.width, stepDownButton.width)
		val stepperHeights = stepUpButton.height + vGap + stepDownButton.height

		val textWidth = if (explicitWidth == null) {
			val layoutData = textInput.layoutData as BasicLayoutData?
			layoutData?.width
		} else {
			explicitWidth - hGap - stepperWidths
		}
		textInput.setSize(textWidth, explicitHeight ?: stepperHeights)

		out.height = maxOf(stepperHeights, textInput.height)
		out.width = textInput.width + hGap + stepperWidths

		val tIW = textInput.width
		stepUpButton.moveTo(tIW + hGap, 0f)
		stepDownButton.moveTo(tIW + hGap, stepUpButton.height + vGap)
	}

	override fun dispose() {
		super.dispose()
		changed.dispose()
	}

	companion object : StyleTag {
		val STEP_UP_STYLE = styleTag()
		val STEP_DOWN_STYLE = styleTag()
	}
}

class NumericStepperStyle : StyleBase() {

	override val type: StyleType<NumericStepperStyle> = NumericStepperStyle

	var hGap by prop(0f)
	var vGap by prop(0f)

	companion object : StyleType<NumericStepperStyle>
}

fun Owned.numericStepper(init: ComponentInit<NumericStepper> = {}): NumericStepper {
	val c = NumericStepper(this)
	c.init()
	return c
}