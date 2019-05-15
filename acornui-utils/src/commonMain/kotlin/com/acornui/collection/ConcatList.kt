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

/**
 * A List that is backed by two lists, virtually concatenated.
 * @param listA The first list.
 * @param listB The second list, this will begin where [listA] ends.
 */
class ConcatList<out E>(private val listA: List<E>, private val listB: List<E>) : ListBase<E>() {

	override val size: Int
		get() = listA.size + listB.size

	override fun get(index: Int): E {
		return if (index >= listA.size) listB[index - listA.size] else listA[index]
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
}

infix fun <T> List<T>.concat(other: List<T>): List<T> = ConcatList(this, other)
