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
import com.acornui.component.DivComponent
import com.acornui.component.Labelable
import com.acornui.component.UiComponentImpl
import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import com.acornui.dom.createElement
import org.w3c.dom.HTMLDivElement
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface TextField : Labelable {

	/**
	 * Sets this text field's contents to a simple text flow.
	 */
	var text: String

	/**
	 * Sets [text].
	 */
	override var label: String
		get() = text
		set(value) {
			text = value
		}
}

class TextFieldImpl(owner: Context) : TextField, UiComponentImpl<HTMLDivElement>(owner, createElement("span")) {

	init {
		addClass(styleTag)
	}

	override var text: String
		get() = dom.innerText
		set(value) {
			dom.innerText = value
		}

	companion object {
		val styleTag = StyleTag("TextFieldImpl")
	}
}

/**
 * Creates a [TextField] span.
 * @param text The initial text to set.
 * @param init The initializer block.
 */
inline fun Context.text(text: String = "", init: ComponentInit<TextFieldImpl> = {}): TextFieldImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.text = text
	t.init()
	return t
}