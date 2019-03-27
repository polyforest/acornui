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

@file:Suppress("UNUSED_PARAMETER")

package com.acornui.component.text

import com.acornui.collection.ConcurrentListImpl
import com.acornui.collection.addOrReorder
import com.acornui.collection.forEach2
import com.acornui.component.ContainerImpl
import com.acornui.component.ElementParent
import com.acornui.component.ValidationFlags
import com.acornui.component.layout.algorithm.LineInfo
import com.acornui.component.layout.algorithm.LineInfoRo
import com.acornui.component.text.collection.JoinedList
import com.acornui.core.ParentRo
import com.acornui.core.di.Owned
import com.acornui.core.selection.SelectionRange
import com.acornui.math.MinMaxRo

interface TextNodeContainerRo : TextNodeRo, ParentRo<TextNodeRo>
interface TextNodeContainer : TextNode, TextNodeContainerRo

abstract class TextElementContainerImpl<T : TextNode>(owner: Owned) : TextNodeBase(owner), ElementParent<T>, TextNodeContainer {

	private val _elements = ConcurrentListImpl<T>()
	override val children: List<TextNodeRo> = _elements

	override val elements: List<T>
		get() = _elements

	private val _lines = ArrayList<LineInfo>()
	private val _textElements = JoinedList(_elements) { it.textElements }

	/**
	 * A list of all the text elements within the text nodes.
	 */
	override val textElements: List<TextElementRo>
		get() {
			validate(TEXT_ELEMENTS)
			return _textElements
		}

	protected var bubblingFlags = ValidationFlags.HIERARCHY_ASCENDING or
			ValidationFlags.LAYOUT

	protected var cascadingFlags = ContainerImpl.defaultCascadingFlags

	override var allowClipping: Boolean = true
		set(value) {
			if (field == value) return
			field = value
			for (i in 0.._elements.lastIndex) {
				_elements[i].allowClipping = value
			}
		}

	init {
		validation.addNode(TEXT_ELEMENTS, dependencies = ValidationFlags.HIERARCHY_ASCENDING, dependents = ValidationFlags.LAYOUT, onValidate = _textElements::dirty)
		validation.addNode(LINES, dependencies = ValidationFlags.LAYOUT, onValidate = ::updateLines)
	}

	override var textField: TextField? = null
		set(value) {
			if (field == value) return
			field = value
			for (i in 0.._elements.lastIndex) {
				_elements[i].textField = value
			}
		}

	override val placeholder: TextElementRo?
		get() = _elements.lastOrNull()?.placeholder

	override fun setSelection(rangeStart: Int, selection: List<SelectionRange>) {
		var r = rangeStart
		for (i in 0.._elements.lastIndex) {
			val element = _elements[i]
			element.setSelection(r, selection)
			r += element.textElements.size
		}
	}

	override val lines: List<LineInfoRo>
		get() {
			validate(LINES)
			return _lines
		}

	override fun <S : T> addElement(index: Int, element: S): S {
		if (element.parent != null) throw Exception("Remove element first.")
		_elements.addOrReorder(index, element) { oldIndex, newIndex ->
			element.parent = this
			element.textField = textField
			element.allowClipping = allowClipping
			if (oldIndex == -1) {
				element.disposed.add(::elementDisposedHandler)
				element.invalidated.add(::elementInvalidatedHandler)
			}
			invalidate(bubblingFlags)
			element.invalidate(cascadingFlags)
			onElementAdded(oldIndex, newIndex, element)
		}
		return element
	}

	private fun elementDisposedHandler(textNode: TextNode) {
		@Suppress("UNCHECKED_CAST")
		removeElement(textNode as T)
	}

	private fun elementInvalidatedHandler(textNode: TextNode, flagsInvalidated: Int) {
		val bubblingFlags = flagsInvalidated and bubblingFlags
		if (bubblingFlags > 0) {
			invalidate(bubblingFlags)
		}
	}

	override fun removeElement(index: Int): T {
		val element = _elements.removeAt(index)
		element.parent = null
		element.textField = null
		element.disposed.remove(::elementDisposedHandler)
		element.invalidated.remove(::elementInvalidatedHandler)
		invalidate(bubblingFlags)
		element.invalidate(cascadingFlags)
		onElementRemoved(index, element)
		return element
	}

	override fun clearElements(dispose: Boolean) {
		while (_elements.isNotEmpty()) {
			val element = removeElement(_elements.lastIndex)
			if (dispose) element.dispose()
		}
	}

	override fun onInvalidated(flagsInvalidated: Int) {
		val flagsToCascade = flagsInvalidated and cascadingFlags
		if (flagsToCascade > 0) {
			// This component has flags that have been invalidated that must cascade down to the children.
			for (i in 0.._elements.lastIndex) {
				_elements[i].invalidate(flagsToCascade)
			}
		}
	}

	/**
	 * Invoked when an external element has been added or reordered.
	 */
	protected open fun onElementAdded(oldIndex: Int, newIndex: Int, element: T) {}

	/**
	 * Invoked when an external element has been removed.
	 */
	protected open fun onElementRemoved(index: Int, element: T) {}

	protected open fun updateLines() {
		// Create line info objects with attributes relative to this container.
		_lines.forEach2(LineInfo.Companion::free)
		_lines.clear()
		var relativeIndex = 0
		for (i in 0.._elements.lastIndex) {
			val element = _elements[i]
			val elementLines = element.lines
			for (j in 0..elementLines.lastIndex) {
				val line = LineInfo.obtain()
				line.set(elementLines[j])
				line.x += element.x
				line.y += element.y
				line.startIndex += relativeIndex
				line.endIndex += relativeIndex
				_lines.add(line)
			}
			relativeIndex += element.textElements.size
		}
	}

	private val childrenIterator = _elements.iteratorPool.obtain()

	override fun update() {
		super.update()
		val size = _elements.size
		if (size == 0) return
		else if (size == 1) {
			_elements[0].update()
		} else {
			val c = childrenIterator
			while (c.hasNext()) {
				c.next().update()
			}
			c.clear()
		}
	}

	override fun render(clip: MinMaxRo) {
		for (i in 0.._elements.lastIndex) {
			_elements[i].render(clip)
		}
	}

	override fun toString(builder: StringBuilder) {
		for (i in 0.._elements.lastIndex) {
			_elements[i].toString(builder)
		}
	}


	companion object {
		private const val TEXT_ELEMENTS = 1 shl 16
		private const val LINES = 1 shl 17
	}

}
