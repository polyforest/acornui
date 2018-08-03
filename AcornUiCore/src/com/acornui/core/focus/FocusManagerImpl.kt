/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.core.focus

import com.acornui._assert
import com.acornui.collection.poll
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.ContainerRo
import com.acornui.component.Stage
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.core.input.Ascii
import com.acornui.core.input.interaction.KeyInteractionRo
import com.acornui.core.input.interaction.MouseInteractionRo
import com.acornui.core.input.keyDown
import com.acornui.core.input.mouseDown
import com.acornui.core.isBefore
import com.acornui.core.time.callLater
import com.acornui.function.as2
import com.acornui.signal.Cancel
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3

/**
 * @author nbilyk
 */
class FocusManagerImpl : FocusManager {

	private var _root: Stage? = null
	private val root: Stage
		get() = _root!!

	private val focusedChangingCancel: Cancel = Cancel()
	override val focusedChanging: Signal3<UiComponentRo?, UiComponentRo?, Cancel> = Signal3()
	override val focusedChanged: Signal2<UiComponentRo?, UiComponentRo?> = Signal2()

	private var _focused: UiComponentRo? = null
	private var _highlighted: UiComponentRo? = null

	private val invalidFocusables = ArrayList<UiComponentRo>()
	private val _focusables = ArrayList<UiComponentRo>()
	private val focusables: ArrayList<UiComponentRo>
		get() {
			if (invalidFocusables.isNotEmpty()) validateFocusables()
			return _focusables
		}

	private var isDisposed: Boolean = false

	private val rootKeyDownHandler = {
		event: KeyInteractionRo ->
		if (!event.defaultPrevented() && event.keyCode == Ascii.TAB) {
			val previousFocused = focused()
			if (event.shiftKey) focusPrevious()
			else focusNext()
			highlightFocused()
			if (previousFocused != focused())
				event.preventDefault() // Prevent the browser's default tab interactivity if we've handled it.
		} else if (!event.defaultPrevented() && event.keyCode == Ascii.ESCAPE) {
			clearFocused()
		}
	}

	private val rootMouseDownHandler = {
		event: MouseInteractionRo ->
		if (!event.defaultPrevented()) {
			var p: UiComponentRo? = event.target
			while (p != null) {
				if (p.focusEnabled) {
					focused(p)
					break
				}
				p = p.parent
			}
		}
		Unit
	}

	private var _highlight: UiComponent? = null
	override var highlight: UiComponent?
		get() = _highlight
		set(value) {
			if (value == _highlight) return
			val wasHighlighted = _highlighted != null
			unhighlightFocused()
			_highlight = value
			if (value != null) {
				value.includeInLayout = false
			}
			if (wasHighlighted) highlightFocused()
		}

	override fun init(root: Stage) {
		_assert(_root == null, "Already initialized.")
		_root = root
		_focused = root
		root.keyDown().add(rootKeyDownHandler)
		root.mouseDown(isCapture = true).add(rootMouseDownHandler)
	}

	override fun invalidateFocusableOrder(value: UiComponentRo) {
		if (!invalidFocusables.contains(value)) {
			_focusables.remove(value)
			if (value.isActive && value.focusEnabled && !value.isFocusContainer)
				invalidFocusables.add(value)
			else {
				if (_focused === value) {
					focused(null)
				}
			}
		}
	}

	private val focusOrder = ArrayList<Float>()
	private val iFocusOrder = ArrayList<Float>()

	private fun validateFocusables() {
		while (invalidFocusables.isNotEmpty()) {
			// Use poll instead of pop because the invalid focusables are more likely to be closer to already in order than not.
			val focusable = invalidFocusables.poll()
			if (!focusable.isActive || !focusable.focusEnabled) continue
			if (_focusables.isEmpty()) {
				// Trivial case
				_focusables.add(focusable)
			} else {
				calculateFocusOrder(focusable, focusOrder)
				val focusOrderComparator = {
					it: UiComponentRo ->
					calculateFocusOrder(it, iFocusOrder)
					focusOrder.compareTo(iFocusOrder)
				}
				val indexA = _focusables.sortedInsertionIndex(matchForwards = false, comparator = focusOrderComparator)
				val indexB = _focusables.sortedInsertionIndex(fromIndex = indexA, comparator = focusOrderComparator)
				if (indexA >= indexB) {
					_focusables.add(indexA, focusable)
				} else {
					val childOrderComparator = {
						it: UiComponentRo ->
						if (focusable.isBefore(it)) -1 else 1
					}
					val index = _focusables.sortedInsertionIndex(indexA, indexB, comparator = childOrderComparator)
					_focusables.add(index, focusable)
				}
			}
		}
	}

