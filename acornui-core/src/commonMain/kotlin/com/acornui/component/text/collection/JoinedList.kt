/*
 * Copyright 2019 PolyForest
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

package com.acornui.component.text.collection

import com.acornui.collection.ListBase
import com.acornui.collection.sortedInsertionIndex

/**
 * Virtually joins sub-lists, after applying a transformation function on them.
 */
internal class JoinedList<T, E>(private val list: List<T>, private val transform: (T) -> List<E>) : ListBase<E>() {

	private var isDirty = true

	private val counts = ArrayList<Int>()

	override val size: Int
		get() {
			if (isDirty) validate()
			return counts.lastOrNull() ?: 0
		}

	override fun get(index: Int): E {
		if (isDirty) validate()
		if (index < 0 || index >= size) throw IndexOutOfBoundsException()
		val listIndex = counts.sortedInsertionIndex(index, matchForwards = true) - 1
		val relativeIndex = index - counts[listIndex]
		return transform(list[listIndex])[relativeIndex]
	}

	/**
	 * Flags that the text elements need to be re-counted.
	 */
	fun dirty() {
		isDirty = true
	}

	private fun validate() {
		isDirty = false
		var size = 0
		counts.clear()
		counts.add(0)
		for (i in 0..list.lastIndex) {
			size += transform(list[i]).size
			counts.add(size)
		}
	}
}

internal fun <E> joinedList(list: List<List<E>>) = JoinedList(list) { it }