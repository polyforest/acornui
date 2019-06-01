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

package com.acornui.core.focus

import com.acornui._assert
import com.acornui.collection.addSorted
import com.acornui.collection.firstOrNull2
import com.acornui.collection.poll
import com.acornui.component.ElementContainer
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.core.DisposedException
import com.acornui.core.di.ownerWalk
import com.acornui.core.input.Ascii
import com.acornui.core.input.interaction.KeyInteractionRo
import com.acornui.core.input.interaction.MouseInteractionRo
import com.acornui.core.input.interaction.TouchInteractionRo
import com.acornui.core.input.keyDown
import com.acornui.core.input.mouseDown
import com.acornui.core.input.touchStart
import com.acornui.core.isBefore
import com.acornui.signal.Cancel
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3

// TODO: handle blur/focus when not the only html element on screen.

/**
 * @author nbilyk
 */
class FocusManagerImpl() : FocusManager {

	constructor(target: ElementContainer<UiComponent>) : this() {
		init(target)
	}

	private var _root: ElementContainer<UiComponent>? = null
	private val root: ElementContainer<UiComponent>
		get() = _root!!

	private val focusedChangingCancel: Cancel = Cancel()
	private val _focusedChanging = Signal3<UiComponentRo?, UiComponentRo?, Cancel>()
	override val focusedChanging = _focusedChanging.asRo()
	private val _focusedChanged = Signal2<UiComponentRo?, UiComponentRo?>()
	override val focusedChanged = _focusedChanged.asRo()

	private var _focused: UiComponentRo? = null

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

	private fun rootMouseDownHandler(event: MouseInteractionRo) {
		if (event.defaultPrevented()) return
		focusFirstAncestor(event.target)
	}

	private fun rootTouchStartHandler(event: TouchInteractionRo) {
		if (event.defaultPrevented()) return
		if (event.touches.size == 1)
			focusFirstAncestor(event.target)
	}

	private fun focusFirstAncestor(target: UiComponentRo?) {
		var p: UiComponentRo? = target
		while (p != null) {
			if (p.focusEnabled) {
				focused(p)
				break
			}
			p = p.parent
		}
		Unit
	}

	override fun init(root: ElementContainer<UiComponent>) {
		_assert(_root == null, "Already initialized.")
		_root = root
		_focused = root
		root.keyDown().add(rootKeyDownHandler)
		root.mouseDown(isCapture = false).add(::rootMouseDownHandler)
		root.touchStart(isCapture = false).add(::rootTouchStartHandler)
	}

	override fun invalidateFocusableOrder(value: UiComponentRo) {
		if (!invalidFocusables.contains(value)) {
			_focusables.remove(value)
			if (value.isActive && value.focusEnabled)
				invalidFocusables.add(value)
			else {
				if (_focused === value) {
					focused(null)
				}
			}
		}
	}

	private val focusDemarcations1 = ArrayList<UiComponentRo>()
	private val focusDemarcations2 = ArrayList<UiComponentRo>()

	private fun focusOrderComparator(o1: UiComponentRo, o2: UiComponentRo): Int {
		o1.calculateFocusDemarcations(focusDemarcations1)
		o2.calculateFocusDemarcations(focusDemarcations2)

		var r = 0
		for (i in 0..minOf(focusDemarcations1.lastIndex, focusDemarcations2.lastIndex)) {
			val d1 = focusDemarcations1[i]
			val d2 = focusDemarcations2[i]
			r = d1.compareFocusOrder(d2)
			if (r != 0) break
		}
		focusDemarcations1.clear()
		focusDemarcations2.clear()
		return r
	}

	private fun UiComponentRo.calculateFocusDemarcations(out: MutableList<UiComponentRo>) {
		out.add(this)
		ownerWalk { if (it is UiComponentRo && it.isFocusContainer) out.add(it); true }
	}

	private fun UiComponentRo.compareFocusOrder(other: UiComponentRo): Int {
		if (this === other) return 0
		val r1 = focusOrder.compareTo(other.focusOrder)
		if (r1 != 0) return r1
		return if (isBefore(other)) -1 else 1
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
				_focusables.addSorted(focusable, comparator = ::focusOrderComparator)
			}
		}
	}

	override val focused: UiComponentRo?
		get() {
//			val focused = _focused ?: return null
//			if (!focused.isActive) {
//				focused(root)
//			}
			return _focused
		}

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
		if (focusedChangingCancel.canceled)
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
		var index = focusables.indexOf(_focused ?: root)
		if (index == -1) index = 0
		for (i in 1..focusables.lastIndex) {
			var j = index + i
			if (j > focusables.lastIndex) j -= focusables.size
			val element = focusables[j]
			if (element.canFocusSelf) return element
		}
		return _focused ?: root
	}

	override fun previousFocusable(): UiComponentRo {
		var index = focusables.indexOf(_focused ?: root)
		if (index == -1) index = focusables.size
		for (i in 1..focusables.lastIndex) {
			var j = index - i
			if (j < 0) j += focusables.size
			val element = focusables[j]
			if (element.canFocusSelf) return element
		}
		return _focused ?: root
	}

	private var highlighted: UiComponentRo? = null
		set(value) {
			if (field != value) {
				field?.showFocusHighlight = false
				field = value
				field?.showFocusHighlight = true
			}
		}

	override fun unhighlightFocused() {
		highlighted = null
	}

	override fun highlightFocused() {
		highlighted = if (_focused != _root) _focused else null
	}

	override fun dispose() {
		if (isDisposed) throw DisposedException()
		isDisposed = true
		highlighted = null
		pendingFocusable = null
		_focused = null
		_focusedChanged.dispose()
		_focusedChanging.dispose()
		val root = _root ?: throw Exception("Not initialized.")
		root.keyDown().remove(rootKeyDownHandler)
		root.mouseDown(isCapture = false).remove(::rootMouseDownHandler)
		root.touchStart(isCapture = false).remove(::rootTouchStartHandler)
		_root = null
	}

}
