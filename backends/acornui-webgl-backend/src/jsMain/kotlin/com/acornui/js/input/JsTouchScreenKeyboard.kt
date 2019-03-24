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

package com.acornui.js.input

import com.acornui.core.Disposable
import com.acornui.core.input.TouchScreenKeyboard
import com.acornui.core.input.TouchScreenKeyboardRef
import com.acornui.core.input.TouchScreenKeyboardType
import com.acornui.js.html.hide
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import kotlin.browser.document

class JsTouchScreenKeyboard(private val rootElement: HTMLElement, private val canvas: HTMLElement) : TouchScreenKeyboard, Disposable {

	private val input = document.createElement("input") as HTMLInputElement

	private val stack = ArrayList<TouchScreenKeyboardSettings>()

	init {
		input.hide()
		rootElement.appendChild(input)
		input.onkeydown = {
			println("Key down")
		}

		input.onkeyup = {
			println("Key up")
		}
		input.onkeypress = {
			println("Key press ${it.charCode}")
		}

	}

	override fun open(type: TouchScreenKeyboardType): TouchScreenKeyboardRef {
		val settings = TouchScreenKeyboardSettings(type)
		stack.add(settings)
		refresh()
		return TouchScreenKeyboardRefImpl(settings)
	}

	private fun refresh() {
		val settings = stack.lastOrNull()
		if (settings != null) {
			input.inputMode = when (settings.type) {
				TouchScreenKeyboardType.DEFAULT -> "text"
				TouchScreenKeyboardType.DECIMAL -> "decimal"
				TouchScreenKeyboardType.NUMERIC -> "numeric"
				TouchScreenKeyboardType.TEL -> "tel"
				TouchScreenKeyboardType.SEARCH -> "search"
				TouchScreenKeyboardType.EMAIL -> "email"
				TouchScreenKeyboardType.URL -> "url"
			}
			input.focus()
		} else {
			canvas.focus()
		}
	}

	private inner class TouchScreenKeyboardRefImpl(val settings: TouchScreenKeyboardSettings) : TouchScreenKeyboardRef {
		override fun close() {
			stack.remove(settings)
			refresh()
		}
	}

	private class TouchScreenKeyboardSettings(val type: TouchScreenKeyboardType)

	override fun dispose() {
		rootElement.removeChild(input)
	}
}

