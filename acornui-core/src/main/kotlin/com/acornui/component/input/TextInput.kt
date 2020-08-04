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

// Documentation by Mozilla Contributors is licensed under CC-BY-SA 2.5.

@file:Suppress("UNUSED_PARAMETER", "unused")

package com.acornui.component.input

import com.acornui.component.ComponentInit
import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import org.intellij.lang.annotations.Language
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class TextInput(owner: Context, type: String = "text") : TextInputBase(owner, type) {

	/**
	 * Returns / Sets the current value of the control.
	 */
	var value: String
		get() = dom.value
		set(value) {
			dom.value = value
		}

	/**
	 * Returns / Sets the default value as originally specified in the HTML that created this object.
	 */
	var defaultValue: String
		get() = dom.defaultValue
		set(value) {
			dom.defaultValue = value
		}
}

abstract class TextInputBase(owner: Context, type: String) : InputUiComponentImpl(owner, type) {

	init {
		addClass(styleTag)
	}

	/**
	 * The pattern attribute, when specified, is a regular expression that the input's value must match in order for
	 * the value to pass constraint validation.
	 */
	val pattern: String
		get() = dom.pattern

	/**
	 * The pattern attribute, when specified, is a regular expression that the input's value must match in order for
	 * the value to pass constraint validation.
	 */
	fun pattern(@Language("regexp") value: String) {
		dom.pattern = value
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
	 * Input mode is an enumerated attribute that hints at the type of data that might be entered by the user while
	 * editing the element or its contents. It can have the following values:
	 * @see InputMode
	 */
	var inputMode: String
		get() = dom.inputMode
		set(value) {
			dom.inputMode = value
		}

	/**
	 * The autocomplete property sets or returns the value of the autocomplete attribute in a form.
	 * When autocomplete is on, the browser automatically complete values based on values that the user has entered
	 * before.
	 *
	 * Tip: It is possible to have autocomplete "on" for the form, and "off" for specific input fields, or vice versa.
	 *
	 * See:
	 * [https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#autofilling-form-controls%3A-the-autocomplete-attribute]
	 */
	var autocomplete: String
		get() = dom.autocomplete
		set(value) {
			dom.autocomplete = value
		}

	companion object {

		val styleTag = StyleTag("TextInput")

	}

}

inline fun Context.textInput(init: ComponentInit<TextInput> = {}): TextInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextInput(this)
	t.init()
	return t
}

inline fun Context.passwordInput(init: ComponentInit<TextInput> = {}): TextInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return TextInput(this, "password").apply(init)
}

inline fun Context.searchInput(init: ComponentInit<TextInput> = {}): TextInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return TextInput(this, "search").apply {
		inputMode = InputMode.SEARCH
		init()
	}
}

open class NumberInput(owner: Context) : TextInputBase(owner, "number") {

	init {
		inputMode = InputMode.NUMERIC
	}

	var valueAsNumber: Double
		get() = dom.valueAsNumber
		set(value) {
			dom.valueAsNumber = value
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

	var min: Double?
		get() = dom.min.toDoubleOrNull()
		set(value) {
			if (value == null)
				dom.attributes.removeNamedItem("min")
			else
				dom.min = value.toString()
		}

	var max: Double?
		get() = dom.max.toDoubleOrNull()
		set(value) {
			if (value == null)
				dom.attributes.removeNamedItem("max")
			else
				dom.max = value.toString()
		}

	var step: Double?
		get() = dom.step.toDoubleOrNull()
		set(value) {
			if (value == null)
				dom.attributes.removeNamedItem("step")
			else
				dom.step = value.toString()
		}
}

inline fun Context.numberInput(init: ComponentInit<NumberInput> = {}): NumberInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return NumberInput(this).apply(init)
}

inline fun Context.telInput(init: ComponentInit<TextInput> = {}): TextInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextInput(this)
	t.dom.type = "tel"
	t.inputMode = InputMode.TEL
	t.init()
	return t
}

inline fun Context.urlInput(init: ComponentInit<TextInput>): TextInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextInput(this, "url")
	t.inputMode = InputMode.URL
	t.init()
	return t
}

inline fun Context.emailInput(init: ComponentInit<TextInput> = {}): TextInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextInput(this, "email")
	t.inputMode = InputMode.EMAIL
	t.init()
	return t
}


object InputType {

	/**
	 * A push button with no default behavior displaying the value of the value attribute, empty by default.
	 */
	const val BUTTON = "button"

