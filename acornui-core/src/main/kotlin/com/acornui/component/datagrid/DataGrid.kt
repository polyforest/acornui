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

@file:Suppress("CssReplaceWithShorthandSafely", "CssInvalidPropertyValue", "CssUnresolvedCustomProperty",
	"CssNoGenericFontName", "MemberVisibilityCanBePrivate", "unused"
)

package com.acornui.component.datagrid

import com.acornui.Disposable
import com.acornui.component.*
import com.acornui.component.input.Button
import com.acornui.component.style.cssClass
import com.acornui.component.style.cssVar
import com.acornui.component.text.TextField
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.dom.*
import com.acornui.google.Icons
import com.acornui.input.*
import com.acornui.observe.DataChangeEvent
import com.acornui.recycle.Clearable
import com.acornui.recycle.recycle
import com.acornui.signal.SignalSubscription
import com.acornui.signal.signal
import com.acornui.skins.CssProps
import com.acornui.time.nextFrameCallback
import kotlinx.browser.document
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.ParentNode
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.Delegates
import com.acornui.dom.footer as footerEl

open class DataGrid<E>(owner: Context) : Div(owner) {

	/**
	 * This grid's [data] has changed.
	 */
	val dataChanged = signal<DataChangeEvent<List<E>>>()

	class RowSubmittedEvent<E>(

		/**
		 * The row index within [data]
		 */
		val index: Int,

		/**
		 * The row index within [dataView]
		 */
		val viewIndex: Int,

		val oldData: E,
		val newData: E
	) : Event()

	/**
	 * Dispatched when the row editor form has been submitted and the form is valid.
	 * This is typically when data requests should be made.
	 * @see autoMutate
	 */
	val rowSubmitted = signal<RowSubmittedEvent<E>>()

	class RowEditEvent<E>(val item: E?) : Event()

	/**
	 * The currently edited row has changed. This may be cancelled.
	 */
	val rowEdited = signal<RowEditEvent<E>>()

	var rowBuilder: (DataGridRow<E>.() -> Unit)? = null
		private set

	/**
	 * Sets the callback to populate a new row with cells.
	 * Rows are recycled and should set their values within [DataGridRow.data] blocks.
	 *
	 * Example:
	 *
	 * ```
	 * rows = {
	 *   +cell {
	 *     data {
	 *       label = it.name
	 *     }
	 *     tabIndex = 0
	 *   }
	 * }
	 * ```
	 */
	fun rows(builder: DataGridRow<E>.() -> Unit) {
		displayRows.forEach(Disposable::dispose)
		displayRows.clear()
		rowBuilder = builder
		refreshRows()
	}

	var data: List<E> = emptyList()
		set(value) {
			val old = field
			if (old == value) return
			field = value
			dataChanged.dispatch(DataChangeEvent(old, value))
			refreshRows()
		}

	var sort: List<ColumnSort<E>> = emptyList()
		set(value) {
			if (field == value) return
			field = value
			refreshRows()
		}

	fun sort(vararg columnSort: ColumnSort<E>) {
		this.sort = columnSort.toList()
	}

	private fun sortComparator(a: E, b: E): Int {
		for (columnSort in sort) {
			if (columnSort.direction == ColumnSortDirection.NONE) continue
			val v1 = columnSort.selector(a)
			val v2 = columnSort.selector(b)
			val diff = compareValues(v1, v2)
			if (diff != 0) return if (columnSort.direction == ColumnSortDirection.ASCENDING) diff else -diff
		}
		return 0
	}

	/**
	 * The data with any view transformations such as sorting.
	 */
	var dataView: List<E> = emptyList()
		private set

	/**
	 * Necessary only for Safari
	 * https://stackoverflow.com/questions/57934803/workaround-for-a-safari-position-sticky-webkit-sticky-bug
	 */
	val mainContainer = addChild(div {
		addClass(DataGridStyle.mainContainerStyle)
	})

