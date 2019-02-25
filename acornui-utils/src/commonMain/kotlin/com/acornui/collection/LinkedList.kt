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

package com.acornui.collection

import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool

// TODO: TEMPORARY until kotlin finishes their linked list.

class Entry<T> : Clearable {

	var value: T? = null
	var previous: Entry<T>? = null
	var next: Entry<T>? = null

	override fun clear() {
		value = null
		previous = null
		next = null
	}
}

private class LinkedListIterator<T>(val list: LinkedList<T>) : Clearable, MutableIterator<T> {

	private var current: Entry<T>? = list.head

	override fun next(): T {
		val c = current!!.value
		current = current!!.next
		return c!!
	}

	override fun hasNext(): Boolean {
		return current != null
	}

	override fun clear() {
		current = list.head
	}

	override fun remove() {
		if (current != null) {
			list.remove(current!!)
		}
	}
}

/**
 * A minimal, but optimized linked list.
 * Linked lists theoretically have faster insert and delete than array lists, but in practice, this is not always true
 * due to lower level optimizations around arrays.
 *
 * Always measure before attempting to make an optimization!
 */
class LinkedList<T>: Iterable<T>, Clearable {

	private val pool = ClearableObjectPool { Entry<T>() }

	var head: Entry<T>? = null
	var tail: Entry<T>? = null
	private var size:Int = 0

	fun size():Int = size
	fun isEmpty():Boolean = size == 0
	fun isNotEmpty():Boolean = size > 0

	fun add(value: T): Entry<T> {
		val entry = pool.obtain()
		entry.value = value
		if (head == null) {
			head = entry
			tail = entry
		} else {
			tail!!.next = entry
			entry.previous = tail
			tail = entry
		}
		size++
		return entry
	}

	fun addAll(source: Array<out T>, offset: Int = 0, length: Int = source.size) {
		for (i in offset..offset+length-1) {
			add(source[i])
		}
	}

	fun addAll(vararg array: T) {
		for (i in array) {
			add(i)
		}
	}

	fun remove(entry: Entry<T>) {
		if (entry.previous != null) {
			entry.previous!!.next = entry.next
		}
		if (entry.next != null) {
			entry.next!!.previous = entry.previous
		}
		if (entry == head) {
			head = entry.next
		}
		if (entry == tail) {
			tail = entry.previous
		}
		pool.free(entry)
		size--
	}

	fun find(value: T) : Entry<T>? {
		var next = head
		while (next != null) {
			if (next.value == value) return next
			next = next.next
		}
		return null
	}

	fun poll(): T? {
		if (head == null) return null
		val value = head!!.value
		remove(head!!)
		return value
	}

	fun peek(): T? {
		if (tail == null) return null
		val value = tail!!.value
		remove(tail!!)
		return value
	}

	fun offer(e: T): Boolean {
		add(e)
		return true
	}

	override fun clear() {
		head = null
		tail = null
		size = 0
	}

	override fun toString(): String {
		if (size == 0) return "[]"
		var buffer = ""
		buffer += "["
		var current = head
		while (current != null) {
			if (current != head) buffer += ", "
			buffer += current.value
			current = current.next
		}
		buffer += ']'
		return buffer
	}

	override fun iterator(): MutableIterator<T> = LinkedListIterator(this)

}