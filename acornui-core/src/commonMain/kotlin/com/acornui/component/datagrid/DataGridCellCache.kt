package com.acornui.component.datagrid

import com.acornui.collection.ObjectPool
import com.acornui.collection.ObservableList
import com.acornui.collection.ObservableListMapping
import com.acornui.component.ElementContainerImpl
import com.acornui.component.UiComponent
import com.acornui.core.Disposable
import com.acornui.core.cache.IndexedCache
import com.acornui.core.cache.UsedTracker
import com.acornui.core.cache.disposeAndClear

/**
 * @suppress
 */
//internal class DataGridCellCache<RowData>(
//		//private val container: ElementContainerImpl<UiComponent>,
//		private val columnCaches: List<DataGrid<RowData>.ColumnCache>,
//		columns: ObservableList<DataGridColumn<RowData, *>>
//) : Disposable {
//
//	val usedGroupHeaders = UsedTracker<UiComponent>()
//
//	val columnCellCaches = ObservableListMapping(
//			target = columns,
//			factory = { column ->
//				IndexedCache(columnCaches)
//			},
//			disposer = { cellCache ->
//				cellCache.disposeAndClear()
//			}
//	)
//
//	override fun dispose() {
//		columnCellCaches.dispose()
//	}
//}