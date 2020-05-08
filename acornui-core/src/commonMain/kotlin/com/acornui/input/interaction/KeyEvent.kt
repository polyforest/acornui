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

package com.acornui.input.interaction

import com.acornui.system.Platform
import com.acornui.input.Ascii
import com.acornui.input.EventBase
import com.acornui.input.EventRo
import com.acornui.input.EventType
import com.acornui.system.userInfo

interface KeyEventRo : EventRo {

	/**
	 * The ascii keyCode.
	 * @see Ascii
	 */
	val keyCode: Int

	/**
	 * The location of the key.
	 * If there is only one location for the related key, it will have a location of STANDARD.
	 */
	val location: KeyLocation

	/**
	 * True if the Alt key was active when the KeyboardEvent was generated.
	 */
	val altKey: Boolean

	/**
	 * True if the Control key was active when the KeyboardEvent was generated.
	 */
	val ctrlKey: Boolean

	/**
	 * True if the Meta key, (or Command key on OS X) was active when the KeyboardEvent was generated.
	 * Also called the "super" key.
	 */
	val metaKey: Boolean

	/**
	 * True if the Shift key was active when the KeyboardEvent was generated.
	 */
	val shiftKey: Boolean

	/**
	 * The time (in milliseconds since the Unix Epoch) this key event took place.
	 */
	val timestamp: Long

	/**
	 * If the key was held down, a KEY_DOWN event will repeat, and this will be true
	 */
	val isRepeat: Boolean

	/**
	 * If true, this interaction was triggered from code, not user input.
	 */
	val isFabricated: Boolean

	companion object {
		val KEY_DOWN = EventType<KeyEventRo>("keyDown")
		val KEY_UP = EventType<KeyEventRo>("keyUp")
	}
}

/**
 * Returns true if [KeyEventRo.altKey], [KeyEventRo.ctrlKey], [KeyEventRo.metaKey], or
 * [KeyEventRo.shiftKey] is true.
 */
val KeyEventRo.hasAnyModifier: Boolean
	get() = altKey || ctrlKey || metaKey || shiftKey

/**
 * An event representing an interaction with the keyboard.
 * @author nbilyk
 */
open class KeyEvent : EventBase(), KeyEventRo {

	/**
	 * The ascii keyCode.
	 * @see Ascii
	 */
	override var keyCode: Int = 0

	/**
	 * The location of the key.
	 * If there is only one location for the related key, it will have a location of STANDARD.
	 */
	override var location: KeyLocation = KeyLocation.STANDARD

	/**
	 * True if the Alt key was active when the KeyboardEvent was generated.
	 */
	override var altKey: Boolean = false

	/**
	 * True if the Control key was active when the KeyboardEvent was generated.
	 */
	override var ctrlKey: Boolean = false

	/**
	 * True if the Meta key, (or Command key on OS X) was active when the KeyboardEvent was generated.
	 * Also called the "super" key.
	 */
	override var metaKey: Boolean = false

	/**
	 * True if the Shift key was active when the KeyboardEvent was generated.
	 */
	override var shiftKey: Boolean = false

	/**
	 * The time (in milliseconds since the Unix Epoch) this key event took place.
	 */
	override var timestamp: Long = 0

	/**
	 * If the key was held down, a KEY_DOWN event will repeat, and this will be true
	 */
	override var isRepeat: Boolean = false


	/**
	 * If true, this interaction was triggered from code, not user input.
	 */
	override var isFabricated: Boolean = false

	fun set(other: KeyEventRo) {
		keyCode = other.keyCode
		location = other.location
		altKey = other.altKey
		ctrlKey = other.ctrlKey
		metaKey = other.metaKey
		shiftKey = other.shiftKey
		timestamp = other.timestamp
		isRepeat = other.isRepeat
		isFabricated = other.isFabricated
	}

	fun keyName(): String {
		return Ascii.toString(keyCode) ?: "Unknown"
	}

	override fun clear() {
		super.clear()
		keyCode = 0
		location = KeyLocation.STANDARD
		altKey = false
		ctrlKey = false
		metaKey = false
		shiftKey = false
		timestamp = 0
		isRepeat = false
		isFabricated = false
	}

}

enum class KeyLocation {
	STANDARD,
	LEFT,
	RIGHT,
	NUMBER_PAD,
	UNKNOWN
}

/**
 * The command key on mac os, otherwise, the ctrl key.
 */
val KeyEventRo.commandPlat: Boolean
	get() = if (userInfo.platform == Platform.APPLE) metaKey else ctrlKey

val KeyEventRo.isEnterOrReturn: Boolean
	get() = keyCode == Ascii.ENTER || keyCode == Ascii.RETURN
