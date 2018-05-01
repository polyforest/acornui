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
import com.acornui.component.layout.LayoutElement
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.component.layout.LayoutElementRo
import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.di.inject
import com.acornui.core.di.owns
import com.acornui.core.parentWalk
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
	fun init(root: Focusable)

	/**
	 * Dispatched when the focused object is about to change.
	 * (oldFocusable, newFocusable, cancel)
	 */
	val focusedChanging: Signal3<Focusable?, Focusable?, Cancel>

	/**
	 * Dispatched when the focused object changes.
	 * (oldFocusable, newFocusable)
	 */
	val focusedChanged: Signal2<Focusable?, Focusable?>

	/**
	 * This component will be provided to [Focusable] objects as their highlight clip.
	 */
	var highlight: UiComponent?

	/**
	 * Returns the currently focused element. This will be null if the root of the focus manager is being removed.
	 */
	fun focused(): Focusable?

	/**
	 * Sets the currently focused element.
	 * The provided focusable element may have [Focusable.focusEnabled] set to false; It will still be focused.
	 *
	 * @param value The target to focus. If this is null, the manager's root will be focused. (This is typically the
	 * stage)
	 * @see Focusable.focus
	 * @see Focusable.blur
	 */
	fun focused(value: Focusable?): FocusChangeResult

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
	fun nextFocusable(): Focusable

	/**
	 * Sets focus to [previousFocusable].
	 */
	fun focusPrevious() {
		focused(previousFocusable())
	}

	/**
	 * Returns the previous enabled focusable element.
	 */
	fun previousFocusable(): Focusable

	/**
	 * Iterates all focusable elements in order.
	 * If the callback returns false, the iteration will not continue.
	 */
	fun iterateFocusables(callback: (Focusable) -> Boolean)

	/**
	 * Iterates all focusable elements in reverse order.
	 * If the callback returns false, the iteration will not continue.
	 */
	fun iterateFocusablesReversed(callback: (Focusable) -> Boolean)

	fun unhighlightFocused()

	fun highlightFocused()

	companion object : DKey<FocusManager>
}

enum class FocusChangeResult {
	UNCHANGED,
	CHANGED,
	CANCELED
}

/**
 * A demarcation for focus order. That is, focus order is considered to be relative to the closest FocusContainer
 * ancestor.
 *
 * A focus container does not need to be focusable, although it can be.
 */
interface FocusContainer : LayoutElementRo {

	/**
	 * The focus order weight. A higher number means descendant focusable elements will be focused later in the order
	 * than focusable elements descendant of a sibling focus container with a lower focus order.
	 */
	var focusOrder: Float

}

/**
 * An interface for a component that may be focused.
 */
interface Focusable : UiComponentRo {

	/**
	 * True if this Focusable object may be focused.
	 * If this is false, this element will be skipped in the focus order.
	 */
	var focusEnabled: Boolean

	/**
	 * The focus order weight. A higher number means this component will be later in the order.
	 * (An order of 1f will be focused before a Focusable component with the same parent with an order of 2f)
	 * In the case of a tie, the order within the display graph (breadth-first) is used.
	 */
	val focusOrder: Float

	/**
	 * A UiComponent for rendering the focus highlight is passed to components where the focus manager
	 * requests a highlight.
	 * The focus highlighted component is then responsible for adding the highlight to the display graph and
	 * sizing / positioning the component in its updateLayout, and rendering the component in its render.
	 */
	var highlight: UiComponent?

	/**
	 * Sets focus to this element.
	 */
	fun focus() {
		inject(FocusManager).focused(this)
	}

	/**
	 * Removes focus from this element.
	 */
	fun blur() {
		inject(FocusManager).clearFocused()
	}

	/**
	 * Returns true if this component is currently in focus.
	 *
	 * This is not true if one of its ancestors is focused.
	 */
	val isFocused: Boolean
		get() {
			return this === inject(FocusManager).focused()
		}

	/**
	 * Finds the first parent focus container.
	 */
	val parentFocusableContainer: FocusContainer?
		get() {
			parentWalk {
				if (it != this && it is FocusContainer) {
					return it
				}
				true
			}
			return null
		}
}

/**
 * Finds the first focusable child in this container with focusEnabled == true.
 * If no focusable element is found, null is returned.
 */
val Container.firstFocusableChild: Focusable?
	get() {
		val focusManager = inject(FocusManager)
		var found: Focusable? = null
		focusManager.iterateFocusables {
			if (it.focusEnabled && it != this && owns(it))
				found = it
			found == null
		}
		return found
	}

fun UiComponent.focusFirst() {
	val focus = inject(FocusManager)
	if (this is Focusable && focusEnabled) focus()
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