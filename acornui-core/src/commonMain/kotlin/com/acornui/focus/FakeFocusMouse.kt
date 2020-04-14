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
import com.acornui.di.ContextImpl
import com.acornui.input.*
import com.acornui.input.interaction.*
import com.acornui.time.nowMs
import com.acornui.toDisposable

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
	private var downElement: UiComponentRo? = null

	private val keyDownHandler = { event: KeyInteractionRo ->
		if (!event.handled) {
			val target = focus.focused
			if (target != null) {
				val isRepeat = event.isRepeat && target.downRepeatEnabled()
				if ((downKey == null || isRepeat) && !event.hasAnyModifier && (event.keyCode == Ascii.SPACE || event.isEnterOrReturn)) {
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
			if (!downElement.dispatchClick().handled)
				getEnterTarget(event)?.dispatchClick()
		}
	}

	private fun dispatchFakeMouseEvent(target: UiComponentRo, type: InteractionType<MouseInteractionRo>) {
		fakeMouseEvent.clear()
		fakeMouseEvent.isFabricated = true
		fakeMouseEvent.type = type
		fakeMouseEvent.button = WhichButton.LEFT
		fakeMouseEvent.timestamp = nowMs()
		interactivity.dispatch(target, fakeMouseEvent)
	}

	private fun getEnterTarget(event: KeyInteractionRo): UiComponentRo? {
		val target = focus.focused?.findParent {
			it.getAttachment<EnterTarget>(EnterTarget)?.filter?.invoke(event) == true
		}
		return target?.enterTarget ?: target
	}

	init {
		stage.keyDown().add(keyDownHandler)
		stage.keyUp().add(keyUpHandler)
	}

	override fun dispose() {
		super.dispose()
		stage.keyDown().remove(keyDownHandler)
		stage.keyUp().remove(keyUpHandler)
	}
}

/**
 * The target for when ENTER or RETURN is pressed.
 */
class EnterTarget(val target: UiComponentRo, val filter: (KeyInteractionRo) -> Boolean) {

	companion object
}

private val UiComponentRo.enterTarget: UiComponentRo?
	get() = getAttachment<EnterTarget>(EnterTarget)?.target

/**
 * The FakeFocusMouse by default will respond to ENTER or RETURN key presses and fabricate MOUSE_DOWN and MOUSE_UP
 * events on the focused element.  If the enter target is set on an ancestor of the focused element, that target will
 * be used instead.
 */
fun UiComponentRo.enterTarget(target: UiComponentRo, filter: (KeyInteractionRo) -> Boolean = { !it.handled && it.keyCode != Ascii.SPACE }): Disposable {
	createOrReuseAttachment(EnterTarget) {
		EnterTarget(target, filter)
	}
	return {
		removeAttachment<EnterTarget>(EnterTarget)
	}.toDisposable()
}
