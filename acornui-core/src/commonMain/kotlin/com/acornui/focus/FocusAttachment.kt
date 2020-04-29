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
import com.acornui.di.ContextImpl
import com.acornui.di.owns
import com.acornui.signal.Signal
import com.acornui.signal.Signal0

/**
 * The focus attachment takes general focus changing/changed signals and separates it into signals more relevant to
 * the target component.
 */
class FocusAttachment(
		private val target: UiComponentRo
) : ContextImpl(target) {

	private val focusManager = inject(FocusManager)

	private val _focused = Signal0()

	/**
	 * Dispatched when the previously focused is not owned by this component and the newly focused is.
	 *
	 * `!target.owns(old) && target.owns(new)`
	 *
	 * @see isFocused
	 */
	val focused = _focused.asRo()

	private val _focusedSelf = Signal0()

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

	private val _blurred = Signal0()

	/**
	 * Dispatched when the previously focused is owned by this component and the newly focused is not.
	 *
	 * `target.owns(old) && !target.owns(new)`
	 *
	 * @see isFocused
	 * @see FocusManager.focus
	 */
	val blurred = _blurred.asRo()

	private val _blurredSelf = Signal0()

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
		focusManager.focusedChanged.add(::focusChangedHandler)
	}

	private fun focusChangedHandler(event: FocusChangedEventRo) {
		val old = event.old
		val new = event.new

		if (_focusedSelf.isNotEmpty() && new === target && old !== target)
			_focusedSelf.dispatch()
		if (_blurredSelf.isNotEmpty() && old === target && new !== target)
			_blurredSelf.dispatch()
		if (_blurred.isNotEmpty() && target.owns(old) && !target.owns(new))
			_blurred.dispatch()
		if (_focused.isNotEmpty() && !target.owns(old) && target.owns(new))
			_focused.dispatch()
	}

	override fun dispose() {
		super.dispose()
		focusManager.focusedChanged.remove(::focusChangedHandler)
		_focused.dispose()
		_focusedSelf.dispose()
		_blurred.dispose()
		_blurredSelf.dispose()
	}

	companion object
}

fun UiComponentRo.focusAttachment(): FocusAttachment {
	return createOrReuseAttachment(FocusAttachment) { FocusAttachment(this) }
}

/**
 * @see FocusAttachment.focused
 */
fun UiComponentRo.focused(): Signal<() -> Unit> {
	return focusAttachment().focused
}

/**
 * @see FocusAttachment.blurred
 */
fun UiComponentRo.blurred(): Signal<() -> Unit> {
	return focusAttachment().blurred
}

/**
 * @see FocusAttachment.focusedSelf
 */
fun UiComponentRo.focusedSelf(): Signal<() -> Unit> {
	return focusAttachment().focusedSelf
}

/**
 * @see FocusAttachment.blurredSelf
 */
fun UiComponentRo.blurredSelf(): Signal<() -> Unit> {
	return focusAttachment().blurredSelf
}
