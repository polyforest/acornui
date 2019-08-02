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

package com.acornui.component.datagrid

import com.acornui.component.ContainerImpl
import com.acornui.component.DatePicker
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.text.*
import com.acornui.compareTo
import com.acornui.compareTo2
import com.acornui.di.Injector
import com.acornui.di.Owned
import com.acornui.di.Scoped
import com.acornui.di.own
import com.acornui.selection.selectAll
import com.acornui.math.Bounds
import com.acornui.signal.Signal0
import com.acornui.signal.bind
import com.acornui.system.userInfo
import com.acornui.text.*
import com.acornui.time.DateRo

abstract class IntColumn<in E>(override val injector: Injector) : DataGridColumn<E, Int?>(), Scoped {

	val formatter = numberFormatter().apply {
		maxFractionDigits = 0
	}

	init {
		cellHAlign = HAlign.RIGHT
		sortable = true
	}

	override fun createCell(owner: Owned): DataGridCell<Int?> = NumberCell(owner, formatter)
	override fun createEditorCell(owner: Owned): DataGridEditorCell<Int?> = IntEditorCell(owner)

	override fun compareRows(row1: E, row2: E): Int {
		return getCellData(row1).compareTo(getCellData(row2))
	}
}

abstract class FloatColumn<in E>(override val injector: Injector) : DataGridColumn<E, Float?>(), Scoped {

	val formatter = numberFormatter()

	init {
		cellHAlign = HAlign.RIGHT
		sortable = true
	}

	override fun createCell(owner: Owned): DataGridCell<Float?> = NumberCell(owner, formatter)
	override fun createEditorCell(owner: Owned): DataGridEditorCell<Float?> = FloatEditorCell(owner)

	override fun compareRows(row1: E, row2: E): Int {
		return getCellData(row1).compareTo(getCellData(row2))
	}
}

class NumberCell(owner: Owned, private val formatter: NumberFormatter) : ContainerImpl(owner), DataGridCell<Number?> {

	private val textField = addChild(text { selectable = false; flowStyle.horizontalAlign = FlowHAlign.RIGHT })
	private var _data: Number? = null

	init {
		own(userInfo.currentLocale.changed.bind {
			textField.label = formatter.format(_data)
		})
	}

	override fun setData(value: Number?) {
		if (_data == value) return
		_data = value
		textField.label = formatter.format(value)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		textField.setSize(explicitWidth, explicitHeight)
		out.set(textField.bounds)
	}
}

abstract class NumberEditorCell(owner: Owned) : ContainerImpl(owner) {

	private val _changed = own(Signal0())
	val changed = _changed.asRo()

	protected val input = addChild(textInput())
	private var _data: Number? = null

	init {
		input.changed.add(_changed::dispatch)
		input.selectAll()
	}

	protected fun setNumber(value: Number?) {
		if (_data == value) return
		_data = value
		input.text = value?.toString() ?: ""
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		input.setSize(explicitWidth, explicitHeight)
		out.set(input.bounds)
	}
}

class IntEditorCell(owner: Owned) : NumberEditorCell(owner), DataGridEditorCell<Int?> {

	init {
		input.restrictPattern = RestrictPatterns.INTEGER
	}

	override fun validateData(): Boolean {
		return true
	}

	override fun getData(): Int? {
		return input.text.toIntOrNull()
	}

	override fun setData(value: Int?) = setNumber(value)
}

class FloatEditorCell(owner: Owned) : NumberEditorCell(owner), DataGridEditorCell<Float?> {

	init {
		input.restrictPattern = RestrictPatterns.FLOAT
	}

	override fun validateData(): Boolean {
		return true
	}

	override fun getData(): Float? {
		return input.text.toFloatOrNull()
	}

	override fun setData(value: Float?) = setNumber(value)
}

abstract class StringColumn<in E> : DataGridColumn<E, String>() {

	/**
	 * Whether to ignore case when sorting.
	 */
	var ignoreCase = true

	init {
		sortable = true
	}

	override fun createCell(owner: Owned): DataGridCell<String> = StringCell(owner)

	override fun createEditorCell(owner: Owned): DataGridEditorCell<String> = StringEditorCell(owner)

	override fun compareRows(row1: E, row2: E): Int {
		return getCellData(row1).compareTo2(getCellData(row2), ignoreCase = ignoreCase)
	}
}

class StringCell<E>(owner: Owned, val formatter: StringFormatter<E> = ToStringFormatter) : TextFieldImpl(owner), DataGridCell<E> {

	override fun setData(value: E) {
		label = formatter.format(value)
	}
}

class StringEditorCell(owner: Owned) : ContainerImpl(owner), DataGridEditorCell<String> {

	private val _changed = own(Signal0())
	override val changed = _changed.asRo()

	private val input = addChild(textInput())

	init {
		input.changed.add(_changed::dispatch)
		input.selectAll()
	}

	override fun validateData(): Boolean {
		return true
	}

	override fun getData(): String {
		return input.text
	}

	override fun setData(value: String) {
		input.text = value
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		input.setSize(explicitWidth, explicitHeight)
		out.set(input.bounds)
	}
}

abstract class DateColumn<in E>(override val injector: Injector) : DataGridColumn<E, DateRo?>(), Scoped {

	val formatter = dateFormatter {
		dateStyle = DateTimeFormatStyle.SHORT
	}

	private val parser = dateParser().apply {
		allowTwoDigitYears = true
		yearIsOptional = true
	}

	init {
		sortable = true
	}

	override fun createCell(owner: Owned): DataGridCell<DateRo?> = DateCell(owner, formatter)

	override fun createEditorCell(owner: Owned): DataGridEditorCell<DateRo?> = DateEditorCell(owner).apply {
		formatter = this@DateColumn.formatter
		open()
	}

	override fun compareRows(row1: E, row2: E): Int {
		return getCellData(row1).compareTo(getCellData(row2))
	}
}

class DateCell(owner: Owned, private val formatter: StringFormatter<DateRo>) : ContainerImpl(owner), DataGridCell<DateRo?> {

	private val textField = addChild(text { selectable = false })

	override fun setData(value: DateRo?) {
		textField.label = if (value == null) "" else formatter.format(value)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		textField.setSize(explicitWidth, explicitHeight)
		out.set(textField.bounds)
	}

}

open class DateEditorCell(owner: Owned) : DatePicker(owner), DataGridEditorCell<DateRo?> {

	init {
		open()
	}

	override fun validateData(): Boolean {
		return true
	}

	override fun getData(): DateRo? {
		return selectedDate
	}

	override fun setData(value: DateRo?) {
		selectedDate = value
	}
}

// TODO: Boolean column with checkbox
