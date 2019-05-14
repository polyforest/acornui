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

class SingleList<E> : MutableListBase<E>() {

	private var element: E? = null

	override var size: Int = 0
		private set

	@Suppress("UNCHECKED_CAST")
	override fun get(index: Int): E = if (size == 0) throw IndexOutOfBoundsException() else element as E

	override fun add(index: Int, element: E) {
		if (index != 0) throw IndexOutOfBoundsException("SingleList may only contain a single element.")
		size = 1
		this.element = element
	}

	override fun removeAt(index: Int): E {
		if (index < 0 || index >= size) throw IndexOutOfBoundsException()
		size = 0
		val old = element
		element = null
		@Suppress("UNCHECKED_CAST")
		return old as E
	}

	override fun set(index: Int, element: E): E {
		if (index != 0) throw IndexOutOfBoundsException()
		this.element = element
		size = 1
		return element
	}

	fun get(): E = element!!
	fun set(element: E): E {
		this.element = element
		size = 1
		return element
	}
}

/**
 * A filtered view of a [SingleList].
 */
class SingleListView<E>(
		private val source: SingleList<E>,

		/**
		 * The filter to apply to the single element of a SingleList.
		 */
		var filter: Filter<E>? = null
) : ListBase<E>() {

	override val size: Int
		get() {
			val filter = filter
			return if (source.size == 0 || filter == null) source.size
			else if (filter(source.get())) 1
			else 0
		}

	override fun get(index: Int): E {
		if (size == 0) throw IndexOutOfBoundsException()
		return source.get()
	}
}
