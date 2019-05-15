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

import com.acornui.component.UiComponentRo
import com.acornui.component.createOrReuseAttachment
import com.acornui.component.parentWalk
import com.acornui.component.stage
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.input.*
import com.acornui.core.input.interaction.*
import com.acornui.core.time.time

/**
 * Dispatches mouse events when using SPACE or ENTER key presses on the focused element.
 */
class FakeFocusMouse(
		override val injector: Injector
) : Scoped, Disposable {

	private val focus = inject(FocusManager)
	private val interactivity = inject(InteractivityManager)

	private val fakeMouseEvent = MouseInteraction()
	private var downKey: Int? = null
	private var downElement: UiComponentRo? = null

	private val keyDownHandler = { event: KeyInteractionRo ->
		if (!event.handled) {
			val target = getTarget(event)
			if (target != null) {
				val isRepeat = event.isRepeat && downKey == event.keyCode && target.downRepeatEnabled()
				if ((downKey == null || isRepeat) && event.keyCode == Ascii.SPACE || event.isEnterOrReturn) {
					event.handled = true
					downKey = event.keyCode
					downElement = target
					dispatchFakeMouseEvent(target, MouseInteractionRo.MOUSE_DOWN)
				}
			}
		}
	}

	private val keyUpHandler = { event: KeyInteractionRo ->
		if (event.keyCode == downKey) {
			downKey = null

			val downElement = downElement!!
			this.downElement = null
			dispatchFakeMouseEvent(downElement, MouseInteractionRo.MOUSE_UP)
			if (!event.handled && getTarget(event) == focus.focused) {
				downElement.dispatchClick()
			}
			event.handled = true
		}
	}

	private fun dispatchFakeMouseEvent(target: UiComponentRo, type: InteractionType<MouseInteractionRo>) {
		fakeMouseEvent.clear()
		fakeMouseEvent.isFabricated = true
		fakeMouseEvent.type = type
		fakeMouseEvent.button = WhichButton.LEFT
		fakeMouseEvent.timestamp = time.nowMs()
		interactivity.dispatch(target, fakeMouseEvent)
	}

	private fun getTarget(event: KeyInteractionRo): UiComponentRo? {
		val focused = focus.focused ?: return null
		var target: UiComponentRo = focused
		focused.parentWalk {
			if (it.click().isNotEmpty()) {
				// If a parent has a click handler, use the focused element, don't continue to check for an
				// EnterTarget attachment.
				false
			} else {
				val attachment = it.getAttachment<EnterTarget>(EnterTarget)
				if (attachment != null) {
					if (attachment.filter(event)) {
						target = attachment.target
						false
					} else {
						true
					}
				} else {
					true
				}
			}

		}
		return target
	}

	init {
		stage.keyDown().add(keyDownHandler)
		stage.keyUp().add(keyUpHandler)
	}

	override fun dispose() {
		stage.keyDown().remove(keyDownHandler)
		stage.keyUp().remove(keyUpHandler)
	}
}

fun Scoped.fakeFocusMouse(): FakeFocusMouse {
	return FakeFocusMouse(injector)
}

/**
 * The target for when ENTER or RETURN is pressed.
 */
class EnterTarget(val target: UiComponentRo, val filter: (KeyInteractionRo) -> Boolean) {

	companion object
}

/**
 * The FakeFocusMouse by default will respond to ENTER or RETURN key presses and fabricate MOUSE_DOWN and MOUSE_UP
 * events on the focused element.  If the enter target is set on an ancestor of the focused element, that target will
 * be used instead.
 */
fun UiComponentRo.enterTarget(target: UiComponentRo, filter: (KeyInteractionRo) -> Boolean = { !it.handled && it.keyCode != Ascii.SPACE }): Disposable {
	createOrReuseAttachment(EnterTarget) {
		EnterTarget(target, filter)
	}
	return object : Disposable {
		override fun dispose() {
			removeAttachment<EnterTarget>(EnterTarget)
		}
	}
}
