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

import com.acornui.collection.ListView
import com.acornui.collection.ObservableListMapping
import com.acornui.component.ElementContainerImpl
import com.acornui.component.UiComponent
import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.recycle.*

/**
 * The DataGrid uses complex caching mechanisms for performance. These display properties are separated for
 * readability.
 *
 * @suppress
 */
internal class DataGridCache<E>(private val grid: DataGrid<E>) : Disposable {

	val usedBottomGroupHeaders = UsedTracker<UiComponent>()
	val usedGroupHeaders = UsedTracker<UiComponent>()

	val defaultGroupCache = arrayListOf(GroupCache(DataGrid.defaultGroups<E>().first()))

	/**
	 * Marked which columns are used in an update layout.
	 */
	val usedColumns = UsedTracker<Int>()

	val rowBackgroundsCache = IndexedRecycleList { grid.style.rowBackground(grid) }

	val columnCaches = ObservableListMapping(
			target = grid.columns,
			factory = { _, column ->
				ColumnCache(grid, column)
			},
			disposer = { columnIndex, columnCache ->
				usedColumns.forget(columnIndex)
				columnCache.dispose()
			}
	)

	val groupCaches = ObservableListMapping(
			target = grid.groups,
			factory = { _, group ->
				GroupCache(group)
			},
			disposer = { _, groupCache ->
				dispose(groupCache)
				groupCache.dispose()
			}
	)

	/**
	 * @suppress
	 */
	internal val displayGroupCaches: List<DataGridCache<E>.GroupCache>
		get() = if (grid.groups.isEmpty()) defaultGroupCache else groupCaches

	private fun dispose(groupCache: GroupCache) {
		if (groupCache.header != null) {
			usedGroupHeaders.forget(groupCache.header!!)
		}
		if (groupCache.bottomHeader != null) {
			usedBottomGroupHeaders.forget(groupCache.bottomHeader!!)
		}
	}

	override fun dispose() {
		columnCaches.dispose()
		groupCaches.dispose()
	}

	/**
	 * Cached display values for a column.
	 */
	internal inner class ColumnCache(owner: Owned, val column: DataGridColumn<E, *>) : Disposable {

		var headerCell: UiComponent? = null

		private val cellPool = ObjectPool {
			val newCell = column.createCell(owner)
			newCell.styleTags.add(DataGrid.BODY_CELL)
			newCell
		}

		val cellCache = IndexedRecycleList(cellPool)
		val bottomCellCache = IndexedRecycleList(cellPool)

		override fun dispose() {
			headerCell?.dispose()
			headerCell = null
			cellCache.clear()
			bottomCellCache.clear()
			cellPool.disposeAndClear()
		}
	}

	/**
	 * Cached display values for a data grid group.
	 * @suppress
	 */
	internal inner class GroupCache(val group: DataGridGroup<E>) : Disposable {

		val list: ListView<E> = ListView(grid.dataView).apply { filter = group.filter }

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

		override fun dispose() {
			header?.dispose()
			header = null
			bottomHeader?.dispose()
			bottomHeader = null
		}
	}
}

internal fun <T : UiComponent> IndexedRecycleList<T>.removeAndFlip(container: ElementContainerImpl<UiComponent>) {
	forEachUnused { index, element ->
		container.removeElement(element)
	}.flip()
}