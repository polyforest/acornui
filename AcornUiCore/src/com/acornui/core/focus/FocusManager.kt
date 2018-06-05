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

import com.acornui.component.Container
import com.acornui.component.Stage
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.di.owns
import com.acornui.core.isAncestorOf
import com.acornui.signal.Cancel
import com.acornui.signal.Signal2
import com.acornui.signal.Signal3

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
	val focusedChanging: Signal3<UiComponentRo?, UiComponentRo?, Cancel>

	/**
	 * Dispatched when the focused object changes.
	 * (oldFocusable, newFocusable)
	 */
	val focusedChanged: Signal2<UiComponentRo?, UiComponentRo?>

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
	 * If the callback returns false, the iteration will not continue.
	 */
	fun iterateFocusables(callback: (UiComponentRo) -> Boolean)

	/**
	 * Iterates all focusable elements in reverse order.
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
	 * If true, this is a demarcation for focus order on all Focusable descendants.
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
 * Finds the first focusable child in this container with focusEnabled == true.
 * If no focusable element is found, null is returned.
 */
val Container.firstFocusableChild: UiComponentRo?
	get() {
		val focusManager = inject(FocusManager)
		var found: UiComponentRo? = null
		focusManager.iterateFocusables {
			if (it.focusEnabled && it.isRendered() && it != this && isAncestorOf(it))
				found = it
			found == null
		}
		return found
	}

fun UiComponent.focusFirst() {
	val focus = inject(FocusManager)
	if (focusEnabled) focus()
	else if (this is Container) {
		val firstFocusable = firstFocusableChild
		if (firstFocusable == null) {
			focus.clearFocused()
		} else {
			firstFocusable.focus()
		}
	} else {
		focus.clearFocused()
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