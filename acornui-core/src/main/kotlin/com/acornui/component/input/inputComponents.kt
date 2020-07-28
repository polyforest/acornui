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

/**
 * Thanks to Aaron Iker
 * https://css-tricks.com/custom-styling-form-inputs-with-modern-css-features/
 */

@file:Suppress("unused")

package com.acornui.component.input

import com.acornui.Disposable
import com.acornui.UidUtil
import com.acornui.component.*
import com.acornui.component.style.CommonStyleTags
import com.acornui.component.style.StyleTag
import com.acornui.component.text.text
import com.acornui.css.cssVar
import com.acornui.css.prefix
import com.acornui.di.Context
import com.acornui.dom.addCssToHead
import com.acornui.dom.createElement
import com.acornui.formatters.*
import com.acornui.google.Icons
import com.acornui.google.icon
import com.acornui.input.clicked
import com.acornui.input.mousePressOnKey
import com.acornui.input.mousePressed
import com.acornui.input.mouseReleased
import com.acornui.number.zeroPadding
import com.acornui.observe.Observable
import com.acornui.properties.afterChange
import com.acornui.signal.once
import com.acornui.signal.signal
import com.acornui.signal.unmanagedSignal
import com.acornui.skins.Theme
import com.acornui.time.Date
import org.w3c.dom.Element
import org.w3c.dom.HTMLLabelElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.js.Date as JsDate

object InputStyles {

	val switch = StyleTag("switch")

