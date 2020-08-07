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

package com.acornui.component.text

import com.acornui.component.ComponentInit
import com.acornui.component.Div
 import com.acornui.component.style.cssClass
import com.acornui.di.Context
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class TextField(owner: Context) : Div(owner) {

	init {
		addClass(TextFieldStyles.text)
	}

}
object TextFieldStyles {
	val text by cssClass()
}

/**
 * Creates a [TextField] component.
 * @param text The initial text to set.
 * @param init The initializer block.
 */
inline fun Context.text(text: String = "", init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextField(this)
	t.text = text
	t.init()
	return t
}