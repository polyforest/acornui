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
import com.acornui.recycle.Clearable

interface ConcurrentList<out E> : List<E> {

	fun concurrentIterator(): ConcurrentListIterator<E>
}

interface MutableConcurrentList<E> : ConcurrentList<E>, MutableList<E> {

	override fun concurrentIterator(): MutableConcurrentListIterator<E>
}

/**
 * A ConcurrentListIterator watches concurrent add and remove changes on the list and moves the cursor logically
 * so that items are neither skipped nor repeated.
 */
interface ConcurrentListIterator<out E> : Clearable, ListIterator<E>, Iterable<E>, Disposable {

	/**
	 * The size of the list.
	 */
	val size: Int

	/**
	 * The current cursor index.
	 */
	var cursor: Int

}

inline fun <E> ConcurrentListIterator<E>.iterate(body: (E) -> Boolean) {
	clear()
	while (hasNext()) {
		val shouldContinue = body(next())
		if (!shouldContinue) break
	}
}

inline fun <E> ConcurrentListIterator<E>.iterateReversed(body: (E) -> Boolean) {
	clear()
	cursor = size
	while (hasPrevious()) {
		val shouldContinue = body(previous())
		if (!shouldContinue) break
	}
}

fun <E> ConcurrentListIteratorImpl(
		list: ObservableList<E>
) : ConcurrentListIterator<E> {
	return WatchedConcurrentListIteratorImpl(ConcurrentListIteratorImpl(list as List<E>), list)
}

private open class WatchedConcurrentListIteratorImpl<out E>(private val impl: ConcurrentListIteratorImpl<E>, private val list: ObservableList<E>) : ConcurrentListIterator<E> by impl {

	private val addedHandler = {
		index: Int, _: E ->
		impl.notifyAddedAt(index)
	}

	private val removedHandler = {
		index: Int, _: E ->
		impl.notifyRemovedAt(index)
	}

	init {
		list.added.add(addedHandler)
		list.removed.add(removedHandler)
	}

	override fun dispose() {
		list.added.remove(addedHandler)
		list.removed.remove(removedHandler)
	}
}

fun <E> MutableConcurrentListIteratorImpl(
		list: MutableObservableList<E>
) : MutableConcurrentListIterator<E> {
	return WatchedMutableConcurrentListIteratorImpl(MutableConcurrentListIteratorImpl(list as MutableList<E>), list)
}

private open class WatchedMutableConcurrentListIteratorImpl<E>(private val impl: MutableConcurrentListIteratorImpl<E>, private val list: MutableObservableList<E>) : WatchedConcurrentListIteratorImpl<E>(impl, list), MutableConcurrentListIterator<E> {

	override fun iterator(): MutableIterator<E> = list.iterator()

	override fun add(element: E) {
		impl.add(element)
	}

	override fun remove() {
		impl.remove()
	}

	override fun set(element: E) {
		impl.set(element)
	}
}

open class ConcurrentListIteratorImpl<out E>(
		private val list: List<E>
) : ConcurrentListIterator<E> {

	override val size: Int
		get() = list.size

	/**
	 * Index of next element to return
	 */
	override var cursor: Int = 0

	/**
	 * Index of last element returned; -1 if no such
	 */
	protected var lastRet: Int = -1

	fun notifyAddedAt(index: Int) {
		if (cursor > index) cursor++
		if (lastRet > index) lastRet++
	}

	fun notifyRemovedAt(index: Int) {
		if (cursor > index) cursor--
		if (lastRet > index) lastRet--
	}

	fun notifyCleared() {
		cursor = Int.MAX_VALUE
	}

	override fun hasNext(): Boolean {
		return cursor < list.size
	}

	override fun next(): E {
		val i = cursor
		if (i >= list.size)
			throw NoSuchElementException()
		cursor = i + 1
		lastRet = i
		return list[i]
	}

	override fun nextIndex(): Int {
		return cursor
	}

	override fun hasPrevious(): Boolean {
		return cursor > 0
	}

	override fun previous(): E {
		val i = cursor - 1
		if (i < 0)
			throw NoSuchElementException()
		cursor = i
		lastRet = i
		return list[i]
	}

	override fun previousIndex(): Int {
		return cursor - 1
	}

	override fun clear() {
		cursor = 0
		lastRet = -1
	}

	override fun iterator(): Iterator<E> {
		return this
	}

	override fun dispose() {
	}
}

interface MutableConcurrentListIterator<E> : ConcurrentListIterator<E>, MutableListIterator<E>, MutableIterable<E>

/**
 * A ConcurrentListIterator watches concurrent changes on the list and moves the cursor logically
 * so that items are neither skipped nor repeated.
 */
open class MutableConcurrentListIteratorImpl<E>(
		private val list: MutableList<E>
) : ConcurrentListIteratorImpl<E>(list), MutableConcurrentListIterator<E> {

	override fun iterator(): MutableIterator<E> {
		return this
	}

	override fun remove() {
		if (lastRet < 0)
			throw NoSuchElementException()

		list.removeAt(lastRet)
		cursor = lastRet
		lastRet = -1
	}

	override fun set(element: E) {
		if (lastRet < 0)
			throw IllegalStateException()
		list[lastRet] = element
	}

	override fun add(element: E) {
		val i = cursor
		list.add(i, element)
		cursor = i + 1
		lastRet = -1
	}
}
