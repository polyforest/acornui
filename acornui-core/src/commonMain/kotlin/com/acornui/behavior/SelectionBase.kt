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

package com.acornui.behavior

import com.acornui.recycle.Clearable
import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.Disposable
import com.acornui.signal.Cancel
import com.acornui.signal.Signal
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3

interface SelectionRo<E : Any> {

	/**
	 * Dispatched when the selection status of an item has changed via user input.
	 * The handler should have the signature:
	 * oldSelected: List<E>, newSelected: List<E>
	 */
	val changed: Signal<(List<E>, List<E>) -> Unit>

	/**
	 * The first selected item. If there is more than one item selected, this will only return the first.
	 */
	val selectedItem: E?

	/**
	 * Populates the [out] list with the selected items.
	 * @param ordered If true, the list will be ordered. (Slower)
	 * @param out The list to fill with the selected items.
	 * @return Returns the [out] list.
	 */
	fun getSelectedItems(ordered: Boolean, out: MutableList<E>): MutableList<E>

	/**
	 * Populates the [out] list with the selected items (unordered).
	 * @param out The list to fill with the selected items.
	 * @return Returns the [out] list.
	 * @see getSelectedItems
	 */
	fun getSelectedItems(out: MutableList<E>): MutableList<E> = getSelectedItems(false, out)

	val isEmpty: Boolean
	val isNotEmpty: Boolean
	val selectedItemsCount: Int
	fun getItemIsSelected(item: E): Boolean
}

interface Selection<E : Any> : SelectionRo<E>, Clearable {

	/**
	 * Dispatched when an item's selection status is about to change via user input. This provides an opportunity to
	 * cancel the selection change.
	 * (oldSelected: List<E>, newSelected: List<E>, cancel)
	 */
	val changing: Signal<(List<E>, List<E>, Cancel) -> Unit>

	/**
	 * Gets/sets the first selected item.
	 * If set, the selection is cleared and the selection will be the provided item if not null.
	 * Note: setting this does not invoke a [changing] or [changed] signal.
	 */
	override var selectedItem: E?

	/**
	 * Sets the selected items.
	 * Note: setting this does not invoke a [changing] or [changed] signal.
	 * @see setSelectedItemsUser
	 */
	fun setSelectedItems(items: List<E>)

	/**
	 * Sets the selected items and invokes [changing] and [changed] signals.
	 */
	fun setSelectedItemsUser(items: List<E>)

	/**
	 * Selects all items. Does not dispatch signals.
	 */
	fun selectAll()

}

/**
 * An object for tracking selection state.
 * This is typically paired with a selection controller.
 * @author nbilyk
 */
abstract class SelectionBase<E : Any> : Selection<E>, Disposable {

	private val _changing = Signal3<List<E>, List<E>, Cancel>()

	/**
	 * The currently selected items are changing. This provides an opportunity to cancel the event before [changed]
	 * is invoked.
	 */
	override val changing = _changing.asRo()

	private val cancel = Cancel()

	private val _changed = Signal2<List<E>, List<E>>()

	/**
	 * Dispatched when the selection has changed based on user interaction from via [setSelectedItemsUser] and the
	 * [changing] signal was not canceled.
	 * This is not no-oped; calling [setSelectedItemsUser] with the currently selected items will still result
	 * in a [changed] event.
	 */
	override val changed = _changed.asRo()

	private val _selectedMap = HashMap<E, Boolean>()

	/**
	 * Walks all selectable items, in order.
	 */
	protected abstract fun walkSelectableItems(callback: (item: E) -> Unit)

	/**
	 * Populates the [out] list with the selected items.
	 * @param out The list to fill with the selected items.
	 * @param ordered If true, the list will be ordered. (Slower)
	 */
	override fun getSelectedItems(ordered: Boolean, out: MutableList<E>): MutableList<E> {
		out.clear()
		if (ordered) {
			walkSelectableItems {
				if (getItemIsSelected(it))
					out.add(it)
			}
		} else {
			for (item in _selectedMap.keys) {
				out.add(item)
			}
		}
		return out
	}

	/**
	 * The first selected item.
	 */
	override var selectedItem: E?
		get() = _selectedMap.keys.firstOrNull()
		set(value) {
			setSelectedItems(if (value == null) emptyList() else listOf(value))
		}

	override val isEmpty: Boolean
		get() = _selectedMap.isEmpty()

	override val isNotEmpty: Boolean
		get() = _selectedMap.isNotEmpty()

	override val selectedItemsCount: Int
		get() = _selectedMap.size

	override fun getItemIsSelected(item: E): Boolean {
		return _selectedMap[item] ?: false
	}

	override fun setSelectedItems(items: List<E>) {
		val oldSelection = _selectedMap.keys.toList()
		_selectedMap.clear()
		_selectedMap.putAll(items.map { it to true })
		onSelectionChanged(oldSelection, items)
	}

	/**
	 * The user has changed the selection. This will invoke the [changing] and if not canceled, [changed] signals.
	 *
	 * This is not no-oped; calling [setSelectedItemsUser] with the currently selected items will still result
	 * in a [changed] event.
	 */
	override fun setSelectedItemsUser(items: List<E>) {
		val previousSelection = _selectedMap.keys.toList()
		if (changing.isNotEmpty()) {
			_changing.dispatch(previousSelection, items, cancel.reset())
			if (cancel.canceled) return
		}
		setSelectedItems(items)
		_changed.dispatch(previousSelection, items)
	}

	protected abstract fun onSelectionChanged(oldSelection: List<E>, newSelection: List<E>)

	override fun clear() {
		setSelectedItems(emptyList())
	}

	override fun selectAll() {
		val newSelection = ArrayList<E>()
		walkSelectableItems {
			newSelection.add(it)
		}
		setSelectedItems(newSelection)
	}

	override fun dispose() {
		_changing.dispose()
		_changed.dispose()
	}
}

fun <E : Any> Selection<E>.deselectNotContaining(list: List<E>) {
	val tmp = arrayListObtain<E>()
	val current = getSelectedItems(false, tmp)
	setSelectedItems(current.intersect(list).toList())
	arrayListPool.free(tmp)
}
