/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.input

import com.acornui.Disposable
import com.acornui.collection.Filter
import com.acornui.component.UiComponent
import com.acornui.di.ContextImpl
import com.acornui.dom.handle
import com.acornui.dom.isFabricated
import com.acornui.dom.isHandled
import com.acornui.own
import com.acornui.signal.EventOptions
import com.acornui.signal.asWithEventTarget
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit
import kotlin.browser.window

/**
 * Dispatches mouse events when using SPACE or ENTER key presses on the focused element.
 */
class KeyToMouseBinding(
		host: UiComponent
) : ContextImpl(host) {

	/**
	 * The filter for a key down interaction to determine if the mouse event should be triggered.
	 * Default is:
	 * `!event.hasAnyModifier && (event.keyCode == Ascii.SPACE || event.isEnterOrReturn)`
	 */
	var filter: Filter<KeyboardEvent> = { event ->
		!event.hasAnyModifier && (event.keyCode == Ascii.SPACE || event.isEnterOrReturn)
	}

	private val win = window.asWithEventTarget()

	init {
		own(host.keyPressed.listen(EventOptions(isPassive = false), ::keyDownHandler))
	}

	private fun keyDownHandler(event: KeyboardEvent) {
		if (event.isHandled) return
		val isRepeat = event.repeat //&& target.downRepeatEnabled()
		if ((downKey == null || isRepeat) && filter(event)) {
			event.handle()
			downKey = event.keyCode
			val e = dispatchFakeMouseEvent(event.target!!, "mousedown")
			if (e.isHandled)
				event.handle()
			if (event.keyCode == Ascii.SPACE)
				event.preventDefault() // Prevent SPACE from scrolling.
		}
	}

	private fun keyUpHandler(event: KeyboardEvent) {
		if (event.keyCode == downKey) {
			event.handle()
			downKey = null
			val e = dispatchFakeMouseEvent(event.target!!, "mouseup")
			if (e.isHandled)
				event.handle()
			if (e.defaultPrevented)
				event.preventDefault()
			dispatchFakeMouseEvent(event.target!!, "click")
		}
	}

	private var keyUpWatch: Disposable? = null
	private var downKey: Int? = null
		set(value) {
			val old = field
			if (old == value) return
			field = value
			if ((old == null) != (value == null)) {
				if (value != null)
					keyUpWatch = win.keyReleased.listen(::keyUpHandler)
				else
					keyUpWatch?.dispose()
			}
		}

	private fun dispatchFakeMouseEvent(target: EventTarget, type: String): MouseEvent {
		val event = MouseEvent(type, MouseEventInit(button = WhichButton.LEFT, bubbles = true))
		event.isFabricated = true
		target.dispatchEvent(event)
		return event
	}

	override fun dispose() {
		downKey = null
		super.dispose()
	}
}

/**
 * If ENTER, RETURN, or SPACE is pressed on the host, the host will receive a fabricated mouse event.
 * @return Returns the binding, which may be disposed.
 */
fun UiComponent.mousePressOnKey(): KeyToMouseBinding {
	return KeyToMouseBinding(this)
}

// TODO: Enter Target
///**
// * If an unhandled ENTER or RETURN is pressed on the host, the target will receive a fabricated click event.
// */
//class EnterTargetClickBinding(val host: UiComponent, var target: UiComponent?) : ManagedDisposable, DisposableBase() {
//
//	private val stage = host.stage
//
//	init {
//		host.ownThis()
//		own(stage.keyUp.listen(::stageKeyUpHandler))
//	}
//
//	private fun stageKeyUpHandler(event: KeyboardEvent) {
//		if (!event.isHandled && !event.hasAnyModifier && event.isEnterOrReturn && host.isAncestorOf(event.target)) {
//			target?.dispatchClick()
//		}
//	}
//
//	override fun dispose() {
//		super.dispose()
//		target = null
//
//	}
//}
//
///**
// * If ENTER or RETURN is pressed on the receiver, the target will receive a fabricated click event.
// * @return Returns a handle to dispose the binding or change the target. This will automatically be disposed when this
// * host is disposed.
// */
//fun UiComponent.enterTarget(target: UiComponent): EnterTargetClickBinding {
//	return EnterTargetClickBinding(this, target)
//}
