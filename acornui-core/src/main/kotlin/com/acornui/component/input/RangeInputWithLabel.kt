/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.component.input

import com.acornui.component.ComponentInit
import com.acornui.component.Div
import com.acornui.component.layout.LayoutStyles
import com.acornui.component.style.cssClass
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.dom.addStyleToHead
import com.acornui.formatters.NumberFormatter
import com.acornui.formatters.StringFormatter
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class RangeInputWithLabel(owner: Context) : DivWithInputComponent(owner) {

	override val inputComponent: RangeInput = addChild(rangeInput())
	protected val labelComponent = addChild(text())

	var formatter: StringFormatter<Number?> = NumberFormatter()

	init {
		addClass(LayoutStyles.hGroup)
		addClass(RangeInputWithLabelStyle.rangeInputWithLabel)
		input.listen {
			refreshLabel()
		}
	}

	var valueAsNumber: Double
		get() = inputComponent.valueAsNumber
		set(value) {
			inputComponent.valueAsNumber = value
			refreshLabel()
		}

	/**
	 * Returns / Sets the default value as originally specified in the HTML that created this object.
	 */
	var defaultValue: Double?
		get() = inputComponent.defaultValue
		set(value) {
			inputComponent.defaultValue = value
			refreshLabel()
		}

	var min: Double
		get() = inputComponent.min
		set(value) {
			inputComponent.min = value
			refreshLabel()
		}

	var max: Double
		get() = inputComponent.max
		set(value) {
			inputComponent.max = value
			refreshLabel()
		}

	var step: Double
		get() = inputComponent.step
		set(value) {
			inputComponent.step = value
			refreshLabel()
		}

	/**
	 * Returns/sets the percentage between min (0.0) and max (1.0).
	 */
	var percent: Double
		get() = inputComponent.percent
		set(value) {
			inputComponent.percent = value
			refreshLabel()
		}

	open fun refreshLabel() {
		labelComponent.text = formatter.format(valueAsNumber)
	}

}

object RangeInputWithLabelStyle {

	val rangeInputWithLabel by cssClass()

	init {
		addStyleToHead(
			"""
$rangeInputWithLabel {

}				
		"""
		)
	}
}

inline fun Context.rangeInputWithLabel(init: ComponentInit<RangeInputWithLabel> = {}): RangeInputWithLabel {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return RangeInputWithLabel(this).apply(init)
}