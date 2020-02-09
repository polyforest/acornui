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

import com.acornui.collection.ObservableList
import com.acornui.component.InteractivityMode
import com.acornui.component.UiComponent
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.VAlign
import com.acornui.di.Context
import com.acornui.observe.ObservableBase

abstract class DataGridColumn<in RowData, CellData> : ObservableBase() {

	/**
	 * If false, this column will not be shown.
	 */
	var visible by watched(true)

	fun toggleVisible() {
		visible = !visible
	}

	/**
	 * If true, elements in this column can be edited.
	 * If this is true, [createEditorCell] must be implemented.
	 */
	var editable by watched(true)

	/**
	 * If true, this column can be resized by the user.
	 */
	var resizable by watched(true)

	/**
	 * If true, the column can be sorted by the user.
	 */
	var sortable by watched(false)

	/**
	 * If true, the column can be reordered (moved) by the user.
	 */
	var reorderable by watched(true)

	var width: Float? by watched(null)
	var widthPercent: Float? by watched(null)
	var minWidth: Float by watched(8f)

	/**
	 * The vertical alignment of the header cells.
	 * If this is not set, the data grid's style will be used.
	 */
	var headerCellVAlign: VAlign? by watched(null)

	/**
	 * The horizontal alignment of the header cells.
	 * If this is not set, the data grid's style will be used.
	 */
	var headerCellHAlign: HAlign? by watched(null)

	/**
	 * The vertical alignment of the body cells.
	 * If this is not set, the data grid's style will be used.
	 */
	var cellVAlign: VAlign? by watched(null)

	/**
	 * The horizontal alignment of the body cells.
	 * If this is not set, the data grid's style will be used.
	 */
	var cellHAlign: HAlign? by watched(null)

	/**
	 * A flexible column will flex its size to fit within the available bounds of the container.
	 * A column is considered flexible if:
	 * Neither [width] or [widthPercent] have been set, [flexible] is true, or [flexible] is null and [resizable] is
	 * true and [widthPercent] is not null.
	 */
	var flexible: Boolean? by watched(null)

	/**
	 * Constructs a new header cell to be placed at the top of the grid. This should not include the header background
	 * or sorting arrows.
	 * The cell is cached, so this method should not return inconsistent results.
	 */
	abstract fun createHeaderCell(owner: Context): UiComponent

	/**
	 * The header cell interactivity mode. This should be [InteractivityMode.NONE] unless the header cell has something
	 * special that's interactive.
	 */
	open val headerCellInteractivityMode: InteractivityMode
		get() = InteractivityMode.NONE

	/**
	 * Constructs a new cell for this column.
	 * The cell is cached, so this method should not return inconsistent results.
	 */
	abstract fun createCell(owner: Context): DataGridCell<CellData>

	/**
	 * The cell's interactivity mode. This should be [InteractivityMode.NONE] unless the cell has something
	 * special that's interactive.
	 */
	open val cellInteractivityMode: InteractivityMode
		get() = InteractivityMode.NONE

	/**
	 * Constructs a new footer cell for this column.
	 * This should be overriden if [DataGrid.showFooterRow] or any group's [DataGridGroup.showFooter] property is true.
	 */
	open fun createFooterRowCell(owner: Context, list: ObservableList<RowData>): UiComponent {
		throw Exception("A footer cell was requested, but createFooterCell was not implemented.")
	}

	/**
	 * The footer cell's interactivity mode. This should be [InteractivityMode.NONE] unless the cell has something
	 * special that's interactive.
	 */
	open val footerCellInteractivityMode: InteractivityMode
		get() = InteractivityMode.NONE

	/**
	 * Retrieves the column value from the given row.
	 */
	abstract fun getCellData(row: RowData): CellData

	/**
	 * Sets the column value.
	 */
	abstract fun setCellData(row: RowData, value: CellData)

	open fun compareRows(row1: RowData, row2: RowData): Int {
		throw Exception("Column is sortable but compareRows was not overridden.")
	}

	/**
	 * Constructs a new editor cell.
	 */
	abstract fun createEditorCell(owner: Context): DataGridEditorCell<CellData>

	fun getPreferredWidth(availableWidth: Float?): Float? {
		var w = if (availableWidth == null || widthPercent == null) width else widthPercent!! * availableWidth
		if (w != null && minWidth > w) w = minWidth
		return w
	}

	/**
	 * @see flexible
	 */
	fun getIsFlexible(): Boolean {
		return (width == null && widthPercent == null) || flexible ?: (resizable == true && widthPercent != null)
	}
}
