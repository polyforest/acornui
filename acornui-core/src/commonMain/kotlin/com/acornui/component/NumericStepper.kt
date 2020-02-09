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

import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.styleTag
import com.acornui.component.text.RestrictPatterns
import com.acornui.component.text.textInput
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.input.Ascii
import com.acornui.input.interaction.enableDownRepeat
import com.acornui.input.keyDown
import com.acornui.input.mouseDown
import com.acornui.math.Bounds
import com.acornui.math.MathUtils.clamp
import com.acornui.math.MathUtils.roundToNearest
import com.acornui.math.fractionDigits
import com.acornui.reflect.afterChange
import com.acornui.signal.Signal1
import com.acornui.text.NumberFormatter
import com.acornui.text.numberFormatter
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class NumericStepper(owner: Context) : ElementContainerImpl<UiComponent>(owner) {

	val style = bind(NumericStepperStyle())

	private val _changed = own(Signal1<NumericStepper>())

	/**
	 * The user has changed this stepper's value.
	 * This will not be dispatched on a programmatic change, only user input.
	 */
	val changed = _changed.asRo()

	val formatter: NumberFormatter = numberFormatter().apply {
		useGrouping = false
	}

	/**
	 * Sets the step size of the up and down buttons.
	 */
	var step: Float by afterChange(1f) {
		refreshFormatter()
	}

	/**
	 * Sets the smallest fraction the user can set as a value.
	 */
	var minStep: Float by afterChange(0.01f) {
		refreshFormatter()
	}

	/**
	 * When stepping, offset is added before rounding to the nearest [step].
	 * Setting this will also set the formatter fraction digits.
	 */
	var offset: Float by afterChange(0f) {
		refreshFormatter()
	}

	private fun refreshFormatter() {
		val fractionDigits = maxOf(offset.fractionDigits, minStep.fractionDigits)
		formatter.minFractionDigits = step.fractionDigits
		formatter.maxFractionDigits = fractionDigits
		invalidateProperties()
	}

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

	private val textInput = +textInput {
		restrictPattern = RestrictPatterns.FLOAT
		changed.add {
			val newValue = text.toFloatOrNull() ?: 0f
			userChange(newValue)
		}
	}

	private val stepUpButton = +button {
		styleTags.add(STEP_UP_STYLE)
		mouseDown().add {
			if (!it.handled) {
				it.handled = true
				userChange(value + step)
			}
		}
		enableDownRepeat()
	}

	private val stepDownButton = +button {
		styleTags.add(STEP_DOWN_STYLE)
		mouseDown().add {
			if (!it.handled) {
				it.handled = true
				userChange(value - step)
			}
		}
		enableDownRepeat()
	}

	private var _value: Float = 0f

	var value: Float
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

		styleTags.add(NumericStepper)
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
	fun userChange(value: Float, min: Float = this.min, max: Float = this.max) {
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

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val hGap = style.hGap
		val vGap = style.vGap
		val stepperWidths = maxOf(stepUpButton.width, stepDownButton.width)

		val textWidth = if (explicitWidth == null) null else explicitWidth - hGap - stepperWidths
		textInput.setSize(textWidth, explicitHeight)
		val stepperHeight = (textInput.height - vGap) * 0.5f
		stepDownButton.height(stepperHeight)
		stepUpButton.height(stepperHeight)
		val stepperHeights = stepUpButton.height + vGap + stepDownButton.height

		out.height = maxOf(stepperHeights, textInput.height)
		out.width = textInput.width + hGap + stepperWidths
		out.baseline = textInput.baselineY

		val tIW = textInput.width
		stepUpButton.setPosition(tIW + hGap, 0f)
		stepDownButton.setPosition(tIW + hGap, stepUpButton.height + vGap)
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

class NumericStepperStyle : StyleBase() {

	override val type: StyleType<NumericStepperStyle> = NumericStepperStyle

	var hGap by prop(0f)
	var vGap by prop(0f)

	companion object : StyleType<NumericStepperStyle>
}

inline fun Context.numericStepper(init: ComponentInit<NumericStepper> = {}): NumericStepper  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val c = NumericStepper(this)
	c.init()
	return c
}
