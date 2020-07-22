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

import com.acornui.DisposableBase
import com.acornui.signal.Cancel
import com.acornui.signal.CancelRo
import com.acornui.signal.Signal
import com.acornui.signal.signal

class SelectionChangedEvent<E>(val oldSelection: Set<E>, val newSelection: Set<E>)

interface SelectionRo<E : Any> {

	/**
	 * Dispatched when the selection status of an item has changed via user input.
	 * The handler should have the signature:
	 * oldSelected: List<E>, newSelected: List<E>
	 */
	val changed: Signal<SelectionChangedEvent<E>>

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
	fun getSelectedItems(ordered: Boolean = false, out: MutableSet<E> = HashSet()): MutableSet<E>

	val isEmpty: Boolean
	val isNotEmpty: Boolean
	val selectedItemsCount: Int

	/**
	 * Returns true if the given item is selected.
	 */
	fun getItemIsSelected(item: E): Boolean
}

class SelectionChangingEvent<E>(val oldSelection: Set<E>, val newSelection: Set<E>, val cancel: CancelRo)

interface Selection<E : Any> : SelectionRo<E> {

	/**
	 * Dispatched when an item's selection status is about to change via user input. This provides an opportunity to
	 * cancel the selection change.
	 * (oldSelected: List<E>, newSelected: List<E>, cancel)
	 */
	val changing: Signal<SelectionChangingEvent<E>>

	/**
	 * Gets/sets the first selected item.
	 * If set, the selection is cleared and the selection will be the provided item if not null.
	 * Note: setting this does not invoke a [changing] or [changed] signal.
	 */
	override var selectedItem: E?

	/**
	 * Sets the selected items and invokes [changing] and [changed] signals.
	 */
	@Deprecated("", ReplaceWith("setSelectedItems(items, true)"))
	fun setSelectedItemsUser(items: Set<E>) = setSelectedItems(items, true)

	fun setSelectedItems(items: Set<E>, isUserInteraction: Boolean = false)

	/**
	 * Clears current selection.
	 * @param isUserInteraction If true, [Selection.changing] and [Selection.changed] signals will be dispatched.
	 */
	fun clear(isUserInteraction: Boolean = false)

	/**
	 * Selects all items.
	 * @param isUserInteraction If true, [Selection.changing] and [Selection.changed] signals will be dispatched.
	 */
	fun selectAll(isUserInteraction: Boolean = false)

}

/**
 * An object for tracking selection state.
 */
abstract class SelectionBase<E : Any> : Selection<E>, DisposableBase() {

	/**
	 * The currently selected items are changing. This provides an opportunity to cancel the event before [changed]
	 * is invoked.
	 */
	override val changing = signal<SelectionChangingEvent<E>>()

	private val cancel = Cancel()

	/**
	 * Dispatched when the selection has changed based on user interaction from via [setSelectedItemsUser] and the
	 * [changing] signal was not cancelled.
	 * This is not no-oped; calling [setSelectedItemsUser] with the currently selected items will still result
	 * in a [changed] event.
	 */
	override val changed = signal<SelectionChangedEvent<E>>()

	private val _selection = HashSet<E>()

	/**
	 * Walks all selectable items, in order.
	 */
	protected abstract fun walkSelectableItems(callback: (item: E) -> Unit)

	/**
	 * Populates the [out] list with the selected items.
	 * @param out The list to fill with the selected items.
	 * @param ordered If true, the list will be ordered. (Slower)
	 */
	override fun getSelectedItems(ordered: Boolean, out: MutableSet<E>): MutableSet<E> {
		out.clear()
		if (ordered) {
			walkSelectableItems {
				if (getItemIsSelected(it))
					out.add(it)
			}
		} else {
			out.addAll(_selection)
		}
		return out
	}

	/**
	 * The first selected item.
	 * Setting this will not invoke selection change events.
	 * @see setSelectedItems
	 */
	override var selectedItem: E?
		get() = _selection.firstOrNull()
		set(value) {
			setSelectedItems(if (value == null) emptySet() else setOf(value), isUserInteraction = false)
		}

	override val isEmpty: Boolean
		get() = _selection.isEmpty()

	override val isNotEmpty: Boolean
		get() = _selection.isNotEmpty()

	override val selectedItemsCount: Int
		get() = _selection.size

	override fun getItemIsSelected(item: E): Boolean {
		return _selection.contains(item)
	}

	override fun setSelectedItems(items: Set<E>, isUserInteraction: Boolean) {
		if (isUserInteraction) {
			val previousSelection = HashSet(_selection)
			changing.dispatch(SelectionChangingEvent(previousSelection, items, cancel.reset()))
			if (cancel.isCancelled) return
			setSelectedItemsInternal(items)
			changed.dispatch(SelectionChangedEvent(previousSelection, items))
		} else {
			setSelectedItemsInternal(items)
		}
	}

	private fun setSelectedItemsInternal(items: Set<E>) {
		val oldSelection = HashSet(_selection)
		_selection.clear()
		_selection.addAll(items)
		onSelectionChanged(oldSelection, items)
	}

	protected abstract fun onSelectionChanged(oldSelection: Set<E>, newSelection: Set<E>)

	override fun clear(isUserInteraction: Boolean) {
		setSelectedItems(emptySet(), isUserInteraction)
	}

	override fun selectAll(isUserInteraction: Boolean) {
		val newSelection = HashSet<E>()
		walkSelectableItems {
			newSelection.add(it)
		}
		setSelectedItems(newSelection, isUserInteraction)
	}
}

/**
 * Sets the selection to the intersection of the current selection and the provided selection.
 * @see MutableSet.retainAll
 * @param isUserInteraction If true, [Selection.changing] and [Selection.changed] signals will be dispatched.
 */
fun <E : Any> Selection<E>.retainAll(elements: Collection<E>, isUserInteraction: Boolean = true) {
	val selected = getSelectedItems(false)
	selected.retainAll(elements)
	setSelectedItems(selected, isUserInteraction)
}

/**
 * Adds all of the specified elements to the current selection.
 * @see MutableSet.addAll
 * @param isUserInteraction If true, [Selection.changing] and [Selection.changed] signals will be dispatched.
 */
fun <E : Any> Selection<E>.addAll(elements: Collection<E>, isUserInteraction: Boolean = true) {
	val selected = getSelectedItems(false)
	selected.addAll(elements)
	setSelectedItems(selected, isUserInteraction)
}

/**
 * Removes all of the specified elements from the current selection.
 * @see MutableSet.removeAll
 * @param isUserInteraction If true, [Selection.changing] and [Selection.changed] signals will be dispatched.
 */
fun <E : Any> Selection<E>.removeAll(elements: Collection<E>, isUserInteraction: Boolean = true) {
	val selected = getSelectedItems(false)
	selected.removeAll(elements)
	setSelectedItems(selected, isUserInteraction)
}

/**
 * Toggles the selection status of the given element.
 */
fun <E : Any> Selection<E>.toggleSelected(element: E, isUserInteraction: Boolean = true) {
	setItemIsSelected(element, !getItemIsSelected(element), isUserInteraction)
}

fun <E : Any> Selection<E>.setItemIsSelected(element: E, value: Boolean, isUserInteraction: Boolean = true) {
	if (value) {
		addAll(listOf(element), isUserInteraction)
	} else {
		removeAll(listOf(element), isUserInteraction)
	}
}