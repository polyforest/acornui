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

import com.acornui.collection.filterTo2
import com.acornui.component.ElementContainer
import com.acornui.component.UiComponent

/**
 * Similar to [IndexedCache], to use this class, make a series of [markUsed] calls to indicate that an element was ued,
 * then call [forEachUnused] to iterate over the elements that were marked used on the last set, but weren't marked on
 * this set, then call [flip] to set the used elements as unused for the next set.
 */
class UsedTracker<E> {

	private var elements = ArrayList<E>()
	private val used = HashMap<E, Boolean>()
	private var tmp = ArrayList<E>()

	/**
	 * Marks an element as used. [element] will not be included in a [forEachUnused] call until after the next [flip].
	 */
	fun markUsed(element: E) {
		val isUsed = used[element]
		if (isUsed == null) {
			elements.add(element)
		}
		used[element] = true
	}

	/**
	 * Removes the given element from the unused list.
	 */
	fun forget(element: E) {
		used.remove(element)
		elements.remove(element)
	}

	/**
	 * Iterates over the unused items.
	 */
	fun forEachUnused(callback: (element: E) -> Unit) {
		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			if (used[element] != true) {
				callback(element)
			}
		}
	}

	/**
	 * Clears the unused list and moves the current used list into the unused list.
	 */
	fun flip() {
		val swap = elements
		elements.filterTo2(tmp) { used[it] == true }
		elements.clear()
		elements = tmp
		tmp = swap
		used.clear()
		for (i in 0..elements.lastIndex) {
			used[elements[i]] = false
		}
	}

}

/**
 * Removes all unused cache instances from the given container before flipping.
 */
fun <E : UiComponent> UsedTracker<E>.removeAndFlip(parent: ElementContainer<UiComponent>) {
	forEachUnused {
		parent.removeElement(it)
	}
	flip()
}

/**
 * Sets visible to false on all unused cache instances before flipping.
 */
fun <E : UiComponent> UsedTracker<E>.hideAndFlip() {
	forEachUnused {
		it.visible = false
	}
	flip()
}
