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

import com.acornui.focus.FocusManager
import com.acornui.input.interaction.KeyEvent
import com.acornui.input.interaction.KeyEventRo
import com.acornui.input.interaction.set
import com.acornui.signal.Signal0
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document

class SoftKeyboardManagerImpl(
		private val focusManager: FocusManager,
		private val interactivityManager: InteractivityManager,
		private val root: HTMLElement
) : SoftKeyboardManager {

	override fun create(): SoftKeyboard? = SoftKeyboardImpl(root, interactivityManager)

}

class SoftKeyboardImpl(private val root: HTMLElement, private val interactivityManager: InteractivityManager) : SoftKeyboard {

	private val _input = Signal0()
	override val input = _input.asRo()
	private val _selectionChanged = Signal0()

	override val selectionChanged = _selectionChanged.asRo()

	private var isFocused: Boolean = false

	private val hiddenInput: HTMLInputElement
	private val keyEvent = KeyEvent()
	private val delegatedKeys = listOf(Ascii.ESCAPE, Ascii.TAB, Ascii.ENTER, Ascii.RETURN)

	init {
		hiddenInput = document.createElement("input") as HTMLInputElement

		hiddenInput.apply {
			type = "text"
			tabIndex = -1
			style.position = "absolute"
			style.opacity = "0"
			style.asDynamic().pointerEvents = "none"
			style.zIndex = "0"

			// hide native blue text cursor on iOS
			style.transform = "scale(0)"

			// Delegate key events that need to be handled by the canvas.

			val options = js("{}")
			options["capture"] = true
			options["passive"] = false

			addEventListener("keydown", { e ->
				delegateEvent(e as KeyboardEvent, KeyEventRo.KEY_DOWN)
			}, options)

			addEventListener("keyup", { e ->
				delegateEvent(e as KeyboardEvent, KeyEventRo.KEY_UP)
			}, options)

			addEventListener("input", ::inputHandler)
		}
		document.body?.appendChild(hiddenInput)
	}

	private fun delegateEvent(e: KeyboardEvent, type: EventType<EventRo>) {
		if (delegatedKeys.contains(e.keyCode)) {
			e.preventDefault()
			val focused = interactivityManager.activeElement
			keyEvent.set(e)
			keyEvent.type = type
			interactivityManager.dispatch(keyEvent, focused)
		}
	}

	override var type: String
		get() = hiddenInput.type
		set(value) {
			hiddenInput.type = value
		}

	override var text: String
		get() = hiddenInput.value
		set(value) {
			hiddenInput.value = value
		}

	override fun focus() {
		if (isFocused) return

		isFocused = true
		focused = hiddenInput
		hiddenInput.focus()
		updateSelection()
		document.addEventListener("selectionchange", ::selectionChangeHandler)
		hiddenInput.addEventListener("blur", ::preventBlur, true)
	}

	override fun blur() {
		if (!isFocused) return
		isFocused = false
		focused = null
		document.removeEventListener("selectionchange", ::selectionChangeHandler)
		hiddenInput.removeEventListener("blur", ::preventBlur, true)

		root.focus()
	}

	private fun preventBlur(e: Event) {
		e.preventDefault()
		hiddenInput.focus()
	}

	private fun inputHandler(e: Event) {
		_input.dispatch()
	}

	private fun selectionChangeHandler(e: Event) {
		val previousStart = selectionStart
		val previousEnd = selectionEnd
		selectionStart = if (hiddenInput.selectionDirection == "backward") hiddenInput.selectionEnd
				?: 0 else hiddenInput.selectionStart ?: 0
		selectionEnd = if (hiddenInput.selectionDirection == "backward") hiddenInput.selectionStart
				?: 0 else hiddenInput.selectionEnd ?: 0
		if (previousStart != selectionStart || previousEnd != selectionEnd)
			_selectionChanged.dispatch()
	}

	override var selectionStart = 0

	override var selectionEnd = 0

	override fun setSelectionRange(selectionStart: Int, selectionEnd: Int) {
		this.selectionStart = selectionStart
		this.selectionEnd = selectionEnd
		if (isFocused)
			updateSelection()
	}

	/**
	 * May only be done while focused.
	 */
	private fun updateSelection() {
		hiddenInput.setSelectionRange(minOf(selectionStart, selectionEnd), maxOf(selectionStart, selectionEnd), if (selectionEnd >= selectionStart) "forward" else "backward")
	}

	override fun position(x: Float, y: Float) {
		val toX = x.toInt() + root.offsetLeft
		val toY = y.toInt() + root.offsetTop
		hiddenInput.style.left = "${toX}px"
		hiddenInput.style.top = "${toY + 20}px"
	}

	// TODO: Canvas position / size

	override fun dispose() {
		if (isFocused) blur()
		_input.dispose()
		_selectionChanged.dispose()
		document.body?.removeChild(hiddenInput)
	}

	companion object {
		private var focused: HTMLInputElement? = null
	}
}