	init {

		@Suppress("CssUnresolvedCustomProperty", "CssInvalidFunction", "CssInvalidPropertyValue")
		addCssToHead(
			"""

input {
	font: inherit;
}	

input[type="datetime-local"]:after,
input[type="week"]:after,
input[type="month"]:after,
input[type="time"]:after,
input[type="date"]:after {
	font-family: 'Material Icons';
	-webkit-font-feature-settings: 'liga';
	-webkit-font-smoothing: antialiased;
	text-transform: none;
	font-size: 1.2em;
	margin-right: 3px;
	pointer-events: none;
}

input[type="week"]:after,
input[type="month"]:after,
input[type="date"]:after {
	content: 'calendar_today';
}

input[type="time"]:after {
	content: 'access_time';
}

::-webkit-calendar-picker-indicator {
	background: transparent;
	color: rgba(0, 0, 0, 0);
	opacity: 1;
	margin-right: -20px;
}



input[type='date'],
input[type='month'],
input[type='number'],
input[type='email'],
input[type='password'],
input[type='search'],
input[type='time'],
input[type='color'],
input[type='text'] {
	color: ${cssVar(Theme::inputTextColor)};
	border-width: ${cssVar(Theme::borderThickness)};;
	border-color: ${cssVar(Theme::border)};
	border-radius: ${cssVar(Theme::inputBorderRadius)};
	border-style: solid;
	padding: ${cssVar(Theme::inputPadding)};
	background: ${cssVar(Theme::inputBackground)};
	box-shadow: ${cssVar(Theme::componentShadow)};
}

input:disabled {
	background: ${cssVar(Theme::disabledInner)};
	border-color: ${cssVar(Theme::disabled)};
	color: ${cssVar(Theme::toggledInnerDisabled)};
	pointer-events: none;
}

input:active {
	border-color: ${cssVar(Theme::borderActive)};
}

input[type='date'],
input[type='month'],
input[type='time'],
input[type='datetime-local'],
input[type='button'],
input[type='submit'],
input[type='reset'],
input[type='search'],
input[type='checkbox'],
input[type='radio'] {
	${prefix("appearance", "none")};
}

input:disabled {
	opacity: ${cssVar(Theme::disabledOpacity)};
}

input[type='checkbox'],
input[type='radio'] {
	height: 21px;
	outline: none;
	display: inline-block;
	vertical-align: top;
	position: relative;
	margin: 0;
	cursor: pointer;
	border: 1px solid var(--bc, ${cssVar(Theme::border)});
	background: var(--b, ${cssVar(Theme::background)});
	transition: background 0.3s, border-color 0.3s, box-shadow 0.2s;
}

input[type='checkbox']::after,
input[type='radio']::after {
	content: '';
	display: block;
	left: 0;
	top: 0;
	position: absolute;
	transition: transform var(--d-t, 0.3s) var(--d-t-e, ease), opacity var(--d-o, 0.2s);
	box-sizing: border-box;
}

input[type='checkbox']:indeterminate,
input[type='checkbox']:checked,
input[type='radio']:checked {
	--b: ${cssVar(Theme::toggled)};
	--bc: ${cssVar(Theme::toggled)};
	--d-o: 0.3s;
	--d-t: 0.6s;
	--d-t-e: cubic-bezier(0.2, 0.85, 0.32, 1.2);
}

input[type='checkbox']:disabled,
input[type='radio']:disabled {
	--b: ${cssVar(Theme::disabled)};
	cursor: not-allowed;
}

input[type='checkbox']:disabled:indeterminate,
input[type='checkbox']:disabled:checked,
input[type='radio']:disabled:checked {
	--b: ${cssVar(Theme::disabledInner)};
	--bc: ${cssVar(Theme::borderDisabled)};
}

input[type='checkbox']:disabled + label,
input[type='radio']:disabled + label {
	cursor: not-allowed;
	opacity: ${cssVar(Theme::disabledOpacity)};
}

input[type='checkbox']:hover:not(:indeterminate):not(:disabled),
input[type='checkbox']:hover:not(:checked):not(:disabled),
input[type='radio']:hover:not(:checked):not(:disabled) {
	--bc: ${cssVar(Theme::borderHover)};
}

input[type='checkbox']:not($switch),
input[type='radio']:not($switch) {
	width: 21px;
}

input[type='checkbox']:not($switch)::after,
input[type='radio']:not($switch)::after {
	opacity: var(--o, 0);
}

input[type='checkbox']:not($switch):indeterminate,
input[type='checkbox']:not($switch):checked,
input[type='radio']:not($switch):checked {
	--o: 1;
}

input[type='checkbox'] + label,
input[type='radio'] + label {
	display: inline-block;
	vertical-align: top;
	cursor: pointer;
	user-select: none;
	-moz-user-select: none;
}

input[type='checkbox']:not($switch) {
	border-radius: 7px;
}

input[type='checkbox']:not($switch):not(:indeterminate)::after {
	width: 5px;
	height: 9px;
	border: 2px solid ${cssVar(Theme::toggledInner)};
	border-top: 0;
	border-left: 0;
	left: 7px;
	top: 4px;
	transform: rotate(var(--r, 20deg));
}

input[type='checkbox']:not(.switch):indeterminate::after {
	width: 9px;
	height: 2px;
	border-top: 2px solid ${cssVar(Theme::toggledInner)};
	left: 5px;
	top: 9px;
}

input[type='checkbox']:not($switch)::after {
	width: 7px;
	height: 2px;
	border-top: 2px solid ${cssVar(Theme::toggledInner)};
	left: 4px;
	top: 7px;
}

input[type='checkbox']:not($switch):checked {
	--r: 43deg;
}

input[type='checkbox']$switch {
	width: 38px;
	border-radius: 11px;
}

input[type='checkbox']$switch::after {
	left: 2px;
	top: 2px;
	border-radius: 50%;
	width: 15px;
	height: 15px;
	background: var(--ab, ${cssVar(Theme::border)});
	transform: translateX(var(--x, 0));
}

input[type='checkbox']$switch:checked {
	--ab: ${cssVar(Theme::toggledInner)};
	--x: 17px;
}

input[type='checkbox']$switch:disabled:not(:checked)::after {
	opacity: 0.6;
}

input[type='radio'] {
	border-radius: 50%;
}

input[type='radio']:checked {
	--scale: 0.5;
}

input[type='radio']::after {
	width: 19px;
	height: 19px;
	border-radius: 50%;
	background: ${cssVar(Theme::toggledInner)};
	opacity: 0;
	transform: scale(var(--scale, 0.7));
}

input::before,
input::after {
	box-sizing: border-box;
}

input:-webkit-autofill,
input:-webkit-autofill:hover, 
input:-webkit-autofill:focus,
textarea:-webkit-autofill,
textarea:-webkit-autofill:hover,
textarea:-webkit-autofill:focus,
select:-webkit-autofill,
select:-webkit-autofill:hover,
select:-webkit-autofill:focus {
	-webkit-text-fill-color: ${cssVar(Theme::inputTextColor)};
	-webkit-box-shadow: 0 0 0 1000px ${cssVar(Theme::inputBackground)} inset;
	font-style: italic;
}

		"""
		)
	}
}