	private fun calculateFocusOrder(value: UiComponentRo, focusOrderOut: MutableList<Float>) {
		focusOrderOut.clear()
		if (!value.focusEnabled || !value.isActive) return
		focusOrderOut.add(value.focusOrder)
		var p: ContainerRo? = value.parent
		while (p != null) {
			if (p.isFocusContainer)
				focusOrderOut.add(0, p.focusOrder)
			p = p.parent
		}
		focusOrderOut.add(value.focusOrder)
	}

	private fun <T : Comparable<T>> List<T>.compareTo(other: List<T>): Int {
		for (i in 0..minOf(lastIndex, other.lastIndex)) {
			val r = this[i].compareTo(other[i])
			if (r != 0)
				return r
		}
		return 0
	}

	override fun focused(): UiComponentRo? {
		return _focused
	}

	private var pendingFocusable: UiComponentRo? = null

	override fun focused(value: UiComponentRo?) {
		if (focusedChanging.isDispatching || focusedChanged.isDispatching) {
			pendingFocusable = value
			return
		}
		val newValue = if (value?.isActive == true) value else root
		val oldFocused = _focused
		if (oldFocused == newValue)
			return focusPending()
		focusedChanging.dispatch(oldFocused, newValue, focusedChangingCancel.reset())
		if (focusedChangingCancel.canceled())
			return focusPending()
		unhighlightFocused()

		_focused = if (!newValue.isActive) {
			// Only happens when the root is being removed.
			null
		} else {
			newValue
		}
		focusedChanged.dispatch(oldFocused, _focused)
		focusPending()
	}

	private fun focusPending() {
		if (pendingFocusable != null) {
			val next = pendingFocusable
			pendingFocusable = null
			focused(next)
		}
	}

	override fun nextFocusable(): UiComponentRo {
		val index = focusables.indexOf(_focused)
		for (i in 1..focusables.lastIndex) {
			var j = index + i
			if (j > focusables.lastIndex) j -= focusables.size
			val element = focusables[j]
			if (element.canFocus) return element
		}
		return _focused ?: root
	}

	override fun previousFocusable(): UiComponentRo {
		val index = focusables.indexOf(_focused)
		for (i in 1..focusables.lastIndex) {
			var j = index - i
			if (j < 0) j += focusables.size
			val element = focusables[j]
			if (element.canFocus) return element
		}
		return _focused ?: root
	}

	override fun iterateFocusables(callback: (UiComponentRo) -> Boolean) {
		for (i in 0..focusables.lastIndex) {
			val shouldContinue = callback(focusables[i])
			if (!shouldContinue) break
		}
	}

	override fun iterateFocusablesReversed(callback: (UiComponentRo) -> Boolean) {
		for (i in focusables.lastIndex downTo 0) {
			val shouldContinue = callback(focusables[i])
			if (!shouldContinue) break
		}
	}

	override fun unhighlightFocused() {
		if (_highlight != null) {
			_highlight?.visible = false
			root.removeElement(_highlight!!)
		}
		_highlighted?.invalidated?.remove(this::highlightedInvalidatedHandler.as2)
		_highlighted = null
	}

	override fun highlightFocused() {
		if (_focused != _root) {
			_highlighted = _focused
			if (_highlighted != null) {
				_highlight?.visible = true
				_highlighted!!.invalidated.add(this::highlightedInvalidatedHandler.as2)
			}
			highlightedInvalidatedHandler()
		}
	}

	private fun highlightedInvalidatedHandler() {
		root.callLater(this::updateHighlight)
	}

	private fun updateHighlight() {
		val highlighted = _highlighted ?: return
		val highlight = _highlight ?: return
		root.addElement(highlight)
		highlight.setSize(highlighted.width, highlighted.height)
		highlight.customTransform = highlighted.concatenatedTransform
	}

	override fun dispose() {
		_assert(!isDisposed, "Already disposed.")
		isDisposed = true
		unhighlightFocused()
		highlight = null
		pendingFocusable = null
		_focused = null
		focusedChanged.dispose()
		focusedChanging.dispose()
		val root = _root
		if (root != null) {
			root.keyDown().remove(rootKeyDownHandler)
			root.mouseDown(isCapture = true).remove(rootMouseDownHandler)
			_root = null

		}
	}

}