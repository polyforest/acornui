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

import com.acornui.compareTo
import com.acornui.compareTo2
import com.acornui.component.ContainerImpl
import com.acornui.component.DatePicker
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.text.*
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.input.SoftKeyboardType
import com.acornui.math.Bounds
import com.acornui.observe.bind
import com.acornui.selection.selectAll
import com.acornui.signal.Signal1
import com.acornui.system.userInfo
import com.acornui.text.*
import com.acornui.time.DateRo

abstract class IntColumn<in E> : DataGridColumn<E, Int?>() {

	val formatter = numberFormatter().apply {
		maxFractionDigits = 0
	}

	init {
		cellHAlign = HAlign.RIGHT
		sortable = true
	}

	override fun createCell(owner: Context): DataGridCell<Int?> = NumberCell(owner, formatter)
	override fun createEditorCell(owner: Context): DataGridEditorCell<Int?> = IntEditorCell(owner)

	override fun compareRows(row1: E, row2: E): Int {
		return getCellData(row1).compareTo(getCellData(row2))
	}
}

abstract class FloatColumn<in E> : DataGridColumn<E, Float?>() {

	val formatter = numberFormatter()

	init {
		cellHAlign = HAlign.RIGHT
		sortable = true
	}

	override fun createCell(owner: Context): DataGridCell<Float?> = NumberCell(owner, formatter)
	override fun createEditorCell(owner: Context): DataGridEditorCell<Float?> = FloatEditorCell(owner)

	override fun compareRows(row1: E, row2: E): Int {
		return getCellData(row1).compareTo(getCellData(row2))
	}
}

class NumberCell<T : Number>(owner: Context, private val formatter: NumberFormatter) : ContainerImpl(owner), DataGridCell<T?> {

	private val textField = addChild(text { selectable = false; flowStyle.horizontalAlign = FlowHAlign.RIGHT })

	init {
		bind(userInfo.currentLocale.changed) {
			textField.label = formatter.format(value)
		}
	}

	override var value: T? = null
		set(value) {
			if (field == value) return
			field = value
			textField.label = formatter.format(value)
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		textField.size(explicitWidth, explicitHeight)
		out.set(textField.bounds)
	}
}

abstract class NumberEditorCell(owner: Context) : ContainerImpl(owner) {

	protected val input = addChild(textInput())
	private var _data: Number? = null

	init {
		input.selectAll()
	}

	protected fun setNumber(value: Number?) {
		if (_data == value) return
		_data = value
		input.text = value?.toString() ?: ""
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		input.size(explicitWidth, explicitHeight)
		out.set(input.bounds)
	}
}

class IntEditorCell(owner: Context) : NumberEditorCell(owner), DataGridEditorCell<Int?> {

	private val _changed = own(Signal1<IntEditorCell>())
	override val changed = _changed.asRo()

	init {
		input.changed.add {
			_changed.dispatch(this)
		}
		input.restrictPattern = RestrictPatterns.INTEGER
		input.softKeyboardType = SoftKeyboardType.NUMERIC
	}

	override var value: Int?
		get() = input.text.toIntOrNull()
		set(value) = setNumber(value)

}

class FloatEditorCell(owner: Context) : NumberEditorCell(owner), DataGridEditorCell<Float?> {

	private val _changed = own(Signal1<FloatEditorCell>())
	override val changed = _changed.asRo()

	init {
		input.changed.add {
			_changed.dispatch(this)
		}
		input.restrictPattern = RestrictPatterns.FLOAT
		input.softKeyboardType = SoftKeyboardType.DECIMAL
	}

	override var value: Float?
		get() = input.text.toFloatOrNull()
		set(value) = setNumber(value)
}

abstract class StringColumn<in E> : DataGridColumn<E, String>() {

	/**
	 * Whether to ignore case when sorting.
	 */
	var ignoreCase = true

	init {
		sortable = true
	}

	override fun createCell(owner: Context): DataGridCell<String> = StringCell(owner)

	override fun createEditorCell(owner: Context): DataGridEditorCell<String> = StringEditorCell(owner)

	override fun compareRows(row1: E, row2: E): Int {
		return getCellData(row1).compareTo2(getCellData(row2), ignoreCase = ignoreCase)
	}
}

class StringCell<E>(owner: Context, val formatter: StringFormatter<E?> = ToStringFormatter) : TextFieldImpl(owner), DataGridCell<E> {

	override var value: E? = null
		set(value) {
			if (field == value) return
			field = value
			text = formatter.format(value)
		}
}

class StringEditorCell(owner: Context) : ContainerImpl(owner), DataGridEditorCell<String> {

	private val _changed = own(Signal1<StringEditorCell>())
	override val changed = _changed.asRo()

	private val input = addChild(textInput())

	init {
		input.changed.add {
			_changed.dispatch(this)
		}
		input.selectAll()
	}

	override var value: String?
		get() = input.text
		set(value) {
			input.text = value ?: ""
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		input.size(explicitWidth, explicitHeight)
		out.set(input.bounds)
	}
}

abstract class DateColumn<in E> : DataGridColumn<E, DateRo?>() {

	val formatter = dateFormatter {
		dateStyle = DateTimeFormatStyle.SHORT
	}.withNull()

	private val parser = dateParser().apply {
		allowTwoDigitYears = true
		yearIsOptional = true
	}

	init {
		sortable = true
	}

	override fun createCell(owner: Context): DataGridCell<DateRo?> = DateCell(owner, formatter)

	override fun createEditorCell(owner: Context): DataGridEditorCell<DateRo?> = DateEditorCell(owner).apply {
		formatter = this@DateColumn.formatter
		activated.add {
			open()
		}
	}

	override fun compareRows(row1: E, row2: E): Int {
		return getCellData(row1).compareTo(getCellData(row2))
	}
}

class DateCell(owner: Context, private val formatter: StringFormatter<DateRo?>) : ContainerImpl(owner), DataGridCell<DateRo?> {

	private val textField = addChild(text { selectable = false })

	override var value: DateRo? = null
		set(value) {
			if (field == value) return
			field = value
			textField.label = formatter.format(value)
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		textField.size(explicitWidth, explicitHeight)
		out.set(textField.bounds)
	}
}

open class DateEditorCell(owner: Context) : DatePicker(owner), DataGridEditorCell<DateRo?> {

	init {
		activated.add {
			open()
		}
	}
}

// TODO: Boolean column with checkbox
