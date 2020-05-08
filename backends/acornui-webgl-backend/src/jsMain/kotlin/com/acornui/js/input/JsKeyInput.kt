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
import com.acornui.input.KeyInput
import com.acornui.input.interaction.*
import com.acornui.signal.Signal1
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import kotlin.collections.isNotEmpty
import kotlin.collections.set

/**
 * @author nbilyk
 */
class JsKeyInput(
		canvas: HTMLElement
) : KeyInput {

	private val _keyDown = Signal1<KeyEventRo>()
	override val keyDown = _keyDown.asRo()
	private val _keyUp = Signal1<KeyEventRo>()
	override val keyUp = _keyUp.asRo()
	private val _char = Signal1<CharEventRo>()
	override val char = _char.asRo()

	private val keyEvent = KeyEvent()
	private val charEvent = CharEvent()

	private val downMap: MutableMultiMap2<Int, KeyLocation, Boolean> = multiMap2()

	private val keyDownHandler = { jsEvent: Event ->
		(jsEvent as KeyboardEvent)
		keyEvent.set(jsEvent)
		keyEvent.type = KeyEventRo.KEY_DOWN
		if (!jsEvent.repeat) {
			downMap[keyEvent.keyCode][keyEvent.location] = true
		}
		_keyDown.dispatch(keyEvent)
		if (keyEvent.defaultPrevented()) jsEvent.preventDefault()

	}

	private val keyUpHandler = { jsEvent: Event ->
		(jsEvent as KeyboardEvent)
		keyEvent.set(jsEvent)
		keyEvent.type = KeyEventRo.KEY_UP
		if (downMap.containsKey(keyEvent.keyCode))
			downMap[keyEvent.keyCode].clear() // Browsers give incorrect key location properties on key up.
		_keyUp.dispatch(keyEvent)
		if (keyEvent.defaultPrevented()) jsEvent.preventDefault()
	}

	private val keyPressHandler = { jsEvent: Event ->
		(jsEvent as KeyboardEvent)
		_char.dispatch(charEvent.set(jsEvent))
		if (charEvent.defaultPrevented()) jsEvent.preventDefault()
	}

	private val blurHandler = { jsEvent: Event ->
		downMap.clear()
	}

	private val eventTarget: EventTarget = canvas

	init {
		val options = js("{}")
		options["capture"] = true
		options["passive"] = false

		eventTarget.addEventListener("keydown", keyDownHandler, options)
		eventTarget.addEventListener("keyup", keyUpHandler, options)
		eventTarget.addEventListener("keypress", keyPressHandler, options)
		eventTarget.addEventListener("blur", blurHandler, options)
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
		eventTarget.removeEventListener("keydown", keyDownHandler, true)
		eventTarget.removeEventListener("keyup", keyUpHandler, true)
		eventTarget.removeEventListener("keypress", keyPressHandler, true)
		eventTarget.removeEventListener("blur", blurHandler, true)
		_keyDown.dispose()
		_keyUp.dispose()
		_char.dispose()
	}
}