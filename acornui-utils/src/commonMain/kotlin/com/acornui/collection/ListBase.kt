/*
 * Copyright 2019 Nicholas Bilyk
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

import com.acornui.recycle.Clearable

/**
 * Provides a partial implementation of the List interface.
 */
abstract class ListBase<out E> : List<E> {

	val lastIndex: Int
		get() = size - 1

	override fun indexOf(element: @UnsafeVariance E): Int {
		for (i in 0..lastIndex) {
			if (this[i] == element) return i
		}
		return -1
	}

	override fun lastIndexOf(element: @UnsafeVariance E): Int {
		for (i in lastIndex downTo 0) {
			if (this[i] == element) return i
		}
		return -1
	}

	override fun contains(element: @UnsafeVariance E): Boolean {
		for (i in 0..lastIndex) {
			if (this[i] == element) return true
		}
		return false
	}

	override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean {
		for (element in elements) {
			if (!contains(element)) return false
		}
		return true
	}

	override fun isEmpty(): Boolean {
		return size == 0
	}

	override fun iterator(): Iterator<E> {
		return ListIteratorImpl(this)
	}

	override fun listIterator(): ListIterator<E> {
		return ListIteratorImpl(this)
	}

	override fun listIterator(index: Int): ListIterator<E> {
		val t = ListIteratorImpl(this)
		t.cursor = index
		return t
	}

	override fun subList(fromIndex: Int, toIndex: Int): List<E> {
		return SubList(this, fromIndex, toIndex)
	}
}

abstract class MutableListBase<E> : ListBase<E>(), Clearable, MutableList<E> {

	override fun add(element: E): Boolean {
		add(size, element)
		return true
	}

	override fun addAll(index: Int, elements: Collection<E>): Boolean {
		var i = index
		for (element in elements) {
			add(i++, element)
		}
		return true
	}

	override fun addAll(elements: Collection<E>): Boolean {
		for (element in elements) {
			add(element)
		}
		return true
	}

	override fun clear() {
		for (i in lastIndex downTo 0) {
			removeAt(i)
		}
	}

	override fun remove(element: E): Boolean {
		val index = indexOf(element)
		if (index == -1) return false
		removeAt(index)
		return true
	}

	override fun removeAll(elements: Collection<E>): Boolean {
		var changed = false
		for (i in elements) {
			changed = changed && remove(i)
		}
		return changed
	}

	override fun retainAll(elements: Collection<E>): Boolean {
		var changed = false
		for (i in 0..lastIndex) {
			val element = this[i]
			if (!elements.contains(element)) {
				changed = true
				remove(element)
			}
		}
		return changed
	}

	override fun iterator(): MutableIterator<E> {
		return MutableListIteratorImpl(this)
	}

	override fun listIterator(): MutableListIterator<E> {
		return MutableListIteratorImpl(this)
	}

	override fun listIterator(index: Int): MutableListIterator<E> {
		val iterator = MutableListIteratorImpl(this)
		iterator.cursor = index
		return iterator
	}

	override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
		return MutableSubList(this, fromIndex, toIndex)
	}
}

open class MutableListIteratorImpl<E>(private val mutableList: MutableList<E>) : ListIteratorImpl<E>(mutableList), MutableListIterator<E> {

	override fun add(element: E) {
		mutableList.add(cursor, element)
		cursor++
		lastRet++
	}

	override fun remove() {
		if (lastRet < 0)
			throw Exception("Cannot remove before iteration.")
		mutableList.removeAt(lastRet)
	}

	override fun set(element: E) {
		if (lastRet < 0)
			throw Exception("Cannot set before iteration.")
		mutableList[lastRet] = element
	}
}

class SubList<E>(
		private val target: List<E>,
		private val fromIndex: Int,
		private val toIndex: Int
) : ListBase<E>() {

	override val size: Int
		get() {
			return toIndex - fromIndex
		}

	override fun get(index: Int): E = target[index - fromIndex]
}

class MutableSubList<E>(
		private val target: MutableList<E>,
		private val fromIndex: Int,
		private val toIndex: Int
) : MutableListBase<E>() {

	override val size: Int
		get() {
			return toIndex - fromIndex
		}

	override fun get(index: Int): E = target[index - fromIndex]

	override fun add(index: Int, element: E) {
		target.add(index - fromIndex, element)
	}

	override fun removeAt(index: Int): E {
		return target.removeAt(index - fromIndex)
	}

	override fun set(index: Int, element: E): E {
		return target.set(index - fromIndex, element)
	}
}