open class Button(owner: Context, type: String = "button") : DivWithInputComponent(owner), Labelable {

	var toggleOnClick = false

	final override val inputComponent = addChild(InputUiComponentImpl(this, type).apply {
		style.display = "none"
	})

	val labelComponent = addChild(text())

	var type: String
		get() = inputComponent.dom.type
		set(value) {
			inputComponent.dom.type = value
		}

	init {
		addClass(styleTag)
		dom.tabIndex = 0

		mousePressOnKey()

		mousePressed.listen {
			active = true
			stage.mouseReleased.once {
				active = false
			}
		}

		clicked.listen {
			if (it.target != inputComponent.dom) {
				inputComponent.dom.dispatchEvent(MouseEvent("click", MouseEventInit(bubbles = false)))
				if (toggleOnClick) {
					toggled = !toggled
					toggledChanged.dispatch(Unit)
				}
			}
		}
	}

	override var label: String
		get() = labelComponent.label
		set(value) {
			labelComponent.label = value
		}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		labelComponent.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: WithNode) {
		labelComponent.removeElement(index)
	}

	override var disabled: Boolean by afterChange(false) {
		inputComponent.disabled = it
		toggleClass(CommonStyleTags.disabled)
	}

	private var active: Boolean by afterChange(false) {
		toggleClass(CommonStyleTags.active)
	}

	/**
	 * If [toggleOnClick] is true, when the user clicks this button, [toggled] is changed and [toggledChanged] is
	 * dispatched.
	 * This will only dispatch on a user event, not on setting [toggled].
	 */
	val toggledChanged = signal<Unit>()

	/**
	 * Returns true if the dom contains the class [CommonStyleTags.toggled].
	 */
	var toggled: Boolean
		get() = containsClass(CommonStyleTags.toggled)
		set(value) {
			if (value == toggled) return
			if (value)
				addClass(CommonStyleTags.toggled)
			else
				removeClass(CommonStyleTags.toggled)
		}

	companion object {

		val styleTag = StyleTag("Button")

		init {
			addCssToHead("""

$styleTag {
	padding: ${cssVar(Theme::componentPadding)};
	border-radius: ${cssVar(Theme::borderRadius)};
	background: ${cssVar(Theme::buttonBackground)};
	border-color: ${cssVar(Theme::border)};
	border-width: ${cssVar(Theme::borderThickness)};

	color: ${cssVar(Theme::buttonTextColor)};
	font-size: inherit;
	
	/*text-shadow: 1px 1px 1px #0004;*/
	border-style: solid;
	user-select: none;
	-moz-user-select: none;
	text-align: center;
	vertical-align: middle;
	overflow: hidden;
	box-sizing: border-box;
	box-shadow: ${cssVar(Theme::componentShadow)};
	
	cursor: pointer;
}

$styleTag:hover {
	background: ${cssVar(Theme::buttonBackgroundHover)};
	border-color: ${cssVar(Theme::borderHover)};
	color: ${cssVar(Theme::buttonTextHoverColor)};
}

$styleTag${CommonStyleTags.active} {
	background: ${cssVar(Theme::buttonBackgroundActive)};
	border-color: ${cssVar(Theme::borderActive)};
	color: ${cssVar(Theme::buttonTextActiveColor)};
}

$styleTag${CommonStyleTags.toggled} {
	background: ${cssVar(Theme::toggled)};
	border-color: ${cssVar(Theme::toggled)};
	color: ${cssVar(Theme::toggledInner)};
}

$styleTag${CommonStyleTags.toggled}:hover {
	background: ${cssVar(Theme::toggled)};
	border-color: ${cssVar(Theme::borderHover)};
}

$styleTag${CommonStyleTags.toggled}${CommonStyleTags.active} {
	border-color: ${cssVar(Theme::borderActive)};
}

$styleTag${CommonStyleTags.disabled} {
	background: ${cssVar(Theme::disabledInner)};
	border-color: ${cssVar(Theme::disabled)};
	color: ${cssVar(Theme::toggledInnerDisabled)};
	pointer-events: none;
	opacity: ${cssVar(Theme::disabledOpacity)};
}

$styleTag > div {
	overflow: hidden;
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: center;
	pointer-events: none;
}
			""")
		}
	}
}

