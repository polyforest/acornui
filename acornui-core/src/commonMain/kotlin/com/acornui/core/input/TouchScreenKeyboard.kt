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

import com.acornui.core.di.DKey
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject

interface TouchScreenKeyboard {

	fun open(type: TouchScreenKeyboardType): TouchScreenKeyboardRef

	companion object : DKey<TouchScreenKeyboard>
}

interface TouchScreenKeyboardRef {
	fun close()
}

enum class TouchScreenKeyboardType {

	/**
	 * Standard text input keyboard for the user's current locale.
	 */
	DEFAULT,

	/**
	 * Fractional numeric input keyboard containing the digits and the appropriate separator character for the user's
	 * locale (typically either "." or ",").
	 */
	DECIMAL,

	/**
	 * Numeric input keyboard; all that is needed are the digits 0 through 9.
	 */
	NUMERIC,

	/**
	 * A telephone keypad input, including the digits 0 through 9, the asterisk ("*"), and the pound ("#") key.
	 */
	TEL,

	/**
	 * A virtual keyboard optimized for search input. For instance, the return key may be re-labeled "Search", and
	 * there may be other optimizations.
	 */
	SEARCH,

	/**
	 * A virtual keyboard optimized for entering email addresses; typically this includes the "@" character as well as
	 * other optimizations.
	 */
	EMAIL,

	/**
	 * A  keypad optimized for entering URLs. This may have the "/" key more prominently available, for example.
	 */
	URL
}

val Scoped.touchScreenKeyboard: TouchScreenKeyboard
	get() = inject(TouchScreenKeyboard)