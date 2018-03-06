/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.core.behavior

import com.acornui.core.Disposable
import com.acornui.signal.Cancel
import com.acornui.signal.Signal
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3

interface SelectionRo<E> {

	/**
	 * Dispatched when the selection status of an item has changed.
	 * The handler should have the signature:
	 * item: E, newState: Boolean
	 */
	val changed: Signal<(E, Boolean) -> Unit>

	/**
	 * The first selected item.
	 */
	val selectedItem: E?

	/**
	 * Populates the [out] list with the selected items.
	 * @param out The list to fill with the selected items.
	 * @param ordered If true, the list will be ordered. (Slower)
	 */
	fun selectedItems(out: MutableList<E>, ordered: Boolean)

	fun isEmpty(): Boolean
	fun isNotEmpty(): Boolean
	fun selectedItemsCount(): Int
	fun getItemIsSelected(item: E): Boolean
}

interface Selection<E> : SelectionRo<E> {

	/**
	 * Dispatched when an item's selection status is about to change. This provides an opportunity to cancel the
	 * selection change.
	 */
	val changing: Signal<(E, Boolean, Cancel) -> Unit>

	override var selectedItem: E?

	fun setItemIsSelected(item: E, value: Boolean)
	fun setSelectedItems(items: Map<E, Boolean>)
	fun clearSelection()
	fun selectAll()

}

/**
 * An object for tracking selection state.
 * This is typically paired with a selection controller.
 * @author nbilyk
 */
abstract class SelectionBase<E> : Selection<E>, Disposable {

	private val _changing = Signal3<E, Boolean, Cancel>()

	/**
	 * Dispatched when an item's selection status is about to change. This provides an opportunity to cancel the
	 * selection change.
	 */
	override val changing: Signal<(E, Boolean, Cancel) -> Unit>
		get() = _changing

	private val cancel = Cancel()

	private val _changed = Signal2<E, Boolean>()

	/**
	 * Dispatched when the selection status of an item has changed.
	 * The handler should have the signature:
	 * item: E, newState: Boolean
	 */
	override val changed: Signal<(E, Boolean) -> Unit>
		get() = _changed

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
	override fun selectedItems(out: MutableList<E>, ordered: Boolean) {
		out.clear()
		if (ordered) {
			walkSelectableItems {
				if (getItemIsSelected(it)) out.add(it)
			}
		} else {
			for (item in _selectedMap.keys) {
				out.add(item)
			}
		}
	}

	/**
	 * The first selected item.
	 */
	override var selectedItem: E?
		get() {
			return _selectedMap.keys.firstOrNull()
		}
		set(value) {
			if (value == null && _selectedMap.isEmpty()) return
			else if (value != null && getItemIsSelected(value)) return
			clearSelection()
			if (value != null)
				setItemIsSelected(value, true)
		}

	override fun isEmpty(): Boolean {
		return _selectedMap.isEmpty()
	}

	override fun isNotEmpty(): Boolean {
		return _selectedMap.isNotEmpty()
	}

	override fun selectedItemsCount(): Int {
		return _selectedMap.size
	}

	override fun getItemIsSelected(item: E): Boolean {
		return _selectedMap[item] ?: false
	}

	override fun setItemIsSelected(item: E, value: Boolean) {
		if (getItemIsSelected(item) == value) return // no-op
		if (changing.isNotEmpty()) {
			_changing.dispatch(item, value, cancel.reset())
			if (cancel.canceled()) return
		}
		if (value)
			_selectedMap[item] = true
		else
			_selectedMap.remove(item)
		onItemSelectionChanged(item, value)
		_changed.dispatch(item, value)
	}

	protected abstract fun onItemSelectionChanged(item: E, selected: Boolean)

	override fun setSelectedItems(items: Map<E, Boolean>) {
		for (item in _selectedMap.keys) {
			if (!items.containsKey(item) || items[item] == false) {
				setItemIsSelected(item, false)
			}
		}
		for (i in items.keys) {
			setItemIsSelected(i, true)
		}
	}

	override fun clearSelection() {
		for (key in _selectedMap.keys) {
			setItemIsSelected(key, false)
		}
	}

	override fun selectAll() {
		walkSelectableItems {
			setItemIsSelected(it, true)
		}
	}

	override fun dispose() {
		_changing.dispose()
		_changed.dispose()
		clearSelection()
	}

}
