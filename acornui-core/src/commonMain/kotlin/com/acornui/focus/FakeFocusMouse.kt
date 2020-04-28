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

@file:Suppress("UNUSED_PARAMETER")

package com.acornui.focus

import com.acornui.Disposable
import com.acornui.component.UiComponentRo
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.input.*
import com.acornui.input.interaction.*
import com.acornui.time.nowMs

/**
 * Dispatches mouse events when using SPACE or ENTER key presses on the focused element.
 */
class FakeFocusMouse(
		owner: Context
) : ContextImpl(owner) {

	private val focus = inject(FocusManager)
	private val interactivity = inject(InteractivityManager)

	private val fakeMouseEvent = MouseInteraction()
	private var downKey: Int? = null

	private fun dispatchFakeMouseEvent(target: UiComponentRo, type: InteractionType<MouseInteractionRo>) {
		fakeMouseEvent.clear()
		fakeMouseEvent.isFabricated = true
		fakeMouseEvent.type = type
		fakeMouseEvent.button = WhichButton.LEFT
		fakeMouseEvent.timestamp = nowMs()
		interactivity.dispatch(target, fakeMouseEvent)
	}

	init {
		focus.focusedChanged.add(::focusChangedHandler)
	}

	private fun focusChangedHandler(old: UiComponentRo?, new: UiComponentRo?) {
		focused = new
	}

	/**
	 * Handle key interaction on the focused element, this will mean the [InteractionEvent.handled] value will be set
	 * in time for other handlers.
	 */
	private var focused: UiComponentRo? = null
		set(value) {
			field?.keyDown()?.remove(::keyDownHandler)
			field?.keyUp()?.remove(::keyUpHandler)
			field = value
			value?.keyDown()?.add(::keyDownHandler)
			value?.keyUp()?.add(::keyUpHandler)
			downKey = null
		}

	private fun keyDownHandler(event: KeyInteractionRo) {
		if (!event.handled) {
			val target = event.target
			val isRepeat = event.isRepeat && target.downRepeatEnabled()
			if ((downKey == null || isRepeat) && !event.hasAnyModifier && (event.keyCode == Ascii.SPACE || event.isEnterOrReturn)) {
				downKey = event.keyCode
				dispatchFakeMouseEvent(target, MouseInteractionRo.MOUSE_DOWN)
				if (fakeMouseEvent.handled)
					event.handled = true
			}
		}
	}

	private fun keyUpHandler(event: KeyInteractionRo) {
		if (event.keyCode == downKey) {
			downKey = null
			dispatchFakeMouseEvent(event.target, MouseInteractionRo.MOUSE_UP)
			if (fakeMouseEvent.handled)
				event.handled = true
			val fakeClickEvent = event.target.dispatchClick()
			if (fakeClickEvent.handled)
				event.handled = true
		}
	}

	override fun dispose() {
		focused = null
		super.dispose()
	}
}

/**
 * @param target //The target for when ENTER or RETURN is pressed.
 */
class EnterTarget(val host: UiComponentRo, val target: UiComponentRo) : Disposable {

	init {
		host.keyUp().add(::hostKeyUpHandler)
	}

	private fun hostKeyUpHandler(event: KeyInteractionRo) {
		if (!event.handled && !event.hasAnyModifier && event.isEnterOrReturn) {
			target.dispatchClick()
		}
	}

	override fun dispose() {
		host.keyUp().remove(::hostKeyUpHandler)
	}
}

/**
 *
 */
fun UiComponentRo.enterTarget(target: UiComponentRo): Disposable {
	return EnterTarget(this, target)
}
