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

@file:Suppress("UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER", "unused", "RedundantModalityModifier")

package com.acornui.component.datagrid

import com.acornui.assertionsEnabled
import com.acornui.collection.*
import com.acornui.component.*
import com.acornui.component.layout.*
import com.acornui.component.scroll.*
import com.acornui.component.style.*
import com.acornui.component.text.TextStyleTags
import com.acornui.core.EqualityCheck
import com.acornui.core.cache.*
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.di.own
import com.acornui.core.floor
import com.acornui.core.focus.blurred
import com.acornui.core.focus.focus
import com.acornui.core.focus.focusSelf
import com.acornui.core.focus.isFocused
import com.acornui.core.input.Ascii
import com.acornui.core.input.KeyState
import com.acornui.core.input.interaction.ClickInteractionRo
import com.acornui.core.input.interaction.KeyInteractionRo
import com.acornui.core.input.interaction.click
import com.acornui.core.input.interaction.dragAttachment
import com.acornui.core.input.keyDown
import com.acornui.core.input.wheel
import com.acornui.math.Bounds
import com.acornui.math.Corners
import com.acornui.math.MathUtils.clamp
import com.acornui.math.Pad
import com.acornui.math.Vector2
import com.acornui.observe.IndexBinding
import com.acornui.signal.*
import kotlin.math.abs
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// TODO: Selection

/**
 * A component for displaying a virtualized table of data.
 *
 * Key features include:
 * Columns - resizable, reorderable, sortable
 * Grouping - header and footer rows, collapsible
 * Cell editing
 * Toss scrolling
 * Styling
 */
