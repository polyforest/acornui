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
import com.acornui.component.*
import com.acornui.di.Context
import com.acornui.isAncestorOf
import com.acornui.js.html.isAncestorOf
import com.acornui.signal.StoppableSignal
import org.w3c.dom.HTMLElement
import kotlin.browser.document


/**
 * Returns true if this component is not an ancestor of a focus container that has [Focusable.focusEnabled] set to
 * false.
 */
val UiComponent.focusEnabledAncestry: Boolean
	get() {
//		var p = parent
//		while (p != null) {
//			if (!p.focusEnabledChildren) return false
//			p = p.parent
//		}
		return true
	}

val UiComponent.focusEnabled: Boolean
	get() = true

/**
 * Finds the first focusable child that may be focused.
 * If no focusable element is found, null is returned.
 */
val UiComponent.firstFocusable: UiComponent?
	get() {
		if (!focusEnabledAncestry || !isRendered) return null
//		if (focusEnabled) return this
//		val focusManager = inject(FocusManager)
//		return focusManager.focusables.firstOrNull {
//			it != this && isAncestorOf(it) && it.canFocusSelf
//		}
		return this
	}

/**
 * Finds the last focusable child that may be focused.
 * If no focusable element is found, null is returned.
 */
val UiComponent.lastFocusable: UiComponent?
	get() {
		if (!focusEnabledAncestry || !isRendered) return null
//		val focusManager = inject(FocusManager)
//		return focusManager.focusables.lastOrNull {
//			isAncestorOf(it) && it.canFocusSelf
//		}
		return this
	}


/**
 * Returns true if this component is currently in focus.
 *
 * This is not true if one of its descendants is focused.
 * @see isFocused
 */
val UiComponent.isFocusedSelf: Boolean
	get() = htmlElement === document.activeElement

/**
 * Sets focus to this element, ignoring all focus rules such as focus order, focus enabled, visibility, etc.
 *
 * @see focus
 */
fun UiComponent.focusSelf(options: FocusOptions = FocusOptions.default) {
	htmlElement.focus(options)
}

fun HTMLElement.focus(options: FocusOptions) {
	asDynamic().focus(options)
}

/**
 * Removes focus from this element if it is currently focused.
 */
fun UiComponent.blurSelf() {
	htmlElement.blur()
}

/**
 * Returns true if this component [isAncestorOf] the currently focused element.
 */
val UiComponent.isFocused: Boolean
	get() {
		val focused = document.activeElement ?: return false
		return htmlElement.isAncestorOf(focused)
	}

/**
 * Removes focus from this element or any descendent if it is currently focused.
 */
fun UiComponent.blur() {
	if (isFocused)
		document.activeElement.unsafeCast<HTMLElement>().blur()
}

/**
 * Returns true if this component can be focused via [focus].
 */
val UiComponent.canFocus: Boolean
	get() = firstFocusable != null

/**
 * Sets focus to the first descendant (abiding by focus order) that can be focused, if possible.
 *
 * @see canFocus
 */
fun UiComponent.focus(options: FocusOptions = FocusOptions.default) {
	val toFocus = firstFocusable ?: return
	toFocus.focusSelf(options)
}

data class FocusOptions(val preventScroll: Boolean = false) {
	companion object {
		val default = FocusOptions()
	}
}


/**
 *
 */
fun UiComponent.focusEvent(isCapture: Boolean = false): StoppableSignal<FocusEventRo> {
//	return createOrReuse(FocusEventRo.FOCUS, isCapture)
}

/**
 *
 */
fun UiComponent.blurEvent(isCapture: Boolean = false): StoppableSignal<FocusEventRo> {
//	return createOrReuse(FocusEventRo.BLUR, isCapture)
}