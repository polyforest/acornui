/*
 * Copyright 2018 Poly Forest
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

import com.acornui.core.Disposable
import com.acornui.signal.Signal
import com.acornui.signal.Signal0
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3

class ObservableConcatList<out E>(private val listA: ObservableList<E>, private val listB: ObservableList<E>) : ListBase<E>(), ObservableList<E>, Disposable {

	private val _added = Signal2<Int, E>()
	override val added: Signal<(Int, E) -> Unit>
		get() = _added

	private val _removed = Signal2<Int, E>()
	override val removed: Signal<(Int, E) -> Unit>
		get() = _removed

	private val _changed = Signal3<Int, E, E>()
	override val changed: Signal<(Int, E, E) -> Unit>
		get() = _changed

	private val _modified = Signal2<Int, E>()
	override val modified: Signal<(Int, E) -> Unit>
		get() = _modified

	private val _reset = Signal0()
	override val reset: Signal<() -> Unit>
		get() = _reset

	override val size: Int
		get() = listA.size + listB.size

	override fun get(index: Int): E {
		return if (index >= listA.size) listB[index - listA.size] else listA[index]
	}

	init {
		listA.added.add {
			index, element ->
			_added.dispatch(index, element)
		}
		listB.added.add {
			index, element ->
			_added.dispatch(index + listA.size, element)
		}
		listA.removed.add {
			index, element ->
			_removed.dispatch(index, element)
		}
		listB.removed.add {
			index, element ->
			_removed.dispatch(index + listA.size, element)
		}
		listA.changed.add {
			index, oldElement, newElement ->
			_changed.dispatch(index, oldElement, newElement)
		}
		listB.changed.add {
			index, oldElement, newElement ->
			_changed.dispatch(index + listA.size, oldElement, newElement)
		}
		listA.modified.add {
			index, element ->
			_modified.dispatch(index, element)
		}
		listB.modified.add {
			index, element ->
			_modified.dispatch(index + listA.size, element)
		}
		listA.reset.add {
			_reset.dispatch()
		}
		listB.reset.add {
			_reset.dispatch()
		}
	}

	/**
	 * Copies this concatenation, creating a new list. The new list will no longer be backed by the two lists
	 * backing this concatenation.
	 */
	fun copy(): List<E> {
		val newList = ArrayList<E>(size)
		newList.addAll(this)
		return newList
	}

	override fun concurrentIterator(): ConcurrentListIterator<E> {
		return ConcurrentListIteratorImpl(this)
	}

	override fun notifyElementModified(index: Int) {
		_modified.dispatch(index, get(index))
	}

	override fun dispose() {
		_added.dispose()
		_removed.dispose()
		_changed.dispose()
		_modified.dispose()
		_reset.dispose()
	}
}

infix fun <T> ObservableList<T>.concat(other: ObservableList<T>): ObservableList<T> = ObservableConcatList(this, other)