inline fun Context.button(label: String = "", init: ComponentInit<Button> = {}): Button {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Button(this).apply {
		this.label = label
		init()
	}
}

inline fun Context.checkbox(defaultChecked: Boolean = false, init: ComponentInit<ToggleInput> = {}): ToggleInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ToggleInput(this, "checkbox").apply {
		this.defaultChecked = defaultChecked
		init()
	}
}

inline fun Context.colorInput(init: ComponentInit<InputUiComponentImpl> = {}): InputUiComponentImpl {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return InputUiComponentImpl(this, "color").apply(init)
}

open class DateInput(owner: Context) : InputUiComponentImpl(owner, "date") {

	private val parser = DateTimeParser()

	/**
	 * Sets/gets the default date.
	 */
	var defaultValueAsDate: Date?
		get() = parseDate(dom.defaultValue, isUtc = true)
		set(value) {
			dom.defaultValue = if (value == null) "" else "${value.utcFullYear}-${value.utcMonth.zeroPadding(2)}-${value.utcDayOfMonth.zeroPadding(2)}"
		}

	var valueAsDate: Date?
		get() = Date(dom.valueAsDate.unsafeCast<JsDate>())
		set(value) {
			dom.valueAsDate = value?.jsDate
		}

	var step: Double?
		get() = dom.step.toDoubleOrNull()
		set(value) {
			dom.step = value.toString()
		}

	var min: Date?
		get() = parseDate(dom.min, isUtc = true)
		set(value) {
			dom.min = if (value == null) "" else "${value.utcFullYear}-${value.utcMonth.zeroPadding(2)}-${value.utcDayOfMonth.zeroPadding(2)}"
		}

	var max: Date?
		get() = parseDate(dom.max, isUtc = true)
		set(value) {
			dom.max = if (value == null) "" else "${value.utcFullYear}-${value.utcMonth.zeroPadding(2)}-${value.utcDayOfMonth.zeroPadding(2)}"
		}

	/**
	 * Returns a String representation of the value.
	 * This will use the [DateTimeFormatter], set to UTC timezone.
	 */
	fun valueToString(year: YearFormat = YearFormat.NUMERIC, month: MonthFormat = MonthFormat.TWO_DIGIT, day: TimePartFormat = TimePartFormat.TWO_DIGIT) : String {
		val valueAsDate = valueAsDate ?: return ""
		val formatter = DateTimeFormatter(timeZone = "UTC", year = year, month = month, day = day)
		return formatter.format(valueAsDate)
	}
}

inline fun Context.dateInput(init: ComponentInit<DateInput> = {}): DateInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return DateInput(this).apply(init)
}

// Does not work in Fx
@ExperimentalJsExport
inline fun Context.dateTimeInput(init: ComponentInit<InputUiComponentImpl> = {}): InputUiComponentImpl {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return InputUiComponentImpl(this, "datetime-local").apply(init)
}

/**
 * Creates an input element of type file, styled like a button.
 * By default this will contain a file upload icon and the given label. This may be changed by clearing the elements
 * and adding new ones, or by instead creating `Button(owner, "file")`
 *
 */
inline fun Context.fileInput(label: String = "Choose File", init: ComponentInit<Button> = {}): Button {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Button(this, "file").apply {
		+icon(Icons.FILE_UPLOAD)
		+text(label) {
			style.marginRight = "4px" // Material design icons have embedded whitespace, this balances that out.
		}
		init()
	}
}

inline fun Context.hiddenInput(init: ComponentInit<InputUiComponentImpl> = {}): InputUiComponentImpl {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return InputUiComponentImpl(this, "hidden").apply(init)
}