class DataGrid<E>(
		owner: Owned
) : ContainerImpl(owner) {

	constructor(owner: Owned, data: List<E>) : this(owner) {
		data(data)
	}

	constructor(owner: Owned, data: ObservableList<E>) : this(owner) {
		data(data)
	}

	private val cellClickedCancel = Cancel()

	private val _cellClicked = own(Signal2<CellLocationRo<E>, Cancel>())

	/**
	 * Dispatched when the contents area has been clicked. Use [CellLocation.isValid] to determine whether or not
	 * a valid cell has been clicked before getting the row/column.
	 * If the cancel object is invoked, the default behavior for clicking a cell will be prevented. (I.e. cell editing)
	 */
	val cellClicked = _cellClicked.asRo()

	val style = bind(DataGridStyle())

	var hScrollPolicy: ScrollPolicy by validationProp(ScrollPolicy.OFF, COLUMNS_WIDTHS_VALIDATION or ValidationFlags.SIZE_CONSTRAINTS)
	var vScrollPolicy: ScrollPolicy by validationProp(ScrollPolicy.AUTO, COLUMNS_WIDTHS_VALIDATION)

	/**
	 * The maximum number of rows to display.
	 * Note that if maxRows is too large and minRowHeight is too small, a very large number of cells may be created.
	 */
	var maxRows by validationProp(100, ValidationFlags.LAYOUT)

	/**
	 * The minimum height a row can be.
	 * Note that if maxRows is too large and minRowHeight is too small, a very large number of cells may be created.
	 */
	var minRowHeight by validationProp(15f, ValidationFlags.LAYOUT)

	/**
	 * If set, each row will be this height, instead of being measured based on the grid cells.
	 */
	var rowHeight: Float? by validationProp(null, ValidationFlags.LAYOUT)

	/**
	 * When the data has changed, this method will be used when recovering state such as selection, the currently
	 * edited cell, etc.
	 * This is particularly important to set when using immutable data and the old element != new element.
	 */
	var equalityCheck: EqualityCheck<E> = { a, b -> a == b }
		set(value) {
			field = value
			cellFocusRow.equality = value
		}

	/**
	 * The starting y offset of the visible rows.
	 * This is local to the contents.
	 */
	private val rowsY: Float
		get() = if (rowHeights.isEmpty()) 0f else -rowHeights.first() * (vScrollModel.value - vScrollModel.value.floor())

	/**
	 * The currently visible row heights.
	 */
	private val rowHeights = ArrayList<Float>()

	private var background: UiComponent? = null

	private val _dataView = own(ListView<E>())
	val dataView: ListViewRo<E> = _dataView

	private val _columns = own(ActiveList<DataGridColumn<E, *>>())

	/**
	 * The columns this data grid will display.
	 * This list should be unique, that is no two columns should equal each other.
	 */
	val columns: MutableObservableList<DataGridColumn<E, *>> = _columns

	/**
	 * Add data grid groups to group data under collapsible headers.
	 */
	internal val _groups = own(ActiveList<DataGridGroup<E>>())

	/**
	 * The groups this data grid will display. If this is empty, there will be no grouping.
	 * This list should be unique, that is no two groups should equal each other.
	 */
	val groups: MutableObservableList<DataGridGroup<E>> = _groups

	/**
	 * The case when there are no groups.
	 */
	private val defaultGroups = activeListOf(DataGridGroup<E>().apply {
		showHeader = false
		showFooter = false
		filter = null
	})

	/**
	 * If the set groups are empty, use defaultGroups, which is simply showing all data.
	 */
	internal val displayGroups: List<DataGridGroup<E>>
		get() = if (_groups.isEmpty()) defaultGroups else _groups


	private var _observableData: ObservableList<E> = emptyObservableList()
	private var _data: List<E> = emptyList()

	/**
	 * The data source for this list, as set via `data()`
	 */
	val data: List<E>
		get() = _data

	fun data(source: List<E>?) {
		_data = source ?: emptyList()
		_observableData = emptyObservableList()
		_dataView.data(_data)
		cellFocusRow.data(source)
	}

	fun data(source: ObservableList<E>?) {
		_data = source ?: emptyList()
		_observableData = source ?: emptyObservableList()
		_dataView.data(_observableData)
		cellFocusRow.data(source)
	}

	//------------------------------------------
	// Display children
	//------------------------------------------

	private val clipper = addChild(scrollRect())

	/**
	 * Used for measurement of max v scroll position.
	 */
	private val bottomContents = clipper.addElement(container { interactivityMode = InteractivityMode.NONE; alpha = 0f })
	private val usedBottomGroupHeaders = UsedTracker<UiComponent>()
	private val rowBackgrounds = clipper.addElement(container())
	private val rowBackgroundsCache = IndexedCache { style.rowBackground(rowBackgrounds) }
	private val contents = clipper.addElement(container())
	private val columnDividersContents = clipper.addElement(container { interactivityMode = InteractivityMode.NONE })
	private val groupHeadersAndFooters = clipper.addElement(container { interactivityMode = InteractivityMode.CHILDREN })
	private val usedGroupHeaders = UsedTracker<UiComponent>()
	private val editorCellContainer = clipper.addElement(container { interactivityMode = InteractivityMode.CHILDREN })

	private val headerCellBackgrounds = clipper.addElement(container { interactivityMode = InteractivityMode.CHILDREN })
	private val headerCells = clipper.addElement(container { interactivityMode = InteractivityMode.CHILDREN })
	private val usedHeaderCells = UsedTracker<UiComponent>()
	private val columnResizeHandles = clipper.addElement(container { interactivityMode = InteractivityMode.CHILDREN })
	private var headerDivider: UiComponent? = null
	private val columnDividersHeader = clipper.addElement(container { interactivityMode = InteractivityMode.NONE })

	private var cellFocusHighlight: UiComponent? = null
	private var editorCell: DataGridEditorCell<*>? = null
	private var cellFocusRow = IndexBinding<E>()
	private var cellFocusCol = IndexBinding(_columns)

	private val feedback = clipper.addElement(container { interactivityMode = InteractivityMode.NONE })
	private var columnMoveIndicator = feedback.addElement(rect { styleTags.add(COLUMN_MOVE_INDICATOR); visible = false })
	private var columnInsertionIndicator = feedback.addElement(vr { styleTags.add(COLUMN_INSERTION_INDICATOR); visible = false })

	private val hScrollBar = clipper.addElement(hScrollBar { visible = false; styleTags.add(SCROLL_BAR) })
	private val vScrollBar = clipper.addElement(vScrollBar { visible = false; styleTags.add(SCROLL_BAR) })

	/**
	 * Use this to set the horizontal scroll position.
	 * The horizontal position is the column position index.
	 */
	val hScrollModel: ScrollModel
		get() = hScrollBar.scrollModel

	/**
	 * Use this to set the vertical scroll position.
	 * The vertical position is the row position index.
	 */
	val vScrollModel: ScrollModel
		get() = vScrollBar.scrollModel

	private val tossScroller = contents.enableTossScrolling()
	private val tossBinding = own(TossScrollModelBinding(tossScroller, hScrollModel, vScrollModel))

	/**
	 * Toggles whether or not toss scrolling is enabled.
	 */
	var tossEnabled: Boolean
		get() = tossScroller.enabled
		set(value) {
			tossScroller.enabled = value
		}

	/**
	 * Stops the current momentum of any toss.
	 */
	fun stopToss() = tossScroller.stop()

	// Column sorting
	private var _sortColumn: DataGridColumn<E, *>? = null
	private var _sortDirection = ColumnSortDirection.NONE
	private var _customSortComparator: SortComparator<E>? = null
	private var _customSortReversed = false

	/**
	 * If true, the user may click on header cells to sort them.
	 */
	var columnSortingEnabled by validationProp(true, ValidationFlags.LAYOUT)

	/**
	 * If true, the user may drag header cells to reorder them.
	 */
	var columnReorderingEnabled by validationProp(true, ValidationFlags.LAYOUT)

	private var _columnResizingEnabled: Boolean = true

	/**
	 * If true, the user may resize columns.
	 * To prevent a single column from being resized, change the [DataGridColumn.resizable] property.
	 */
	var columnResizingEnabled: Boolean
		get() = _columnResizingEnabled
		set(value) {
			_columnResizingEnabled = value
			columnResizeHandles.interactivityMode = if (value) InteractivityMode.CHILDREN else InteractivityMode.NONE
		}

	/**
	 * If true, the user may edit cells.
	 * To prevent a single column from being edited, change the [DataGridColumn.editable] property.
	 * Note: This will not close a currently opened editor.
	 * @see [closeCellEditor]
	 */
	var editable: Boolean = false

	private var sortDownArrow: UiComponent? = null
	private var sortUpArrow: UiComponent? = null
	private var topRight: UiComponent? = null

	private val columnCaches = own(ObservableListMapping(_columns, {
		column ->
		column.changed.add(this::columnChangedHandler)
		invalidateColumnWidths()
		ColumnCache(headerCells, column)
	}, {
		cache ->
		cache.column.changed.remove(this::columnChangedHandler)
		dirty(cache)
		invalidateColumnWidths()
	}))

	private val groupCaches = own(ObservableListMapping(_groups, {
		group ->
		group.changed.add(this::groupChangedHandler)
		invalidateLayout()
		GroupCache(group)
	}, {
		cache ->
		cache.group.changed.remove(this::groupChangedHandler)
		dirty(cache)
		invalidateLayout()
	}))

	/**
	 * Marked which columns are used in an update layout.
	 */
	private val usedColumns = UsedTracker<ColumnCache>()

	private val defaultGroupCache = arrayListOf(GroupCache(defaultGroups[0]))

	/**
	 * @suppress
	 */
	internal val displayGroupCaches: List<GroupCache>
		get() = if (_groups.isEmpty()) defaultGroupCache else groupCaches

	private val rowIterator = RowLocation(this)

	/**
	 * @suppress
	 */
	internal var _totalRows = 0

	/**
	 * The total number of rows, counting header and footer rows.
	 */
	val totalRows: Int
		get() {
			validateLayout()
			return _totalRows
		}

	/**
	 * Returns true if there is a cell editor opened.
	 */
	val isEditing: Boolean
		get() = editorCell != null

	init {
		focusEnabled = true
		styleTags.add(Companion)
		if (assertionsEnabled) {
			_columns.bindUniqueAssertion()
			_groups.bindUniqueAssertion()
		}
		validation.addNode(COLUMNS_WIDTHS_VALIDATION, 0, ValidationFlags.LAYOUT, this::updateColumnWidths)
		validation.addNode(COLUMNS_VISIBLE_VALIDATION, COLUMNS_WIDTHS_VALIDATION, ValidationFlags.LAYOUT, this::updateColumnVisibility)
		hScrollBar.scrollModel.snap = 1f
		vScrollModel.changed.add { invalidateLayout() }
		hScrollModel.changed.add { invalidate(COLUMNS_VISIBLE_VALIDATION) }

		watch(style) {
			topRight?.dispose()
			topRight = it.headerCellBackground(this)
			clipper.addElementBefore(topRight!!, headerCellBackgrounds)
			topRight!!.interactivityMode = InteractivityMode.NONE

			headerDivider?.dispose()
			headerDivider = clipper.addOptionalElement(it.headerDivider(this))

			columnDividersContents.clearElements(dispose = true)
			columnDividersHeader.clearElements(dispose = true)

			clipper.style.borderRadii = Corners().set(it.borderRadius).deflate(it.borderThickness)
			background?.dispose()
			background = addChild(0, it.background(this))

			sortDownArrow?.dispose()
			sortDownArrow = headerCells.addElement(it.sortDownArrow(this))
			sortDownArrow!!.interactivityMode = InteractivityMode.NONE

			sortUpArrow?.dispose()
			sortUpArrow = headerCells.addElement(it.sortUpArrow(this))
			sortUpArrow!!.interactivityMode = InteractivityMode.NONE

			rowBackgroundsCache.disposeAndClear()

			cellFocusHighlight?.dispose()
			val cellFocusHighlight = editorCellContainer.addElement(it.cellFocusHighlight(this))
			cellFocusHighlight.interactivityMode = InteractivityMode.NONE
			cellFocusHighlight.includeInLayout = false
			cellFocusHighlight.visible = false
			this.cellFocusHighlight = cellFocusHighlight

			for (i in 0..columnResizeHandles.elements.lastIndex) {
				val resizeHandle = columnResizeHandles.elements[i] as Spacer
				resizeHandle.defaultWidth = style.resizeHandleWidth
			}

			invalidateColumnWidths()
		}

		_dataView.bind(this::invalidateLayout)

		// User interaction:

		contents.click().add(this::contentsClickedHandler)

		keyDown().add(this::keyDownHandler)

		wheel().add {
			vScrollModel.value += it.deltaY / vScrollBar.modelToPixels
		}

		blurred().add(this::blurredHandler)
	}

	private fun blurredHandler() {
		editorCellCheck()
		commitCellEditorValue()
		disposeCellEditor()
	}

	private fun keyDownHandler(event: KeyInteractionRo) {
		if (event.defaultPrevented()) return

		if (isEditing) {
			when (event.keyCode) {
				Ascii.HOME -> {
					event.handled = true
					val newLocation = cellFocusLocation!!
					newLocation.position = 0
					if (!newLocation.isElementRow) newLocation.moveToNextRowUntil{ it.isElementRow }
					commitCellEditorValue()
					focusCell(newLocation)
				}
				Ascii.END -> {
					event.handled = true
					val newLocation = cellFocusLocation!!
					newLocation.position = totalRows - 1
					if (!newLocation.isElementRow) newLocation.moveToPreviousRowUntil { it.isElementRow }

					commitCellEditorValue()
					focusCell(newLocation)
				}
				Ascii.PAGE_DOWN -> {
					event.handled = true
					val newLocation = cellFocusLocation!!
					vScrollModel.value = newLocation.position.toFloat()
					validateLayout()
					newLocation.position = clamp(newLocation.position + (rowHeights.lastIndex - 1), 0, totalRows - 1)
					if (!newLocation.isElementRow) newLocation.moveToNextRowUntil { it.isElementRow }

					commitCellEditorValue()
					focusCell(newLocation)
				}
				Ascii.PAGE_UP -> {
					event.handled = true
					val newLocation = cellFocusLocation!!
					newLocation.position = clamp(newLocation.position - (rowHeights.lastIndex - 1), 0, totalRows - 1)
					commitCellEditorValue()
					if (!newLocation.isElementRow) newLocation.moveToPreviousRowUntil { it.isElementRow }
					focusCell(newLocation)
				}
				Ascii.TAB -> {
					// Edit the next column
					event.handled = true
					if (event.shiftKey) focusPreviousCell(true) else focusNextCell(true)
					event.preventDefault() // Prevent default tab-focus behavior
				}
				Ascii.ENTER, Ascii.RETURN -> {
					// Edit the next row
					event.handled = true
					if (event.shiftKey) focusPreviousRow(true) else focusNextRow(true)
				}
				Ascii.ESCAPE -> {
					event.handled = true
					closeCellEditor(false)
				}
				else -> return
			}
			return
		} else {
			when (event.keyCode) {
				Ascii.HOME -> vScrollModel.value = 0f
				Ascii.END -> vScrollModel.value = totalRows - 1f
				Ascii.PAGE_DOWN -> vScrollModel.value += rowHeights.lastIndex - 1
				Ascii.PAGE_UP -> vScrollModel.value -= rowHeights.lastIndex - 1
				Ascii.DOWN -> vScrollModel.value++
				Ascii.UP -> vScrollModel.value--
				Ascii.RIGHT -> hScrollModel.value += 20f
				Ascii.LEFT -> hScrollModel.value -= 20f
				else -> return
			}
			event.handled = true
			return
		}
	}

	private val tmp = Vector2()

	/**
	 * Given a canvas coordinate, this method returns the cell location of that position.
	 * The cell position returned may be out of bounds, use [CellLocation.isValid] to check.
	 */
	fun getCellFromPosition(canvasX: Float, canvasY: Float): CellLocationRo<E> {
		validate(ValidationFlags.LAYOUT)
		val p = tmp
		canvasToLocal(p.set(canvasX, canvasY))
		val columnIndex = if (p.x < 0 || p.x > width) -1 else _columnPositions.sortedInsertionIndex(p.x + hScrollModel.value) - 1
		val headerHeight = headerCells.height
		if (firstVisibleColumn == -1 || p.y < headerHeight || p.y > height) return CellLocation(this, -1, columnIndex)

		rowIterator.position = vScrollModel.value.toInt() - 1
		var rowIndex = 0
		var measuredHeight = headerHeight + rowsY
		while (rowIterator.hasNextRow && measuredHeight < p.y) {
			rowIterator.moveToNextRow()
			measuredHeight += rowHeights[rowIndex++]
		}
		return CellLocation(this, rowIterator, columnIndex)
	}

	private fun contentsClickedHandler(event: ClickInteractionRo) {
		if (event.handled) return
		val cell = getCellFromPosition(event.canvasX, event.canvasY)
		event.handled = true
		_cellClicked.dispatch(cell, cellClickedCancel.reset())
		if (!cellClickedCancel.canceled) {
			if (isEditing) {
				commitCellEditorValue()
				disposeCellEditor()
			}
			if (editable) {
				focusCell(cell)
			}
		}
	}

	/**
	 * Given an index within the [data] list, this will return a [RowLocation] object representing the local position
	 * which takes into account filtering, sorting, and grouping.
	 * This will return null if the element is filtered out.
	 *
	 * Note - If the groups do not provide unique filtering, this method returns the first group found where the
	 * element is not filtered out.
	 *
	 * @param sourceIndex The index of the element within [data] to convert. This should be between 0 and
	 * `data.lastIndex` (inclusive)
	 */
	fun sourceIndexToLocal(sourceIndex: Int): RowLocation<E>? {
		val element = data[sourceIndex]
		if (_dataView.filter?.invoke(element) == false) return null
		var position = 0
		for (groupIndex in 0..displayGroupCaches.lastIndex) {
			val groupCache = displayGroupCaches[groupIndex]
			if (groupCache.showList && groupCache.list.filter?.invoke(element) != false) {
				val rowIndex = if (_groups.isEmpty()) _dataView.sourceIndexToLocal(sourceIndex) else groupCache.list.sourceIndexToLocal(_dataView.sourceIndexToLocal(sourceIndex))
				return RowLocation(this, position + rowIndex + groupCache.listStartIndex)
			}
			position += groupCache.size
		}
		return null
	}

	/**
	 * Returns a new CellLocation object representing the currently focused cell, or null if there is no current cell
	 * being focused.
	 */
	val cellFocusLocation: CellLocation<E>?
		get() {
			if (editorCell == null) return null
			return CellLocation(this, sourceIndexToLocal(cellFocusRow.index)!!, cellFocusCol.index)
		}

	/**
	 * Sets focus to the first element in the [data] list that matches the provided [element].
	 * Note that if the data elements are not unique, one of the [focusCell] overloads may be more appropriate to
	 * avoid ambiguity.
	 */
	fun focusCell(element: E, column: DataGridColumn<E, *>) = focusCell(data.indexOf(element), _columns.indexOf(column))

	fun focusCell(rowLocation: RowLocationRo<E>, columnIndex: Int) = focusCell(CellLocation(this, rowLocation, columnIndex))
	fun focusCell(sourceIndex: Int, columnIndex: Int) = focusCell(CellLocation(this, sourceIndexToLocal(cellFocusRow.index)!!, columnIndex))

	/**
	 * Focuses the cell at the given location.
	 */
	fun focusCell(cellLocation: CellLocationRo<E>) {
		val columnIndex = cellLocation.columnIndex
		val sourceIndex = cellLocation.sourceIndex
		if (sourceIndex == cellFocusRow.index && columnIndex == cellFocusCol.index) return // no-op
		disposeCellEditor()
		if (!cellLocation.editable) return
		cellFocusCol.index = columnIndex
		cellFocusRow.index = sourceIndex

		val col = _columns[columnIndex]
		val row = data[sourceIndex]
		@Suppress("unchecked_cast")
		val editorCell = col.createEditorCell(this) as DataGridEditorCell<Any?>
		editorCell.changed.add(this::commitCellEditorValue)
		editorCell.setData(col.getCellData(row))
		editorCellContainer.addElement(editorCell)
		editorCell.focus()
		this.editorCell = editorCell
		bringIntoView(cellLocation)
	}

	/**
	 * Edits the previous cell in a flow-layout pattern. That is, when the column index reaches the first column,
	 * the previous cell will be the last column of the previous row.
	 * If there are no editable cells left, the editor will be closed.
	 */
	fun focusPreviousCell(commit: Boolean) {
		val newLocation = cellFocusLocation ?: return
		newLocation.moveToPreviousCellUntil { it.editable }
		if (commit) commitCellEditorValue()
		focusCell(newLocation)
	}

	/**
	 * Edits the next cell in a flow-layout pattern. That is, when the column index reaches the right-most column,
	 * the next cell will be the first column of the next row.
	 * If there are no editable cells left, the editor will be closed.
	 */
	fun focusNextCell(commit: Boolean) {
		val newLocation = cellFocusLocation ?: return
		newLocation.moveToNextCellUntil { it.editable }
		if (commit) commitCellEditorValue()
		focusCell(newLocation)
	}

	/**
	 * Edits the previous cell in a flow-layout pattern. That is, when the column index reaches the left-most column,
	 * the previous cell will be the last column of the previous row.
	 * If there are no editable cells left, the editor will be closed.
	 */
	fun focusPreviousRow(commit: Boolean) {
		val newLocation = cellFocusLocation ?: return
		newLocation.moveToPreviousRowUntil { it.isElementRow }
		if (commit) commitCellEditorValue()
		focusCell(newLocation)
	}

	fun focusNextRow(commit: Boolean) {
		val newLocation = cellFocusLocation ?: return
		newLocation.moveToNextRowUntil { it.isElementRow }
		if (commit) commitCellEditorValue()
		focusCell(newLocation)
	}

	fun closeCellEditor(commit: Boolean = false) {
		if (editorCell == null) return
		val wasFocused = isFocused
		if (commit) commitCellEditorValue()
		disposeCellEditor()
		if (wasFocused) focusSelf()
	}

	/**
	 * Brings the given row into view.
	 */
	fun bringIntoView(rowLocation: RowLocationRo<E>) {
		validateLayout()
		// TODO: this could be better for variable row heights.
		val bottomRowCount = _totalRows - vScrollBar.scrollModel.max
		vScrollModel.value = clamp(vScrollModel.value, rowLocation.position - bottomRowCount - 1f, rowLocation.position.toFloat())
	}

	/**
	 * Brings the given cell into view.
	 */
	fun bringIntoView(cellLocation: CellLocationRo<E>) {
		bringIntoView(cellLocation as RowLocationRo<E>)
		hScrollModel.value = clamp(hScrollModel.value, _columnPositions[cellLocation.columnIndex] + _columnWidths[cellLocation.columnIndex] - contents.width, _columnPositions[cellLocation.columnIndex])
	}

	private fun commitCellEditorValue() {
		val editorCell = editorCell ?: return
		@Suppress("UNCHECKED_CAST")
		val column = _columns[cellFocusCol.index] as DataGridColumn<E, Any?>
		val element = data[cellFocusRow.index]
		column.setCellData(element, editorCell.getData())
		_observableData.notifyElementModified(cellFocusRow.index)
	}

	private fun disposeCellEditor() {
		val editorCell = editorCell ?: return
		cellFocusCol.clear()
		cellFocusRow.clear()
		this.editorCell = null
		editorCell.dispose()
	}

	private fun columnChangedHandler(column: DataGridColumn<E, *>) {
		invalidateColumnWidths()
	}

	fun dirtyColumnCache(column: DataGridColumn<E, *>) {
		dirty(columnCaches[_columns.indexOf(column)])
		invalidateColumnWidths()
	}

	private fun groupChangedHandler(group: DataGridGroup<E>) {
		invalidateLayout()
	}

	fun dirtyGroupCache(group: DataGridGroup<E>) {
		dirty(groupCaches[_groups.indexOf(group)])
		invalidateLayout()
	}

	private fun dirty(columnCache: ColumnCache) {
		if (columnCache.headerCell != null) {
			usedHeaderCells.forget(columnCache.headerCell!!)
			columnCache.headerCell!!.dispose()
			columnCache.headerCell = null
		}
		columnCache.cellCache.disposeAndClear()
		columnCache.bottomCellCache.disposeAndClear()
		usedColumns.forget(columnCache)
	}

	private fun dirty(groupCache: GroupCache) {
		if (groupCache.header != null) {
			usedGroupHeaders.forget(groupCache.header!!)
			groupCache.header!!.dispose()
			groupCache.header = null
		}
		if (groupCache.bottomHeader != null) {
			usedBottomGroupHeaders.forget(groupCache.bottomHeader!!)
			groupCache.bottomHeader?.dispose()
			groupCache.bottomHeader = null
		}
	}

	/**
	 * Indicates that the column sizes are invalid.
	 */
	private fun invalidateColumnWidths() {
		invalidate(COLUMNS_WIDTHS_VALIDATION or ValidationFlags.SIZE_CONSTRAINTS)
	}

	/**
	 * Clears all current sorting.
	 */
	fun clearSorting() {
		_customSortComparator = null
		_sortColumn = null
		_sortDirection = ColumnSortDirection.NONE
		_dataView.sortComparator = null
		invalidateLayout()
	}

	/**
	 * Sets a column to be used as the sort comparator.
	 */
	fun setSortColumn(column: DataGridColumn<E, *>, direction: ColumnSortDirection = ColumnSortDirection.ASCENDING) {
		_sortColumn = column
		_sortDirection = direction
		_dataView.sortComparator = if (_sortDirection == ColumnSortDirection.NONE) null else {
			row1, row2 ->
			_sortColumn!!.compareRows(row1, row2)
		}
		_dataView.reversed = _sortDirection == ColumnSortDirection.DESCENDING
		invalidateLayout()
	}

	/**
	 * A custom sort comparator.
	 * Note that no sorting arrows will be displayed in the header.
	 */
	var dataSortComparator: SortComparator<E>?
		get() = _customSortComparator
		set(value) {
			_customSortComparator = value
			_sortColumn = null
			_sortDirection = ColumnSortDirection.NONE
			_dataView.sortComparator = _customSortComparator
			invalidateLayout()
		}

	/**
	 * If true, the data view will be reversed.
	 */
	var dataSortReversed: Boolean
		get() = _customSortReversed
		set(value) {
			if (_customSortReversed == value) return
			_sortColumn = null
			_sortDirection = ColumnSortDirection.NONE
			_customSortReversed = value
			_dataView.reversed = value
			invalidateLayout()
		}

	/**
	 * Sets a filter to be applied to the data view.
	 * If this filter is set, the row data will be passed to the filter function, if false is returned, the row will
	 * not be shown.
	 */
	var dataFilter: ((E) -> Boolean)?
		get() = _dataView.filter
		set(value) {
			_dataView.filter = value
		}

	override fun setSize(width: Float?, height: Float?) {
		if (_explicitWidth == width && _explicitHeight == height) return
		invalidate(COLUMNS_WIDTHS_VALIDATION)
		super.setSize(width, height)
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		if (hScrollPolicy == ScrollPolicy.OFF) {
			var minW = if (vScrollPolicy == ScrollPolicy.OFF) 0f else vScrollBar.minWidth ?: 0f
			for (i in 0.._columns.lastIndex) {
				minW += _columns[i].minWidth
			}
			out.width.min = minW
		}
	}

	private val _columnWidths = ArrayList<Float>()

	/**
	 * The measured width of each column.
	 */
	val columnWidths: List<Float>
		get() {
			validate(ValidationFlags.LAYOUT)
			return _columnWidths
		}

	private val _columnPositions = ArrayList<Float>()

	/**
	 * The x position of each column.
	 */
	val columnPositions: List<Float>
		get() {
			validate(ValidationFlags.LAYOUT)
			return _columnPositions
		}

	/**
	 * The sum of [columnWidths]
	 */
	var columnsWidth = 0f
		private set

	private fun updateColumnWidths() {
		var availableW = style.borderThickness.reduceWidth(explicitWidth)
		val vScrollBarW = if (vScrollPolicy == ScrollPolicy.OFF) 0f else vScrollBar.minWidth ?: 0f
		if (availableW != null) availableW -= vScrollBarW

		_columnWidths.clear()
		var totalColW = 0f
		var inflexibleW = 0f

		for (i in 0.._columns.lastIndex) {
			val col = _columns[i]
			if (!col.visible) {
				_columnWidths.add(0f)
				continue
			}
			val prefWidth = getPreferredColumnWidth(col)
			inflexibleW += if (!col.getIsFlexible()) {
				prefWidth
			} else {
				col.minWidth
			}
			totalColW += prefWidth
			_columnWidths.add(prefWidth)
		}

		if (availableW != null && hScrollPolicy == ScrollPolicy.OFF) {
			// If hScrollPolicy is off, resize the columns to fit in the available space.
			columnsWithSpace.clear()

			val flexibleW = totalColW - inflexibleW
			if (flexibleW > 0f) {
				// Fit flexible columns into the available space.
				val newFlexibleW = availableW - inflexibleW
				for (i in 0.._columns.lastIndex) {
					val col = _columns[i]
					if (col.visible && col.getIsFlexible()) {
						columnsWithSpace.add(i)
					}
				}
				totalColW += adjustColumnWidths(newFlexibleW - flexibleW, update = false)
			}
			if (totalColW > availableW) {
				// We aren't allowed to scroll, and even after resizing the flexible columns, there isn't enough available
				// width, so we resize the inflexible columns as a last resort.
				columnsWithSpace.clear()
				for (i in 0.._columns.lastIndex) {
					val col = _columns[i]
					if (col.visible && !col.getIsFlexible()) {
						columnsWithSpace.add(i)
					}
				}
				totalColW += adjustColumnWidths(availableW - totalColW, update = false)
			}
		}

		var x = 0f
		_columnPositions.clear()
		for (i in 0.._columns.lastIndex) {
			_columnPositions.add(x)
			x += _columnWidths[i]
		}

		columnsWidth = totalColW
		val w = if (availableW == null || columnsWidth < availableW) columnsWidth else availableW

		// Update horizontal scroll model.
		var m = columnsWidth - w
		if (m < 0.01f) m = 0f
		hScrollBar.scrollModel.max = m
		hScrollBar.visible = hScrollPolicy == ScrollPolicy.ON || (hScrollPolicy == ScrollPolicy.AUTO && hScrollBar.scrollModel.max > 0f)
	}

	var firstVisibleColumn = 0
	var lastVisibleColumn = 0

	/**
	 * Calculates the first visible column, and the last visible column.
	 */
	private fun updateColumnVisibility() {
		var availableW = style.borderThickness.reduceWidth(explicitWidth)
		val vScrollBarW = if (vScrollPolicy == ScrollPolicy.OFF) 0f else vScrollBar.minWidth ?: 0f
		if (availableW != null) availableW -= vScrollBarW

		val scrollX = hScrollModel.value
		firstVisibleColumn = _columnPositions.sortedInsertionIndex(scrollX) - 1
		lastVisibleColumn = firstVisibleColumn
		if (availableW == null) {
			lastVisibleColumn = _columns.lastIndex
		} else {
			for (i in firstVisibleColumn + 1.._columns.lastIndex) {
				if (_columnPositions[i] - scrollX < availableW) {
					lastVisibleColumn = i
				}
			}
		}
	}

	private val columnsWithSpace = ArrayList<Int>()

	/**
	 * Adjusts the widths of the columns in [columnsWithSpace] evenly by [delta] / n respecting
	 * [DataGridColumn.minWidth].
	 *
	 * @param delta The space to disburse amongst [columnsWithSpace].
	 * @param update If true, [setColumnWidth] will be called with the new width.
	 *
	 * @return Returns the delta that was successfully adjusted.
	 */
	private fun adjustColumnWidths(delta: Float, update: Boolean): Float {
		var d = delta
		while (abs(d) > 0.01f && columnsWithSpace.isNotEmpty()) {
			val wInc = d / columnsWithSpace.size
			var n = columnsWithSpace.size
			var i = 0
			while (i < n) {
				val j = columnsWithSpace[i]
				val col = _columns[j]
				val oldColWidth = _columnWidths[j]
				var newColW = oldColWidth + wInc
				if (newColW < col.minWidth) {
					newColW = col.minWidth
					columnsWithSpace.removeAt(i)
					i--; n--
				}
				val iWDiff = newColW - oldColWidth
				_columnWidths[j] = newColW
				if (update) setColumnWidth(j, newColW)
				d -= iWDiff
				i++
			}
		}
		return delta - d
	}

	/**
	 * Updates the column's width to the given value. If the column previously used percent width, it will continue
	 * to have percent width.
	 */
	private fun setColumnWidth(columnIndex: Int, newWidth: Float) {
		val column = _columns[columnIndex]
		val availableWidth = if (column.widthPercent == null) null else style.borderThickness.reduceWidth(explicitWidth)
		if (availableWidth != null) {
			column.widthPercent = newWidth / availableWidth
		} else {
			column.widthPercent = null
			column.width = newWidth
		}
	}

	/**
	 * Gets the preferred width of a column.
	 */
	private fun getPreferredColumnWidth(column: DataGridColumn<E, *>): Float {
		if (!column.visible) return 0f
		val width = style.borderThickness.reduceWidth(explicitWidth)
		return column.getPreferredWidth(width) ?: maxOf(column.minWidth, style.defaultColumnWidth)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		iterateVisibleColumnsInternal { columnIndex, column, columnX, columnWidth ->
			// Mark the visible columns as used.
			usedColumns.markUsed(columnCaches[columnIndex])
			true
		}
		val border = style.borderThickness
		val contentsW = columnsWidth - hScrollBar.scrollModel.max
		val bodyW = contentsW + if (vScrollPolicy != ScrollPolicy.OFF) vScrollBar.minWidth ?: 0f else 0f

		out.width = border.expandWidth2(bodyW)

		updateHeader(contentsW)

		val hScrollBarH = if (hScrollBar.visible) hScrollBar.minHeight ?: 0f else 0f

		_totalRows = calculateTotalRows()
		var contentsH = if (explicitHeight == null) null else border.reduceHeight2(explicitHeight) - headerCells.height - hScrollBarH
		val bottomRowCount = updateBottomRows(contentsW, contentsH)
		vScrollBar.modelToPixels = bottomContents.height / maxOf(0.0001f, bottomRowCount)
		vScrollBar.scrollModel.max = maxOf(0f, _totalRows - bottomRowCount)
		vScrollBar.visible = vScrollPolicy != ScrollPolicy.OFF && vScrollBar.scrollModel.max > 0.0001f
		val vScrollBarW = if (vScrollBar.visible) vScrollBar.minWidth ?: 0f else 0f

		contentsH = bottomContents.height
		updateRows(contentsW, contentsH)
		updateEditorCell()

		rowBackgrounds.setSize(contentsW, contentsH)
		rowBackgrounds.setPosition(0f, headerCells.height)
		contents.setSize(contentsW, contentsH)
		contents.setPosition(0f, headerCells.height)
		editorCellContainer.setSize(contentsW, contentsH)
		editorCellContainer.setPosition(0f, headerCells.height)

		groupHeadersAndFooters.setSize(contentsW, contentsH)
		groupHeadersAndFooters.setPosition(0f, headerCells.height)

		out.height = border.expandHeight2(headerCells.height + contents.height + hScrollBarH)

		hScrollBar.setSize(out.width - vScrollBarW, hScrollBarH)
		hScrollBar.setPosition(-border.left, out.height - hScrollBarH - border.top)
		vScrollBar.setSize(vScrollBarW, out.height - hScrollBarH - headerCells.height)
		vScrollBar.setPosition(out.width - vScrollBarW - border.right, headerCells.height - border.top)

		tossBinding.modelToPixelsY = vScrollBar.modelToPixels

		updateVerticalDividers(contentsW, out.height)

		val topRight = topRight
		if (topRight != null) {
			topRight.visible = vScrollBar.visible
			topRight.setSize(vScrollBar.minWidth ?: 0f, headerCells.height)
			topRight.setPosition(bodyW - topRight.width, 0f)
		}
		val headerDivider = this.headerDivider
		if (headerDivider != null) {
			headerDivider.setSize(bodyW, null)
			headerDivider.moveTo(0f, headerCells.height - headerDivider.height)
		}

		clipper.setSize(bodyW, contents.height + headerCells.height + hScrollBarH)
		clipper.setPosition(border.left, border.top)

		background?.setSize(out)
		usedColumns.flip()
	}

	private fun updateHeader(width: Float) {
		sortDownArrow?.visible = false
		sortUpArrow?.visible = false

		// Update the header cells
		val cellPad = style.headerCellPadding
		var headerCellHeight = 0f
		iterateVisibleColumnsInternal {
			columnIndex, column, x, columnWidth ->
			val columnCache = columnCaches[columnIndex]
			if (columnCache.headerCell == null) {
				val newHeaderCell  = column.createHeaderCell(headerCells)
				newHeaderCell.interactivityMode = column.headerCellInteractivityMode
				newHeaderCell.styleTags.addAll(DataGrid.HEADER_CELL, TextStyleTags.h2)
				headerCells.addElement(minOf(columnIndex, headerCells.elements.size), newHeaderCell)
				columnCache.headerCell = newHeaderCell
			}
			val headerCell = columnCache.headerCell!!
			usedHeaderCells.markUsed(headerCell)
			headerCell.visible = true

			val sortArrow: UiComponent? = if (column == _sortColumn) {
				if (_sortDirection == ColumnSortDirection.ASCENDING) {
					sortDownArrow
				} else if (_sortDirection == ColumnSortDirection.DESCENDING) {
					sortUpArrow
				} else {
					null
				}
			} else {
				null
			}

			if (sortArrow != null) {
				sortArrow.visible = true
				sortArrow.x = x + columnWidth - cellPad.right - sortArrow.width
				headerCell.setSize(cellPad.reduceWidth(columnWidth - style.headerCellGap - sortArrow.width), null)
			} else {
				headerCell.setSize(cellPad.reduceWidth(columnWidth), null)
			}
			headerCellHeight = maxOf(headerCellHeight, headerCell.height)
			true
		}
		usedHeaderCells.hideAndFlip()

		val sortArrow = if (sortUpArrow?.visible == true) sortUpArrow else if (sortDownArrow?.visible == true) sortDownArrow else null
		if (sortArrow != null) {
			sortArrow.y = when (style.headerSortArrowVAlign) {
				VAlign.TOP -> cellPad.top
				VAlign.MIDDLE -> cellPad.top + (headerCellHeight - sortArrow.height) * 0.5f
				VAlign.BOTTOM -> cellPad.top + (headerCellHeight - sortArrow.height)
			}
		}

		// Update the header cell backgrounds.
		headerCellBackgrounds.interactivityMode = if (columnSortingEnabled || columnReorderingEnabled) InteractivityMode.CHILDREN else InteractivityMode.NONE
		val headerHeight = cellPad.expandHeight2(headerCellHeight)
		var cellBackgroundIndex = 0
		iterateVisibleColumnsInternal {
			i, col, colX, colWidth ->
			val columnCache = columnCaches[i]
			val headerCell = columnCache.headerCell!!
			headerCell.visible = true
			val y = cellPad.top + maxOf(0f, when (col.headerCellVAlign ?: style.headerCellVAlign) {
				VAlign.TOP -> 0f
				VAlign.MIDDLE -> (headerCellHeight - headerCell.height) * 0.5f
				VAlign.BOTTOM -> (headerCellHeight - headerCell.height)
			})
			val headerCellWidth = headerCell.explicitWidth ?: headerCell.width
			val x = cellPad.left + maxOf(0f, when (col.headerCellHAlign ?: style.headerCellHAlign) {
				HAlign.LEFT -> 0f
				HAlign.CENTER -> (headerCellWidth - headerCell.width) * 0.5f
				HAlign.RIGHT -> (headerCellWidth - headerCell.width)
			})
			headerCell.moveTo(colX + x, y)

			// Header cell background
			val headerCellBackground = headerCellBackgrounds.elements.getOrNull(cellBackgroundIndex) ?: createHeaderCellBackground()
			headerCellBackground.interactivityMode = if (
					(columnSortingEnabled && col.sortable) ||
					(columnReorderingEnabled && col.reorderable)
			) InteractivityMode.ALL else InteractivityMode.NONE
			headerCellBackground.setAttachment(COL_INDEX_KEY, i)
			headerCellBackground.visible = true
			headerCellBackground.setSize(colWidth, headerHeight)
			headerCellBackground.setPosition(colX, 0f)

			// Column resize handle
			val resizeHandle = columnResizeHandles.elements.getOrNull(cellBackgroundIndex) ?: createColumnResizeHandle()
			resizeHandle.setAttachment(COL_INDEX_KEY, i)
			resizeHandle.visible = col.resizable
			resizeHandle.setSize(null, headerHeight)
			resizeHandle.setPosition(colWidth + colX - resizeHandle.width * 0.5f, 0f)

			cellBackgroundIndex++
			true
		}

		// Hide header cell backgrounds and resize handles that are no longer shown.
		for (i in cellBackgroundIndex..headerCellBackgrounds.elements.size - 1) {
			headerCellBackgrounds.elements.getOrNull(i)!!.visible = false
			columnResizeHandles.elements.getOrNull(i)!!.visible = false
		}

		headerCells.setSize(width, headerHeight)
		columnResizeHandles.setSize(width + 5f, headerHeight)
	}

	private fun calculateTotalRows(): Int {
		var total = 0
		val groupCaches = displayGroupCaches
		for (i in 0..groupCaches.lastIndex) {
			val groupCache = groupCaches[i]
			val group = groupCache.group
			if (!group.visible) continue
			if (!group.collapsed) {
				total += groupCache.list.size
			}
			if (group.showHeader) total++
		}
		return total
	}

	/**
	 * Calculates how many rows can be rendered at the tail end of vertical scrolling.
	 * This method must set the size of [bottomContents], and return the number of visible rows.
	 */
	private fun updateBottomRows(width: Float, height: Float?): Float {
		var rowsY = 0f
		val visibleRows: Float
		val rowHeight = rowHeight
		if (rowHeight != null) {
			if (height == null) {
				rowsY = rowHeight * maxRows
				visibleRows = maxRows.toFloat()
			} else {
				visibleRows = height / rowHeight
			}
		} else {
			val maxHeight = height ?: Float.MAX_VALUE

			val pad = style.cellPadding
			var rowsShown = 0
			rowsY = 0f
			var iRowHeight = 0f

			rowIterator.moveToLastRow()
			var rowIndex = rowIterator.position
			while (rowIterator.hasPreviousRow && rowsShown < maxRows && rowsY < maxHeight) {
				rowIterator.moveToPreviousRow()
				iRowHeight = minRowHeight
				if (rowIterator.isHeader) {
					val groupCache = rowIterator.groupCache
					val group = rowIterator.group
					if (groupCache.bottomHeader == null) {
						groupCache.bottomHeader = group.createHeader(bottomContents, groupCache.list)
					}
					val header = groupCache.bottomHeader!!
					if (header.parent == null) {
						bottomContents.addElement(header)
					}
					header.collapsed = group.collapsed
					usedBottomGroupHeaders.markUsed(header)

					header.setSize(width, null)
					iRowHeight = rowHeight ?: maxOf(iRowHeight, header.height)
				} else if (rowIterator.isFooter) {
					// TODO:
				} else {
					val element = rowIterator.element!!
					rowIndex--
					iterateVisibleColumnsInternal {
						columnIndex, column, columnX, columnWidth ->
						val columnCache = columnCaches[columnIndex]
						@Suppress("unchecked_cast")
						val cell = columnCache.bottomCellCache.obtain(rowIndex) as DataGridCell<Any?>
						if (cell.parent == null) {
							bottomContents.addElement(cell)
						}

						val cellData = column.getCellData(element)
						cell.setData(cellData)

						cell.setSize(pad.reduceWidth2(columnWidth), null)
						iRowHeight = maxOf(iRowHeight, pad.expandHeight2(cell.height))
						true
					}
				}
				rowsY += iRowHeight
				rowsShown++
			}
			visibleRows = if (rowsY <= maxHeight) rowsShown.toFloat() else rowsShown.toFloat() - (rowsY - maxHeight) / iRowHeight
		}

		bottomContents.setSize(width, if (height == null || rowsY < height) rowsY else height)

		// Recycle unused cells.
		iterateVisibleColumnsInternal {
			columnIndex, _, _, _ ->
			columnCaches[columnIndex].bottomCellCache.removeAndFlip(bottomContents)
			true
		}
		usedColumns.forEach {
			it.bottomCellCache.removeAndFlip(bottomContents)
		}
		usedBottomGroupHeaders.removeAndFlip(bottomContents)

		return visibleRows
	}

	private val rowCells = ArrayList<UiComponent>()

	private fun updateRows(width: Float, height: Float) {
		val rowHeight = rowHeight

		val pad = style.cellPadding
		val rowHeight2 = pad.reduceHeight(rowHeight)
		var rowsShown = 0
		var rowsY = 0f
		var iRowHeight: Float
		rowHeights.clear()

		rowIterator.position = vScrollModel.value.toInt() - 1
		val firstP = vScrollModel.value - vScrollModel.value.floor()
		var rowIndex = rowIterator.position
		while (rowIterator.hasNextRow && rowsY < height) {
			rowIterator.moveToNextRow()
			iRowHeight = minRowHeight
			if (rowIterator.isHeader) {
				val groupCache = rowIterator.groupCache
				val group = rowIterator.group
				if (groupCache.header == null) {
					groupCache.header = group.createHeader(groupHeadersAndFooters, groupCache.list)
				}
				val header = groupCache.header!!
				if (header.parent == null) {
					groupHeadersAndFooters.addElement(header)
				}
				header.collapsed = group.collapsed
				usedGroupHeaders.markUsed(header)

				header.setSize(width, null)
				iRowHeight = rowHeight ?: maxOf(iRowHeight, header.height)

				if (rowsShown == 0) {
					// Start the first row off accounting for partial row visibility
					rowsY = -iRowHeight * firstP
				}
				header.setPosition(0f, rowsY)
			} else if (rowIterator.isFooter) {
				// TODO:
			} else {
				val element = rowIterator.element!!
				rowIndex++
				iterateVisibleColumnsInternal {
					columnIndex, column, columnX, columnWidth ->
					val columnCache = columnCaches[columnIndex]
					@Suppress("unchecked_cast")
					val cell = columnCache.cellCache.obtain(rowIndex) as DataGridCell<Any?>
					if (cell.parent == null) {
						contents.addElement(cell)
					}
					val cellData = column.getCellData(element)
					cell.setData(cellData)

					cell.setSize(pad.reduceWidth2(columnWidth), rowHeight2)
					iRowHeight = rowHeight ?: maxOf(iRowHeight, pad.expandHeight2(cell.height))
					rowCells.add(cell)
					true
				}

				if (rowsShown == 0) {
					// Start the first row off accounting for partial row visibility
					rowsY = -iRowHeight * firstP
				}

				// Position
				iterateVisibleColumnsInternal { columnIndex, column, columnX, columnWidth ->
					val cell = rowCells.poll()
					val y = pad.top + maxOf(0f, when (column.cellVAlign ?: style.cellVAlign) {
						VAlign.TOP -> 0f
						VAlign.MIDDLE -> (iRowHeight - cell.height) * 0.5f
						VAlign.BOTTOM -> (iRowHeight - cell.height)
					})
					val cellWidth = cell.explicitWidth ?: cell.width

					val x = pad.left + maxOf(0f, when (column.cellHAlign ?: style.headerCellHAlign) {
						HAlign.LEFT -> 0f
						HAlign.CENTER -> (cellWidth - cell.width) * 0.5f
						HAlign.RIGHT -> (cellWidth - cell.width)
					})
					cell.moveTo(columnX + x, rowsY + y)
					true
				}
				rowCells.clear()

				// Row background
				val rowBackground = rowBackgroundsCache.obtain(rowIndex)
				rowBackground.rowIndex = rowIterator.rowIndex
				rowBackground.visible = true
				if (rowBackground.parent == null) {
					rowBackgrounds.addElement(rowBackground)
				}
				rowBackground.setSize(width, iRowHeight)
				rowBackground.setPosition(0f, rowsY)
			}
			rowHeights.add(iRowHeight)
			rowsY += iRowHeight
			rowsShown++
		}

		// Recycle unused cells.
		iterateVisibleColumnsInternal {
			columnIndex, _, _, _ ->
			columnCaches[columnIndex].cellCache.removeAndFlip(contents)
			true
		}
		usedColumns.forEach {
			it.cellCache.removeAndFlip(contents)
		}
		usedGroupHeaders.removeAndFlip(groupHeadersAndFooters)
		rowBackgroundsCache.hideAndFlip()
	}

	private fun updateEditorCell() {
		editorCellCheck()
		val editorCell = editorCell ?: return
		val columnIndex = cellFocusCol.index
		val x = _columnPositions[columnIndex] - hScrollModel.value

		rowIterator.sourceIndex = cellFocusRow.index
		val position = rowIterator.position
		val rowIndex = position - vScrollModel.value.toInt()

		if (rowIndex >= 0 && rowIndex < rowHeights.size) {
			editorCell.alpha = 1f
			editorCell.interactivityMode = InteractivityMode.ALL
			var y = 0f
			for (i in 0..rowIndex - 1) {
				y += rowHeights[i]
			}
			// Partial row visibility
			y -= rowHeights[0] * (vScrollModel.value - vScrollModel.value.floor())
			editorCell.setSize(_columnWidths[columnIndex], rowHeights[rowIndex])
			editorCell.setPosition(x, y)
		} else {
			// We make it hidden instead of invisible so that the phone's keyboard appearing doesn't cause the cause
			// the cell to move out of view and therefore close the keyboard again.
			editorCell.alpha = 0f
			editorCell.interactivityMode = InteractivityMode.NONE
		}
	}

	/**
	 * Checks if the editor cell is still valid (i.e. The data still exists and the column is still editable),
	 * and if not, closes the cell.
	 */
	private fun editorCellCheck() {
		if (!isEditing) return
		val columnIndex = cellFocusCol.index
		if (_columns.getOrNull(columnIndex)?.editable != true || cellFocusRow.index == -1) {
			closeCellEditor(false)
			return
		}
	}

	fun iterateVisibleRows(callback: (row: RowLocation<E>, rowY: Float, rowHeight: Float) -> Boolean) {
		validateLayout()
		var rowY = 0f
		var i = 0
		rowIterator.position = vScrollModel.value.toInt() - 1
		while (rowIterator.hasNextRow && i < rowHeights.size) {
			rowIterator.moveToNextRow()
			val rowHeight = rowHeights[i++]
			callback(rowIterator, rowY, rowHeight)
			rowY += rowHeight
		}
	}

	/**
	 * Iterates over the currently visible columns.
	 * The callback will be invoked for every column fully or partially visible, providing the
	 * the columnIndex, column, x position, and width.
	 */
	fun iterateVisibleColumns(callback: (columnIndex: Int, column: DataGridColumn<E, *>, columnX: Float, columnWidth: Float) -> Boolean) {
		validate(COLUMNS_WIDTHS_VALIDATION)
		iterateVisibleColumnsInternal(callback)
	}

	/**
	 * Iterates over all of the columns, invoking the callback with the columnIndex, column, x position, and width.
	 * This assumes the COLUMNS_WIDTHS_VALIDATION flag has already been validated.
	 */
	private inline fun iterateVisibleColumnsInternal(callback: (columnIndex: Int, column: DataGridColumn<E, *>, columnX: Float, columnWidth: Float) -> Boolean) {
		val xOffset = -hScrollModel.value
		for (i in firstVisibleColumn..lastVisibleColumn) {
			val col = _columns[i]
			if (!col.visible) continue
			val colWidth = _columnWidths[i]
			val colX = _columnPositions[i] + xOffset
			val shouldContinue = callback(i, col, colX, colWidth)
			if (!shouldContinue) break
		}
	}

	private fun createHeaderCellBackground(): UiComponent {
		val headerCellBackground = style.headerCellBackground(this)
		val drag = headerCellBackground.dragAttachment()
		drag.dragStart.add { e ->
			val columnIndex = e.currentTarget.getAttachment<Int>(COL_INDEX_KEY)!!
			if (columnReorderingEnabled && _columns[columnIndex].reorderable) {
				columnMoveIndicator.visible = true
				columnMoveIndicator.setSize(e.currentTarget.bounds)
				columnInsertionIndicator.visible = true
				columnInsertionIndicator.setSize(null, height)
			} else {
				e.preventDefault()
			}
		}

		drag.drag.add { e ->
			columnMoveIndicator.setPosition(e.currentTarget.x + (e.position.x - e.startPosition.x), 0f)
			val localP = headerCells.canvasToLocal(Vector2.obtain().set(e.position))

			val currX = localP.x + hScrollModel.value
			var index = _columnPositions.sortedInsertionIndex(currX)
			if (index > 0 && currX < _columnPositions[index - 1] + _columnWidths[index - 1] * 0.5f) index--
			index = maxOf(index, _columns.indexOfFirst2 { it.visible && it.reorderable })
			index = minOf(index, _columns.indexOfLast2 { it.visible && it.reorderable } + 1)
			val insertX = (if (index <= 0) 0f else if (index >= _columnPositions.size) _columnPositions.last() + _columnWidths.last() else _columnPositions[index]) - hScrollModel.value
			columnInsertionIndicator.setPosition(insertX - columnInsertionIndicator.width * 0.5f, 0f)
			Vector2.free(localP)
		}

		drag.dragEnd.add { e ->
			columnMoveIndicator.visible = false
			columnInsertionIndicator.visible = false

			val localP = headerCells.canvasToLocal(Vector2.obtain().set(e.position))

			val fromIndex = e.currentTarget.getAttachment<Int>(COL_INDEX_KEY)!!
			val currX = localP.x + hScrollModel.value
			var toIndex = _columnPositions.sortedInsertionIndex(currX)
			if (toIndex > 0 && currX < _columnPositions[toIndex - 1] + _columnWidths[toIndex - 1] * 0.5f) toIndex--
			toIndex = maxOf(toIndex, _columns.indexOfFirst2 { it.visible && it.reorderable })
			toIndex = minOf(toIndex, _columns.indexOfLast2 { it.visible && it.reorderable } + 1)
			Vector2.free(localP)
			moveColumn(fromIndex, toIndex)
		}

		// Click to sort
		headerCellBackground.click().add(this::headerCellBackgroundClickedHandler)

		headerCellBackgrounds.addElement(headerCellBackground)
		return headerCellBackground
	}

	private fun headerCellBackgroundClickedHandler(e: ClickInteractionRo) {
		if (!columnSortingEnabled || e.handled) {
			return
		}
		val columnIndex = e.currentTarget.getAttachment<Int>(COL_INDEX_KEY) ?: return
		val column = _columns[columnIndex]
		if (!column.sortable) return
		e.handled = true

		if (inject(KeyState).keyIsDown(Ascii.CONTROL)) {
			_sortColumn = null
			_sortDirection = ColumnSortDirection.NONE
			_dataView.sortComparator = _customSortComparator
			_dataView.reversed = _customSortReversed
			invalidateLayout()
		} else {
			val direction = if (_sortColumn == column) {
				if (_sortDirection == ColumnSortDirection.ASCENDING) ColumnSortDirection.DESCENDING else ColumnSortDirection.ASCENDING
			} else {
				ColumnSortDirection.ASCENDING
			}
			setSortColumn(column, direction)
		}
	}

	private fun createColumnResizeHandle(): UiComponent {
		val resizeHandle = spacer(style.resizeHandleWidth, 0f)
		resizeHandle.interactivityMode = InteractivityMode.ALL
		resizeHandle.cursor(StandardCursors.RESIZE_E)

		val drag = resizeHandle.dragAttachment(0f)
		var colResizeStartX = 0f
		var columnIndex = 0
		drag.dragStart.add {
			columnIndex = it.currentTarget.getAttachment(COL_INDEX_KEY)!!
			colResizeStartX = -hScrollModel.value + _columnPositions[columnIndex]

			if (explicitWidth != null && hScrollPolicy == ScrollPolicy.OFF) {
				// Set the column widths to their measured sizes so we don't have to compensate for flex size.
				for (i in 0.._columns.lastIndex) {
					if (!_columns[i].visible) continue
					setColumnWidth(i, _columnWidths[i])
				}
			}
		}

		drag.drag.add {
			val column = _columns[columnIndex]
			val localP = columnResizeHandles.canvasToLocal(Vector2.obtain().set(it.position))

			val availableWidth = style.borderThickness.reduceWidth(explicitWidth)
			var newWidth: Float = maxOf(column.minWidth, localP.x - colResizeStartX)
			Vector2.free(localP)
			if (availableWidth == null || hScrollPolicy != ScrollPolicy.OFF) {
				setColumnWidth(columnIndex, newWidth)
			} else {
				columnsWithSpace.clear()
				var minW = 0f
				for (i in columnIndex + 1.._columns.lastIndex) {
					val col = _columns[i]
					if (!col.visible) continue
					if (!col.resizable) {
						minW += _columnWidths[i]
					} else {
						minW += col.minWidth
						columnsWithSpace.add(i)
					}
				}
				newWidth = minOf(availableWidth - colResizeStartX - minW, newWidth)
				val oldWidth = _columnWidths[columnIndex]
				setColumnWidth(columnIndex, newWidth)
				adjustColumnWidths(oldWidth - newWidth, update = true)
			}
		}

		columnResizeHandles.addElement(resizeHandle)
		return resizeHandle
	}

	/**
	 * Moves the column at [fromIndex] to index [toIndex]
	 */
	fun moveColumn(fromIndex: Int, toIndex: Int) {
		var toIndex2 = toIndex
		if (toIndex2 > fromIndex) toIndex2--
		if (fromIndex != toIndex2) {
			val removed = _columns.removeAt(fromIndex)
			_columns.add(toIndex2, removed)
		}
	}

	private fun updateVerticalDividers(width: Float, height: Float) {
		var shownColumns = 0
		iterateVisibleColumnsInternal { columnIndex, column, columnX, columnWidth ->
			if (columnDividersHeader.elements.size <= shownColumns) {
				columnDividersHeader.addElement(style.verticalDivider(this))
				columnDividersContents.addElement(style.verticalDivider(this))
			}
			val headerDivider = columnDividersHeader.elements.getOrNull(shownColumns)!!
			headerDivider.visible = true
			headerDivider.setSize(null, headerCells.height)
			headerDivider.moveTo(columnX + columnWidth, 0f)
			val contentsDivider = columnDividersContents.elements.getOrNull(shownColumns)!!
			contentsDivider.visible = (columnIndex != lastVisibleColumn)
			contentsDivider.setSize(null, height - headerCells.height)
			contentsDivider.moveTo(columnX + columnWidth, headerCells.height)
			shownColumns++
			true
		}
		// Hide extra dividers.
		for (i in shownColumns..columnDividersHeader.elements.size - 1) {
			columnDividersHeader.elements.getOrNull(i)!!.visible = false
			columnDividersContents.elements.getOrNull(i)!!.visible = false
		}
	}

	override fun dispose() {
		disposeCellEditor()
		super.dispose()
	}

	/**
	 * Cached display values for a column.
	 */
	private inner class ColumnCache(owner: Owned, val column: DataGridColumn<E, *>) {

		var headerCell: UiComponent? = null

		private val cellPool = ObjectPool<DataGridCell<*>> {
			val newCell = column.createCell(owner)
			newCell.styleTags.add(BODY_CELL)
			newCell
		}

		val cellCache = IndexedCache(cellPool)
		val bottomCellCache = IndexedCache(cellPool)
	}

	/**
	 * Cached display values for a data grid group.
	 * @suppress
	 */
	internal inner class GroupCache(val group: DataGridGroup<E>) {

		val list: ListView<E> = ListView(dataView).apply { filter = group.filter }

		var header: DataGridGroupHeader? = null
		var bottomHeader: DataGridGroupHeader? = null

		val shouldRender: Boolean
			get() = showHeader || showList || showFooter

		val showHeader: Boolean
			get() = group.visible && group.showHeader

		val showList: Boolean
			get() = group.visible && !group.collapsed && list.isNotEmpty()

		val showFooter: Boolean
			get() = group.visible && group.showFooter && (!group.collapsed || group.showFooterWhenCollapsed)

		val size: Int
			get() {
				var total = 0
				if (showHeader) total++
				if (showFooter) total++
				if (showList) total += list.size
				return total
			}

		val lastIndex: Int
			get() = size - 1

		val listStartIndex: Int
			get() = if (showList) (if (showHeader) 1 else 0) else -1

		val listLastIndex: Int
			get() = if (showList) (list.lastIndex + if (showHeader) 1 else 0) else -1

		val listSize: Int
			get() = if (showList) list.size else 0

		val footerIndex: Int
			get() = if (showFooter) lastIndex else -1

	}

	companion object : StyleTag {

		/**
		 * The validation flag for column widths.
		 */
		private const val COLUMNS_WIDTHS_VALIDATION = 1 shl 16
		private const val COLUMNS_VISIBLE_VALIDATION = 1 shl 17

		private const val COL_INDEX_KEY = "columnIndex"

		val HEADER_CELL = styleTag()
		val BODY_CELL = styleTag()

		val COLUMN_MOVE_INDICATOR = styleTag()
		val COLUMN_INSERTION_INDICATOR = styleTag()

		val SCROLL_BAR = styleTag()

	}
}