	/**
	 * A check box allowing single values to be selected/deselected.
	 */
	const val CHECKBOX = "checkbox"

	/**
	 * A control for specifying a color; opening a color picker when active in supporting browsers.
	 */
	const val COLOR = "color"

	/**
	 * A control for entering a date (year, month, and day, with no time). Opens a date picker or numeric wheels for
	 * year, month, day when active in supporting browsers.
	 */
	const val DATE = "date"

	/**
	 * A control for entering a date and time, with no time zone. Opens a date picker or numeric wheels for date- and
	 * time-components when active in supporting browsers.
	 */
	const val DATETIME_LOCAL = "datetime-local"

	/**
	 * A field for editing an email address. Looks like a text input, but has validation parameters and relevant
	 * keyboard in supporting browsers and devices with dynamic keyboards.
	 */
	const val EMAIL = "email"

	/**
	 * A control that lets the user select a file. Use the accept attribute to define the types of files that the
	 * control can select.
	 */
	const val FILE = "file"

	/**
	 * A control for entering a month and year, with no time zone.
	 */
	const val MONTH = "month"

	/**
	 * A control for entering a number. Displays a spinner and adds default validation when supported. Displays a
	 * numeric keypad in some devices with dynamic keypads.
	 */
	const val NUMBER = "number"

	/**
	 * A single-line text field whose value is obscured. Will alert user if site is not secure.
	 */
	const val PASSWORD = "password"

	/**
	 * A radio button, allowing a single value to be selected out of multiple choices with the same name value.
	 */
	const val RADIO = "radio"

	/**
	 * A control for entering a number whose exact value is not important. Displays as a range widget defaulting to the
	 * middle value. Used in conjunction min and max to define the range of acceptable values.
	 */
	const val RANGE = "range"

	/**
	 * A single-line text field for entering search strings. Line-breaks are automatically removed from the input value.
	 * May include a delete icon in supporting browsers that can be used to clear the field. Displays a search icon
	 * instead of enter key on some devices with dynamic keypads.
	 */
	const val SEARCH = "search"

	/**
	 * A control for entering a telephone number. Displays a telephone keypad in some devices with dynamic keypads.
	 */
	const val TEL = "tel"

	/**
	 * The default value. A single-line text field. Line-breaks are automatically removed from the input value.
	 */
	const val TEXT = "text"

	/**
	 * A control for entering a time value with no time zone.
	 */
	const val TIME = "time"

	/**
	 * A field for entering a URL. Looks like a text input, but has validation parameters and relevant keyboard in
	 * supporting browsers and devices with dynamic keyboards.
	 */
	const val URL = "url"

	/**
	 * A control for entering a date consisting of a week-year number and a week number with no time zone.
	 */
	const val WEEK = "week"
}

object InputMode {

	/**
	 * No virtual keyboard. For when the page implements its own keyboard input control.
	 */
	const val NONE = "none"

	/**
	 * Standard input keyboard for the user's current locale.
	 * (default value)
	 */
	const val TEXT = "text"

	/**
	 * Fractional numeric input keyboard containing the digits and the appropriate separator character for the user's
	 * locale (typically either "." or ",").
	 */
	const val DECIMAL = "decimal"

	/**
	 * Numeric input keyboard; all that is needed are the digits 0 through 9.
	 */
	const val NUMERIC = "numeric"

	/**
	 * A telephone keypad input, including the digits 0 through 9, the asterisk ("*"), and the pound ("#") key.
	 */
	const val TEL = "tel"

	/**
	 * A virtual keyboard optimized for search input. For instance, the return key may be re-labeled "Search", and
	 * there may be other optimizations.
	 */
	const val SEARCH = "search"

	/**
	 * A virtual keyboard optimized for entering email addresses; typically this includes the "@" character as well as
	 * other optimizations.
	 */
	const val EMAIL = "email"

	/**
	 * A keypad optimized for entering URLs. This may have the "/" key more prominently available, for example.
	 */
	const val URL = "url"

}

//interface TextArea : TextInput {
//
//	val hScrollModel: ClampedScrollModel
//	val vScrollModel: ClampedScrollModel
//
//	companion object {
//		val styleTag = StyleTag("TextInput")
//	}
//}

/**
 * Common text restrict patterns.
 * These shouldn't be used as validation patterns; they are meant to restrict the types of characters that can be
 * typed into an input text.
 */
object RestrictPatterns {

	val INTEGER = Regex("[^0-9+-]")
	val FLOAT = Regex("[^0-9+-.]")
	val COLOR = Regex("[^0-9a-fA-F#x]")
}