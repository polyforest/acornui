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

import com.acornui.Disposable
import com.acornui.collection.Filter
import com.acornui.component.layout.algorithm.GridLayoutStyle
import com.acornui.component.style.*
import com.acornui.component.text.selectable
import com.acornui.component.text.textInput
import com.acornui.cursor.StandardCursors
import com.acornui.cursor.cursor
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.focus.blurred
import com.acornui.focus.focus
import com.acornui.input.interaction.click
import com.acornui.input.interaction.isEnterOrReturn
import com.acornui.input.keyDown
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.popup.PopUpManager
import com.acornui.popup.lift
import com.acornui.recycle.Clearable
import com.acornui.reflect.afterChange
import com.acornui.signal.Signal0
import com.acornui.text.*
import com.acornui.time.Date
import com.acornui.time.DateRo

open class DatePicker(
		owner: Context
) : ContainerImpl(owner), Clearable {

	private val _input = own(Signal0())

	/**
	 * Dispatched on each input character.
	 * This does not dispatch when selecting a date from the picker.
	 */
	val input = _input.asRo()

	private val _changed = own(Signal0())

	/**
	 * Dispatched on value commit.
	 * It is dispatched when the user selects a date, or commits the value of the text input. It is not dispatched
	 * when the selected date or text is programmatically changed.
	 */
	val changed = _changed.asRo()

	/**
	 * The formatter to be used when converting a date element to a string.
	 */
	var formatter: StringFormatter<DateRo> = dateFormatter {
		dateStyle = DateTimeFormatStyle.SHORT
	}

	/**
	 * The parser to be used when converting a string to a date.
	 */
	var parser: StringParser<DateRo> = dateParser {
		allowTwoDigitYears = true
	}

	/**
	 * Sets the currently selected date.
	 * Note that this does not invoke [input] or [changed] signals.
	 */
	var selectedDate: DateRo?
		get() = calendar.selection.selectedItem
		set(value) {
			calendar.selection.selectedItem = value
			textInput.text = if (value == null) "" else formatter.format(value)
			val d = value ?: Date()
			calendar.month = d.month
			calendar.fullYear = d.fullYear
		}

	/**
	 * If true, the calendar will open when typing into the input field.
	 */
	var openOnInput = true

	private val textInput = textInput {
		input.add {
			if (openOnInput) open()
			selectDateFromText()
			_input.dispatch()
		}
		keyDown().add {
			if (it.isEnterOrReturn) {
				if (isOpen) {
					close()
					_changed.dispatch()
				} else {
					open()
				}
			}
		}
		focusHighlightDelegate = this@DatePicker
	}

	private var handCursor: Disposable? = null

	/**
	 * If false, the date picker will not accept type input, and dates may only be selected via the dropdown.
	 */
	var editable: Boolean by afterChange(true) {
		textInput.editable = it
		textInput.selectable = it
		handCursor?.dispose()
		if (it) handCursor = cursor(StandardCursors.HAND)
	}

	/**
	 * If true, this date picker will use the CommonStyleTags.disabled style tag and have interactivity disabled.
	 */
	var disabled: Boolean by afterChange(false) {
		interactivityMode = if (it) InteractivityMode.NONE else InteractivityMode.ALL
		disabledTag = it
	}

	private var background: UiComponent? = null
	private var downArrow: UiComponent? = null
	private val calendar: Calendar = calendar {
		selection.changed.add { _, newSelection ->
			val value = newSelection.firstOrNull()
			textInput.text = if (value == null) "" else formatter.format(value)
			this@DatePicker.focus()
			close()
			_changed.dispatch()
		}
	}

	/**
	 * If set, only the dates that pass the filter will be enabled.
	 */
	var dateEnabledFilter: Filter<DateRo>?
		get() = calendar.dateEnabledFilter
		set(value) {
			calendar.dateEnabledFilter = value
		}

	/**
	 * If the date enabled filter
	 */
	fun invalidateDateEnabled() {
		calendar.invalidateDateEnabled()
	}

	private val calendarLift = lift {
		focus = false
		+calendar layout { fill() }
		onClosed = {
			close()
		}
	}

	val style = bind(DatePickerStyle())

	val panelStyle: PanelStyle
		get() = calendar.panelStyle

	val calendarStyle: CalendarStyle
		get() = calendar.style

	val calendarLayoutStyle: GridLayoutStyle
		get() = calendar.layoutStyle

	fun rendererFactory(value: Context.() -> CalendarItemRenderer) {
		calendar.rendererFactory(value)
	}

	init {
		isFocusContainer = true

		styleTags.add(DatePicker)
		addChild(textInput)

		watch(style) {
			background?.dispose()
			background = addOptionalChild(0, it.background(this))

			downArrow?.dispose()
			val downArrow = addChild(it.downArrow(this))
			downArrow.focusEnabled = true
			downArrow.cursor(StandardCursors.HAND)
			downArrow.click().add { e ->
				if (!e.handled) {
					e.handled = true
					toggleOpen()
					calendar.focus()
				}
			}
			this.downArrow = downArrow
		}
		blurred().add {
			close()
			if (isActive)
				_changed.dispatch()
		}
	}

	private var _isOpen = false

	/**
	 * True if the calendar component is currently shown.
	 */
	val isOpen: Boolean
		get() = _isOpen

	/**
	 * Displays the calendar component.
	 */
	fun open() {
		if (_isOpen) return
		_isOpen = true
		calendar.highlighted.clear()
		selectDateFromText()
		calendarLift.priority = inject(PopUpManager).currentPopUps.lastOrNull()?.priority ?: 0f
		addChild(calendarLift)
		textInput.focus()
	}

	/**
	 * Hides the calendar component.
	 */
	fun close() {
		if (!_isOpen) return
		_isOpen = false
		removeChild(calendarLift)
	}

	/**
	 * Toggles the display of the calendar component.
	 */
	fun toggleOpen() {
		if (_isOpen) close()
		else open()
	}

	var text: String
		get() = textInput.text
		set(value) {
			textInput.text = value
			selectDateFromText()
		}

	private fun selectDateFromText() {
		val date = parser.parse(text)
		calendar.selection.selectedItem = date
		calendar.highlighted.selectedItem = date
		val d = date ?: Date()
		calendar.month = d.month
		calendar.fullYear = d.fullYear
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val pad = style.padding
		val w = pad.reduceWidth(explicitWidth)
		val h = pad.reduceHeight(explicitHeight)
		val downArrow = this.downArrow!!
		textInput.setSize(if (w == null) null else w - style.gap - downArrow.width, h)
		textInput.setPosition(pad.left, pad.top)
		downArrow.moveTo(pad.left + textInput.width + style.gap, pad.top + (textInput.height - downArrow.height) * 0.5f)
		out.set(pad.expandWidth(textInput.width + style.gap + downArrow.width), pad.expandHeight(maxOf(textInput.height, downArrow.height)), textInput.baselineY)
		background?.setSize(out.width, out.height)

		calendarLift.moveTo(0f, out.height)
	}

	override fun clear() {
		textInput.clear()
		selectedDate = null
	}

	override fun dispose() {
		close()
		super.dispose()
	}

	companion object : StyleTag
}

class DatePickerStyle : StyleBase() {
	override val type: StyleType<DatePickerStyle> = DatePickerStyle

	/**
	 * The background of the text input / down arrow area.
	 * Skins should ensure the text input doesn't have a background.
	 */
	var background by prop(noSkinOptional)

	/**
	 * The padding between the background and the text input / down arrow area.
	 */
	var padding by prop(Pad(0f))

	var downArrow by prop(noSkin)

	/**
	 * The gap between the down arrow and the text field.
	 */
	var gap by prop(2f)

	companion object : StyleType<DatePickerStyle>
}

fun Context.datePicker(
		init: ComponentInit<DatePicker> = {}): DatePicker {
	val t = DatePicker(this)
	t.init()
	return t
}
