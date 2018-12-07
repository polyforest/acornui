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
import com.acornui.component.layout.algorithm.GridLayoutStyle
import com.acornui.component.layout.algorithm.grid
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.text.text
import com.acornui.core.behavior.Selection
import com.acornui.core.behavior.SelectionBase
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.input.interaction.ClickInteractionRo
import com.acornui.core.input.interaction.click
import com.acornui.core.text.DateTimeFormatStyle
import com.acornui.core.text.DateTimeFormatType
import com.acornui.core.text.dateFormatter
import com.acornui.core.time.DateRo
import com.acornui.core.time.time
import com.acornui.core.zeroPadding
import com.acornui.math.Bounds

open class Calendar(
		owner: Owned
) : ContainerImpl(owner) {

	/**
	 * If true, clicking on a calendar cell will set the selection to that cell.
	 * This does not affect setting the selection via code.
	 */
	var selectable = true

	val selection: Selection<DateRo> = own(object : SelectionBase<DateRo>() {
		override fun walkSelectableItems(callback: (item: DateRo) -> Unit) {
			for (i in 0..cells.lastIndex) {
				val date = cells[i].data ?: continue
				callback(date)
			}
		}

		override fun onSelectionChanged(oldSelection: List<DateRo>, newSelection: List<DateRo>) {
			for (i in 0..cells.lastIndex) {
				val cell = cells[i]
				val d = cell.data
				cell.toggled = if (d == null) false else getItemIsSelected(d)
			}
		}
	})

	val highlighted: Selection<DateRo> =  own(object : SelectionBase<DateRo>() {
		override fun walkSelectableItems(callback: (item: DateRo) -> Unit) {
			for (i in 0..cells.lastIndex) {
				val date = cells[i].data ?: continue
				callback(date)
			}
		}

		override fun onSelectionChanged(oldSelection: List<DateRo>, newSelection: List<DateRo>) {
//			for (i in 0..oldSelection.lastIndex) {
//				val cell = getCellByDate(oldSelection[i]) ?: continue
//				cell.highlighted = false
//			}
//			for (i in 0..newSelection.lastIndex) {
//				val cell = getCellByDate(newSelection[i]) ?: continue
//				cell.highlighted = true
//			}
		}
	})

	protected fun getCellByDate(cellDate: DateRo): ListItemRenderer<DateRo>? {
		val dayOffset = date.dayOfWeek - dayOfWeek
		val i = dayOffset + cellDate.dayOfMonth - 1
		return cells.get(i)
	}

	var _headerFactory: Owned.() -> Labelable = {
		text {
			charStyle.selectable = false
		}
	}

	fun headerFactory(value: Owned.() -> Labelable) {
		val headers = _headers
		if (headers != null) {
			// Dispose old header cells when switching renderer factories.
			for (i in 0..headers.lastIndex) {
				headers[i].dispose()
			}
			_headers = null
		}
		_headerFactory = value
	}

	private var _headers: Array<Labelable>? = null

	val headers: Array<Labelable>
		get() {
			if (_headers == null) {
				grid.apply {
					_headers = Array(7) {
						addElement(it, _headerFactory())
					}
				}
			}
			return _headers!!
		}

	private var _rendererFactory: Owned.() -> ListItemRenderer<DateRo> = { calendarItemRenderer() }

	fun rendererFactory(value: Owned.() -> ListItemRenderer<DateRo>) {
		val cells = _cells
		if (cells != null) {
			// Dispose old cells when switching renderer factories.
			for (i in 0..cells.lastIndex) {
				cells[i].dispose()
			}
			_cells = null
		}
		_rendererFactory = value
	}

	private var _cells: Array<ListItemRenderer<DateRo>>? = null
	protected val cells: Array<ListItemRenderer<DateRo>>
		get() {
			if (_cells == null) {
				grid.apply {
					_cells = Array(42) {
						+_rendererFactory().apply {
							index = it
							click().add(this@Calendar::cellClickedHandler)
						} layout { widthPercent = 1f }
					}
				}
			}
			return _cells!!
		}

	private fun cellClickedHandler(e: ClickInteractionRo) {
		if (selectable && !e.handled) {
			@Suppress("UNCHECKED_CAST")
			val cell = e.currentTarget as ListItemRenderer<DateRo>
			e.handled = true
			selection.setSelectedItemsUser(listOf(cell.data!!))
		}
	}

	private val grid = grid()

	private val panel = addChild(panel {
		+grid layout { fill() }
	})

	val panelStyle: PanelStyle
		get() = panel.style

	val style = bind(CalendarStyle())

	val layoutStyle: GridLayoutStyle
		get() = grid.style

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
	var month: Int
		get() = date.month
		set(value) {
			date.month = value
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
			val cell = cells[i]
			val iDate = date.copy()
			iDate.dayOfMonth = i - dayOffset + 1
			cell.data = iDate
			cell.toggled = selection.getItemIsSelected(iDate)
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
		isFocusContainer = true
		focusEnabled = true
		styleTags.add(Companion)
		validation.addNode(ValidationFlags.PROPERTIES, 0, ValidationFlags.SIZE_CONSTRAINTS, this::updateProperties)
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