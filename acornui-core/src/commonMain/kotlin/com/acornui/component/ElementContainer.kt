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

@file:Suppress("unused", "CascadeIf")

package com.acornui.component

import com.acornui.collection.*
import com.acornui.component.layout.LayoutElement
import com.acornui.di.Owned
import com.acornui.math.Bounds

interface ElementParentRo<out T> {

	/**
	 * A list of externally added objects.
	 */
	val elements: List<T>

}

/**
 * An element parent is an interface that externally exposes the ability to add and remove elements.
 * How those elements are added to the display graph is implementation specific.
 */
interface ElementParent<T> : ElementParentRo<T> {

	/**
	 * A list of externally added objects.
	 *
	 * The behavior of this mutable list differs in one respect: implementations are expected to handle reordering.
	 * That is, when an element already exists in this list, and is added again, the element's index will change.
	 */
	override val elements: MutableList<T>

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

	fun <S : T> addElement(child: S): S = addElement(elements.size, child)
	fun <S : T> addOptionalElement(child: S?): S? {
		if (child == null) return null
		return addElement(child)
	}

	fun <S : T> addOptionalElement(index: Int, child: S?): S? {
		if (child == null) return null
		return addElement(index, child)
	}

	/**
	 * Adds or reorders an external element to this container at the given index.
	 *
	 * Unlike children, an element can belong to multiple element containers.
	 *
	 * @param index The index at which to add the element. This must be between 0 and `elements.size`
	 * @throws IndexOutOfBoundsException
	 */
	fun <S : T> addElement(index: Int, element: S): S

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

interface ElementContainerRo<out T : UiComponentRo> : ContainerRo, ElementParentRo<T>


/**
 * An ElementContainer is a component that can be provided a list of components as part of its external API.
 * It is up to this element container how to treat added elements. It may add them as children to the display graph,
 * or it may provide the element to a child element container.
 */
interface ElementContainer<E : UiComponent> : ElementContainerRo<E>, ElementParent<E>, Container

/**
 * The base class for a component that contains children and external elements.
 *
 * @author nbilyk
 */
open class ElementContainerImpl<E : UiComponent>(
		owner: Owned
) : ContainerImpl(owner), ElementContainer<E>, Container {

	//-------------------------------------------------------------------------------------------------
	// Element methods.
	//-------------------------------------------------------------------------------------------------

	private val _elements = ElementsList()

	final override val elements: MutableList<E> = _elements

	private val _elementsToLayout = ArrayList<LayoutElement>()
	protected val elementsToLayout: List<LayoutElement>
		get() {
			_elementsToLayout.clear()
			elements.filterTo2(_elementsToLayout, LayoutElement::shouldLayout)
			return _elementsToLayout
		}

	override fun <S : E> addElement(index: Int, element: S): S {
		_elements.add(index, element)
		return element
	}

	private fun elementDisposedHandler(element: UiComponent) {
		@Suppress("UNCHECKED_CAST")
		removeElement(element as E)
	}

	/**
	 * Invoked when an external element has been added or reordered. If this is overridden and the [addChild] is
	 * delegated, the [onElementRemoved] should mirror the delegation.
	 *
	 * Example:
	 *```
	 * private val otherContainer = addChild(container())
	 *
	 * override fun onElementAdded(oldIndex: Int, newIndex: Int, element: T) {
	 *     otherContainer.addElement(newIndex, child)
	 * }
	 * override fun onElementRemoved(index: Int, element: T) {
	 *     otherContainer.removeElement(index)
	 * }
	 * ```
	 */
	protected open fun onElementAdded(oldIndex: Int, newIndex: Int, element: E) {
		if (newIndex == elements.size - 1) {
			addChild(element)
		} else if (newIndex == 0) {
			val nextElement = _elements[newIndex + 1]
			_children.addBefore(element, nextElement)
		} else {
			val previousElement = _elements[newIndex - 1]
			_children.addAfter(element, previousElement)
		}
	}

	override fun removeElement(index: Int): E {
		return _elements.removeAt(index)
	}

	protected open fun onElementRemoved(index: Int, element: E) {
		removeChild(element)
	}

	override fun clearElements(dispose: Boolean) {
		val c = elements
		while (c.isNotEmpty()) {
			val element = removeElement(c.lastIndex)
			if (dispose) element.dispose()
		}
	}

	//-------------------------------------------------------------------------------------------------

	/**
	 * A Container implementation will by default measure its children for dimensions that were not explicit.
	 */
	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (explicitWidth != null && explicitHeight != null) return // Use explicit dimensions.
		elementsToLayout.forEach2 { element ->
			if (explicitWidth == null) {
				if (element.right > out.width)
					out.width = element.right
			}
			if (explicitHeight == null) {
				if (element.bottom > out.height)
					out.height = element.bottom
			}
			if (element.baseline > out.baseline)
				out.baseline = element.baseline
		}
	}

	/**
	 * Disposes this container and all its children.
	 */
	override fun dispose() {
		clearElements(dispose = false) // The elements this container owns will be disposed in the disposed signal.
		super.dispose()
	}

	private inner class ElementsList : MutableListBase<E>() {

		private val list = ArrayList<E>()

		override fun add(index: Int, element: E) {
			val oldIndex = list.indexOf(element)
			if (oldIndex == -1) {
				element.disposed.add(::elementDisposedHandler)
				list.add(index, element)
			} else {
				val newIndex = if (oldIndex < index) index - 1 else index
				if (index == oldIndex || index == oldIndex + 1) return
				list.removeAt(oldIndex)
				list.add(newIndex, element)
			}
			onElementAdded(oldIndex, index, element)
		}

		override fun removeAt(index: Int): E {
			val element = list.removeAt(index)
			element.disposed.remove(::elementDisposedHandler)
			onElementRemoved(index, element)
			require(element.parent == null) { "Removing an element should remove from the display." }
			return element
		}

		override val size: Int
			get() = list.size

		override fun get(index: Int): E = list[index]

		override fun set(index: Int, element: E): E {
			val oldElement = list.set(index, element)
			if (oldElement == element) return element
			oldElement.disposed.remove(::elementDisposedHandler)
			onElementRemoved(index, element)
			require(element.parent == null) { "Removing an element should remove from the display." }
			element.disposed.add(::elementDisposedHandler)
			onElementAdded(index, index, element)
			return oldElement
		}
	}
}