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

@file:Suppress("JoinDeclarationAndAssignment")

package com.acornui.input

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window

class SoftKeyboardManagerImpl() : SoftKeyboardManager {

	override fun create(): SoftKeyboard = SoftKeyboardImpl()
}

class SoftKeyboardImpl : SoftKeyboard {

	private var isFocused: Boolean = false
	private val hiddenInput: HTMLInputElement

	init {
		// create the hidden input element
		hiddenInput = document.createElement("input") as HTMLInputElement
		hiddenInput.apply {
			type = "text";
			style.position = "absolute"
			style.opacity = "0"
			style.asDynamic().pointerEvents = "none"
			style.zIndex = "0"

			// hide native blue text cursor on iOS
			style.transform = "scale(0)"

			// setup the keydown listener
			addEventListener("keydown", { e ->
				e as KeyboardEvent

				if (isFocused) {
					// hack to fix touch event bug in iOS Safari
					window.focus();
					focus();

					// continue with the keydown event
					println("Mobile c: " + e.charCode)
					//self.keydown(e, self);
				}
			});
		}
		document.body?.appendChild(hiddenInput)
	}
	override var text: String
		get() = hiddenInput.value ?: ""
		set(value) {
			hiddenInput.value = value
		}

	override fun focus() {
		isFocused = true
//		hiddenInput.focus()
	}

	override fun blur() {
		isFocused = false
//		hiddenInput.blur()
	}

	// TODO: Canvas position / size

	override fun dispose() {
		document.body?.removeChild(hiddenInput)
	}
}
