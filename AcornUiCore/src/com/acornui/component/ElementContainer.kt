@file:Suppress("unused")

package com.acornui.component

import com.acornui._assert
import com.acornui.collection.ConcurrentListImpl
import com.acornui.core.di.Owned
import com.acornui.math.Bounds

interface ElementParentRo<out T> {

	val elements: List<T>

}

/**
 * An element parent is an interface that externally exposes the ability to add and remove elements.
 */
interface ElementParent<T> : ElementParentRo<T> {

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
	 * Adds an external element to this container at the given index.
	 * Unlike children, adding element to an [ElementContainer] where the element
	 * has already been added, the element will be removed.
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
		val index = elements.indexOf(element)
		if (index == -1) return false
		removeElement(index)
		return true
	}

	/**
	 * Removes the external element at the given index.
	 * @param index Must be between 0 and `elements.lastIndex`
	 * @return Returns the removed element.
	 */
	fun removeElement(index: Int): T

	fun clearElements(dispose: Boolean = true)

	/**
	 * Adds an element after the provided element.
	 */
	fun addElementAfter(element: T, after: T): Int {
		val index = elements.indexOf(after)
		if (index == -1) return -1
		addElement(index + 1, element)
		return index + 1
	}

	/**
	 * Adds an element before the provided element.
	 */
	fun addElementBefore(element: T, before: T): Int {
		val index = elements.indexOf(before)
		if (index == -1) return -1
		addElement(index, element)
		return index
	}
}

interface ElementContainerRo<out T : UiComponentRo> : ContainerRo, ElementParentRo<T>


/**
 * An ElementContainer is a component that can be provided a list of components as part of its external API.
 * It is up to this element container how to treat added elements. It may add them as children, it may provide the
 * element to a child element container.
 */
interface ElementContainer<T : UiComponent> : ElementContainerRo<T>, ElementParent<T>, Container

/**
 * @author nbilyk
 */
open class ElementContainerImpl<T : UiComponent>(
		owner: Owned
) : ContainerImpl(owner), ElementContainer<T>, Container {

	//-------------------------------------------------------------------------------------------------
	// Element methods.
	//-------------------------------------------------------------------------------------------------

	protected val _elements = ConcurrentListImpl<T>()
	final override val elements: List<T>
		get() = _elements

	override fun <S : T> addElement(index: Int, element: S): S {
		var newIndex = index
		val oldIndex = elements.indexOf(element)
		if (oldIndex != -1) {
			if (newIndex == oldIndex) return element // Element was added in the same spot it previously was.
			// Handle the case where after the element is removed, the new index needs to decrement to compensate.
			if (oldIndex < newIndex)
				newIndex--
			_elements.removeAt(oldIndex)
		} else {
			element.disposed.add(this::elementDisposedHandler)
		}
		_elements.add(newIndex, element)
		onElementAdded(oldIndex, newIndex, element)
		return element
	}

	private fun elementDisposedHandler(element: UiComponent) {
		@Suppress("UNCHECKED_CAST")
		removeElement(element as T)
	}

	/**
	 * Invoked when an external element has been added. If this is overriden and the [addChild] is delegated, the
	 * [onElementRemoved] should mirror the delegation.
	 *
	 * Example:
	 *
	 * private val otherContainer = addChild(container())
	 *
	 * override fun onElementAdded(oldIndex: Int, newIndex: Int, child: T) { otherContainer.addElement(newIndex, child) }
	 * override fun onElementRemoved(index: Int, child: T) { otherContainer.removeElement(index) }
	 */
	protected open fun onElementAdded(oldIndex: Int, newIndex: Int, element: T) {
		if (newIndex == elements.size - 1) {
			addChild(element)
		} else if (newIndex == 0) {
			val nextElement = _elements[newIndex + 1]
			addChildBefore(element, nextElement)
		} else {
			val previousElement = _elements[newIndex - 1]
			addChildAfter(element, previousElement)
		}
	}

	override fun removeElement(index: Int): T {
		val element = _elements.removeAt(index)
		element.disposed.remove(this::elementDisposedHandler)
		onElementRemoved(index, element)
		if (element.parent != null) throw Exception("Removing an element should remove from the display.")
		return element
	}

	protected open fun onElementRemoved(index: Int, element: T) {
		removeChild(element)
	}

	override fun clearElements(dispose: Boolean) {
		val c = elements
		while (c.isNotEmpty()) {
			val element = removeElement(c.lastIndex)
			if (dispose) element.dispose()
		}
	}

	/**
	 * A Container implementation will measure its children for dimensions that were not explicit.
	 */
	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		if (explicitWidth != null && explicitHeight != null) return // Use explicit dimensions.
		_elements.iterate { element ->
			if (element.shouldLayout) {
				if (explicitWidth == null) {
					if (element.right > out.width)
						out.width = element.right
				}
				if (explicitHeight == null) {
					if (element.bottom > out.height)
						out.height = element.bottom
				}
			}
			true
		}
	}

	/**
	 * Disposes this container and all its children.
	 */
	override fun dispose() {
		clearElements(dispose = false) // The elements this container owns will be disposed in the disposed signal.
		super.dispose()
	}
}


/**
 * Given a factory method that produces a new element [T], if this element container already
 * contains an element of that type, it will be reused. Otherwise, the previous contents will be disposed and
 * the factory will generate new contents.
 */
inline fun <reified T : UiComponent> ElementContainer<UiComponent>.createOrReuseContents(factory: Owned.() -> T): T {
	_assert(elements.size <= 1, "createOrReuseContents should not be used on element containers with more than one child.")
	val existing: T
	val contents = elements.getOrNull(0)
	if (contents !is T) {
		contents?.dispose()
		existing = factory()
		addElement(existing)
	} else {
		existing = contents
	}
	return existing
}