	val header = mainContainer.addElement(div {
		addClass(DataGridStyle.headerRowStyle)
	})

	/**
	 * Calls the specified function [block] with [header] as its receiver.
	 */
	fun header(block: UiComponent.() -> Unit) {
		header.apply(block)
	}

	val contents = mainContainer.addElement(div {
		addClass(DataGridStyle.contentsContainerStyle)
	})

	private val displayRows = ArrayList<DataGridRow<E>>()

	private var isSecondSaveAttempt = false

	/**
	 * The row editor. This may be configured via [rowEditor]
	 */
	val rowEditor = dataGridRowEditor<E> {
		keyPressed.listen {
			if (it.keyCode == Ascii.ENTER && it.shiftKey)
				if (submitRow(reportValidity = false))
					editPreviousRow()
		}
		submitted.listen {
			if (submitRow(reportValidity = false))
				editNextRow()
		}
		input.listen {
			isSecondSaveAttempt = false
		}
		focusedOutContainer.listen {
			if (submitRow() || isSecondSaveAttempt)
				editRow(null)
			else
				isSecondSaveAttempt = true
		}
	}

	/**
	 * Returns the currently edited row.
	 */
	val editedRow: E?
		get() = rowEditor.data

	/**
	 * Returns the currently focused cell within the row, or -1 if the row isn't focused.
	 */
	val editedCellIndex: Int
		get() = rowEditor.activeCellIndex

	private val UiComponent.activeCellIndex: Int
		get() {
			val activeElement = document.activeElement ?: return -1
			return elements.indexOfFirst {
				it.dom.contains(activeElement)
			}
		}

	/**
	 * Calls the specified function [block] with [rowEditor] as its receiver.
	 */
	fun rowEditor(block: DataGridRowEditor<E>.() -> Unit) {
		rowEditor.apply(block)
	}

	val footer = mainContainer.addElement(footerEl {
		addClass(DataGridStyle.footerRowStyle)
	})

	/**
	 * Calls the specified function [block] with [footer] as its receiver.
	 */
	fun footer(block: UiComponent.() -> Unit) {
		footer.apply(block)
	}

	private val refreshRows = nextFrameCallback {
		dataView = data.sortedWith(::sortComparator)

		header.elements.forEach {
			if (it is HeaderCell) {
				it.sortDisplay = ColumnSortDirection.NONE
			}
		}
		for (columnSort in sort) {
			columnSort.headerCell?.sortDisplay = columnSort.direction
		}

		val previouslyFocused = editedCellIndex
		val previouslyEdited = editedRow
		editRow(null)
		recycle(dataView, displayRows, factory = { _: E, _: Int ->
			createRow()
		}, configure = { element: DataGridRow<E>, item: E, index: Int ->
			element.data = item
			contents.removeElement(element)
			contents.addElement(index, element)
		}, disposer = {
			it.dispose()
		}, retriever = { element ->
			element.data
		})
		editRow(previouslyEdited, previouslyFocused)
	}

	private fun createRow() = dataGridRow<E> {
		(rowBuilder ?: error("rows not set")).invoke(this)
		focusedInContainer.listen {
			editRow(data, activeCellIndex)
		}
	}

	/**
	 * If true, the [data] list will automatically mutate to the new value produced by the row editor.
	 */
	var autoMutate = true

	init {
		addClass(PanelStyle.colors)
		addClass(DataGridStyle.dataGrid)

		keyPressed.listen {
			when (it.keyCode) {
				Ascii.ESCAPE -> editRow(null)
			}
		}

		rowSubmitted.listen { e ->
			if (autoMutate) {
				data = data.toPersistentList().mutate {
					it[e.index] = e.newData!!
				}
			}
		}
	}

