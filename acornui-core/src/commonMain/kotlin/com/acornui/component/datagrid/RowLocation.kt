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

import com.acornui.collection.indexOfFirst2
import com.acornui.collection.indexOfLast2

interface RowLocationRo<RowData> {

	/**
	 * The index of this location's group. This corresponds to the grid's [DataGrid.groups] list.
	 */
	val groupIndex: Int

	/**
	 * groupPosition is the index within a group. It includes headers, footers, and list rows.
	 *
	 * It is mapped as follows:
	 * Group header (if showHeader)
	 * List elements (if !collapsed)
	 * Footer (if showFooter && (!collapsed || showFooterWhenCollapsed))
	 */
	val groupPosition: Int

	/**
	 * The row position within the grid.
	 * Reading this property is O(C)
	 */
	val position: Int

	/**
	 * The index of the element within the group's filtered list.
	 */
	val rowIndex: Int

	/**
	 * The element. This may be null if this location represents a header or footer.
	 * It will be guaranteed to be not null if [isElementRow] is true.
	 */
	val element: RowData?

	/**
	 * Returns true if the [groupPosition] represents a header row.
	 */
	val isHeader: Boolean

	/**
	 * Returns true if the [groupPosition] represents an element and not a header or footer.
	 */
	val isElementRow: Boolean

	/**
	 * Returns true if the [groupPosition] represents a footer row.
	 */
	val isFooter: Boolean

	/**
	 * Returns true if this location is within bounds.
	 */
	val isValid: Boolean

	/**
	 * The index of this row within the source [DataGrid.data] list.
	 * Note: This will return -1 if this row location doesn't represent an element. (That is, it's a header or
	 * footer row)
	 * @see isElementRow
	 *
	 * This property is derived.
	 */
	val sourceIndex: Int

	val hasPreviousRow: Boolean

	val hasNextRow: Boolean

	fun copy(): RowLocationRo<RowData>

}

/**
 * An object representing a row within the grid.
 * This position includes header and footer rows.
 */
open class RowLocation<RowData>(protected val dataGrid: DataGrid<RowData>) : RowLocationRo<RowData> {

	private val displayGroupCaches: List<DataGridCache<RowData>.GroupCache>
		get() = dataGrid.cache.displayGroupCaches

	private var _groupIndex: Int = 0

	/**
	 * The index of this location's group. This corresponds to the grid's [DataGrid.groups] list.
	 */
	final override val groupIndex: Int
		get() = _groupIndex

	private var _groupPosition: Int = -1

	/**
	 * groupPosition is the index within a group. It includes headers, footers, and list rows.
	 *
	 * It is mapped as follows:
	 * Group header (if showHeader)
	 * List elements (if !collapsed)
	 * Footer (if showFooter && (!collapsed || showFooterWhenCollapsed))
	 */
	final override val groupPosition: Int
		get() = _groupPosition

	private var _position: Int = -1

	/**
	 * The row position within the grid.
	 * Reading this property is O(C)
	 * Writing this property is O(groups.size)
	 * Setting this property iterates over the groups, to increment or decrement, use [hasNextRow], [moveToNextRow],
	 * [hasPreviousRow], and [moveToPreviousRow]
	 */
	final override var position: Int
		get() = _position
		set(newPosition) {
			if (newPosition == dataGrid._totalRows) {
				moveToLastRow() // An optimization for the common case.
				return
			}
			var r = 0
			_position = newPosition
			val groupCaches = displayGroupCaches
			for (i in 0..groupCaches.lastIndex) {
				val next = r + groupCaches[i].size
				if (newPosition < next) {
					_groupIndex = i
					_groupPosition = _position - r
					return
				}
				r = next
			}
			_groupIndex = groupCaches.lastIndex
			_groupPosition = groupCaches.last().lastIndex
		}

	val group: DataGridGroup<RowData>
		get() = dataGrid.displayGroups[this.groupIndex]

	internal val groupCache: DataGridCache<RowData>.GroupCache
		get() = displayGroupCaches[groupIndex]

	/**
	 * The index of the element within the group's filtered list.
	 */
	final override val rowIndex: Int
		get() = _groupPosition - groupCache.listStartIndex

	/**
	 * The element. This may be null if this location represents a header or footer.
	 * It will be guaranteed to be not null if [isElementRow] is true.
	 */
	final override val element: RowData?
		get() = groupCache.list.getOrNull(rowIndex)

	/**
	 * Returns true if the [groupPosition] represents a header row.
	 */
	final override val isHeader: Boolean
		get() = groupCache.showHeader && _groupPosition == 0

	/**
	 * Returns true if the [groupPosition] represents an element and not a header or footer.
	 */
	final override val isElementRow: Boolean
		get() = groupCache.showList && _groupPosition >= groupCache.listStartIndex && _groupPosition <= groupCache.listLastIndex

	/**
	 * Returns true if the [groupPosition] represents a footer row.
	 */
	final override val isFooter: Boolean
		get() = groupCache.showFooter && _groupPosition == groupCache.footerIndex

