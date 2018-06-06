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

import com.acornui.component.*
import com.acornui.core.Disposable
import com.acornui.core.TreeWalk
import com.acornui.core.childWalkLevelOrder
import com.acornui.core.di.DKey
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.di.owns
import com.acornui.core.isAncestorOf
import com.acornui.signal.Cancel
import com.acornui.signal.Signal

/**
 * @author nbilyk
 */
interface FocusManager : Disposable {

	/**
	 * Initializes the focus manager with the given root focusable.
	 */
	fun init(root: Stage)

	/**
	 * Dispatched when the focused object is about to change.
	 * (oldFocusable, newFocusable, cancel)
	 */
	val focusedChanging: Signal<(UiComponentRo?, UiComponentRo?, Cancel) -> Unit>

	/**
	 * Dispatched when the focused object changes.
	 * (oldFocusable, newFocusable)
	 */
	val focusedChanged: Signal<(UiComponentRo?, UiComponentRo?) -> Unit>

	/**
	 * This component will be used to highlight the focused element.
	 * This component will be added to the stage provided during [init] and sized and positioned automatically.
	 */
	var highlight: UiComponent?

	/**
	 * Refreshes the focusable's order in the focus list.
	 */
	fun invalidateFocusableOrder(value: UiComponentRo)

	/**
	 * Returns the currently focused element. This will be null if the root of the focus manager is being removed.
	 */
	fun focused(): UiComponentRo?

	/**
	 * Sets the currently focused element.
	 * The provided focusable element may have [Focusable.focusEnabled] set to false; It will still be focused.
	 *
	 * @param value The target to focus. If this is null, the manager's root will be focused. (This is typically the
	 * stage)
	 * @see UiComponentRo.focus
	 * @see UiComponentRo.blur
	 */
	fun focused(value: UiComponentRo?)

	/**
	 * Clears the current focus.
	 */
	fun clearFocused() = focused(null)

	/**
	 * Sets focus to the [nextFocusable].
	 */
	fun focusNext() {
		focused(nextFocusable())
	}

	/**
	 * Returns the next enabled focusable element. If the current focus is null, this will be the root.
	 */
	fun nextFocusable(): UiComponentRo

	/**
	 * Sets focus to [previousFocusable].
	 */
	fun focusPrevious() {
		focused(previousFocusable())
	}

	/**
	 * Returns the previous enabled focusable element.
	 */
	fun previousFocusable(): UiComponentRo

	/**
	 * Iterates all focusable elements in order.
	 * This may return components that shouldn't be focused due to [UiComponentRo.visible] or
	 * [UiComponentRo.focusEnabledInherited] flags. Use [UiComponentRo.canFocus] to check.
	 *
	 * If the callback returns false, the iteration will not continue.
	 */
	fun iterateFocusables(callback: (UiComponentRo) -> Boolean)

	/**
	 * Iterates all focusable elements in reverse order.
	 *
	 *
	 * If the callback returns false, the iteration will not continue.
	 */
	fun iterateFocusablesReversed(callback: (UiComponentRo) -> Boolean)

	fun unhighlightFocused()

	fun highlightFocused()

	companion object : DKey<FocusManager>
}

/**
 * An interface for a component that may be focused.
 */
interface Focusable : Scoped {

	/**
	 * True if this Focusable object may be focused.
	 * If this is false, this element will be skipped in the focus order.
	 */
	val focusEnabled: Boolean

	/**
	 * The focus order weight. This number is relative to the closest ancestor where [isFocusContainer] is true, if
	 * there is one. A higher number means this component will be later in the order.
	 * (An order of 1f will be focused before a Focusable component with the same parent with an order of 2f)
	 * In the case of a tie, the order within the display graph (breadth-first) is used.
	 */
	val focusOrder: Float

	/**
	 * If true, this is component will be considered a focus container. That means -
	 * - It is a demarcation for focus order on all Focusable descendants.
	 * - It will not itself be part of the focus order. (It can still be focused manually)
	 * - If focusEnabled is false, all children will be excluded from the focus order.
	 */
	val isFocusContainer: Boolean

	/**
	 * Returns true if this component is currently in focus.
	 *
	 * This is not true if one of its ancestors is focused.
	 */
	val isFocused: Boolean
		get() {
			return this === inject(FocusManager).focused()
		}
}

/**
 * Returns true if this component has [Focusable.focusEnabled] and is not contained in a focus container
 * ([Focusable.isFocusContainer]) that has [Focusable.focusEnabled] false.
 */
val UiComponentRo.focusEnabledInherited: Boolean
	get() {
		if (!focusEnabled) return false
		var p = parent
		while (p != null) {
			if (p.isFocusContainer && !p.focusEnabled) return false
			p = p.parent
		}
		return true
	}

/**
 * Finds the first focusable child in this container with focusEnabled == true.
 * If no focusable element is found, null is returned.
 */
val UiComponentRo.firstFocusableChild: UiComponentRo?
	get() {
		validateFocusOrder()
		val focusManager = inject(FocusManager)
		var found: UiComponentRo? = null
		focusManager.iterateFocusables {
			if (it != this && isAncestorOf(it) && it.canFocus && !it.isFocusContainer)
				found = it
			found == null
		}
		return found
	}

fun UiComponentRo.focusFirst() {
	val focus = inject(FocusManager)
	if (focusEnabled) focus()
	else {
		val firstFocusable = firstFocusableChild
		if (firstFocusable == null) {
			focus.clearFocused()
		} else {
			firstFocusable.focus()
		}
	}
}

/**
 * Returns true if this component owns the currently focused element.
 *
 * Reminder: `this.owns(this) == true`
 */
fun UiComponent.ownsFocused(): Boolean {
	val focused = inject(FocusManager).focused()
	return focused != null && owns(focused)
}

/**
 * Validates the focus order for this component and all descendants.
 */
fun UiComponentRo.validateFocusOrder() {
	childWalkLevelOrder {
		it.validate(ValidationFlags.FOCUS_ORDER)
		TreeWalk.CONTINUE
	}
}

/**
 * Sets focus to this element.
 * Note that this does not matter if the component can actually be focused. To do a safe focus request,
 * use [focusFirst].
 * @see canFocus
 */
fun UiComponentRo.focus() {
	inject(FocusManager).focused(this)
}

/**
 * Removes focus from this element if it is currently focused.
 */
fun UiComponentRo.blur() {
	val focusManager = inject(FocusManager)
	if (focusManager.focused() == this)
		focusManager.clearFocused()
}

/**
 * Returns true if this component can be focused. This is dependent on the [focusEnabledInherited] and
 * [UiComponentRo.isRendered] properties.
 */
val UiComponentRo.canFocus: Boolean
	get() = focusEnabledInherited && isRendered()