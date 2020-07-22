/*
 * Copyright 2020 Poly Forest, LLC
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

@file:Suppress("unused", "CascadeIf")

package com.acornui.component

import com.acornui.Disposable
import com.acornui.Owner

/**
 * An element parent is an interface that externally exposes the ability to add and remove elements.
 *
 * This separates a component's api from its display graph.
 * For example a window panel may add externally added elements to the display graph of the panel's body area.
 */
interface ElementParent<T> {

	/**
	 * A list of externally added objects.
	 *
	 * The behavior of this mutable list differs in one respect: implementations are expected to handle reordering.
	 * That is, when an element already exists in this list, and is added again, the element's index will change.
	 */
	val elements: MutableList<T>

	/**
	 * Syntax sugar for addElement.
	 */
	operator fun <P : T> P.unaryPlus(): P {
		this@ElementParent.addElement(this@ElementParent.elements.size, this)
		return this
	}

	operator fun <P : T> P.unaryMinus(): P {
		this@ElementParent.removeElement(this)
		return this
	}

	fun <S : T?> addElement(child: S): S = addElement(elements.size, child)

	/**
	 * Adds or reorders an external element to this container at the given index.
	 *
	 * Unlike children, an element can belong to multiple element containers.
	 *
	 * @param index The index at which to add the element. This must be between 0 and `elements.size`
	 * @throws IndexOutOfBoundsException
	 */
	fun <S : T?> addElement(index: Int, element: S): S

	/**
	 * Removes the given element.
	 *
	 * @param element The element to remove.
	 * @return Returns true if the element existed in the elements list and was removed.
	 */
	fun removeElement(element: T?): Boolean {
		if (element == null) return false
		return elements.remove(element)
	}

	/**
	 * Removes the external element at the given index.
	 * @param index Must be between 0 and `elements.lastIndex`
	 * @return Returns the removed element.
	 */
	fun removeElement(index: Int): T

	fun clearElements(dispose: Boolean = true)

}

/**
 * A list of elements that will be automatically removed from this list if they're disposed.
 */
class ElementsList<E : Owner>(

	/**
	 * Invoked when an element has been added or reordered.
	 *
	 * oldIndex - The previous index of the element, or -1 if the element was not previously in the elements list.
	 * newIndex - The new index of the element.
	 */
	val onElementAdded: (oldIndex: Int, newIndex: Int, element: E) -> Unit,

	/**
	 * Invoked when an element has been removed.
	 * This will not be invoked if an element has been reordered.
	 *
	 * index - The index of the element removed.
	 * element - The element removed.
	 */
	val onElementRemoved: (index: Int, element: E) -> Unit

) : AbstractMutableList<E>() {

	private val list = ArrayList<E>()
	private val elementWatchers = mutableMapOf<E, Disposable>()

	private val elementDisposedHandler = { element: Any ->
		@Suppress("UNCHECKED_CAST")
		remove(element as E)
		Unit
	}

	override fun add(index: Int, element: E) {
		val oldIndex = list.indexOf(element)
		if (oldIndex == -1) {
			list.add(index, element)
			elementWatchers[element] = element.disposed.listen(elementDisposedHandler)
			onElementAdded(-1, index, element)
		} else {
			val newIndex = if (oldIndex < index) index - 1 else index
			if (index == oldIndex || index == oldIndex + 1) return
			list.removeAt(oldIndex)
			list.add(newIndex, element)
			onElementAdded(oldIndex, newIndex, element)
		}
	}

	override fun removeAt(index: Int): E {
		val element = list.removeAt(index)
		elementWatchers.remove(element)?.dispose()
		onElementRemoved(index, element)
		return element
	}

	override val size: Int
		get() = list.size

	override fun get(index: Int): E = list[index]

	override fun set(index: Int, element: E): E {
		val oldElement = list.set(index, element)
		if (oldElement == element) return element
		elementWatchers.remove(oldElement)?.dispose()
		onElementRemoved(index, element)
		elementWatchers[element] = element.disposed.listen(elementDisposedHandler)
		onElementAdded(index, index, element)
		return oldElement
	}
}