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

import com.acornui.collection.ObservableList
import com.acornui.core.Disposable

/**
 * Returns a binding that tracks an index within the target list.
 */
fun <E> ObservableList<E>.bindIndex(index: Int, recoverOnReset: Boolean = true) = IndexBinding(this, index, recoverOnReset)


/**
 * Watches an observable list, updating an index when the list changes.
 * This can be useful if the source list contains non-unique elements and you need to follow the changing index of
 * a specific element in the list.
 *
 * If the index watched is removed or changed, the binding will be disposed.
 * If the list is reset,
 *
 * @param list The source list to observe.
 * @param index The index of the element to track.
 */
class IndexBinding<out E>(
		private val list: ObservableList<E>,
		index: Int,
		var recoverOnReset: Boolean = true
) : Disposable {

	private var _element: E? = null

	private var _index: Int = -1

	var index: Int
		get() = _index
		set(value) {
			if (_index == value) return
			_index = value
			_element = list.getOrNull(value)
		}

	init {
		if (index < 0 || index >= list.size) throw Exception("Cannot track an out of bounds index.")
		this.index = index
		list.added.add(this::addedHandler)
		list.removed.add(this::removedHandler)
		list.changed.add(this::changedHandler)
		list.reset.add(this::resetHandler)
	}

	val element: E?
		get() = _element

	private fun addedHandler(i: Int, element: E) {
		if (i <= index) ++index
	}

	private fun removedHandler(i: Int, element: E) {
		if (i == index)
			this.index = -1
		else if (i <= index) --index
	}

	private fun changedHandler(index: Int, old: E, new: E) {
		if (index == this.index)
			this.index = -1
	}

	private fun resetHandler() {
		if (recoverOnReset) {
			if (list.getOrNull(_index) != _element)
				index = list.indexOf(_element)
		}
		else index = -1
	}

	override fun dispose() {
		index = -1
		list.added.remove(this::addedHandler)
		list.removed.remove(this::removedHandler)
		list.changed.remove(this::changedHandler)
		list.reset.remove(this::resetHandler)
	}
}
