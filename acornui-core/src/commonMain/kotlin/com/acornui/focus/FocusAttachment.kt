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

import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.isAncestorOf
import com.acornui.di.ContextImpl
import com.acornui.di.own
import com.acornui.signal.Signal
import com.acornui.signal.Signal1

/**
 * The focus attachment takes general focus changing/changed signals and separates it into signals more relevant to
 * the target component.
 */
class FocusAttachment(
		private val target: UiComponentRo
) : ContextImpl(target) {

	private val _focused = own(Signal1<FocusEventRo>())

	/**
	 * Dispatched when the previously focused is not an ancestor of this component and the newly focused is.
	 *
	 * `!target.isAncestorOf(old) && target.isAncestorOf(new)`
	 *
	 * @see isFocused
	 */
	val focused = _focused.asRo()

	private val _focusedSelf = own(Signal1<FocusEventRo>())

	/**
	 * Dispatched when this is the newly focused component.
	 *
	 * `new === target && old !== target`
	 *
	 * @see isFocusedSelf
	 * @see FocusManager.focus
	 * @see focus
	 */
	val focusedSelf = _focusedSelf.asRo()

	private val _blurred = own(Signal1<FocusEventRo>())

	/**
	 * Dispatched when the previously focused is an ancestor of this component and the newly focused is not.
	 *
	 * `target.isAncestorOf(old) && !target.isAncestorOf(new)`
	 *
	 * @see isFocused
	 * @see FocusManager.focus
	 */
	val blurred = _blurred.asRo()

	private val _blurredSelf = own(Signal1<FocusEventRo>())

	/**
	 * Dispatched when this was the previously focused component.
	 *
	 * `old === target && new !== target`
	 *
	 * @see isFocusedSelf
	 * @see FocusManager.focus
	 * @see blurred
	 */
	val blurredSelf = _blurredSelf.asRo()

	init {
		target.blurEvent().add(::blurredHandler)
		target.focusEvent().add(::focusedHandler)
	}

	private fun blurredHandler(event: FocusEventRo) {
		if (event.target === target)
			_blurredSelf.dispatch(event)
		if (_blurred.isNotEmpty() && !target.isAncestorOf(event.relatedTarget))
			_blurred.dispatch(event)
	}
	private fun focusedHandler(event: FocusEventRo) {
		if (event.target === target)
			_focusedSelf.dispatch(event)
		if (_focused.isNotEmpty() && !target.isAncestorOf(event.relatedTarget))
			_focused.dispatch(event)
	}

	override fun dispose() {
		super.dispose()
		target.blurEvent().remove(::blurredHandler)
		target.focusEvent().remove(::focusedHandler)
	}

	companion object
}

fun UiComponentRo.focusAttachment(): FocusAttachment {
	return createOrReuseAttachment(FocusAttachment) { FocusAttachment(this) }
}

/**
 * @see FocusAttachment.focused
 */
fun UiComponentRo.focused(): Signal<(FocusEventRo) -> Unit> {
	return focusAttachment().focused
}

/**
 * @see FocusAttachment.blurred
 */
fun UiComponentRo.blurred(): Signal<(FocusEventRo) -> Unit> {
	return focusAttachment().blurred
}

/**
 * @see FocusAttachment.focusedSelf
 */
fun UiComponentRo.focusedSelf(): Signal<(FocusEventRo) -> Unit> {
	return focusAttachment().focusedSelf
}

/**
 * @see FocusAttachment.blurredSelf
 */
fun UiComponentRo.blurredSelf(): Signal<(FocusEventRo) -> Unit> {
	return focusAttachment().blurredSelf
}

/**
 * Creates a signal that will be dispatched when none of the provided components is an ancestor of the new focus.
 *
 * @param components The list of components to watch for blur.
 * @return Returns the signal for this condition change. This signal must be disposed if any of the provided components
 * are not owned.
 */
fun blurredAll(vararg components: UiComponentRo): Signal1<FocusEventRo> {
	return object : Signal1<FocusEventRo>() {

		private fun blurHandler(event: FocusEventRo) {
			val anyFocused = components.any {
				it.isAncestorOf(event.relatedTarget)
			}
			if (!anyFocused)
				dispatch(event)
		}

		init {
			components.forEach { c ->
				c.blurEvent().add(::blurHandler)
			}
		}

		override fun dispose() {
			components.forEach { c ->
				c.blurEvent().remove(::blurHandler)
			}
			super.dispose()
		}
	}
}

/**
 * Creates a signal that will be dispatched when any of the provided components is an ancestor of the new focus.
 *
 * @param components The list of components to watch for focus.
 * @return Returns the signal for this condition change. This signal must be disposed if any of the provided components
 * are not owned.
 */
fun focusedAny(vararg components: UiComponentRo): Signal1<FocusEventRo> {
	return object : Signal1<FocusEventRo>() {

		private fun focusHandler(event: FocusEventRo) {
			val anyWereFocused = components.any {
				it.isAncestorOf(event.relatedTarget)
			}
			if (!anyWereFocused)
				dispatch(event)
		}

		init {
			components.forEach { c ->
				c.focusEvent().add(::focusHandler)
			}
		}

		override fun dispose() {
			components.forEach { c ->
				c.focusEvent().remove(::focusHandler)
			}
			super.dispose()
		}
	}
}