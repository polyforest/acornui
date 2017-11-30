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
import com.acornui.collection.ObjectPool
import com.acornui.component.*
import com.acornui.component.layout.LayoutElementRo
import com.acornui.core.LifecycleRo
import com.acornui.core.di.owns
import com.acornui.core.input.Ascii
import com.acornui.core.input.interaction.KeyInteraction
import com.acornui.core.input.interaction.MouseInteraction
import com.acornui.core.input.keyDown
import com.acornui.core.input.mouseDown
import com.acornui.core.parentWalk
import com.acornui.math.Vector3
import com.acornui.signal.Cancel
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3


/**
 * @author nbilyk
 */
open class FocusManagerImpl : FocusManager {

	protected var _root: Focusable? = null
	protected val root: Focusable
		get() = _root!!

	protected val focusedChangingCancel: Cancel = Cancel()
	override val focusedChanging: Signal3<Focusable?, Focusable?, Cancel> = Signal3()
	override val focusedChanged: Signal2<Focusable?, Focusable?> = Signal2()

	protected var _focused: Focusable? = null
	protected var _highlighted: Focusable? = null
	protected var _focusablesValid: Boolean = false
	protected var focusables = ArrayList<Focusable>()

	protected var highlightIsChanging: Boolean = false

	private val rootKeyDownHandler = {
		event: KeyInteraction ->
		if (!event.defaultPrevented() && event.keyCode == Ascii.TAB) {
			event.preventDefault()
			if (event.shiftKey) focusPrevious()
			else focusNext()
			highlightFocused()
		} else if (!event.defaultPrevented() && event.keyCode == Ascii.ESCAPE) {
			event.preventDefault()
			clearFocused()
		}
	}

	private val rootMouseDownHandler = {
		event: MouseInteraction ->
		event.target!!.parentWalk {
			if (it is Focusable && it.focusEnabled) {
				val changed = focused(it)
				if (changed == FocusChangeResult.CANCELED) {
					event.preventDefault()
				}
				false // break
			} else {
				true
			}
		}
		Unit
	}

	private val rootInvalidatedHandler = {
		stage: Validatable, flags: Int ->
		if (!highlightIsChanging && flags and ValidationFlags.HIERARCHY_ASCENDING > 0) {
			_focusablesValid = false
			val f = _focused
			if (f != null && (!f.focusEnabled || !f.visible)) clearFocused()
		}
	}

	private var _highlight: UiComponent? = null
	override var highlight: UiComponent?
		get() = _highlight
		set(value) {
			if (value == _highlight) return
			val wasHighlighted = _highlighted != null
			unhighlightFocused()
			_highlight = value
			if (wasHighlighted) highlightFocused()
		}

	override fun init(root: Focusable) {
		_assert(_root == null, "Already initialized.")
		_root = root
		_focused = root
		root.invalidated.add(rootInvalidatedHandler)
		root.keyDown().add(rootKeyDownHandler)
		root.mouseDown(isCapture = true).add(rootMouseDownHandler)
	}

	override fun focused(): Focusable? {
		return _focused
	}

	private val focusedDeactivatedHandler: (LifecycleRo) -> Unit = {
		focused(null)
	}

	override fun focused(value: Focusable?): FocusChangeResult {
		val newValue = if (value?.isActive == true) value else root
		val oldFocused = _focused
		if (oldFocused == newValue) return FocusChangeResult.UNCHANGED
		focusedChanging.dispatch(oldFocused, newValue, focusedChangingCancel.reset())
		if (focusedChangingCancel.canceled())
			return FocusChangeResult.CANCELED
		unhighlightFocused()
		_focused?.deactivated?.remove(focusedDeactivatedHandler)

		if (!newValue.isActive) {
			// Only happens when the root is being removed.
			_focused = null
		} else {
			_focused = newValue
			_assert(_focused!!.isActive)
			newValue.deactivated.add(focusedDeactivatedHandler)
		}
		onFocusedChanged(oldFocused, newValue)

		focusedChanged.dispatch(oldFocused, _focused)
		return FocusChangeResult.CHANGED
	}

	protected open fun onFocusedChanged(oldFocused: Focusable?, value: Focusable?) {
	}

	override fun nextFocusable(): Focusable {
		refreshFocusOrder()

		val index = focusables.indexOf(_focused)
		for (i in 1..focusables.lastIndex) {
			var j = index + i
			if (j > focusables.lastIndex) j -= focusables.size
			val element = focusables[j]
			if (shouldFocus(element)) return element
		}
		return _focused ?: root
	}

