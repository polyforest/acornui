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

package com.acornui.core.input

import com.acornui.core.Disposable
import com.acornui.core.di.DKey
import com.acornui.core.input.interaction.CharInteraction
import com.acornui.core.input.interaction.KeyInteraction
import com.acornui.core.input.interaction.KeyLocation
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

interface KeyInput : KeyState {

	/**
	 * Dispatched when the user has pressed down a key
	 * Do not keep a reference to this event, it will be recycled.
	 */
	val keyDown: Signal<(KeyInteraction) -> Unit>

	/**
	 * Dispatched when the user has released a key
	 * Do not keep a reference to this event, it will be recycled.
	 */
	val keyUp: Signal<(KeyInteraction) -> Unit>

	/**
	 * Dispatched when the user has inputted a character.
	 * Do not keep a reference to this event, it will be recycled.
	 */
	val char: Signal<(CharInteraction) -> Unit>

	companion object : DKey<KeyInput> {
		override val extends: DKey<*>? = KeyState
	}
}