open class MonthInput(owner: Context) : InputUiComponentImpl(owner, "month") {

	private val parser = DateTimeParser()

	/**
	 * Returns the default month as a UTC Date.
	 */
	var defaultValueAsDate: Date?
		get() = parseDate(dom.defaultValue, isUtc = true)
		set(value) {
			dom.defaultValue = if (value == null) "" else "${value.utcFullYear}-${value.utcMonth.zeroPadding(2)}"
		}

	/**
	 * Returns/sets the selected month as a UTC Date.
	 */
	var valueAsDate: Date?
		get() = Date(dom.valueAsDate.unsafeCast<JsDate>())
		set(value) {
			dom.valueAsDate = value?.jsDate
		}

	var value: String
		get() = dom.value
		set(value) {
			dom.value = value
		}

	/**
	 * Returns a String representation of the value.
	 * This will use the [DateTimeFormatter], set to UTC timezone.
	 */
	fun valueToString(year: YearFormat = YearFormat.NUMERIC, month: MonthFormat = MonthFormat.TWO_DIGIT) : String {
		val valueAsDate = valueAsDate ?: return ""
		val formatter = DateTimeFormatter(timeZone = "UTC", year = year, month = month)
		return formatter.format(valueAsDate)
	}
}

inline fun Context.monthInput(init: ComponentInit<MonthInput> = {}): MonthInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MonthInput(this).apply(init)
}

inline fun Context.resetInput(label: String = "", init: ComponentInit<Button> = {}): Button {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Button(this, "reset").apply {
		this.label = label
		init()
	}
}

inline fun Context.submitInput(label: String = "", init: ComponentInit<Button> = {}): Button {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Button(this, "submit").apply {
		this.label = label
		init()
	}
}

open class TimeInput(owner: Context) : InputUiComponentImpl(owner, "time") {

	private val parser = DateTimeParser()

	/**
	 * Sets/gets the default date.
	 */
	var defaultValueAsDate: Date?
		get() = parseDate(dom.defaultValue)
		set(value) {
			dom.defaultValue = if (value == null) "" else "${value.hours.zeroPadding(2)}:${value.minutes.zeroPadding(2)}"
		}

	fun defaultValueAsDate(value: Date, useMinutes: Boolean = true, useSeconds: Boolean = false, useMilliseconds: Boolean = false) {
		dom.defaultValue = formatTimeForDom(value, useMinutes, useSeconds, useMilliseconds)
	}

	var valueAsDate: Date?
		get() = Date(dom.valueAsDate.unsafeCast<JsDate>())
		set(value) {
			dom.valueAsDate = value?.jsDate
		}

	var step: Double?
		get() = dom.step.toDoubleOrNull()
		set(value) {
			dom.step = value.toString()
		}

	var min: Date?
		get() = parseDate(dom.min)
		set(value) {
			dom.min = formatTimeForDom(value)
		}

	var max: Date?
		get() = parseDate(dom.max, isUtc = true)
		set(value) {
			dom.max = formatTimeForDom(value)
		}

	/**
	 * Returns a String representation of the value.
	 * This will use the [DateTimeFormatter], set to UTC timezone.
	 */
	fun valueToString(hour: TimePartFormat? = TimePartFormat.NUMERIC, minute: TimePartFormat? = TimePartFormat.TWO_DIGIT, second: TimePartFormat? = TimePartFormat.TWO_DIGIT) : String {
		val valueAsDate = valueAsDate ?: return ""
		val formatter = DateTimeFormatter(hour = hour, minute = minute, second = second)
		return formatter.format(valueAsDate)
	}

	private fun formatTimeForDom(value: Date?, useMinutes: Boolean = true, useSeconds: Boolean = false, useMilliseconds: Boolean = false): String {
		if (value == null) return ""
		val minutes = if (useMinutes || useSeconds || useMilliseconds) ":${value.minutes.zeroPadding(2)}" else ""
		val seconds = if (useSeconds || useMilliseconds) ":${value.seconds.zeroPadding(2)}" else ""
		val milli = if (useMilliseconds) ".${value.milliseconds.zeroPadding(3)}" else ""
		return "${value.hours.zeroPadding(2)}$minutes$seconds$milli"
	}
}

