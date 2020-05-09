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

package com.acornui.input

import com.acornui.Disposable
import com.acornui.component.ComponentInit
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.math.Vector3Ro
import com.acornui.signal.Signal

interface SoftKeyboardManager {

	fun create(): SoftKeyboard

	companion object : Context.Key<SoftKeyboardManager>
}

interface SoftKeyboard : Disposable {

	/**
	 * Dispatched after the user has input changes to the text.
	 */
	val input: Signal<() -> Unit>
	val selectionChanged: Signal<() -> Unit>

	/**
	 * The type of virtual keyboard.
	 * @see SoftKeyboardType
	 */
	var type: String

	var text: String

	val selectionStart: Int
	val selectionEnd: Int

	fun setSelectionRange(selectionStart: Int, selectionEnd: Int)

	/**
	 * Sets the canvas position for the input. This is so the canvas is scrolled to the correct
	 * position when the keyboard opens.
	 */
	fun position(x: Float, y: Float)

	fun focus()
	fun blur()
}

fun SoftKeyboard.position(value: Vector3Ro) = position(value.x, value.y)

/**
 * Constructs and owns a new soft keyboard.
 * If the back-end doesn't support soft keyboards, null will be returned.
 */
inline fun Context.softKeyboard(init: ComponentInit<SoftKeyboard> = {}): SoftKeyboard? {
	//contract { callsInPlace(init, InvocationKind.AT_MOST_ONCE) }
	val k = own(injectOptional(SoftKeyboardManager)?.create())
	k?.init()
	return k
}

object SoftKeyboardType {

	/**
	 * Standard text input keyboard for the user's current locale.
	 */
	const val DEFAULT = "default"

	/**
	 * Fractional numeric input keyboard containing the digits and the appropriate separator character for the user's
	 * locale (typically either "." or ",").
	 */
	const val DECIMAL = "decimal"

	/**
	 * Numeric input keyboard; all that is needed are the digits 0 through 9.
	 */
	const val NUMERIC = "numeric"

	/**
	 * A telephone keypad input, including the digits 0 through 9, the asterisk ("*"), and the pound ("#") key.
	 */
	const val TEL = "tel"

	/**
	 * A virtual keyboard optimized for search input. For instance, the return key may be re-labeled "Search", and
	 * there may be other optimizations.
	 */
	const val SEARCH = "search"

	/**
	 * A virtual keyboard optimized for entering email addresses; typically this includes the "@" character as well as
	 * other optimizations.
	 */
	const val EMAIL = "email"

	/**
	 * A keypad optimized for entering URLs. This may have the "/" key more prominently available, for example.
	 */
	const val URL = "url"

	/**
	 * A keypad optimized for entering passwords.
	 */
	const val PASSWORD = "password"
}

val Context.touchScreenKeyboard: SoftKeyboardManager
	get() = inject(SoftKeyboardManager)