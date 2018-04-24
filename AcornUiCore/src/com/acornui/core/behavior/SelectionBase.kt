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

import com.acornui.collection.Clearable
import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.collection.forEach2
import com.acornui.core.Disposable
import com.acornui.signal.Cancel
import com.acornui.signal.Signal
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3

interface SelectionRo<E : Any> {

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
	 * @param ordered If true, the list will be ordered. (Slower)
	 * @param out The list to fill with the selected items.
	 * @return Returns the [out] list.
	 */
	fun getSelectedItems(ordered: Boolean, out: MutableList<E>): MutableList<E>

	val isEmpty: Boolean
	val isNotEmpty: Boolean
	val selectedItemsCount: Int
	fun getItemIsSelected(item: E): Boolean
}

interface Selection<E : Any> : SelectionRo<E>, Clearable {

	/**
	 * Dispatched when an item's selection status is about to change. This provides an opportunity to cancel the
	 * selection change.
	 * (element, toggled, cancel)
	 */
	val changing: Signal<(E, Boolean, Cancel) -> Unit>

	override var selectedItem: E?

	fun setItemIsSelected(item: E, value: Boolean)
	fun setSelectedItems(items: Map<E, Boolean>)
	fun selectAll()

}

/**
 * An object for tracking selection state.
 * This is typically paired with a selection controller.
 * @author nbilyk
 */
abstract class SelectionBase<E : Any> : Selection<E>, Disposable {

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
	override fun getSelectedItems(ordered: Boolean, out: MutableList<E>): MutableList<E> {
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
		return out
	}

	/**
	 * The first selected item.
	 */
	override var selectedItem: E?
		get() = _selectedMap.keys.firstOrNull()
		set(value) {
			for (key in _selectedMap.keys) {
				if (key !== value)
					setItemIsSelected(key, false)
			}
			if (value != null)
				setItemIsSelected(value, true)
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

	override fun clear() {
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
		clear()
	}
}

fun <E : Any> Selection<E>.deselectNotContaining(list: List<E>) {
	val tmp = arrayListObtain<E>()
	getSelectedItems(false, tmp).forEach2 {
		if (!list.contains(it))
			setItemIsSelected(it, false)

	}
	arrayListPool.free(tmp)
}