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

@file:Suppress("UNUSED_PARAMETER")

package com.acornui.observe

import com.acornui.recycle.Clearable
import com.acornui.collection.ObservableList
import com.acornui.collection.rangeCheck
import com.acornui.core.Disposable
import com.acornui.core.EqualityCheck

/**
 * Returns a binding that tracks an index within the target list.
 */
fun <E> ObservableList<E>.bindIndex(index: Int, equality: EqualityCheck<E>? = { a, b -> a == b }) =
		IndexBinding(this, index, equality)


/**
 * Watches an observable list, updating an index when the list changes.
 * This can be useful if the source list contains non-unique elements and you need to follow the changing index of
 * a specific element in the list.
 */
class IndexBinding<E>() : Clearable, Disposable {

	constructor(source: List<E>, index: Int = -1, equality: EqualityCheck<E>? = { a, b -> a == b }) : this() {
		data(source)
		this.equality = equality
		this.index = index
	}

	constructor(source: ObservableList<E>, index: Int = -1, equality: EqualityCheck<E>? = { a, b -> a == b }) : this() {
		data(source)
		this.equality = equality
		this.index = index
	}

	/**
	 * On a list reset or remove + add, the equality property is used to recover the index.
	 */
	var equality: EqualityCheck<E>? = { a, b -> a == b }

	private var lastKnownIsSet = false
	private var lastKnownElement: E? = null

	private var _index: Int = -1
	var index: Int
		get() = _index
		set(value) {
			if (_index == value) return
			_index = value
			refreshLastKnown()
		}

	private fun refreshLastKnown() {
		lastKnownIsSet = _list?.rangeCheck(_index) == true
		lastKnownElement = if (lastKnownIsSet) _list!![_index] else null
	}

	val isEmpty: Boolean
		get() = _list?.rangeCheck(index) != true

	val isNotEmpty: Boolean
		get() = !isEmpty

	val element: E
		@Suppress("UNCHECKED_CAST")
		get() = if (isEmpty) throw Exception("Index binding is empty, use isEmpty before accessing element.") else lastKnownElement as E

	private var _observableList: ObservableList<E>? = null
	private var _list: List<E>? = null

	private fun unbind() {
		val list = _observableList ?: return
		list.added.remove(this::addedHandler)
		list.removed.remove(this::removedHandler)
		list.changed.remove(this::changedHandler)
		list.reset.remove(this::resetHandler)
	}

	fun data(source: List<E>?) {
		unbind()
		_list = source
		resetHandler()
	}

	fun data(source: ObservableList<E>?) {
		unbind()
		if (source != null) {
			source.added.add(this::addedHandler)
			source.removed.add(this::removedHandler)
			source.changed.add(this::changedHandler)
			source.reset.add(this::resetHandler)
		}
		_observableList = source
		_list = source
		resetHandler()
	}

	private fun addedHandler(i: Int, element: E) {
		@Suppress("UNCHECKED_CAST")
		if (_index == -1 && lastKnownIsSet && equality?.invoke(element, lastKnownElement as E) == true) {
			_index = i
			lastKnownElement = element
		} else if (i <= _index) ++_index
	}

	private fun removedHandler(i: Int, element: E) {
		if (i == index)
			this._index = -1
		else if (i <= _index) --_index
	}

	private fun changedHandler(index: Int, old: E, new: E) {
		if (index != this.index) return
		if (equality?.invoke(old, new) == true) {
			lastKnownElement = new
		} else
			this.index = -1
	}

	private fun resetHandler() {
		if (_index == -1) return
		val list = _list!!
		val equality = equality
		if (equality == null || !lastKnownIsSet) {
			// No equality, cannot recover.
			index = -1
			return
		}
		@Suppress("UNCHECKED_CAST")
		val lastKnownElement = lastKnownElement as E

		// On a list reset, first check the greedy option - if the reset list contains an equal element in the same
		// spot. If not, search the list for the first match.
		if (isNotEmpty) {
			val potentialElement = list[_index]
			if (equality.invoke(lastKnownElement, potentialElement)) {
				this.lastKnownElement = potentialElement
				return
			}
		}
		_index = list.indexOfFirst { equality(it, lastKnownElement) }
		this.lastKnownElement = list.getOrNull(_index)
	}

	override fun clear() {
		unbind()
		index = -1
		_list = null
		_observableList = null
	}

	override fun dispose() = clear()
}
