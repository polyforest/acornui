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

import com.acornui.collection.firstOrNull2
import com.acornui.collection.lastOrNull2
import com.acornui.component.ElementContainer
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
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
	 * Initializes this focus manager with the given root focusable.
	 */
	fun init(root: ElementContainer<UiComponent>)

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
	 * Refreshes the focusable's order in the focus list.
	 */
	fun invalidateFocusableOrder(value: UiComponentRo)

	/**
	 * Returns the currently focused element. This will be null if the root of the focus manager is being removed.
	 */
	val focused: UiComponentRo?

	/**
	 * Sets the currently focused element.
	 * The provided focusable element may have [Focusable.focusEnabled] set to false; It will still be focused.
	 *
	 * @param value The target to focus. If this is null, the manager's root will be focused. (This is typically the
	 * stage)
	 * @see UiComponentRo.focusSelf
	 * @see UiComponentRo.blurSelf
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
	 * Returns a list of the focusable elements.
	 */
	val focusables: List<UiComponentRo>

	fun unhighlightFocused()

	fun highlightFocused()

	companion object : DKey<FocusManager>
}

/**
 * An interface for a component that may be focused.
 */
interface Focusable : Scoped {

	val focusableStyle: FocusableStyle

	/**
	 * True if this Focusable object should be included in the focus order.
	 * Note that this does not affect directly setting this element to be focused via [focusSelf] or using
	 * [FocusManager.focused]
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
	 * If true, this is component will be considered a focus container. That means it is a demarcation for focus order
	 * on all Focusable descendants.
	 */
	val isFocusContainer: Boolean

	/**
	 * If false, children will not be considered for focus.
	 */
	val focusEnabledChildren: Boolean

	/**
	 * If true, this component will render its focus highlight as provided by the [focusableStyle].
	 */
	var showFocusHighlight: Boolean
}

/**
 * Returns true if this component is not an ancestor of a focus container that has [Focusable.focusEnabled] set to
 * false.
 */
val UiComponentRo.focusEnabledAncestry: Boolean
	get() {
		var p = parent
		while (p != null) {
			if (!p.focusEnabledChildren) return false
			p = p.parent
		}
		return true
	}

/**
 * Finds the first focusable child that may be focused.
 * If no focusable element is found, null is returned.
 */
val UiComponentRo.firstFocusable: UiComponentRo?
	get() {
		if (!focusEnabledAncestry || !isRendered || !interactivityEnabled) return null
		if (focusEnabled) return this
		val focusManager = inject(FocusManager)
		return focusManager.focusables.firstOrNull2 {
			it != this && isAncestorOf(it) && it.canFocusSelf
		}
	}

/**
 * Finds the last focusable child that may be focused.
 * If no focusable element is found, null is returned.
 */
val UiComponentRo.lastFocusable: UiComponentRo?
	get() {
		if (!focusEnabledAncestry || !isRendered || !interactivityEnabled) return null
		val focusManager = inject(FocusManager)
		return focusManager.focusables.lastOrNull2 {
			isAncestorOf(it) && it.canFocusSelf
		}
	}

/**
 * Invalidates the focus order of this component.
 */
fun UiComponentRo.invalidateFocusOrder() {
	inject(FocusManager).invalidateFocusableOrder(this)
}

/**
 * Invalidates the focus order of this component and all descendants.
 */
fun UiComponentRo.invalidateFocusOrderDeep() {
	if (!isActive) return
	val focusManager = inject(FocusManager)
	childWalkLevelOrder {
		focusManager.invalidateFocusableOrder(it)
		TreeWalk.CONTINUE
	}
}

/**
 * Returns true if this component is currently in focus.
 *
 * This is not true if one of its descendants is focused.
 * @see isFocused
 */
val UiComponentRo.isFocusedSelf: Boolean
	get() {
		return this === inject(FocusManager).focused
	}

/**
 * Returns true if this component is allowed to be focused.
 *
 * @see canFocus
 */
val UiComponentRo.canFocusSelf: Boolean
	get() = focusEnabled && focusEnabledAncestry && isRendered && interactivityEnabled

/**
 * Sets focus to this element, ignoring all focus rules such as focus order, focus enabled, visibility, etc.
 *
 * @see canFocusSelf
 * @see focus
 */
fun UiComponentRo.focusSelf() {
	inject(FocusManager).focused(this)
}

/**
 * Removes focus from this element if it is currently focused.
 */
fun UiComponentRo.blurSelf() {
	val focusManager = inject(FocusManager)
	if (focusManager.focused == this)
		focusManager.clearFocused()
}

/**
 * Returns true if this component owns the currently focused element.
 *
 * Reminder: `this.owns(this) == true`
 */
val UiComponentRo.isFocused: Boolean
	get() {
		val focused = inject(FocusManager).focused
		return focused != null && owns(focused)
	}

/**
 * Removes focus from this element if it is currently focused.
 */
fun UiComponentRo.blur() {
	if (isFocused) inject(FocusManager).clearFocused()
}

/**
 * Returns true if this component can be focused via [focus].
 */
val UiComponentRo.canFocus: Boolean
	get() = firstFocusable != null

/**
 * Sets focus to the first descendant (abiding by focus order) that can be focused.
 *
 * @see canFocus
 */
fun UiComponentRo.focus(highlight: Boolean = false) {
	val focusManager = inject(FocusManager)
	val toFocus = firstFocusable
	if (toFocus == null) {
		focusManager.clearFocused()
	} else {
		toFocus.focusSelf()
		if (highlight)
			focusManager.highlightFocused()
	}
}

class FocusableStyle : StyleBase() {

	override val type = Companion

	var highlighter by prop<FocusHighlighter?>(null)

	companion object : StyleType<FocusableStyle>
}

interface FocusHighlighter : Disposable {

	fun unhighlight(target: UiComponent)

	fun highlight(target: UiComponent)

	companion object {
		const val HIGHLIGHT_PRIORITY = 99999f
	}
}