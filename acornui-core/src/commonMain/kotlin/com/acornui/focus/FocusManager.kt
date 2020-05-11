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

import com.acornui.Disposable
import com.acornui.TreeWalk
import com.acornui.childWalkLevelOrder
import com.acornui.component.*
import com.acornui.component.style.StyleType
import com.acornui.di.Context
import com.acornui.input.EventBase
import com.acornui.input.EventRo
import com.acornui.input.EventType
import com.acornui.input.InteractivityManager
import com.acornui.isAncestorOf
import com.acornui.recycle.Clearable
import com.acornui.signal.StoppableSignal

/**
 * @author nbilyk
 */
interface FocusManager : Disposable {

	/**
	 * Initializes this focus manager with the given root focusable.
	 */
	fun init(root: ElementContainer<UiComponent>)

	/**
	 * Refreshes the focusable's order in the focus list.
	 */
	fun invalidateFocusableOrder(value: UiComponentRo)

	/**
	 * Sets the currently focused element.
	 * The provided focusable element may have [Focusable.focusEnabled] set to false; It will still be focused.
	 *
	 * @param value The target to focus. If this is null, the manager's root will be focused. (This is typically the
	 * stage)
	 * @param options Options to place on the change event.
	 * @param initiator The source of the focus change.
	 *
	 * @see UiComponentRo.focusSelf
	 * @see UiComponentRo.blurSelf
	 */
	fun focus(value: UiComponentRo?, options: FocusOptions = FocusOptions.default, initiator: FocusInitiator = FocusInitiator.OTHER)

	/**
	 * Clears the current focus.
	 */
	fun clearFocused(initiator: FocusInitiator = FocusInitiator.OTHER) = focus(null, FocusOptions.highlight, initiator)

	/**
	 * Sets focus to the [nextFocusable].
	 */
	fun focusNext(options: FocusOptions = FocusOptions.default, initiator: FocusInitiator = FocusInitiator.OTHER) {
		focus(nextFocusable(), options, initiator)
	}

	/**
	 * Returns the next enabled focusable element. If the current focus is null, this will be the root.
	 */
	fun nextFocusable(): UiComponentRo

	/**
	 * Sets focus to [previousFocusable].
	 */
	fun focusPrevious(options: FocusOptions = FocusOptions.default, initiator: FocusInitiator = FocusInitiator.OTHER) {
		focus(previousFocusable(), options, initiator)
	}

	/**
	 * Returns the previous enabled focusable element.
	 */
	fun previousFocusable(): UiComponentRo

	/**
	 * Returns a list of the focusable elements.
	 */
	val focusables: List<UiComponentRo>

	companion object : Context.Key<FocusManager>
}

/**
 * An interface for a component that may be focused.
 */
interface Focusable : Context {

	/**
	 * True if this Focusable object should be included in the focus order.
	 * Note that this does not affect directly setting this element to be focused via [focusSelf] or using
	 * [FocusManager.focus]
	 */
	val focusEnabled: Boolean

	/**
	 * The focus order weight. This number is relative to the closest display ancestor where [isFocusContainer] is true,
	 * if there is one. A higher number means this component will be later in the order.
	 * (An order of 1f will be focused before a Focusable component with the same parent with an order of 2f)
	 * In the case of a tie, the order within the display graph (breadth-first) is used.
	 */
	val focusOrder: Float

	/**
	 * If true, this is component will be considered a focus container. That means it is a demarcation for focus order
	 * on all Focusable display descendants.
	 */
	val isFocusContainer: Boolean

	/**
	 * If false, children will not be considered for focus.
	 */
	val focusEnabledChildren: Boolean

	/**
	 * If set, when the focus manager is changing focus to this component, the provided focus delegate will be given
	 * focus instead. Note that setting this will remove this component from the focus order.
	 */
	var focusDelegate: UiComponentRo?
}

