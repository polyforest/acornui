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
import com.acornui.input.InteractivityManager
import com.acornui.input.interaction.ClickInteractionRo
import com.acornui.input.interaction.KeyInteractionRo
import com.acornui.input.interaction.click
import com.acornui.input.keyDown
import com.acornui.isBefore

// TODO: handle blur/focus when not the only html element on screen.

/**
 * @author nbilyk
 */
class FocusManagerImpl(private val interactivityManager: InteractivityManager) : FocusManager {

	private var _root: ElementContainer<UiComponent>? = null
	private val root: ElementContainer<UiComponent>
		get() = _root!!

	private val invalidFocusables = ArrayList<UiComponentRo>()

	private val _focusables = ArrayList<UiComponentRo>()
	override val focusables: List<UiComponentRo>
		get() {
			if (invalidFocusables.isNotEmpty())
				validateFocusables()
			return _focusables
		}

	private val focused: UiComponentRo
		get() = interactivityManager.activeElement

	private var isDisposed: Boolean = false

	private val rootKeyDownHandler = { event: KeyInteractionRo ->
		if (!event.defaultPrevented() && event.keyCode == Ascii.TAB) {
			val previousFocused = focused
			if (event.shiftKey) focusPrevious(FocusOptions.highlight, FocusInitiator.USER_KEY)
			else focusNext(FocusOptions.highlight, FocusInitiator.USER_KEY)
			if (previousFocused != focused)
				event.preventDefault() // Prevent the browser's default tab interactivity if we've handled it.
		} else if (!event.defaultPrevented() && event.keyCode == Ascii.ESCAPE) {
			clearFocused(FocusInitiator.USER_KEY)
		}
	}

	private fun rootClickDownHandler(event: ClickInteractionRo) {
		if (event.defaultPrevented())
			return
		focusFirstAncestor(event.target)
	}

	private fun focusFirstAncestor(target: UiComponentRo?) {
		var p: UiComponentRo? = target
		while (p != null) {
			if (p.focusEnabled) {
				if (focused != p)
					focus(p, initiator = FocusInitiator.USER_POINT)
				break
			}
			p = p.parent
		}
		Unit
	}

	override fun init(root: ElementContainer<UiComponent>) {
		check(_root == null) { "Already initialized." }
		_root = root
		root.keyDown().add(rootKeyDownHandler)
		root.click(isCapture = true).add(::rootClickDownHandler)
//		root.touchStart(isCapture = false).add(::rootTouchStartHandler)
	}

	override fun invalidateFocusableOrder(value: UiComponentRo) {
		if (!invalidFocusables.contains(value)) {
			_focusables.remove(value)
			if (value.includeInFocusOrder)
				invalidFocusables.add(value)
			else {
				if (interactivityManager.activeElement === value) {
					clearFocused()
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
			val d1 = focusDemarcations1[focusDemarcations1.lastIndex - i]
			val d2 = focusDemarcations2[focusDemarcations2.lastIndex - i]
			r = d1.compareFocusOrder(d2)
			if (r != 0) break
		}
		if (r == 0) r = focusDemarcations1.size.compareTo(focusDemarcations2.size) // A focus enabled container should come before its children.
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

	private data class PendingFocus(val target: UiComponentRo?, val options: FocusOptions, val initiator: FocusInitiator)

	private val focusQueue = ArrayList<PendingFocus>()

	override fun focus(value: UiComponentRo?, options: FocusOptions, initiator: FocusInitiator) {
		val n = focusQueue.size
		check(n < 10) { "Focus call stack exceeded: ${focusQueue.joinToString { it.target.toString() }}" }
		val next = PendingFocus(value, options, initiator)
		if (focusQueue.lastOrNull() != next) {
			focusQueue.add(next)
		}
		if (n == 0) {
			handleQueue()
		}
	}

	private fun handleQueue() {
		var cursor = 0
		while (cursor < focusQueue.size) {
			val next = focusQueue[cursor++]
			val previous = interactivityManager.activeElement
			val blurEvent = FocusEvent()
			blurEvent.type = FocusEventRo.BLURRED
			blurEvent.relatedTarget = next.target
			blurEvent.options = next.options

			interactivityManager.dispatch(blurEvent, previous)

			val target = next.target ?: root
			val focusEvent = FocusEvent()
			focusEvent.relatedTarget = previous
			focusEvent.type = FocusEventRo.FOCUSED
			focusEvent.options = next.options
			focusEvent.initiator = next.initiator

			interactivityManager.dispatch(focusEvent, target)
			if (!blurEvent.defaultPrevented() && !focusEvent.defaultPrevented()) {
				previous.showFocusHighlight = false
				interactivityManager.activeElement(target)
				if (focusEvent.options.highlight) {
					// Show the highlight on the explicit target
					next.target?.showFocusHighlight = true
				}
			}
		}
		focusQueue.clear()
	}

	override fun nextFocusable(): UiComponentRo {
		var index = focusables.indexOf(focused)
		if (index == -1) index = 0
		for (i in 1..focusables.lastIndex) {
			var j = index + i
			if (j > focusables.lastIndex) j -= focusables.size
			val element = focusables[j]
			if (element.canFocusSelf) return element
		}
		return focused
	}

	override fun previousFocusable(): UiComponentRo {
		var index = focusables.indexOf(focused)
		if (index == -1) index = focusables.size
		for (i in 1..focusables.lastIndex) {
			var j = index - i
			if (j < 0) j += focusables.size
			val element = focusables[j]
			if (element.canFocusSelf) return element
		}
		return focused
	}

	override fun dispose() {
		if (isDisposed) throw DisposedException()
		isDisposed = true
		val root = _root ?: error("Not initialized.")
		root.keyDown().remove(rootKeyDownHandler)
		root.click(isCapture = true).remove(::rootClickDownHandler)
		_root = null
	}

	private val UiComponentRo.includeInFocusOrder: Boolean
		get() = isActive && focusEnabled

}
