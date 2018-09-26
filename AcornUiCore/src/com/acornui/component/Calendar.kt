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
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.algorithm.grid
import com.acornui.component.layout.algorithm.gridColumn
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.text.text
import com.acornui.core.di.Owned
import com.acornui.core.input.interaction.click
import com.acornui.core.text.DateTimeFormatStyle
import com.acornui.core.text.DateTimeFormatType
import com.acornui.core.text.dateFormatter
import com.acornui.core.time.DateRo
import com.acornui.core.time.time
import com.acornui.core.zeroPadding
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo

class Calendar(owner: Owned) : ContainerImpl(owner) {

	private val headers = Array(7) {
		text {
			charStyle.selectable = false
		}
	}

	private val cells = Array(42) {
		calendarItemRenderer {
			click().add {

			}
		}
	}

	private val grid = grid {
		for (i in 0..6) {
			style.columns.add(gridColumn { hAlign = HAlign.RIGHT })
		}
	}

	private val panel = addChild(panel {
		+grid layout { fill() }
	})

	val style: PanelStyle
		get() = panel.style

	private val date = time.now().apply {
		hour = 0
		minute = 0
		second = 0
		milli = 0
		dayOfMonth = 1
	}

	init {
		grid.apply {
			for (i in 0..6) {
				+headers[i]
			}

			for (i in 0..41) {
				+cells[i]
			}
		}
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

	override fun updateProperties() {
		super.updateProperties()
		val dayOffset = date.dayOfWeek - dayOfWeek

		for (i in 0..6) {
			val iDate = date.copy()
			iDate.dayOfMonth = i - dayOffset + 1
			headers[i].text = weekDayFormatter.format(iDate)
		}
		for (i in 0..41) {
			val iDate = date.copy()
			iDate.dayOfMonth = i - dayOffset + 1
			cells[i].data = iDate
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		panel.setSize(explicitWidth, explicitHeight)
		out.set(panel.bounds)
	}
}

fun Owned.calendar(init: ComponentInit<Calendar> = {}): Calendar {
	val c = Calendar(this)
	c.init()
	return c
}

class CalendarItemRenderer(owner: Owned) : ContainerImpl(owner), ItemRenderer<DateRo> {

	private val textField = addChild(text {
		interactivityMode = InteractivityMode.NONE
		charStyle.selectable = false
	})
	val style = bind(CalendarItemStyle())

	init {
		styleTags.add(Companion)
		watch(style) {
			refreshLabel()
		}
	}

	private var _data: DateRo? = null
	override var data: DateRo?
		get() = _data
		set(value) {
			if (_data == value) return
			_data = value
			refreshLabel()
		}

	private fun refreshLabel() {
		textField.text = _data?.dayOfMonth?.zeroPadding(if (style.zeroPadDay) 1 else 0) ?: ""
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		val padding = style.padding
		out.width.min = padding.expandWidth2(textField.width)
		out.height.min = padding.expandHeight2(textField.height)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val padding = style.padding
		val childAvailableWidth = padding.reduceWidth(explicitWidth)
		val child = textField

		val childX = padding.left + run {
			val remainingSpace = maxOf(0f, childAvailableWidth ?: 0f-child.width)
			when (style.horizontalAlign) {
				HAlign.LEFT -> 0f
				HAlign.CENTER -> remainingSpace * 0.5f
				HAlign.RIGHT -> remainingSpace
			}
		}
		textField.moveTo(childX, padding.top)

		out.ext(padding.expandWidth2(textField.width), padding.expandHeight2(textField.height))
	}

	companion object : StyleTag
}

open class CalendarItemStyle : StyleBase() {

	override val type: StyleType<CalendarItemStyle> = Companion

	/**
	 * If true, single digit days will be renderered with zeros.  E.g.  "1" becomes "01"
	 */
	val zeroPadDay by prop(false)

	var padding: PadRo by prop(Pad())
	var horizontalAlign by prop(HAlign.LEFT)

	companion object : StyleType<CalendarItemStyle>
}

fun Owned.calendarItemRenderer(init: ComponentInit<CalendarItemRenderer> = {}): CalendarItemRenderer {
	val c = CalendarItemRenderer(this)
	c.init()
	return c
}