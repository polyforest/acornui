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

package com.acornui.focus

import com.acornui.DisposedException
import com.acornui.collection.addSorted
import com.acornui.collection.poll
import com.acornui.component.ElementContainer
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.component.parentWalk
import com.acornui.input.Ascii
import com.acornui.input.interaction.*
import com.acornui.input.keyDown
import com.acornui.isBefore
import com.acornui.signal.Signal1

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

	private val _focusedChanging = Signal1<FocusChangingEventRo>()
	override val focusedChanging = _focusedChanging.asRo()
	private val _focusedChanged = Signal1<FocusChangedEventRo>()
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
			if (event.shiftKey) focusPrevious(FocusOptions.highlight)
			else focusNext(FocusOptions.highlight)
			if (previousFocused != focused)
				event.preventDefault() // Prevent the browser's default tab interactivity if we've handled it.
		} else if (!event.defaultPrevented() && event.keyCode == Ascii.ESCAPE) {
			clearFocused()
		}
	}

	private fun rootClickDownHandler(event: ClickInteractionRo) {
		if (event.defaultPrevented()) return
		focusFirstAncestor(event.target)
	}

//	private fun rootTouchStartHandler(event: TouchInteractionRo) {
//		if (event.defaultPrevented()) return
//		if (event.touches.size == 1)
//			focusFirstAncestor(event.target)
//	}

	private fun focusFirstAncestor(target: UiComponentRo?) {
		var p: UiComponentRo? = target
		while (p != null) {
			if (p.focusEnabled) {
				if (focused != p)
					focus(p)
				break
			}
			p = p.parent
		}
		Unit
	}

	override fun init(root: ElementContainer<UiComponent>) {
		check(_root == null) { "Already initialized." }
		_root = root
		_focused = root
		root.keyDown().add(rootKeyDownHandler)
		root.click(isCapture = false).add(::rootClickDownHandler)
//		root.touchStart(isCapture = false).add(::rootTouchStartHandler)
	}

	override fun invalidateFocusableOrder(value: UiComponentRo) {
		if (!invalidFocusables.contains(value)) {
			_focusables.remove(value)
			if (value.includeInFocusOrder)
				invalidFocusables.add(value)
			else {
				if (_focused === value) {
					focus(null)
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
		parent?.parentWalk {
			if (it.isFocusContainer) {
				out.add(it)
			}
			true 
		}
	}

	private fun UiComponentRo.compareFocusOrder(other: UiComponentRo): Int {
		if (this === other) return 0
		val r1 = focusOrder.compareTo(other.focusOrder)
		if (r1 != 0) return r1
		val b = isBefore(other)
		return if (b == null) 0 else if (b) -1 else 1
	}

	private fun validateFocusables() {
		while (invalidFocusables.isNotEmpty()) {
			// Use poll instead of pop because the invalid focusables are more likely to be closer to already in order than not.
			val focusable = invalidFocusables.poll()
			if (!focusable.includeInFocusOrder) continue
			if (_focusables.isEmpty()) {
				// Trivial case
				_focusables.add(focusable)
			} else {
				_focusables.addSorted(focusable, comparator = ::focusOrderComparator)
			}
		}
	}

	override val focused: UiComponentRo?
		get() = _focused


	private val eventQueue = ArrayList<FocusChangeEvent>()

	override fun focus(value: UiComponentRo?, options: FocusOptions) {
		val delegate = value?.focusDelegate
		if (delegate != null) return focus(delegate)

		val oldFocused = _focused

		val e = FocusChangeEvent()
		e.options = options
		e.new = value ?: root
		e.old = oldFocused
		check( eventQueue.size < 10) { "Call stack exceeded." }
		eventQueue.add(e)

		if (!_focusedChanging.isDispatching && !_focusedChanged.isDispatching) {
			while (eventQueue.isNotEmpty()) {
				val next = eventQueue.poll()
				_focusedChanging.dispatch(next)
				val cancelled = next.isCancelled
				next.isCancelled = false
				if (!cancelled && eventQueue.isEmpty()) {
					_focused?.showFocusHighlight = false
					_focused = next.new
					if (next.options.highlight) {
						next.new?.showFocusHighlight = true
					}
					_focusedChanged.dispatch(next)
				}
			}
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

	override fun dispose() {
		if (isDisposed) throw DisposedException()
		isDisposed = true
		_focused = null
		_focusedChanged.dispose()
		_focusedChanging.dispose()
		val root = _root ?: error("Not initialized.")
		root.keyDown().remove(rootKeyDownHandler)
		root.click(isCapture = false).remove(::rootClickDownHandler)
//		root.touchStart(isCapture = false).remove(::rootTouchStartHandler)
		_root = null
	}

	private val UiComponentRo.includeInFocusOrder: Boolean
		get() = isActive && focusEnabled && focusDelegate == null

}