class DataGridStyle : StyleBase() {

	override val type = Companion

	/**
	 * The background of the data grid. This will be sized to the measured bounds of the grid.
	 */
	var background by prop(noSkin)

	/**
	 * Used for clipping, this should match that of the background border radius.
	 */
	var borderRadius by prop(Corners())

	/**
	 * Used for clipping, this should match that of the background border thickness.
	 */
	var borderThickness by prop(Pad())

	/**
	 * Used if a column has no set width or widthPercent.
	 */
	var defaultColumnWidth by prop(100f)

	/**
	 * The width of the column resize handles.
	 */
	var resizeHandleWidth by prop(16f)

	/**
	 * The divider between the header and the body.
	 */
	var headerDivider by prop<Owned.() -> UiComponent?>({ hr() })

	/**
	 * The skin part for the down sort arrow.
	 */
	var verticalDivider by prop<Owned.() -> UiComponent>({ vr() })

	/**
	 * The skin part for the down sort arrow.
	 */
	var sortDownArrow by prop(noSkin)

	/**
	 * The skin part for the down up arrow.
	 */
	var sortUpArrow by prop(noSkin)

	/**
	 * The padding around the header cells.
	 */
	var headerCellPadding by prop(Pad(4f))

	/**
	 * The gap between the header cell and the sort arrow (if there is one).
	 */
	var headerCellGap by prop(2f)