	/**
	 * Sets the currently edited row.
	 * Dispatches a [rowEdited] signal, which may be cancelled.
	 * @param item The data item to edit.
	 * @param cellIndex The index of the cell within the row to give focus. If there are no tabbable elements within
	 * that cell, the first tabbable element within the row will be focused.
	 */
	fun editRow(item: E?, cellIndex: Int = 0) {
		if (editedRow == item) return

		val e = RowEditEvent(item)
		rowEdited.dispatch(e)
		if (e.defaultPrevented) return

		val editedRowOldIndex = displayRows.indexOfFirst { it.data === editedRow }
		if (editedRowOldIndex != -1) contents.addElement(editedRowOldIndex, displayRows[editedRowOldIndex])
		contents.removeElement(rowEditor)
		rowEditor.data = null
		if (item == null) return

		// Swap the display row with the row editor.
		val editedRowIndex = displayRows.indexOfFirst { it.data === item }
		if (editedRowIndex == -1) return
		rowEditor.data = item
		val editedDisplayRow = displayRows[editedRowIndex]
		contents.removeElement(editedDisplayRow)
		contents.addElement(editedRowIndex, rowEditor)

		// Set focus
		val cellToFocus = rowEditor.elements.getOrNull(cellIndex)?.dom?.unsafeCast<ParentNode>()
		var firstInput = cellToFocus?.getTabbableElements()?.firstOrNull()

		if (firstInput == null) {
			// The requested cell isn't focusable, pick either the first or last focusable element.
			val allTabbable = rowEditor.dom.getTabbableElements()
			firstInput =
				if (cellIndex <= rowEditor.elements.size / 2) allTabbable.firstOrNull() else allTabbable.lastOrNull()
		}

		firstInput?.focus()
		if (firstInput?.tagName.equals("input", ignoreCase = true))
			firstInput.unsafeCast<HTMLInputElement>().select()
	}

	/**
	 * Opens the next row for editing.
	 * Note: This does not submit the current row.
	 * @see submitRow
	 */
	fun editNextRow() {
		val i = dataView.indexOf(editedRow)
		if (i != -1) editRow(dataView.getOrNull(i + 1))
		else editRow(null)
	}

	/**
	 * Opens the previous row for editing.
	 * Note: This does not submit the current row.
	 * @see submitRow
	 */
	fun editPreviousRow() {
		val i = dataView.indexOf(editedRow)
		editRow(dataView.getOrNull(i - 1))
	}

	/**
	 * Submits the current row editor form.
	 * The default behavior after a row has been submitted is if the form is valid and [rowSubmitted] is not default
	 * prevented, then the next row will be opened.
	 * @return Returns true if the submission was successful.
	 */
	fun submitRow(reportValidity: Boolean = true): Boolean {
		if (rowEditor.data == null) return false
		val form = rowEditor.form

		return if (form.checkValidity()) {
			val previousData = rowEditor.data!!
			val submittedEvent = RowSubmittedEvent(
				data.indexOf(previousData),
				dataView.indexOf(previousData),
				previousData,
				rowEditor.dataBuilder.invoke(rowEditor.data!!)
			)
			if (submittedEvent.oldData != submittedEvent.newData) {
				rowSubmitted.dispatch(submittedEvent)
			}
			!submittedEvent.defaultPrevented
		} else {
			if (reportValidity)
				form.reportValidity()
			false
		}
	}

	/**
	 * Closes the editor without submission or validation.
	 */
	fun closeEditor() = editRow(null)

}


object DataGridStyle {

	val dataGrid by cssClass()
	val mainContainerStyle by cssClass()
	val headerRowStyle by cssClass()
	val footerRowStyle by cssClass()
	val headerCellStyle by cssClass()
	val contentsContainerStyle by cssClass()
	val rowStyle by cssClass()
	val rowEditorStyle by cssClass()
	val cellStyle by cssClass()
	val editorCellStyle by cssClass()

	val sortedAsc by cssClass()
	val sortedDesc by cssClass()

	val borderThickness by cssVar()