data class FocusOptions(

		/**
		 * If true, scrollable regions should scroll to show the newly focused component in view.
		 */
		val scrollToFocused: Boolean = false,

		/**
		 * If true, the component should highlight, indicating visually its focus.
		 */
		val highlight: Boolean = false
) {

	companion object {
		val default = FocusOptions()
		val highlight = FocusOptions(highlight = true)
	}
}

enum class FocusInitiator {

	/**
	 * A click event triggered the focus change.
	 */
	USER_POINT,

	/**
	 * A key event triggered the focus change. (TAB)
	 */
	USER_KEY,

	/**
	 * The focus event was triggered by some other event.
	 */
	OTHER
}

interface FocusEventRo : EventRo {

	/**
	 * On a blur event, this is the newly focused component, on a focus event, this is the previously focused component.
	 */
	val relatedTarget: UiComponentRo?

	/**
	 * The source of the focus change.
	 */
	val initiator: FocusInitiator

	val options: FocusOptions

	companion object {

		val BLUR = EventType<FocusEventRo>("blur")
		val FOCUS = EventType<FocusEventRo>("focus")
	}
}

class FocusEvent : EventBase(), FocusEventRo, Clearable {

	override var relatedTarget: UiComponentRo? = null

	override var options: FocusOptions = FocusOptions.default

	override var initiator: FocusInitiator = FocusInitiator.OTHER

	override fun clear() {
		super.clear()
		relatedTarget = null
		options = FocusOptions.default
		initiator = FocusInitiator.OTHER
	}
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
		if (!focusEnabledAncestry || !isRendered) return null
		if (focusEnabled) return this
		val focusManager = inject(FocusManager)
		return focusManager.focusables.firstOrNull {
			it != this && isAncestorOf(it) && it.canFocusSelf
		}
	}

/**
 * Finds the last focusable child that may be focused.
 * If no focusable element is found, null is returned.
 */
val UiComponentRo.lastFocusable: UiComponentRo?
	get() {
		if (!focusEnabledAncestry || !isRendered) return null
		val focusManager = inject(FocusManager)
		return focusManager.focusables.lastOrNull {
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
		it as UiComponentRo
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
	get() = this === inject(InteractivityManager).activeElement

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
fun UiComponentRo.focusSelf(options: FocusOptions = FocusOptions.default, initiator: FocusInitiator = FocusInitiator.OTHER) {
	inject(FocusManager).focus(this, options, initiator)
}

/**
 * Removes focus from this element if it is currently focused.
 */
fun UiComponentRo.blurSelf() {
	val focusManager = inject(FocusManager)
	if (isFocusedSelf)
		focusManager.clearFocused()
}

/**
 * Returns true if this component [isAncestorOf] the currently focused element.
 */
val UiComponentRo.isFocused: Boolean
	get() {
		val focused = inject(InteractivityManager).activeElement
		return isAncestorOf(focused)
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
 * Sets focus to the first descendant (abiding by focus order) that can be focused, if possible.
 *
 * @see canFocus
 */
fun UiComponentRo.focus(options: FocusOptions = FocusOptions.default, initiator: FocusInitiator = FocusInitiator.OTHER) {
	val focusManager = inject(FocusManager)
	val toFocus = firstFocusable ?: return
	focusManager.focus(toFocus, options, initiator)
}

@Deprecated("Use FocusOptions", ReplaceWith("focus(FocusOptions(highlight = highlight))"), DeprecationLevel.ERROR)
fun UiComponentRo.focus(highlight: Boolean) {
}

class FocusableStyle : HighlightStyle() {

	override val type: StyleType<FocusableStyle> = FocusableStyle

	companion object : StyleType<FocusableStyle> {
		override val extends = HighlightStyle
	}
}


/**
 *
 */
fun UiComponentRo.focusEvent(isCapture: Boolean = false): StoppableSignal<FocusEventRo> {
	return createOrReuse(FocusEventRo.FOCUS, isCapture)
}

/**
 *
 */
fun UiComponentRo.blurEvent(isCapture: Boolean = false): StoppableSignal<FocusEventRo> {
	return createOrReuse(FocusEventRo.BLUR, isCapture)
}