	/**
	 * The vertical alignment of the header cells.
	 */
	var headerCellVAlign by prop(VAlign.MIDDLE)

	/**
	 * The horizontal alignment of the header cells.
	 * Note that this does not affect text alignment. To change the cell's text alignment, set the text field's
	 * flowStyle horizontalAlign property. This can be done universally by setting a style rule for
	 * [DataGrid.HEADER_CELL]
	 */
	var headerCellHAlign by prop(HAlign.CENTER)

	/**
	 * The vertical alignment of the sort arrows.
	 */
	var headerSortArrowVAlign by prop(VAlign.MIDDLE)

	/**
	 * The skin part used as the background to each header cell.
	 */
	var headerCellBackground by prop(noSkin)

	/**
	 * The padding around the body cells.
	 */
	var cellPadding by prop(Pad(4f))

	/**
	 * The vertical alignment of the body cells.
	 */
	var cellVAlign by prop(VAlign.TOP)

	/**
	 * The background for each row.
	 */
	var rowBackground by prop<Owned.() -> RowBackground>({ rowBackground() })

	/**
	 * If true, the current group header will always be shown.
	 */
	var alwaysShowHeader by prop(true)

	/**
	 * The skin part that indicates the highlighted cell. It will be sized and positioned to match the cell.
	 */
	var cellFocusHighlight by prop(noSkin)

