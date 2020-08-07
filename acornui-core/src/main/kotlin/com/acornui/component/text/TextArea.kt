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

package com.acornui.component.text

import com.acornui.component.ComponentInit
import com.acornui.component.UiComponentImpl
 import com.acornui.component.style.cssClass
import com.acornui.css.cssVar
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import com.acornui.dom.createElement
import com.acornui.input.ChangeSignal
import com.acornui.observe.Observable
import com.acornui.properties.afterChange
import com.acornui.recycle.Clearable
import com.acornui.signal.Signal
import com.acornui.signal.event
import com.acornui.skins.Theme
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.InputEvent
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class TextArea(owner: Context) : UiComponentImpl<HTMLTextAreaElement>(owner, createElement("textarea")),
	Clearable, Observable {

	/**
	 * Dispatched on each input character.
	 * Note - this does not invoke when the text is programmatically changed.
	 */
	val input: Signal<InputEvent>
		get() = event("input")


	/**
	 * Dispatched on value commit.
	 * This is only dispatched on a user interaction, such as pressing ENTER or TAB. It is not dispatched when
	 * the text is programmatically changed.
	 */
	override val changed = ChangeSignal(this)

	init {
		addClass(TextAreaStyle.textArea)
	}

	var readOnly: Boolean
		get() = dom.readOnly
		set(value) {
			dom.readOnly = value
		}

	/**
	 * The placeholder attribute is a string that provides a brief hint to the user as to what kind of information is
	 * expected in the field.
	 *
	 * It should be a word or short phrase that demonstrates the expected type of data, rather than an explanatory
	 * message. The text must not include carriage returns or line feeds.
	 */
	var placeholder: String
		get() = dom.placeholder
		set(value) {
			dom.placeholder = value
		}

	/**
	 * Returns / Sets the element's minlength attribute, containing the minimum number of characters (in Unicode code
	 * points) that the value can have. (If you set this to a negative number, an exception will be thrown.)
	 */
	var minLength: Int
		get() = dom.minLength
		set(value) {
			dom.minLength = value
		}

	/**
	 * Returns / Sets the element's maxlength attribute, containing the maximum number of characters (in Unicode code
	 * points) that the value can have. (If you set this to a negative number, an exception will be thrown.)
	 */
	var maxLength: Int
		get() = dom.maxLength
		set(value) {
			dom.maxLength = value
		}

	/**
	 * Returns / Sets the current value of the control.
	 */
	var value: String
		get() = dom.textContent ?: ""
		set(value) {
			dom.textContent = value
		}

	/**
	 * Returns / Sets the default value as originally specified in the HTML that created this object.
	 */
	var defaultValue: String
		get() = dom.defaultValue
		set(value) {
			dom.defaultValue = value
		}

	var disabled: Boolean by afterChange(false) {
		dom.disabled = it
	}

	/**
	 * Clears this input to its default value.
	 */
	override fun clear() {
		value = defaultValue
	}

}

object TextAreaStyle {

	val textArea by cssClass()

	init {
		addCssToHead("""
$textArea {
	font: inherit;
	color: ${cssVar(Theme::inputTextColor)};;
	border-width: ${cssVar(Theme::borderThickness)};;
	border-color: ${cssVar(Theme::border)};
	border-radius: ${cssVar(Theme::inputBorderRadius)};
	padding: ${cssVar(Theme::inputPadding)};
	background: ${cssVar(Theme::inputBackground)};
	box-shadow: ${cssVar(Theme::componentShadow)};
}
			
$textArea:active {
	border-color: ${cssVar(Theme::borderActive)};
}

$textArea:disabled {
	background: ${cssVar(Theme::disabledInner)};
	border-color: ${cssVar(Theme::disabled)};
	color: ${cssVar(Theme::toggledInnerDisabled)};
	pointer-events: none;
	opacity: ${cssVar(Theme::disabledOpacity)};
}
			""")
	}
}

inline fun Context.textArea(init: ComponentInit<TextArea> = {}): TextArea {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextArea(this)
	t.init()
	return t
}