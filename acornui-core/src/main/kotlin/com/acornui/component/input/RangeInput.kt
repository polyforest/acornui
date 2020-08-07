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
import com.acornui.component.style.cssClass
import com.acornui.di.Context
import com.acornui.dom.addStyleToHead
import com.acornui.frame
import com.acornui.signal.once
import com.acornui.skins.CssProps
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class RangeInput(owner: Context) : InputImpl(owner, "range") {

	init {
		addClass(RangeInputStyle.rangeInput)
		input.listen {
			refreshPercentCss()
		}
		formReset.listen {
			frame.once {
				refreshPercentCss()
			}
		}
		refreshPercentCss()
	}

	var valueAsNumber: Double
		get() = dom.valueAsNumber
		set(value) {
			dom.valueAsNumber = value
			refreshPercentCss()
		}

	/**
	 * Returns / Sets the default value as originally specified in the HTML that created this object.
	 */
	var defaultValue: Double?
		get() = dom.defaultValue.toDoubleOrNull()
		set(value) {
			if (value == null)
				dom.attributes.removeNamedItem("defaultValue")
			else
				dom.defaultValue = value.toString()
		}

	var min: Double
		get() = dom.min.toDoubleOrNull() ?: 0.0
		set(value) {
			dom.min = value.toString()
			refreshPercentCss()
		}

	var max: Double
		get() = dom.max.toDoubleOrNull() ?: 100.0
		set(value) {
			dom.max = value.toString()
			refreshPercentCss()
		}

	var step: Double
		get() = dom.step.toDoubleOrNull() ?: 1.0
		set(value) {
			dom.step = value.toString()
		}

	/**
	 * Returns/sets the percentage between min (0.0) and max (1.0).
	 */
	var percent: Double
		get() {
			if (min >= max) return 0.0
			return (valueAsNumber - min) / (max - min)
		}
		set(value) {
			valueAsNumber = value * (max - min) + min
		}

	private fun refreshPercentCss() {
		style.setProperty("--percent", "${percent * 100}%")
	}

	override fun clear() {
		super.clear()
		refreshPercentCss()
	}


}

object RangeInputStyle {

	val rangeInput by cssClass()

	init {
		@Suppress("CssUnresolvedCustomProperty", "CssInvalidPropertyValue")
		(addStyleToHead(
			"""

$rangeInput {
	--percent: 0%;
	--trackHeight: 8px;
	--thumbSize: 16px;
	-webkit-appearance: none;
	border-radius: 4px;
	background: transparent;
}

$rangeInput:disabled {
	background: transparent;
}

$rangeInput:disabled::-webkit-slider-runnable-track {
	opacity: 0.9;
	cursor: not-allowed;
	background: linear-gradient(to right, ${CssProps.disabled.v} var(--percent), ${CssProps.disabledInner.v} var(--percent));
	border-color: ${CssProps.borderDisabled.v};
}

$rangeInput::-webkit-slider-runnable-track {
	width: 100%;
	height: var(--trackHeight);
	cursor: pointer;

	background: linear-gradient(to right, ${CssProps.toggled.v} var(--percent), ${CssProps.toggledInner.v} var(--percent));
	border-radius: 8px;
	margin: 6px 3px;
	border: ${CssProps.borderThickness.v} solid ${CssProps.borderColor.v};
	transition: background 0.3s, border-color 0.3s, box-shadow 0.2s;
	box-shadow: ${CssProps.componentShadow.v};
}

$rangeInput:disabled::-webkit-slider-thumb {
	border-color: ${CssProps.borderDisabled.v};
	background: ${CssProps.disabled.v};
}

$rangeInput::-webkit-slider-thumb {
	border: ${CssProps.borderThickness.v} solid ${CssProps.borderColor.v};
	height: var(--thumbSize);
	width: var(--thumbSize);
	border-radius: 50%;
	background: ${CssProps.toggled.v};
	cursor: pointer;
	-webkit-appearance: none;
	margin-top: calc(-1 * (var(--thumbSize) - var(--trackHeight)) * 0.5 - ${CssProps.borderThickness.v});
	transition: background 0.3s, border-color 0.3s, box-shadow 0.2s;
	box-shadow: ${CssProps.componentShadow.v};
}

$rangeInput:hover:not(:disabled)::-webkit-slider-thumb {
	border-color: ${CssProps.borderHover.v};
}

$rangeInput:hover:not(:disabled)::-webkit-slider-runnable-track {
	border-color: ${CssProps.borderHover.v};
}

$rangeInput:disabled::-moz-range-track {
	/** opacity: 0.9; Moz not supported */ 
	cursor: not-allowed;
	background: linear-gradient(to right, ${CssProps.disabled.v} var(--percent), ${CssProps.disabledInner.v} var(--percent));
	border-color: ${CssProps.borderDisabled.v};
}

$rangeInput::-moz-range-track {
	width: 100%;
	height: var(--trackHeight);
	cursor: pointer;

	background: linear-gradient(to right, ${CssProps.toggled.v} var(--percent), ${CssProps.toggledInner.v} var(--percent));
	border-radius: 8px;
	margin: 6px 3px;
	border: ${CssProps.borderThickness.v} solid ${CssProps.borderColor.v};
	transition: background 0.3s, border-color 0.3s, box-shadow 0.2s;
	box-shadow: ${CssProps.componentShadow.v};
}

$rangeInput:disabled::-moz-range-thumb {
	border-color: ${CssProps.borderDisabled.v};
	background: ${CssProps.disabled.v};
}

$rangeInput::-moz-range-thumb {
	border: ${CssProps.borderThickness.v} solid ${CssProps.borderColor.v};
	height: var(--thumbSize);
	width: var(--thumbSize);
	border-radius: 50%;
	background: ${CssProps.toggled.v};
	cursor: pointer;
	-webkit-appearance: none;
	margin-top: calc(-1 * (var(--thumbSize) - var(--trackHeight)) * 0.5 - ${CssProps.borderThickness.v});
	transition: background 0.3s, border-color 0.3s, box-shadow 0.2s;
	box-shadow: ${CssProps.componentShadow.v};
}

$rangeInput:hover:not(:disabled)::-moz-range-track {
	border-color: ${CssProps.borderHover.v};
}

$rangeInput:hover:not(:disabled)::-moz-range-thumb {
	border-color: ${CssProps.borderHover.v};
}				
			"""
		))
	}
}

inline fun Context.rangeInput(init: ComponentInit<RangeInput> = {}): RangeInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return RangeInput(this).apply(init)
}
