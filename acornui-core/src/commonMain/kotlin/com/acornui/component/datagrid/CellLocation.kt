/*
 * Copyright 2019 PolyForest
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

interface CellLocationRo<RowData> : RowLocationRo<RowData> {
	val columnIndex: Int
	val column: DataGridColumn<RowData, *>
	val hasPreviousCell: Boolean
	val hasNextCell: Boolean

	override fun copy(): CellLocationRo<RowData>
}

val <E> CellLocationRo<E>.editable: Boolean
	get() {
		return isValid && column.visible && column.editable && isElementRow
	}

class CellLocation<RowData>(dataGrid: DataGrid<RowData>) : RowLocation<RowData>(dataGrid), CellLocationRo<RowData> {

	override var columnIndex: Int = 0

	constructor(dataGrid: DataGrid<RowData>, position: Int, columnIndex: Int) : this(dataGrid) {
		this.position = position
		this.columnIndex = columnIndex
	}

	/**
	 * Constructs a cell location, setting this row location to match the provided row location.
	 */
	constructor(dataGrid: DataGrid<RowData>, rowLocation: RowLocationRo<RowData>, columnIndex: Int) : this(dataGrid) {
		set(rowLocation)
		this.columnIndex = columnIndex
	}

	override val column: DataGridColumn<RowData, *>
		get() = dataGrid.columns[columnIndex]

	override val isValid: Boolean
		get() = super.isValid && columnIndex >= 0 && columnIndex < dataGrid.columns.size

	override fun copy(): CellLocation<RowData> {
		return CellLocation(dataGrid, position, columnIndex)
	}

	fun set(other: CellLocationRo<RowData>): CellLocation<RowData> {
		super.set(other)
		columnIndex = other.columnIndex
		return this
	}

	override val hasPreviousCell: Boolean
		get() = hasPreviousRow || columnIndex > 0

	override val hasNextCell: Boolean
		get() = hasNextRow || columnIndex < dataGrid.columns.lastIndex

	fun moveToPreviousCell() {
		columnIndex--

		if (columnIndex < 0) {
			moveToPreviousRow()
			columnIndex = dataGrid.columns.lastIndex
		}
	}

	/**
	 * Iterates the column to the right, if the column passes the rightmost column, the column wraps back to 0 and
	 * the row is incremented.
	 */
	fun moveToNextCell() {
		columnIndex++

		if (columnIndex >= dataGrid.columns.size) {
			moveToNextRow()
			columnIndex = 0
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is CellLocationRo<*>) return false
		if (position != other.position) return false
		if (columnIndex != other.columnIndex) return false
		return true
	}

	override fun hashCode(): Int {
		var result = groupIndex
		result = 31 * result + groupPosition
		result = 31 * result + columnIndex
		return result
	}

	override fun toString(): String {
		return "CellLocation(position=$position, groupIndex=$groupIndex, rowIndex=$rowIndex, columnIndex=$columnIndex)"
	}
}

/**
 * Calls [CellLocation.moveToPreviousCell] until there are no more previous cells, or the [predicate] has returned true.
 * @return Returns true if the predicate was ever matched.
 */
fun <T : CellLocation<*>> T.moveToPreviousCellUntil(predicate: (T) -> Boolean): Boolean {
	while (hasPreviousCell) {
		moveToPreviousCell()
		if (predicate(this)) return true
	}
	return false
}

/**
 * Calls [CellLocation.moveToNextCell] until there are no more next cells, or the [predicate] has returned true.
 * @return Returns true if the predicate was ever matched.
 */
fun <T : CellLocation<*>> T.moveToNextCellUntil(predicate: (T) -> Boolean): Boolean {
	while (hasNextCell) {
		moveToNextCell()
		if (predicate(this)) return true
	}
	return false
}