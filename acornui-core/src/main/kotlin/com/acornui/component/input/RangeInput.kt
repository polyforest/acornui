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
import com.acornui.css.cssVar
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import com.acornui.frame
import com.acornui.signal.once
import com.acornui.skins.Theme
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
		(addCssToHead(
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
	background: linear-gradient(to right, ${cssVar(Theme::disabled)} var(--percent), ${cssVar(Theme::disabledInner)} var(--percent));
	border-color: ${cssVar(Theme::borderDisabled)};
}

$rangeInput::-webkit-slider-runnable-track {
	width: 100%;
	height: var(--trackHeight);
	cursor: pointer;

	background: linear-gradient(to right, ${cssVar(Theme::toggled)} var(--percent), ${cssVar(Theme::toggledInner)} var(--percent));
	border-radius: 8px;
	margin: 6px 3px;
	border: ${cssVar(Theme::borderThickness)} solid ${cssVar(Theme::border)};
	transition: background 0.3s, border-color 0.3s, box-shadow 0.2s;
	box-shadow: ${cssVar(Theme::componentShadow)};
}

$rangeInput:disabled::-webkit-slider-thumb {
	border-color: ${cssVar(Theme::borderDisabled)};
	background: ${cssVar(Theme::disabled)};
}

$rangeInput::-webkit-slider-thumb {
	border: ${cssVar(Theme::borderThickness)} solid ${cssVar(Theme::border)};
	height: var(--thumbSize);
	width: var(--thumbSize);
	border-radius: 50%;
	background: ${cssVar(Theme::toggled)};
	cursor: pointer;
	-webkit-appearance: none;
	margin-top: calc(-1 * (var(--thumbSize) - var(--trackHeight)) * 0.5 - ${cssVar(Theme::borderThickness)});
	transition: background 0.3s, border-color 0.3s, box-shadow 0.2s;
	box-shadow: ${cssVar(Theme::componentShadow)};
}

$rangeInput:hover:not(:disabled)::-webkit-slider-thumb {
	border-color: ${cssVar(Theme::borderHover)};
}

$rangeInput:hover:not(:disabled)::-webkit-slider-runnable-track {
	border-color: ${cssVar(Theme::borderHover)};
}

$rangeInput:disabled::-moz-range-track {
	/** opacity: 0.9; Moz not supported */ 
	cursor: not-allowed;
	background: linear-gradient(to right, ${cssVar(Theme::disabled)} var(--percent), ${cssVar(Theme::disabledInner)} var(--percent));
	border-color: ${cssVar(Theme::borderDisabled)};
}

$rangeInput::-moz-range-track {
	width: 100%;
	height: var(--trackHeight);
	cursor: pointer;

	background: linear-gradient(to right, ${cssVar(Theme::toggled)} var(--percent), ${cssVar(Theme::toggledInner)} var(--percent));
	border-radius: 8px;
	margin: 6px 3px;
	border: ${cssVar(Theme::borderThickness)} solid ${cssVar(Theme::border)};
	transition: background 0.3s, border-color 0.3s, box-shadow 0.2s;
	box-shadow: ${cssVar(Theme::componentShadow)};
}

$rangeInput:disabled::-moz-range-thumb {
	border-color: ${cssVar(Theme::borderDisabled)};
	background: ${cssVar(Theme::disabled)};
}

$rangeInput::-moz-range-thumb {
	border: ${cssVar(Theme::borderThickness)} solid ${cssVar(Theme::border)};
	height: var(--thumbSize);
	width: var(--thumbSize);
	border-radius: 50%;
	background: ${cssVar(Theme::toggled)};
	cursor: pointer;
	-webkit-appearance: none;
	margin-top: calc(-1 * (var(--thumbSize) - var(--trackHeight)) * 0.5 - ${cssVar(Theme::borderThickness)});
	transition: background 0.3s, border-color 0.3s, box-shadow 0.2s;
	box-shadow: ${cssVar(Theme::componentShadow)};
}

$rangeInput:hover:not(:disabled)::-moz-range-track {
	border-color: ${cssVar(Theme::borderHover)};
}

$rangeInput:hover:not(:disabled)::-moz-range-thumb {
	border-color: ${cssVar(Theme::borderHover)};
}				
			"""
		))
	}
}

inline fun Context.rangeInput(init: ComponentInit<RangeInput> = {}): RangeInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return RangeInput(this).apply(init)
}
