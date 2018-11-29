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
import com.acornui.core.Disposable
import com.acornui.core.DisposedException
import com.acornui.core.input.Ascii
import com.acornui.core.input.interaction.KeyInteractionRo
import com.acornui.core.input.interaction.MouseInteractionRo
import com.acornui.core.input.keyDown
import com.acornui.core.input.mouseDown
import com.acornui.core.isBefore
import com.acornui.core.time.callLater
import com.acornui.function.as2
import com.acornui.math.Bounds
import com.acornui.math.Matrix4
import com.acornui.signal.Cancel
import com.acornui.signal.Signal
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
	private val _focusedChanging: Signal3<UiComponentRo?, UiComponentRo?, Cancel> = Signal3()
	override val focusedChanging: Signal<(UiComponentRo?, UiComponentRo?, Cancel) -> Unit>
		get() = _focusedChanging
	private val _focusedChanged: Signal2<UiComponentRo?, UiComponentRo?> = Signal2()
	override val focusedChanged: Signal<(UiComponentRo?, UiComponentRo?) -> Unit>
		get() = _focusedChanged

	private var _focused: UiComponentRo? = null
	private var _highlighted: UiComponentRo? = null

	private val invalidFocusables = ArrayList<UiComponentRo>()

	private val _focusables = ArrayList<UiComponentRo>()
	override val focusables: List<UiComponentRo>
		get() {
			if (invalidFocusables.isNotEmpty())
				validateFocusables()
			return _focusables
		}

	private var isDisposed: Boolean = false

	private val rootKeyDownHandler = { event: KeyInteractionRo ->
		if (!event.defaultPrevented() && event.keyCode == Ascii.TAB) {
			val previousFocused = focused
			if (event.shiftKey) focusPrevious()
			else focusNext()
			highlightFocused()
			if (previousFocused != focused)
				event.preventDefault() // Prevent the browser's default tab interactivity if we've handled it.
		} else if (!event.defaultPrevented() && event.keyCode == Ascii.ESCAPE) {
			clearFocused()
		}
	}

	private val rootMouseDownHandler = { event: MouseInteractionRo ->
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
	override fun setHighlightIndicator(value: UiComponent?, disposeOld: Boolean) {
		val old = _highlight
		if (value == old) return
		old?.disposed?.remove(this::highlightDisposedHandler)
		val wasHighlighted = _highlighted != null
		unhighlightFocused()
		_highlight = value
		if (value != null) {
			value.includeInLayout = false
			value.disposed.add(this::highlightDisposedHandler)
		}
		if (wasHighlighted) highlightFocused()
		if (disposeOld)
			old?.dispose()
	}

	private fun highlightDisposedHandler(d: Disposable) {
		setHighlightIndicator(null, false)
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

	// Temp variables for the focus order comparison so they don't need to be recalculated for the subject.
	private lateinit var focusable: UiComponentRo
	private val focusOrder = ArrayList<Float>()
	private val iFocusOrder = ArrayList<Float>()

	private fun focusOrderComparator(it: UiComponentRo): Int {
		calculateFocusOrder(it, iFocusOrder)
		val f = focusOrder.compareTo(iFocusOrder)
		return if (f != 0) f
		else {
			// The explicit focus order chain is equivalent.
			if (focusable.isBefore(it)) -1 else 1
		}
	}

	private fun <T : Comparable<T>> List<T>.compareTo(other: List<T>): Int {
		for (i in 0..minOf(lastIndex, other.lastIndex)) {
			val r = this[i].compareTo(other[i])
			if (r != 0)
				return r
		}
		return size.compareTo(other.size)
	}

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
				this.focusable = focusable
				val indexA = _focusables.sortedInsertionIndex(comparator = this::focusOrderComparator)
				_focusables.add(indexA, focusable)
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

	override val focused: UiComponentRo?
		get() = _focused

	private var pendingFocusable: UiComponentRo? = null
	private val focusStack = ArrayList<UiComponentRo?>()

	override fun focused(value: UiComponentRo?) {
		if (_focusedChanging.isDispatching || _focusedChanged.isDispatching) {
			if (focusStack.contains(value)) {
				throw Exception("Attempted focus overflow on element: $value")
			}
			focusStack.add(value)
			pendingFocusable = value
			return
		}
		val newValue = if (value?.isActive == true) value else root
		val oldFocused = _focused
		if (oldFocused == newValue)
			return focusPending()
		_focusedChanging.dispatch(oldFocused, newValue, focusedChangingCancel.reset())
		if (focusedChangingCancel.canceled())
			return focusPending()
		unhighlightFocused()

		_focused = if (!newValue.isActive) {
			// Only happens when the root is being removed.
			null
		} else {
			newValue
		}
		_focusedChanged.dispatch(oldFocused, _focused)
		focusPending()
	}

	private fun focusPending() {
		if (pendingFocusable == null) focusStack.clear() else {
			val next = pendingFocusable
			pendingFocusable = null
			focused(next)
		}
	}

	override fun nextFocusable(): UiComponentRo {
		val index = focusables.indexOf(_focused ?: root)
		for (i in 1..focusables.lastIndex) {
			var j = index + i
			if (j > focusables.lastIndex) j -= focusables.size
			val element = focusables[j]
			if (element.canFocusSelf) return element
		}
		return _focused ?: root
	}

	override fun previousFocusable(): UiComponentRo {
		val index = focusables.indexOf(_focused ?: root)
		for (i in 1..focusables.lastIndex) {
			var j = index - i
			if (j < 0) j += focusables.size
			val element = focusables[j]
			if (element.canFocusSelf) return element
		}
		return _focused ?: root
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

	private val highlightBounds = Bounds()
	private val highlightTransform = Matrix4()

	private fun updateHighlight() {
		val highlighted = _highlighted ?: return
		val highlight = _highlight ?: return

		highlighted.updateFocusHighlight(highlightBounds, highlightTransform)

		root.addElement(highlight)
		highlight.setSize(highlightBounds.width, highlightBounds.height)
		highlight.customTransform = highlightTransform
	}

	override fun dispose() {
		if (isDisposed) throw DisposedException()
		isDisposed = true
		unhighlightFocused()
		setHighlightIndicator(null, disposeOld = true)
		pendingFocusable = null
		_focused = null
		_focusedChanged.dispose()
		_focusedChanging.dispose()
		val root = _root
		if (root != null) {
			root.keyDown().remove(rootKeyDownHandler)
			root.mouseDown(isCapture = true).remove(rootMouseDownHandler)
			_root = null

		}
	}

}