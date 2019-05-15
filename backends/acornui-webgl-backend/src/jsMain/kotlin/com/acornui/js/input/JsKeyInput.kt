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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.js.input

import com.acornui.collection.MutableMultiMap2
import com.acornui.collection.get
import com.acornui.collection.multiMap2
import com.acornui.core.input.KeyInput
import com.acornui.core.input.interaction.CharInteraction
import com.acornui.core.input.interaction.KeyInteraction
import com.acornui.core.input.interaction.KeyLocation
import com.acornui.signal.Signal1
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.window

/**
 * @author nbilyk
 */
class JsKeyInput(
		canvas: HTMLElement,
		captureAllKeyboardInput: Boolean
) : KeyInput {

	private val _keyDown = Signal1<KeyInteraction>()
	override val keyDown = _keyDown.asRo()
	private val _keyUp = Signal1<KeyInteraction>()
	override val keyUp = _keyDown.asRo()
	private val _char = Signal1<CharInteraction>()
	override val char = _char.asRo()

	private val keyEvent = KeyInteraction()
	private val charEvent = CharInteraction()

	private val downMap: MutableMultiMap2<Int, KeyLocation, Boolean> = multiMap2()

	private val keyDownHandler = { jsEvent: Event ->
		if (jsEvent is KeyboardEvent) {
			keyEvent.clear()
			populateKeyEvent(jsEvent)
			if (!jsEvent.repeat) {
				downMap[keyEvent.keyCode][keyEvent.location] = true
			}
			_keyDown.dispatch(keyEvent)
			if (keyEvent.defaultPrevented()) jsEvent.preventDefault()
		}
	}

	private val keyUpHandler = { jsEvent: Event ->
		if (jsEvent is KeyboardEvent) {
			keyEvent.clear()
			populateKeyEvent(jsEvent)
			if (downMap.containsKey(keyEvent.keyCode))
				downMap[keyEvent.keyCode].clear() // Browsers give incorrect key location properties on key up.
			_keyUp.dispatch(keyEvent)
			if (keyEvent.defaultPrevented()) jsEvent.preventDefault()
		}
	}

	private val keyPressHandler = { jsEvent: Event ->
		if (jsEvent is KeyboardEvent) {
			charEvent.clear()
			charEvent.char = jsEvent.charCode.toChar()
			_char.dispatch(charEvent)
			if (charEvent.defaultPrevented()) jsEvent.preventDefault()
		}
	}

	private val blurHandler = { jsEvent: Event ->
		downMap.clear()
	}

	private val eventTarget: EventTarget = if (captureAllKeyboardInput) window else canvas

	init {
		if (!captureAllKeyboardInput && !canvas.hasAttribute("tabIndex")) {
			canvas.tabIndex = 0
		}
		eventTarget.addEventListener("keydown", keyDownHandler)
		eventTarget.addEventListener("keyup", keyUpHandler)
		eventTarget.addEventListener("keypress", keyPressHandler)
		eventTarget.addEventListener("blur", blurHandler)
	}

	private fun populateKeyEvent(jsEvent: KeyboardEvent) {
		keyEvent.timestamp = jsEvent.timeStamp.toLong()
		keyEvent.location = locationFromInt(jsEvent.location)
		keyEvent.keyCode = jsEvent.keyCode
		keyEvent.altKey = jsEvent.altKey
		keyEvent.ctrlKey = jsEvent.ctrlKey
		keyEvent.metaKey = jsEvent.metaKey
		keyEvent.shiftKey = jsEvent.shiftKey
		keyEvent.isRepeat = jsEvent.repeat
	}

	override fun keyIsDown(keyCode: Int, location: KeyLocation): Boolean {
		return if (location == KeyLocation.UNKNOWN) {
			return if (!downMap.containsKey(keyCode)) false
			else downMap[keyCode].isNotEmpty()
		} else {
			downMap[keyCode, location] ?: false
		}
	}

	override fun dispose() {
		eventTarget.removeEventListener("keydown", keyDownHandler)
		eventTarget.removeEventListener("keyup", keyUpHandler)
		eventTarget.removeEventListener("keypress", keyPressHandler)
		eventTarget.removeEventListener("blur", blurHandler)
		_keyDown.dispose()
		_keyUp.dispose()
		_char.dispose()
	}

	private fun locationFromInt(location: Int): KeyLocation {
		return when (location) {
			0 -> KeyLocation.STANDARD
			1 -> KeyLocation.LEFT
			2 -> KeyLocation.RIGHT
			3 -> KeyLocation.NUMBER_PAD
			else -> KeyLocation.UNKNOWN
		}
	}
}