	companion object : StyleType<DataGridStyle>
}

fun <E> Owned.dataGrid(data: ObservableList<E>, init: ComponentInit<DataGrid<E>>): DataGrid<E> {
	val d = DataGrid(this, data)
	d.init()
	return d
}

fun <E> Owned.dataGrid(data: List<E>, init: ComponentInit<DataGrid<E>>): DataGrid<E> {
	val d = DataGrid(this, data)
	d.init()
	return d
}

fun <E> Owned.dataGrid(init: ComponentInit<DataGrid<E>>): DataGrid<E> {
	val d = DataGrid<E>(this)
	d.init()
	return d
}


interface DataGridCell<in CellData> : UiComponent {
	fun setData(value: CellData)
}

interface DataGridEditorCell<CellData> : DataGridCell<CellData> {

	/**
	 * Dispatched on value commit.
	 */
	val changed: Signal<() -> Unit>

	/**
	 * Returns true if the editor cell has a valid value. It is up to the editor cell to display validation feedback.
	 */
	fun validateData(): Boolean

	/**
	 * Returns the editor's current data.
	 */
	fun getData(): CellData

}

enum class ColumnSortDirection {
	NONE,
	ASCENDING,
	DESCENDING
}

open class DataGridGroup<E> {

	private val _changed = Signal1<DataGridGroup<E>>()
	val changed = _changed.asRo()