	init {

		addStyleToHead(
			"""
	$dataGrid {	
		display: flex;
		flex-direction: column;
		box-sizing: border-box;
		position: relative;
		overflow: auto;
		scroll-padding-top: 4em; /* To prevent the sticky header from covering up the first row when tabbing */
		$borderThickness: 1px;
	}
	
	$cellStyle {
		padding: calc(${CssProps.inputPadding.v} + ${CssProps.borderThickness.v});
		display: flex;
		align-items: center;
	}
	
	$mainContainerStyle {
		min-width: fit-content;
		min-height: fit-content;
		grid-template-columns: inherit;
		align-items: inherit;
		align-content: inherit;
		justify-items: inherit;	
		justify-content: inherit;
	
		display: grid;
		grid-auto-rows: min-content;
		row-gap: ${borderThickness.v};
		column-gap: ${borderThickness.v};
		background-color: ${CssProps.borderColor.v};
	}
	
	$headerRowStyle {
		display: contents;
	}
	
	$headerRowStyle > div:first-child {
		border-top-left-radius: var(--br);
	}
	
	$headerRowStyle > div {
		--br: calc(${CssProps.borderRadius.v} - ${CssProps.borderThickness.v});
		border-radius: 0;
		border: none;
		position: -webkit-sticky;
		position: sticky;
		top: 0;
		font-weight: bolder;
	}
	
	$footerRowStyle {
		display: contents;
	}
	
	$footerRowStyle > div {
		position: -webkit-sticky;
		position: sticky;
		bottom: 0;
		color: inherit;
		background: inherit;
	}
	
	$dataGrid *:focus {
		/* Take the default focus transition, make it snappier and inset to be better for data grid cells. */
		box-shadow: inset 0 0 0 ${CssProps.focusThickness.v} ${CssProps.focus.v};
		border-color: ${CssProps.focus.v};
		transition: box-shadow 0.0s ease-in-out;
	}
	
	$contentsContainerStyle {
		display: contents;
		border-bottom-left-radius: inherit;
	}
	
	$rowStyle {
		display: contents;
	}
	
	$rowEditorStyle > form {
		display: contents;
	}
	
	$editorCellStyle {
		padding: 0;
	}
	
	$contentsContainerStyle > $rowStyle:nth-child(2n+0) $cellStyle {
		background: ${CssProps.dataRowEvenBackground.v};
	}
	
	$contentsContainerStyle > $rowStyle:nth-child(2n+1) $cellStyle {
		background: ${CssProps.dataRowOddBackground.v};
	}
	
	$headerCellStyle {
		user-select: none;
		-moz-user-select: none;
		-webkit-user-select: none;
		-webkit-touch-callout: none;
	}
	
	$headerCellStyle$sortedAsc::after, $headerCellStyle$sortedDesc::after {
		content: "${Icons.ARROW_DOWNWARD.toChar()}";
		font-family: "Material Icons";
		display: inline-block;
		white-space: nowrap;
		-webkit-font-smoothing: antialiased;
		transition: transform 0.3s ease-in-out;
	}
	
	$headerCellStyle$sortedDesc::after {
		transform: rotate(180deg);
	}
				"""
		)
	}
}

inline fun <E> Context.dataGrid(init: ComponentInit<DataGrid<E>> = {}): DataGrid<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return DataGrid<E>(this).apply(init)
}

inline fun <E> Context.dataGrid(data: List<E>, init: ComponentInit<DataGrid<E>> = {}): DataGrid<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return DataGrid<E>(this).apply {
		this.data = data
		init()
	}
}

open class DataGridRow<E>(owner: Context) : Div(owner) {

	val dataChanged = signal<DataChangeEvent<E>>()

	init {
		addClass(DataGridStyle.rowStyle)
	}

	/**
	 * Sets the row's data.
	 */
	var data: E? = null
		set(value) {
			val old = field
			if (old == value) return
			field = value
			dataChanged.dispatch(DataChangeEvent(old, value))
		}

