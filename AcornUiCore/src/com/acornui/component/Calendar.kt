/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui.component.layout.HAlign
import com.acornui.component.layout.ListItemRenderer
import com.acornui.component.layout.algorithm.GridColumn
import com.acornui.component.layout.algorithm.grid
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.text.text
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.text.DateTimeFormatStyle
import com.acornui.core.text.DateTimeFormatType
import com.acornui.core.text.dateFormatter
import com.acornui.core.time.DateRo
import com.acornui.core.time.time
import com.acornui.core.zeroPadding
import com.acornui.math.Bounds
import com.acornui.signal.Signal
import com.acornui.signal.Signal0

class Calendar(
		owner: Owned,
		val headerFactory: Owned.() -> Labelable = {
			text {
				charStyle.selectable = false
			}
		},
		val cellFactory: Owned.() -> ListItemRenderer<DateRo> = { calendarItemRenderer() }
) : ContainerImpl(owner) {

	private val _changed = own(Signal0())

	/**
	 * Dispatched on value commit.
	 * It is dispatched when the user selects an item, or commits the value of the text input. It is not dispatched
	 * when the selected item or text is programmatically changed.
	 */
	val changed: Signal<() -> Unit>
		get() = _changed

	private val headers = Array(7) {
		headerFactory()
	}

	private val cells: Array<ListItemRenderer<DateRo>> = Array(42) {
		cellFactory().apply {
			index = it
		}
	}

	private val grid = grid()

	private val panel = addChild(panel {
		+grid layout { fill() }
	})

	val panelStyle: PanelStyle
		get() = panel.style

	val style = bind(CalendarStyle())

	private val date = time.now().apply {
		hour = 0
		minute = 0
		second = 0
		milli = 0
		dayOfMonth = 1
	}

	/**
	 * The month index. This is 0-based.  January = 0, December = 11
	 */
	var monthIndex: Int
		get() = date.monthIndex
		set(value) {
			date.monthIndex = value
			invalidateProperties()
		}

	/**
	 * The 4 digit year.
	 */
	var fullYear: Int
		get() = date.fullYear
		set(value) {
			date.fullYear = value
			invalidateProperties()
		}

	/**
	 * The starting day of the week. (0 is Sunday, 6 is Saturday)
	 */
	var dayOfWeek: Int by validationProp(0, ValidationFlags.PROPERTIES)

	private val weekDayFormatter = dateFormatter {
		type = DateTimeFormatType.WEEKDAY
		dateStyle = DateTimeFormatStyle.SHORT
	}

	private fun updateProperties() {
		val dayOffset = date.dayOfWeek - dayOfWeek

		for (i in 0..6) {
			val iDate = date.copy()
			iDate.dayOfMonth = i - dayOffset + 1
			headers[i].label = weekDayFormatter.format(iDate)
		}
		for (i in 0..41) {
			val iDate = date.copy()
			iDate.dayOfMonth = i - dayOffset + 1
			cells[i].data = iDate
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		var maxHeaderW = 0f
		for (i in 0..6) {
			maxHeaderW = maxOf(maxHeaderW, headers[i].width)
		}
		val columns = ArrayList<GridColumn>()
		for (i in 0..6) {
			columns.add(GridColumn(hAlign = style.headerHAlign, width = maxHeaderW))
		}
		grid.style.columns = columns

		panel.setSize(explicitWidth, explicitHeight)
		out.set(panel.bounds)
	}

	init {
		styleTags.add(Companion)
		validation.addNode(ValidationFlags.PROPERTIES, 0, ValidationFlags.SIZE_CONSTRAINTS, this::updateProperties)
		grid.apply {
			for (i in 0..6) {
				+headers[i]
			}

			for (i in 0..41) {
				+cells[i] layout { widthPercent = 1f }
			}
		}

	}

	companion object : StyleTag
}

class CalendarStyle : StyleBase() {

	override val type: StyleType<*> = Companion

	var headerHAlign by prop(HAlign.CENTER)

	companion object : StyleType<CalendarStyle>
}

fun Owned.calendar(init: ComponentInit<Calendar> = {}): Calendar {
	val c = Calendar(this)
	c.init()
	return c
}

open class CalendarItemRenderer(owner: Owned) : Button(owner), ListItemRenderer<DateRo> {

	val calendarItemRendererStyle = bind(CalendarItemRendererStyle())

	private var _data: DateRo? = null
	override var data: DateRo?
		get() = _data
		set(value) {
			if (_data == value) return
			_data = value
			refreshLabel()
		}

	override var index: Int = 0

	private fun refreshLabel() {
		label = _data?.dayOfMonth?.zeroPadding(if (calendarItemRendererStyle.zeroPadDay) 1 else 0) ?: ""
	}

	init {
		styleTags.add(Companion)
		watch(calendarItemRendererStyle) {
			refreshLabel()
		}
	}

	companion object : StyleTag
}

open class CalendarItemRendererStyle : StyleBase() {

	override val type: StyleType<CalendarItemRendererStyle> = Companion

	/**
	 * If true, single digit days will be renderered with zeros.  E.g.  "1" becomes "01"
	 */
	var zeroPadDay by prop(false)

	companion object : StyleType<CalendarItemRendererStyle>
}

fun Owned.calendarItemRenderer(init: ComponentInit<CalendarItemRenderer> = {}): CalendarItemRenderer {
	val c = CalendarItemRenderer(this)
	c.init()
	return c
}