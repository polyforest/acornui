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

package com.acornui.core.input

import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.input.interaction.*
import com.acornui.signal.Signal

/**
 * @author nbilyk
 */
interface KeyState : Disposable {

	/**
	 * Returns true if the given key is currently pressed.
	 */
	fun keyIsDown(keyCode: Int, location: KeyLocation = KeyLocation.UNKNOWN): Boolean

	companion object : DKey<KeyState>
}

/**
 * The raw key input. This will only be key input from the system, and never fabricated events.
 * Components shouldn't use this directly, but instead use the the events from the [InteractivityManager].
 *
 * @see com.acornui.core.input.keyDown
 * @see com.acornui.core.input.keyUp
 * @see com.acornui.core.input.char
 */
interface KeyInput : KeyState {

	/**
	 * Dispatched when the user has pressed down a key
	 * Do not keep a reference to this event, it will be recycled.
	 */
	val keyDown: Signal<(KeyInteractionRo) -> Unit>

	/**
	 * Dispatched when the user has released a key
	 * Do not keep a reference to this event, it will be recycled.
	 */
	val keyUp: Signal<(KeyInteractionRo) -> Unit>

	/**
	 * Dispatched when the user has inputted a character.
	 * Do not keep a reference to this event, it will be recycled.
	 */
	val char: Signal<(CharInteractionRo) -> Unit>

	companion object : DKey<KeyInput> {
		override val extends: DKey<*>? = KeyState
	}
}
