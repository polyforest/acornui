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
import com.acornui.component.stage
import com.acornui.di.ContextImpl
import com.acornui.di.own
import com.acornui.di.owns
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
	 * Dispatched when the previously focused is not owned by this component and the newly focused is.
	 *
	 * `!target.owns(old) && target.owns(new)`
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
	 * Dispatched when the previously focused is owned by this component and the newly focused is not.
	 *
	 * `target.owns(old) && !target.owns(new)`
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
		stage.blurEvent().add(::stageBlurredHandler)
		target.focusEvent().add(::focusedHandler)
		stage.focusEvent().add(::stageFocusedHandler)
	}

	private fun blurredHandler(event: FocusEventRo) {
		if (event.target === target)
			_blurredSelf.dispatch(event)
	}
	private fun stageBlurredHandler(event: FocusEventRo) {
		if (_blurred.isNotEmpty() && target.owns(event.target) && !target.owns(event.relatedTarget))
			_blurred.dispatch(event)
	}

	private fun focusedHandler(event: FocusEventRo) {
		if (event.target === target)
			_focusedSelf.dispatch(event)
	}

	private fun stageFocusedHandler(event: FocusEventRo) {
		if (_focused.isNotEmpty() && !target.owns(event.relatedTarget) && target.owns(event.target))
			_focused.dispatch(event)
	}

	override fun dispose() {
		super.dispose()
		target.blurEvent().remove(::blurredHandler)
		stage.blurEvent().remove(::stageBlurredHandler)
		target.focusEvent().remove(::focusedHandler)
		stage.focusEvent().remove(::stageFocusedHandler)
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
