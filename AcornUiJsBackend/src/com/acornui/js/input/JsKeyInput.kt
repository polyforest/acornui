/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.js.input

import com.acornui.collection.DualHashMap
import com.acornui.core.input.KeyInput
import com.acornui.core.input.interaction.CharInteraction
import com.acornui.core.input.interaction.KeyInteraction
import com.acornui.core.input.interaction.KeyLocation
import com.acornui.signal.Signal1
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.window

/**
 * @author nbilyk
 */
class JsKeyInput(
		private val root: HTMLElement
) : KeyInput {

	override val keyDown: Signal1<KeyInteraction> = Signal1()
	override val keyUp: Signal1<KeyInteraction> = Signal1()
	override val char: Signal1<CharInteraction> = Signal1()

	private val keyEvent = KeyInteraction()
	private val charEvent = CharInteraction()

	private val downMap: DualHashMap<Int, KeyLocation, Boolean> = DualHashMap()

	private val keyDownHandler = {
		jsEvent: Event ->
		if (jsEvent is KeyboardEvent) {
			keyEvent.clear()
			populateKeyEvent(jsEvent)
			if (!jsEvent.repeat) {
				downMap.put(keyEvent.keyCode, keyEvent.location, true)
			}
			keyDown.dispatch(keyEvent)
			if (keyEvent.defaultPrevented()) jsEvent.preventDefault()
		}
	}

	private val keyUpHandler = {
		jsEvent: Event ->
		if (jsEvent is KeyboardEvent) {
			keyEvent.clear()
			populateKeyEvent(jsEvent)
			downMap[keyEvent.keyCode]?.clear() // Browsers give incorrect key location properties on key up.
			keyUp.dispatch(keyEvent)
			if (keyEvent.defaultPrevented()) jsEvent.preventDefault()
		}
	}

	private val keyPressHandler = {
		jsEvent: Event ->
		if (jsEvent is KeyboardEvent) {
			charEvent.clear()
			charEvent.char = jsEvent.charCode.toChar()
			char.dispatch(charEvent)
			if (charEvent.defaultPrevented()) jsEvent.preventDefault()
		}
	}

	private val rootBlurHandler = {
		jsEvent: Event ->
		downMap.clear()
	}

	init {
		window.addEventListener("keydown", keyDownHandler)
		window.addEventListener("keyup", keyUpHandler)
		window.addEventListener("keypress", keyPressHandler)
		window.addEventListener("blur", rootBlurHandler)
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
			downMap[keyCode]?.isNotEmpty() ?: false
		} else {
			downMap[keyCode, location] ?: false
		}
	}

	override fun dispose() {
		window.removeEventListener("keydown", keyDownHandler)
		window.removeEventListener("keyup", keyUpHandler)
		window.removeEventListener("keypress", keyPressHandler)
		window.removeEventListener("blur", rootBlurHandler)
		keyDown.dispose()
		keyUp.dispose()
		char.dispose()
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

