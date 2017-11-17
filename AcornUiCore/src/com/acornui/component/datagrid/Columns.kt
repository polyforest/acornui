/*
 * Copyright 2017 Nicholas Bilyk
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
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.text.RestrictPatterns
import com.acornui.component.text.selectable
import com.acornui.component.text.text
import com.acornui.component.text.textInput
import com.acornui.core.compareTo
import com.acornui.core.compareTo2
import com.acornui.core.di.*
import com.acornui.core.selection.selectAll
import com.acornui.core.text.NumberFormatter
import com.acornui.core.text.numberFormatter
import com.acornui.math.Bounds
import com.acornui.signal.Signal0

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

class NumberCell(owner: Owned, private val formatter: NumberFormatter) : ContainerImpl(owner), DataGridCell<Number?> {

	private val textField = addChild(text { selectable = false; flowStyle.horizontalAlign = FlowHAlign.RIGHT })
	private var _data: Number? = null

	init {
		//inject(I18n).currentLocaleChanged.add(this::invalidateProperties)
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

	val changed = own(Signal0())
	protected val input = addChild(textInput())
	protected var _data: Number? = null

	init {
		input.changed.add(changed::dispatch)
		input.selectAll()
	}

	fun setData(value: Number?) {
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

	override fun setData(value: Int?) = super.setData(value)
}

class FloatEditorCell(owner: Owned) : NumberEditorCell(owner), DataGridEditorCell<Float?> {

	init {
		input.restrictPattern = "[^0-9.-]"
	}

	override fun validateData(): Boolean {
		return true
	}

	override fun getData(): Float? {
		return input.text.toFloatOrNull()
	}

	override fun setData(value: Float?) = super.setData(value)
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

class StringCell(owner: Owned) : ContainerImpl(owner), DataGridCell<String> {

	private val textField = addChild(text { selectable = false })

	override fun setData(value: String) {
		textField.label = value
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		textField.setSize(explicitWidth, explicitHeight)
		out.set(textField.bounds)
	}

}

class StringEditorCell(owner: Owned) : ContainerImpl(owner), DataGridEditorCell<String> {

	override val changed = own(Signal0())
	private val input = addChild(textInput())

	init {
		input.changed.add(changed::dispatch)
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