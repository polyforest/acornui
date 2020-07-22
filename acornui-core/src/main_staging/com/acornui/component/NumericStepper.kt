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

import com.acornui.component.style.ObservableBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.styleTag
import com.acornui.component.text.RestrictPatterns
import com.acornui.component.text.textInput
import com.acornui.di.Context

import com.acornui.input.Ascii
import com.acornui.input.interaction.enableDownRepeat
import com.acornui.input.keyDown
import com.acornui.input.mouseDown
import com.acornui.math.Bounds
import com.acornui.math.clamp
import com.acornui.math.roundToNearest
import com.acornui.math.fractionDigits
import com.acornui.properties.afterChange
import com.acornui.signal.Signal1
import com.acornui.text.NumberFormatter
import com.acornui.text.numberFormatter
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class NumericStepper(owner: Context) : ElementContainerImpl<UiComponent>(owner), InputComponent<Double> {

	val style = bind(NumericStepperStyle())

	private val _changed = own(Signal1<NumericStepper>())

	/**
	 * The user has changed this stepper's value.
	 * This will not be dispatched on a programmatic change, only user input.
	 */
	override val changed = _changed.asRo()

	val formatter: NumberFormatter = numberFormatter().apply {
		useGrouping = false
	}

	/**
	 * Sets the step size of the up and down buttons.
	 */
	var step: Double by afterChange(1.0) {
		refreshFormatter()
	}

	/**
	 * Sets the smallest fraction the user can set as a value.
	 */
	var minStep: Double by afterChange(0.01) {
		refreshFormatter()
	}

	/**
	 * When stepping, offset is added before rounding to the nearest [step].
	 * Setting this will also set the formatter fraction digits.
	 */
	var offset: Double by afterChange(0.0) {
		refreshFormatter()
	}

	private fun refreshFormatter() {
		val fractionDigits = maxOf(offset.fractionDigits, minStep.fractionDigits)
		formatter.minFractionDigits = step.fractionDigits
		formatter.maxFractionDigits = fractionDigits
		invalidateProperties()
	}

	private var _max: Double = Double.POSITIVE_INFINITY

	/**
	 * The maximum value this stepper will allow.
	 */
	var max: Double
		get() = _max
		set(newMax) {
			if (newMax == _max) return
			_max = newMax
			if (_value > _max) {
				this.value = _max
			}
		}

	private var _min: Double = Double.NEGATIVE_INFINITY

	/**
	 * The minimum value this stepper will allow.
	 */
	var min: Double
		get() = _min
		set(newMin) {
			if (newMin == _min) return
			_min = newMin
			if (_value < _min) {
				this.value = _min
			}
		}

	private val textInput = +textInput {
		restrictPattern = RestrictPatterns.FLOAT
		changed.add {
			val newValue = text.toFloatOrNull() ?: 0.0
			userChange(newValue)
		}
	}

	private val stepUpButton = +button {
		addClass(STEP_UP_STYLE)
		mouseDown().add {
			if (!it.handled) {
				it.handled = true
				userChange(value + step)
			}
		}
		enableDownRepeat()
	}

	private val stepDownButton = +button {
		addClass(STEP_DOWN_STYLE)
		mouseDown().add {
			if (!it.handled) {
				it.handled = true
				userChange(value - step)
			}
		}
		enableDownRepeat()
	}

	private var _value: Double = 0.0

	override var value: Double
		get() = _value
		set(value) {
			val oldValue = _value
			val newValue = roundToNearest(clamp(value, min, max), minStep, offset)
			if (oldValue == newValue) return
			_value = newValue
			invalidateProperties()
		}

	/**
	 * Sets the max character length of the input.
	 */
	var maxLength: Int?
		get() = textInput.maxLength
		set(value) {
			textInput.maxLength = value
		}

	init {
		validation.addNode(ValidationFlags.PROPERTIES, 0, ValidationFlags.LAYOUT, ::updateProperties)
		isFocusContainer = true

		addClass(NumericStepper)
		keyDown().add { e ->
			if (!e.handled) {
				if (e.keyCode == Ascii.UP) {
					e.handled = true
					userChange(value + step)
				} else if (e.keyCode == Ascii.DOWN) {
					e.handled = true
					userChange(value - step)
				}
			}
		}

		refreshFormatter()
	}

	/**
	 * Sets the value and dispatches a [changed] signal.
	 * @param value The new numeric value.
	 * @param min The min clamp value. Default is [NumericStepper.min]
	 * @param max The max clamp value. Default is [NumericStepper.max]
	 */
	fun userChange(value: Double, min: Double = this.min, max: Double = this.max) {
		val oldValue = _value
		val newValue = roundToNearest(clamp(value, min, max), minStep, offset)
		invalidateProperties() // When the user has committed a value, re-format.
		if (oldValue == newValue) return
		this.min = min
		this.max = max
		this.value = newValue
		_changed.dispatch(this)
	}

	/**
	 * Sets this stepper's text input's default width to fit the character 'M' repeated [textLength] times.
	 * If this stepper has been given an explicit width, or a [defaultWidth], this will have no effect.
	 */
	fun setSizeToFit(textLength: Int) = textInput.setSizeToFit(textLength)

	/**
	 * Sets this text input's default width to fit the given text line.
	 * If this stepper has been given an explicit width, or a [defaultWidth], this will have no effect.
	 */
	fun setSizeToFit(text: String?) = textInput.setSizeToFit(text)

	private fun updateProperties() {
		textInput.text = formatter.format(value)
		stepUpButton.disabled = value >= _max
		stepDownButton.disabled = value <= _min
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val hGap = style.hGap
		val vGap = style.vGap
		val stepperWidths = maxOf(stepUpButton.width, stepDownButton.width)

		val textWidth = if (explicitWidth == null) null else explicitWidth - hGap - stepperWidths
		textInput.size(textWidth, explicitHeight)
		val stepperHeight = (textInput.height - vGap) * 0.5
		stepDownButton.height(stepperHeight)
		stepUpButton.height(stepperHeight)
		val stepperHeights = stepUpButton.height + vGap + stepDownButton.height

		out.height = maxOf(stepperHeights, textInput.height)
		out.width = textInput.width + hGap + stepperWidths
		out.baseline = textInput.baselineY

		val tIW = textInput.width
		stepUpButton.position(tIW + hGap, 0.0)
		stepDownButton.position(tIW + hGap, stepUpButton.height + vGap)
	}

	override fun dispose() {
		super.dispose()
		_changed.dispose()
	}

	companion object : StyleTag {
		val STEP_UP_STYLE = styleTag()
		val STEP_DOWN_STYLE = styleTag()
	}
}

class NumericStepperStyle : ObservableBase() {

	override val type: StyleType<NumericStepperStyle> = NumericStepperStyle

	var hGap by prop(0.0)
	var vGap by prop(0.0)

	companion object : StyleType<NumericStepperStyle>
}

inline fun Context.numericStepper(init: ComponentInit<NumericStepper> = {}): NumericStepper  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = NumericStepper(this)
	c.init()
	return c
}