inline fun Context.timeInput(init: ComponentInit<TimeInput> = {}): TimeInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return TimeInput(this).apply(init)
}

@ExperimentalJsExport
inline fun Context.weekInput(init: ComponentInit<InputUiComponentImpl> = {}): InputUiComponentImpl {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return InputUiComponentImpl(this, "week").apply(init)
}

/**
 * An input component such as a radio, switch, or checkbox that should be inline with a label.
 */
open class ToggleInput(owner: Context, type: String) : DivWithInputComponent(owner), Labelable {

	public final override val inputComponent = addChild(InputUiComponentImpl(this, type))
	val labelComponent = addChild(label(inputComponent))

	init {
		addClass(styleTag)
		inputComponent.style.flexShrink = "0"
	}

	var indeterminate: Boolean
		get() = inputComponent.dom.indeterminate
		set(value) {
			inputComponent.dom.indeterminate = value
		}

	var defaultChecked: Boolean
		get() = inputComponent.dom.defaultChecked
		set(value) {
			inputComponent.dom.defaultChecked = value
		}

	var checked: Boolean
		get() = inputComponent.dom.checked
		set(value) {
			inputComponent.dom.checked = value
		}

	var value: String
		get() = inputComponent.dom.value
		set(value) {
			inputComponent.dom.value = value
		}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		labelComponent.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: WithNode) {
		labelComponent.removeElement(index)
	}

	override var label: String
		get() = labelComponent.label
		set(value) {
			labelComponent.label = value
		}

	companion object {
		val styleTag = StyleTag("LabeledInput")

		init {
			addCssToHead("""
$styleTag {
	display: inline-flex;
	flex-direction: row;
	align-items: center;
}

$styleTag label {
	padding-left: 1ch;
	
	display: inline-flex;
	flex-direction: row;
	align-items: center;
}
			""")
		}
	}
}

inline fun Context.switch(defaultChecked: Boolean = false, init: ComponentInit<ToggleInput> = {}): ToggleInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return ToggleInput(this, "checkbox").apply {
		inputComponent.addClass(InputStyles.switch)
		this.defaultChecked = defaultChecked
		init()
	}
}

/**
 *
 * This name should be unique to the radio button groups on the page.
 */
class RadioGroup(val name: String = UidUtil.createUid()) : Observable, Disposable {

	override val changed = unmanagedSignal<Observable>()

	/**
	 * A list of all radio inputs belonging to this radio group.
	 */
	val allButtons = ArrayList<ToggleInput>()

	var value: String?
		get() = allButtons.first { it.checked }.value
		set(value) {
			for (radio in allButtons) {
				radio.checked = radio.value == value
			}
		}

	fun notifyChanged() {
		changed.dispatch(this)
	}

	override fun dispose() {
		allButtons.clear()
		changed.dispose()
	}
}

open class RadioInput(owner: Context, val group: RadioGroup) : ToggleInput(owner, "radio") {

	init {
		name = group.name
		group.allButtons.add(this)
		changed.listen {
			group.notifyChanged()
		}
	}
}

inline fun Context.radio(group: RadioGroup, value: String, defaultChecked: Boolean = false, init: ComponentInit<RadioInput> = {}): RadioInput {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return RadioInput(this, group).apply {
		this.defaultChecked = defaultChecked
		this.value = value
		init()
	}
}

class LabelComponent(owner: Context) : UiComponentImpl<HTMLLabelElement>(owner, createElement<Element>("label").unsafeCast<HTMLLabelElement>()), Labelable {

	override var label: String
		get() = dom.innerText
		set(value) {
			dom.innerText = value
		}
}

inline fun Context.label(htmlFor: String, value: String = "", init: ComponentInit<LabelComponent> = {}): LabelComponent {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return LabelComponent(this).apply {
		dom.htmlFor = htmlFor
		label = value
		init()
	}
}

inline fun Context.label(forComponent: UiComponent, value: String = "", init: ComponentInit<LabelComponent> = {}): LabelComponent =
	label(forComponent.id, value, init)