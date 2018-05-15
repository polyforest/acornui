/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.core.cache

import com.acornui.collection.arrayListObtain
import com.acornui.collection.arrayListPool
import com.acornui.component.ItemRendererRo

/**
 * Recycles a list of item renderers, creating or disposing renderers only as needed.
 * @param data The updated list of data items.
 * @param existingElements The stale list of item renderers. This will be modified to reflect the new item renderers.
 * @param factory Used to create new item renderers as needed. [configure] will be called after factory to configure
 * the new element.
 * @param configure Used to configure the element.
 * @param disposer Used to dispose the element.
 * @param equality If set, uses custom equality rules. This guides how to know whether an item can be recycled or not.
 */
fun <E, T : ItemRendererRo<E>> recycle(
		data: List<E>?,
		existingElements: MutableList<T>,
		factory: (item: E, index: Int) -> T,
		configure: (element: T, item: E, index: Int) -> Unit,
		disposer: (element: T) -> Unit,
		equality: (a: E?, b: E?) -> Boolean = { a, b -> a == b }
) = recycle(data, existingElements, factory, configure, disposer, { it.data }, equality)


/**
 * Recycles a list of item renderers, creating or disposing renderers only as needed.
 * @param data The updated list of data items.
 * @param existingElements The stale list of item renderers. This will be modified to reflect the new item renderers.
 * @param factory Used to create new item renderers as needed. [configure] will be called after factory to configure
 * the new element.
 * @param configure Used to configure the element.
 * @param disposer Used to dispose the element.
 * @param retriever Returns the current data value for the given element.
 * @param equality If set, uses custom equality rules. This guides how to know whether an item can be recycled or not.
 */
fun <E, T> recycle(
		data: List<E>?,
		existingElements: MutableList<T>,
		factory: (item: E, index: Int) -> T,
		configure: (element: T, item: E, index: Int) -> Unit,
		disposer: (element: T) -> Unit,
		retriever: (element: T) -> E?,
		equality: (a: E?, b: E?) -> Boolean = { a, b -> a == b }
) {
	val unused = arrayListObtain<T>()
	val neededCount = (data?.size ?: 0) - existingElements.size
	for (i in 0..existingElements.lastIndex) {
		val existingElement = existingElements[i]
		val item = retriever(existingElement)
		if (unused.size >= neededCount && (data == null || data.find { equality(it, item) } == null)) {
			disposer(existingElement)
		} else {
			unused.add(existingElement)
		}
	}
	existingElements.clear()
	if (data != null) {
		for (i in 0..data.lastIndex) {
			val item = data[i]
			val foundIndex = unused.indexOfFirst { equality(retriever(it), item) }
			val element = if (foundIndex == -1) {
				factory(item, i)
			} else {
				unused.removeAt(foundIndex)
			}
			configure(element, item, i)
			existingElements.add(element)
		}
	}
	arrayListPool.free(unused)
}

/**
 * Recycles [data] list into [other] list. It is similar to [List.mapTo] except reuses elements instead of generating
 * every time.
 */
fun <E1, E2> recycle(data: List<E1>?, other: MutableList<E2>, factory: (E1) -> E2, compare: (E2, E1) -> Boolean) {
	recycle(data, other, { e, index -> factory(e) }, { e1, e2, index -> }, {}, { element -> data?.find { compare(element, it) } } )
}