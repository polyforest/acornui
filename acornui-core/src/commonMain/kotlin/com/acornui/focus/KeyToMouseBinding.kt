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

import com.acornui.ManagedDisposable
import com.acornui.collection.Filter
import com.acornui.component.UiComponentRo
import com.acornui.component.isAncestorOf
import com.acornui.component.stage
import com.acornui.di.ContextImpl
import com.acornui.function.as1
import com.acornui.input.*
import com.acornui.input.interaction.*
import com.acornui.time.nowMs

/**
 * Dispatches mouse events when using SPACE or ENTER key presses on the focused element.
 */
class KeyToMouseBinding(
		private val host: UiComponentRo
) : ContextImpl(host) {

	private val interactivity by InteractivityManager

	private val fakeMouseEvent = MouseInteraction()

	/**
	 * The filter for a key down interaction to determine if the mouse event should be triggered.
	 * Default is:
	 * `!event.hasAnyModifier && (event.keyCode == Ascii.SPACE || event.isEnterOrReturn)`
	 */
	var filter: Filter<KeyInteractionRo> = { event ->
		!event.hasAnyModifier && (event.keyCode == Ascii.SPACE || event.isEnterOrReturn)
	}

	init {
		host.keyDown().add(::keyDownHandler)
	}

	private fun keyDownHandler(event: KeyInteractionRo) {
		if (!event.handled) {
			val target = event.target
			val isRepeat = event.isRepeat && target.downRepeatEnabled()
			if ((downKey == null || isRepeat) && filter(event)) {
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

	private var downKey: Int? = null
		set(value) {
			if (field == value) return
			field = value
			if (value == null)
				stage.keyUp().remove(::keyUpHandler)
			else
				stage.keyUp().add(::keyUpHandler)
		}

	private fun dispatchFakeMouseEvent(target: UiComponentRo, type: InteractionType<MouseInteractionRo>) {
		fakeMouseEvent.clear()
		fakeMouseEvent.isFabricated = true
		fakeMouseEvent.type = type
		fakeMouseEvent.button = WhichButton.LEFT
		fakeMouseEvent.timestamp = nowMs()
		interactivity.dispatch(fakeMouseEvent, target)
	}

	override fun dispose() {
		host.keyDown().remove(::keyDownHandler)
		downKey = null
		super.dispose()
	}
}

/**
 * If ENTER, RETURN, or SPACE is pressed on the host, the host will receive a fabricated mouse event.
 * @return Returns the binding, which may be disposed.
 */
fun UiComponentRo.mousePressOnKey(): KeyToMouseBinding {
	return KeyToMouseBinding(this)
}

/**
 * If an unhandled ENTER or RETURN is pressed on the host, the target will receive a fabricated click event.
 */
class EnterTargetClickBinding(val host: UiComponentRo, var target: UiComponentRo?) : ManagedDisposable {

	private val stage = host.stage

	init {
		host.disposed.add(::dispose.as1)
		stage.keyUp().add(::stageKeyUpHandler)
	}

	private fun stageKeyUpHandler(event: KeyInteractionRo) {
		if (!event.handled && !event.hasAnyModifier && event.isEnterOrReturn && host.isAncestorOf(event.target)) {
			target?.dispatchClick()
		}
	}

	override fun dispose() {
		target = null
		host.disposed.remove(::dispose.as1)
		stage.keyUp().remove(::stageKeyUpHandler)
	}
}

/**
 * If ENTER or RETURN is pressed on the receiver, the target will receive a fabricated click event.
 * @return Returns a handle to dispose the binding or change the target. This will automatically be disposed when this
 * host is disposed.
 */
fun UiComponentRo.enterTarget(target: UiComponentRo): EnterTargetClickBinding {
	return EnterTargetClickBinding(this, target)
}