	/**
	 * Returns true if this location is within bounds.
	 */
	override val isValid: Boolean
		get() = _position >= 0 && _position < dataGrid._totalRows

	/**
	 * The index of this row within the source [DataGrid.data] list.
	 * Note: This will return -1 if this row location doesn't represent an element. (That is, it's a header or
	 * footer row)
	 * @see isElementRow
	 *
	 * This property is derived.
	 */
	final override var sourceIndex: Int
		get() {
			if (!isElementRow) return -1
			return dataGrid.dataView.localIndexToSource(groupCache.list.localIndexToSource(rowIndex))
		}
		set(value) {
			_groupIndex = 0
			_position = -1
			_groupPosition = -1
			val element = dataGrid.data[value]
			if (dataGrid.dataView.filter?.invoke(element) == false) return
			var newPosition = 0
			for (groupIndex in 0..displayGroupCaches.lastIndex) {
				val groupCache = displayGroupCaches[groupIndex]
				if (groupCache.showList && groupCache.list.filter?.invoke(element) != false) {
					val rowIndex = if (dataGrid.groups.isEmpty()) dataGrid.dataView.sourceIndexToLocal(value) else groupCache.list.sourceIndexToLocal(dataGrid.dataView.sourceIndexToLocal(value))
					_groupPosition = rowIndex + groupCache.listStartIndex
					_groupIndex = groupIndex
					_position = newPosition + _groupPosition
					return
				}
				newPosition += groupCache.size
			}
		}

	constructor(dataGrid: DataGrid<RowData>, position: Int) : this(dataGrid) {
		this.position = position
	}

	override fun copy(): RowLocation<RowData> = RowLocation(dataGrid).set(this)

	fun set(other: RowLocationRo<RowData>): RowLocation<RowData> {
		_groupIndex = other.groupIndex
		_groupPosition = other.groupPosition
		_position = other.position
		return this
	}

	/**
	 * Move the cursor so that [moveToNextRow] will bring us to the first position.
	 */
	fun moveToFirstRow(): RowLocation<RowData> {
		_position = -1
		_groupIndex = maxOf(0, displayGroupCaches.indexOfFirst2 { it.shouldRender })
		_groupPosition = -1
		return this
	}

	/**
	 * Move the cursor so that [moveToPreviousRow] will bring us to the last position.
	 */
	fun moveToLastRow(): RowLocation<RowData> {
		_position = dataGrid._totalRows
		_groupIndex = maxOf(0, displayGroupCaches.indexOfLast2 { it.shouldRender })
		_groupPosition = groupCache.size
		return this
	}

	final override val hasPreviousRow: Boolean
		get() = _position > 0

	final override val hasNextRow: Boolean
		get() = _position < dataGrid._totalRows - 1

	fun moveToPreviousRow() {
		_position--
		_groupPosition--

		if (_groupPosition < 0) {
			while (!displayGroupCaches[--_groupIndex].shouldRender) {
			}
			_groupPosition = groupCache.lastIndex
		}
	}

	fun moveToNextRow() {
		_position++
		_groupPosition++

		if (_groupPosition >= groupCache.size) {
			while (!displayGroupCaches[++_groupIndex].shouldRender) {
			}
			_groupPosition = 0
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is RowLocationRo<*>) return false
		if (this._position != other.position) return false
		return true
	}

	override fun hashCode(): Int {
		return _position
	}

	override fun toString(): String {
		return "RowLocation(position=$position, groupIndex=$groupIndex, rowIndex=$rowIndex)"
	}
}

/**
 * Calls [RowLocation.moveToPreviousRow] until there are no more previous rows, or the [predicate] has returned true.
 * @return Returns true if the predicate was ever matched.
 */
fun <T : RowLocation<*>> T.moveToPreviousRowUntil(predicate: (T) -> Boolean): Boolean {
	while (hasPreviousRow) {
		moveToPreviousRow()
		if (predicate(this)) return true
	}
	return false
}

/**
 * Calls [RowLocation.moveToNextRow] until there are no more next rows, or the [predicate] has returned true.
 * @return Returns true if the predicate was ever matched.
 */
fun <T : RowLocation<*>> T.moveToNextRowUntil(predicate: (T) -> Boolean): Boolean {
	while (hasNextRow) {
		moveToNextRow()
		if (predicate(this)) return true
	}
	return false
}

/**
 * While [predicate] returns false, calls [RowLocation.moveToNextRow].
 * @return Returns true if the predicate was ever matched.
 */
fun <T : RowLocation<*>> T.findNextRow(predicate: (T) -> Boolean): Boolean {
	while (true) {
		if (predicate(this)) return true
		if (!hasNextRow) return false
		moveToNextRow()
	}
}

/**
 * While [predicate] returns false, calls [RowLocation.moveToPreviousRow].
 * @return Returns true if the predicate was ever matched.
 */
fun <T : RowLocation<*>> T.findPreviousRow(predicate: (T) -> Boolean): Boolean {
	while (true) {
		if (predicate(this)) return true
		if (!hasPreviousRow) return false
		moveToPreviousRow()
	}
}