	override fun previousFocusable(): Focusable {
		refreshFocusOrder()

		val index = focusables.indexOf(_focused)
		for (i in 1..focusables.lastIndex) {
			var j = index - i
			if (j < 0) j += focusables.size
			val element = focusables[j]
			if (shouldFocus(element)) return element
		}
		return _focused ?: root
	}

	private fun shouldFocus(element: Focusable): Boolean {
		return element.focusEnabled && element.isRendered() && canClick(element)
	}

	private val midPoint: Vector3 = Vector3()

	/**
	 * If the center of the component is occluded by an interactive element, and therefore unlikely to be clickable,
	 * we should skip this component in the focus order.
	 */
	private fun canClick(element: Focusable): Boolean {
		element.localToWindow(midPoint.set(element.width * element.scaleX * 0.5f, element.height * element.scaleY * 0.5f, 0f))
		val topChild = root.getChildUnderPoint(midPoint.x, midPoint.y, onlyInteractive = true) ?: return false
		return element.owns(topChild)
	}

	override fun iterateFocusables(callback: (Focusable) -> Boolean) {
		refreshFocusOrder()
		for (i in 0..focusables.lastIndex) {
			val shouldContinue = callback(focusables[i])
			if (!shouldContinue) break
		}
	}

	override fun iterateFocusablesReversed(callback: (Focusable) -> Boolean) {
		refreshFocusOrder()
		for (i in focusables.lastIndex downTo 0) {
			val shouldContinue = callback(focusables[i])
			if (!shouldContinue) break
		}
	}

	override fun unhighlightFocused() {
		highlightIsChanging = true
		_highlighted?.highlight = null
		_highlighted = null
		highlightIsChanging = false
	}

	override fun highlightFocused() {
		highlightIsChanging = true
		_highlighted = focused()
		_highlight?.moveTo(0f, 0f)
		_highlighted?.highlight = _highlight
		highlightIsChanging = false
	}

	protected open fun refreshFocusOrder() {
		if (_focusablesValid) return
		_focusablesValid = true
		focusables.clear()
		val rootNode = FocusNode.obtain()
		buildFocusTree(rootNode, root)
		rootNode.flatten(focusables)
		rootNode.free()
	}

	/**
	 * Does a depth-first traversal over the display graph.
	 */
	private fun buildFocusTree(parent: FocusNode, layoutElement: LayoutElementRo) {
		val newParent: FocusNode
		if (layoutElement is FocusContainer) {
			val newNode = FocusNode.obtain()
			newNode.focusOrder = layoutElement.focusOrder
			newNode.focusable = layoutElement as? Focusable // Not all FocusContainer elements are Focusable
			newNode.childIndex = parent.children.size
			parent.children.add(newNode)
			newParent = newNode
		} else if (layoutElement is Focusable) {
			val newNode = FocusNode.obtain()
			newNode.focusOrder = layoutElement.focusOrder
			newNode.focusable = layoutElement
			newNode.childIndex = parent.children.size
			parent.children.add(newNode)
			newParent = parent
		} else {
			newParent = parent
		}

		if (layoutElement is Container) {
			layoutElement.iterateChildren {
				buildFocusTree(newParent, it)
				true
			}
			if (layoutElement is FocusContainer) {
				newParent.children.sort()
			}
		}
	}

	override fun dispose() {
		unhighlightFocused()
		_focused?.deactivated?.remove(focusedDeactivatedHandler)
		focusedChanged.dispose()
		focusedChanging.dispose()
		val root = _root
		if (root != null) {
			root.keyDown().remove(rootKeyDownHandler)
			root.mouseDown(isCapture = true).remove(rootMouseDownHandler)
			root.invalidated.remove(rootInvalidatedHandler)
			_root = null

		}
	}

}

class FocusNode private constructor() : Comparable<FocusNode> {

	var childIndex: Int = 0
	var focusOrder: Float = 0f
	var focusable: Focusable? = null
	val children: MutableList<FocusNode> = ArrayList()

	override fun compareTo(other: FocusNode): Int {
		if (focusOrder == other.focusOrder) return childIndex.compareTo(other.childIndex)
		return focusOrder.compareTo(other.focusOrder)
	}

	fun flatten(list: MutableList<Focusable>) {
		// If the parent FocusContainer is itself Focusable, it is ordered before its children.
		if (focusable != null) list.add(focusable!!)
		for (i in 0..children.lastIndex) {
			children[i].flatten(list)
		}
	}

	fun free() {
		for (i in 0..children.lastIndex) {
			children[i].free()
		}
		children.clear()
		pool.free(this)
	}

	companion object {
		private val pool = ObjectPool { FocusNode() }

		fun obtain(): FocusNode {
			return pool.obtain()
		}
	}
}
