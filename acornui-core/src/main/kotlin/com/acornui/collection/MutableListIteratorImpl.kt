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