	/**
	 * Invokes the callback with the new data when this row's data has changed.
	 */
	fun data(callback: (E) -> Unit): SignalSubscription = dataChanged.listen {
		if (it.newValue != null)
			callback(it.newValue)
	}
}

private inline fun <E> Context.dataGridRow(init: ComponentInit<DataGridRow<E>> = {}): DataGridRow<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return DataGridRow<E>(this).apply(init)
}

open class DataGridRowEditor<E>(owner: Context) : DataGridRow<E>(owner), Clearable {

	val form = addChild(form {
		preventAction()
		+hiddenSubmit()
	})

	/**
	 * Dispatched when the row editor form has been submitted.
	 */
	val submitted = form.submitted

	init {
		addClass(DataGridStyle.rowEditorStyle)
	}

	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		form.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: WithNode) {
		form.removeElement(element)
	}

	/**
	 * When the row is submitted, this will construct the new data element.
	 */
	var dataBuilder: ((E) -> E) by Delegates.notNull()

	override fun clear() {
		data = null
		clearElements(dispose = true)
	}
}

private inline fun <E> Context.dataGridRowEditor(init: ComponentInit<DataGridRowEditor<E>> = {}): DataGridRowEditor<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return DataGridRowEditor<E>(this).apply(init)
}

class HeaderCell(owner: Context) : Button(owner) {

	private var clickListener: SignalSubscription? = null

	init {
		addClass(DataGridStyle.headerCellStyle)
	}

	fun <E, T : Comparable<T>> DataGrid<E>.bindSortingBy(columnSort: (row: E) -> T) {
		clickListener?.dispose()
		clickListener = this@HeaderCell.clicked.listen {
			if (!it.isHandled) {
				it.handle()
				if (it.ctrlKey) {
					sort = emptyList()
				} else {
					val newDirection = if (sortDisplay == ColumnSortDirection.ASCENDING) ColumnSortDirection.DESCENDING else ColumnSortDirection.ASCENDING
					this.sort(ColumnSort(newDirection, this@HeaderCell, columnSort))
				}
			}
		}
		this@HeaderCell.longTouched.listen {
			this.sort()
		}

		this@HeaderCell.longPressed.listen {
			this.sort()
		}
	}

	/**
	 * Adjusts the css class list to represent this column's sorting.
	 * This will be modified by the DataGrid.
	 */
	var sortDisplay = ColumnSortDirection.NONE
		set(value) {
			if (field == value) return
			field = value
			removeClass(DataGridStyle.sortedAsc)
			removeClass(DataGridStyle.sortedDesc)
			if (value == ColumnSortDirection.ASCENDING) {
				addClass(DataGridStyle.sortedAsc)
			} else if (value == ColumnSortDirection.DESCENDING) {
				addClass(DataGridStyle.sortedDesc)
			}
		}
}

data class ColumnSort<E>(

	/**
	 * The direction of the sort.
	 */
	val direction: ColumnSortDirection = ColumnSortDirection.ASCENDING,

	/**
	 * If set, the header cell's [HeaderCell.sortDisplay] will be set.
	 */
	val headerCell: HeaderCell? = null,

	/**
	 * Selects the comparable value on which to sort.
	 */
	val selector: (E) -> Comparable<*>?

)


enum class ColumnSortDirection {
	NONE,
	ASCENDING,
	DESCENDING
}

inline fun Context.headerCell(label: String = "", init: ComponentInit<HeaderCell> = {}): HeaderCell {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return HeaderCell(this).apply {
		this.label = label
		init()
	}
}

inline fun Context.cell(label: String = "", init: ComponentInit<TextField> = {}): TextField {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return text {
		tabIndex = 0
		addClass(DataGridStyle.cellStyle)
		this.label = label
		init()
	}
}

inline fun Context.editorCell(init: ComponentInit<UiComponentImpl<HTMLDivElement>> = {}): UiComponentImpl<HTMLDivElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return div {
		addClass(DataGridStyle.cellStyle)
		addClass(DataGridStyle.editorCellStyle)

		init()
	}
}