	/**
	 * Creates a header to the group. This should not include background or collapse arrow.
	 * The header is cached, so this method should not return inconsistent results.
	 */
	open fun createHeader(owner: Owned, list: ObservableList<E>): DataGridGroupHeader {
		throw Exception("A header cell was requested, but createHeaderCell was not implemented.")
	}

	/**
	 * The filter function for what rows will be shown below this group's header. The filter functions do not need
	 * to be mutually exclusive, but if they aren't, rows may be shown multiple times.
	 */
	var filter by bindable<((E) -> Boolean)?>(null)

	/**
	 * If false, this group, along with its header, will not be shown.
	 */
	var visible by bindable(true)

	/**
	 * If true, this group will be collapsed.
	 */
	var collapsed by bindable(false)

	/**
	 * If false, a header row will not be shown.
	 */
	var showHeader by bindable(true)

	/**
	 * If false, a footer row will not be shown.
	 */
	var showFooter by bindable(false)

	/**
	 * If [showFooter] is true, and showFooterWhenCollapsed is true, a footer row will be displayed even when the group
	 * is collapsed.
	 */
	var showFooterWhenCollapsed by bindable(true)

	private fun <T> bindable(initialValue: T): ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
		override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
			_changed.dispatch(this@DataGridGroup)
		}
	}
}