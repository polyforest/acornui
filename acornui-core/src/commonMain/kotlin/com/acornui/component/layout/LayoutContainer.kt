@file:Suppress("CascadeIf", "unused")

package com.acornui.component.layout

import com.acornui.collection.addAfter
import com.acornui.collection.addBefore
import com.acornui.collection.addOrReorder
import com.acornui.collection.filterTo2
import com.acornui.component.ContainerImpl
import com.acornui.component.UiComponent
import com.acornui.component.layout.algorithm.LayoutAlgorithm
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.style.Style
import com.acornui.di.Owned
import com.acornui.focus.Focusable
import com.acornui.math.Bounds

/**
 * A container that uses a [LayoutAlgorithm] to size and position its internal [elements].
 *
 * Similar to [ElementLayoutContainer] except that its elements are protected instead of public.
 */
abstract class LayoutContainer<S : Style, out U : LayoutData>(
		owner: Owned,
		private val layoutAlgorithm: LayoutAlgorithm<S, U>
) : ContainerImpl(owner), LayoutDataProvider<U>, Focusable {

	protected val elements: MutableList<UiComponent> = ArrayList()

	private val _elementsToLayout = ArrayList<LayoutElement>()
	protected open val elementsToLayout: List<LayoutElement>
		get() {
			_elementsToLayout.clear()
			elements.filterTo2(_elementsToLayout, LayoutElement::shouldLayout)
			return _elementsToLayout
		}

	/**
	 * Syntax sugar for addElement.
	 */
	protected operator fun <S : UiComponent> S.unaryPlus(): S {
		this@LayoutContainer.addElement(this@LayoutContainer.elements.size, this)
		return this
	}

	protected operator fun <S : UiComponent> S.unaryMinus(): S {
		this@LayoutContainer.removeElement(this)
		return this
	}

	protected fun <S : UiComponent> addElement(child: S): S = addElement(elements.size, child)
	protected fun <S : UiComponent> addOptionalElement(child: S?): S? {
		if (child == null) return null
		return addElement(child)
	}

	protected fun <S : UiComponent> addOptionalElement(index: Int, child: S?): S? {
		if (child == null) return null
		return addElement(index, child)
	}

	/**
	 * Adds an element to this container at the given index.
	 * If the element is already added to this container, it will be reordered.
	 *
	 * @param index The index at which to add the element. This must be between 0 and `elements.size`
	 * @throws IndexOutOfBoundsException
	 */
	protected fun <S : UiComponent> addElement(index: Int, element: S): S {
		elements.addOrReorder(index, element) { oldIndex, newIndex ->
			if (oldIndex == -1) element.disposed.add(::elementDisposedHandler)
			onElementAdded(oldIndex, newIndex, element)
		}
		return element
	}

	private fun elementDisposedHandler(element: UiComponent) {
		@Suppress("UNCHECKED_CAST")
		removeElement(element)
	}

	/**
	 * Removes the given element.
	 *
	 * @param element The element to remove.
	 * @return Returns true if the element existed in the elements list and was removed.
	 */
	protected fun removeElement(element: UiComponent?): Boolean {
		if (element == null) return false
		val index = elements.indexOf(element)
		if (index == -1) return false
		removeElement(index)
		return true
	}

	protected fun removeElement(index: Int): UiComponent {
		val element = elements.removeAt(index)
		element.disposed.remove(::elementDisposedHandler)
		onElementRemoved(index, element)
		if (element.parent != null) throw Exception("Removing an element should remove from the display.")
		return element
	}

	/**
	 * Adds an element after the provided element.
	 */
	protected fun addElementAfter(element: UiComponent, after: UiComponent): Int {
		val index = elements.indexOf(after)
		if (index == -1) return -1
		addElement(index + 1, element)
		return index + 1
	}

	/**
	 * Adds an element before the provided element.
	 */
	protected fun addElementBefore(element: UiComponent, before: UiComponent): Int {
		val index = elements.indexOf(before)
		if (index == -1) return -1
		addElement(index, element)
		return index
	}

	/**
	 * Invoked when an element has been added or reordered. If this is overriden and the [addChild] is
	 * delegated, the [onElementRemoved] should mirror the delegation.
	 *
	 * Example:
	 *```
	 * private val otherContainer = addChild(container())
	 *
	 * override fun onElementAdded(oldIndex: Int, newIndex: Int, child: T) {
	 *     otherContainer.addElement(newIndex, child)
	 * }
	 * override fun onElementRemoved(index: Int, child: T) {
	 *     otherContainer.removeElement(index)
	 * }
	 * ```
	 */
	protected open fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		if (newIndex == elements.size - 1) {
			addChild(element)
		} else if (newIndex == 0) {
			val nextElement = elements[newIndex + 1]
			_children.addBefore(element, nextElement)
		} else {
			val previousElement = elements[newIndex - 1]
			_children.addAfter(element, previousElement)
		}
	}

	protected open fun onElementRemoved(index: Int, element: UiComponent) {
		removeChild(element)
	}

	protected fun clearElements(dispose: Boolean) {
		val c = elements
		while (c.isNotEmpty()) {
			val element = removeElement(c.lastIndex)
			if (dispose) element.dispose()
		}
	}

	protected val layoutStyle: S = bind(layoutAlgorithm.style)
	final override fun createLayoutData(): U = layoutAlgorithm.createLayoutData()

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		layoutAlgorithm.layout(explicitWidth, explicitHeight, elementsToLayout, out)
		if (explicitWidth != null && explicitWidth > out.width) out.width = explicitWidth
		if (explicitHeight != null && explicitHeight > out.height) out.height = explicitHeight
	}

}
