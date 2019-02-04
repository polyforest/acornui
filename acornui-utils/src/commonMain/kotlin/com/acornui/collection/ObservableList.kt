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

@file:Suppress("UNUSED_PARAMETER")

package com.acornui.collection

import com.acornui.function.as2
import com.acornui.function.as3
import com.acornui.signal.Bindable
import com.acornui.signal.Signal
import com.acornui.signal.emptySignal

interface ObservableList<out E> : ConcurrentList<E>, Bindable {

	/**
	 * Dispatched when an element has been added.
	 */
	val added: Signal<(index: Int, element: E) -> Unit>

	/**
	 * Dispatched when an element has been removed.
	 */
	val removed: Signal<(index: Int, element: E) -> Unit>

	/**
	 * Dispatched when an element has replaced another.
	 */
	val changed: Signal<(index: Int, oldElement: E, newElement: E) -> Unit>

	/**
	 * Dispatched when an element has been modified.
	 */
	val modified: Signal<(index: Int, element: E) -> Unit>

	/**
	 * Dispatched if this list has drastically changed, such as a batch update or a clear.
	 */
	val reset: Signal<() -> Unit>

	/**
	 * Notifies that an element has changed. This will dispatch a [modified] signal.
	 * Note: This is not in MutableObservableList because an element of an ObservableList may still be modified without
	 * modifying the list itself.
	 */
	fun notifyElementModified(index: Int)

	override fun addBinding(callback: () -> Unit) {
		added.add(callback.as2)
		removed.add(callback.as2)
		changed.add(callback.as3)
		modified.add(callback.as2)
		reset.add(callback)
	}

	override fun removeBinding(callback: () -> Unit) {
		added.remove(callback.as2)
		removed.remove(callback.as2)
		changed.remove(callback.as3)
		modified.remove(callback.as2)
		reset.remove(callback)
	}
}

interface MutableObservableList<E> : ObservableList<E>, MutableConcurrentList<E> {

	/**
	 * Updates this list without invoking signals on each change. When the [inner] method has completed, a [reset]
	 * signal will be dispatched.
	 */
	fun batchUpdate(inner: () -> Unit)

}

fun <E> ObservableList<E>.bindUniqueAssertion() {
	added.add {
		_, element ->
		if (count2 { it == element } > 1) throw Exception("The element added: $element was not unique within this list.")
	}
	changed.add {
		_, _, newElement ->
		if (count2 { it == newElement } > 1) throw Exception("The element added: $newElement was not unique within this list.")
	}
	reset.add {
		assertUnique()
	}
	assertUnique()
}

fun <E> ObservableList<E>.assertUnique() {
	for (i in 0..lastIndex) {
		if (indexOfFirst2(i + 1, lastIndex) { it == this[i] } != -1) {
			throw Exception("The element ${this[i]} is not unique within this list.")
		}
	}
}

internal object EmptyObservableList : ListBase<Nothing>(), ObservableList<Nothing> {
	override val size: Int = 0

	override fun get(index: Int): Nothing = throw IndexOutOfBoundsException()

	override val added: Signal<(index: Int, element: Nothing) -> Unit> = emptySignal()
	override val removed: Signal<(index: Int, element: Nothing) -> Unit> = emptySignal()
	override val changed: Signal<(index: Int, oldElement: Nothing, newElement: Nothing) -> Unit> = emptySignal()
	override val modified: Signal<(index: Int, element: Nothing) -> Unit> = emptySignal()
	override val reset: Signal<() -> Unit> = emptySignal()

	override fun notifyElementModified(index: Int) {}

	override fun concurrentIterator(): ConcurrentListIterator<Nothing> = object : ConcurrentListIterator<Nothing> {
		override val size: Int = 0
		override var cursor: Int = 0
		override fun clear() {}

		override fun iterator(): Iterator<Nothing> = emptyList<Nothing>().iterator()

		override fun hasNext(): Boolean = false
		override fun hasPrevious(): Boolean = false

		override fun next(): Nothing { throw NoSuchElementException() }

		override fun nextIndex(): Int = 0

		override fun previous(): Nothing { throw NoSuchElementException() }

		override fun previousIndex(): Int = -1

		override fun dispose() {}
	}
}

fun <T> emptyObservableList(): ObservableList<T> = EmptyObservableList