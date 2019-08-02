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
import com.acornui.EqualityCheck

/**
 * Binds to a target observable list, invoking [configure] on added elements and [unconfigure] on removed elements.
 *
 *
 * @param equality For changed elements, if [equality] returns true for the old element compared to the new element,
 * a [unconfigure] will not be invoked on the old element and [configure] will not be invoked on the new element.
 */
@Suppress("UNUSED_PARAMETER")
class ObservableListBinding<E>(
		private val target: ObservableList<E>,
		private val configure: (Int, E) -> Unit,
		private val unconfigure: (Int, E) -> Unit,
		private val equality: EqualityCheck<E> = { o1, o2 -> o1 == o2 }
) : Disposable {

	private val _list = ArrayList<E>()

	init {
		target.added.add(::addedHandler)
		target.removed.add(::removedHandler)
		target.changed.add(::changedHandler)
		target.reset.add(::resetHandler)
		resetHandler()
	}

	//---------------------------------------------
	// Target list handlers
	//---------------------------------------------

	private fun addedHandler(index: Int, value: E) {
		configure(index, value)
		_list.add(index, value)
	}

	private fun removedHandler(index: Int, value: E) {
		unconfigure(index, value)
		_list.removeAt(index)
	}

	private fun changedHandler(index: Int, oldValue: E, newValue: E) {
		if (equality(oldValue, newValue)) return
		unconfigure(index, oldValue)
		configure(index, newValue)
		_list[index] = newValue
	}

	private fun resetHandler() {
		for (i in 0..minOf(_list.lastIndex, target.lastIndex)) {
			changedHandler(i, _list[i], target[i])
		}
		for (i in _list.lastIndex downTo target.size) {
			removedHandler(i, _list[i])
		}
		for (i in _list.size .. target.lastIndex) {
			addedHandler(i, target[i])
		}
	}

	private fun clear() {
		for (i in 0.._list.lastIndex) {
			unconfigure(i, _list[i])
		}
		_list.clear()
	}

	override fun dispose() {
		clear()
		target.added.remove(::addedHandler)
		target.removed.remove(::removedHandler)
		target.changed.remove(::changedHandler)
		target.reset.remove(::resetHandler)
	}
}
