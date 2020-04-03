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

package com.acornui.collection

import com.acornui.Disposable
import com.acornui.observe.Observable
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3

/**
 * A list that allows for modification during iteration when using special iteration methods.
 * @author nbilyk
 */
class ActiveList<E>(initialCapacity: Int) : MutableListBase<E>(), MutableObservableList<E>, MutableSnapshotList<E>, Disposable {

	private val wrapped = SnapshotListImpl<E>(initialCapacity)

	private val _added = Signal2<Int, E>()
	override val added = _added.asRo()

	private val _removed = Signal2<Int, E>()
	override val removed = _removed.asRo()

	private val _changed = Signal3<Int, E, E>()
	override val changed = _changed.asRo()

	private val _modified = Signal2<Int, E>()
	override val modified = _modified.asRo()

	private val _reset = Signal0()
	override val reset = _reset.asRo()

	private var updatesEnabled: Boolean = true

	constructor() : this(8)

	constructor(source: List<E>) : this(maxOf(8, source.size)) {
		batchUpdate {
			addAll(source)
		}
	}

	constructor(source: Array<E>) : this(maxOf(8, source.size)) {
		batchUpdate {
			addAll(source)
		}
	}

	override val size: Int
		get() = wrapped.size

	override fun get(index: Int): E = wrapped[index]

	override fun add(index: Int, element: E) {
		wrapped.add(index, element)
		if (updatesEnabled) _added.dispatch(index, element)
	}

	override fun removeAt(index: Int): E {
		val element = wrapped.removeAt(index)
		if (updatesEnabled) _removed.dispatch(index, element)
		return element
	}

	override fun set(index: Int, element: E): E {
		val oldElement = wrapped.set(index, element)
		if (oldElement === element) return oldElement
		if (updatesEnabled) _changed.dispatch(index, oldElement, element)
		return oldElement
	}

	override fun batchUpdate(inner: () -> Unit) {
		updatesEnabled = false
		inner()
		updatesEnabled = true
		_reset.dispatch()
	}

	override fun notifyElementModified(index: Int) {
		if (index < 0 || index >= size) return
		_modified.dispatch(index, get(index))
	}

	/**
	 * Force notifies a reset.
	 */
	fun dirty() {
		if (updatesEnabled)
			_reset.dispatch()
	}

	override fun begin(): List<E> = wrapped.begin()

	override fun end() = wrapped.end()

	override fun dispose() {
		clear()
		_added.dispose()
		_removed.dispose()
		_changed.dispose()
		_modified.dispose()
		_reset.dispose()
	}
}

/**
 * An observable list of observable elements that will notify [modified] when an element has changed.
 */
class WatchedElementsActiveList<E : Observable>(initialCapacity: Int = 8) : MutableListBase<E>(), MutableSnapshotList<E>, MutableObservableList<E>, Disposable {

	constructor(source: List<E>) : this() {
		batchUpdate {
			addAll(source)
		}
	}

	constructor(source: Array<E>) : this() {
		batchUpdate {
			addAll(source)
		}
	}

	private val wrapped = ActiveList<E>(initialCapacity)

	override val added: Signal<(index: Int, element: E) -> Unit> = wrapped.added
	override val removed: Signal<(index: Int, element: E) -> Unit> = wrapped.removed
	override val changed: Signal<(index: Int, oldElement: E, newElement: E) -> Unit> = wrapped.changed
	override val modified: Signal<(index: Int, element: E) -> Unit> = wrapped.modified
	override val reset: Signal<() -> Unit> = wrapped.reset

	override fun removeAt(index: Int): E {
		val e = wrapped.removeAt(index)
		e.changed.remove(::elementModifiedHandler)
		return e
	}

	override fun add(index: Int, element: E) {
		element.changed.add(::elementModifiedHandler)
		wrapped.add(index, element)
	}

	private fun elementModifiedHandler(o: Observable) {
		val i = indexOfFirst2 { it === o } // Use identity equals
		notifyElementModified(i)
	}

	override val size: Int
		get() = wrapped.size

	override fun get(index: Int): E {
		return wrapped[index]
	}

	override fun set(index: Int, element: E): E {
		val oldElement = wrapped.set(index, element)
		oldElement.changed.remove(::elementModifiedHandler)
		element.changed.add(::elementModifiedHandler)
		return oldElement
	}

	override fun notifyElementModified(index: Int) {
		wrapped.notifyElementModified(index)
	}

	override fun batchUpdate(inner: () -> Unit) {
		wrapped.batchUpdate(inner)
	}

	override fun begin(): List<E> = wrapped.begin()

	override fun end() = wrapped.end()

	override fun dispose() = wrapped.dispose()
}

fun <E> activeListOf(vararg elements: E): ActiveList<E> {
	val list = ActiveList<E>()
	list.batchUpdate {
		list.addAll(elements)
	}
	return list
}
