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

import com.acornui.Disposable
import com.acornui.focus.FocusManager
import com.acornui.input.Clipboard
import com.acornui.input.InteractionEventBase
import com.acornui.input.InteractivityManager
import com.acornui.input.interaction.ClipboardItemType
import com.acornui.input.interaction.CopyInteractionRo
import com.acornui.input.interaction.PasteInteractionRo
import com.acornui.js.html.ClipboardEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import kotlin.browser.document
import kotlin.browser.window

class JsClipboard(
		private val canvas: HTMLElement,
		private val focusManager: FocusManager,
		private val interactivityManager: InteractivityManager,
		captureAllKeyboardInput: Boolean
) : Clipboard, Disposable {

	private val pasteEvent = JsPasteInteraction()
	private val copyEvent = JsCopyInteraction()

	/**
	 * If we are fabricating a copy interaction on a temporary text area via [copy], do not handle
	 * the js copy event.
	 */
	private var ignoreCopyEvent = false

	override fun copy(str: String): Boolean {
		ignoreCopyEvent = true
		val el = document.createElement("textarea") as HTMLTextAreaElement
		el.value = str
		el.setAttribute("readonly", "")
		el.style.position = "absolute"
		el.style.left = "0px"
		el.style.top = "0px"
		document.body?.appendChild(el)
		el.select()

		return try {
			document.execCommand("copy")
		} catch (e: Throwable) {
			false
		} finally {
			document.body?.removeChild(el)
			canvas.focus()
			ignoreCopyEvent = false
		}
	}

	override fun triggerCopy(): Boolean {
		return try {
			document.execCommand("copy")
		} catch (e: Throwable) {
			false
		}
	}

	private val pasteHandler: (Event) -> dynamic = {
		val focused = focusManager.focused
		if (focused != null) {
			pasteEvent.clear()
			pasteEvent.type = PasteInteractionRo.PASTE
			pasteEvent.set(it as ClipboardEvent)
			interactivityManager.dispatch(focused, pasteEvent)
			if (pasteEvent.defaultPrevented()) it.preventDefault()
		}
	}

	private val copyHandler: (Event) -> dynamic = {
		val focused = focusManager.focused
		if (focused != null && !ignoreCopyEvent) {
			copyEvent.clear()
			copyEvent.type = CopyInteractionRo.COPY
			copyEvent.set(it as ClipboardEvent)
			interactivityManager.dispatch(focused, copyEvent)
			if (copyEvent.defaultPrevented()) it.preventDefault()
		}
	}

	private val cutHandler: (Event) -> dynamic = {
		val focused = focusManager.focused
		if (focused != null) {
			copyEvent.clear()
			copyEvent.type = CopyInteractionRo.CUT
			copyEvent.set(it as ClipboardEvent)
			interactivityManager.dispatch(focused, copyEvent)
			if (copyEvent.defaultPrevented()) it.preventDefault()
		}
	}

	private val target: EventTarget = if (captureAllKeyboardInput) window else canvas

	init {
		target.addEventListener("paste", pasteHandler, true)
		target.addEventListener("cut", cutHandler, true)
		target.addEventListener("copy", copyHandler, true)
	}

	override fun dispose() {
		target.removeEventListener("paste", pasteHandler, true)
		target.removeEventListener("cut", cutHandler, true)
		target.removeEventListener("copy", copyHandler, true)
	}
}


private class JsPasteInteraction : InteractionEventBase(), PasteInteractionRo {

	private var jsEvent: ClipboardEvent? = null

	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> getItemByType(type: ClipboardItemType<T>): T? {
		val jsEvent = jsEvent ?: return null
		return when (type) {
			ClipboardItemType.PLAIN_TEXT -> {
				jsEvent.clipboardData?.getData("text/plain")
			}
			ClipboardItemType.HTML -> {
				jsEvent.clipboardData?.getData("text/html")
			}
			ClipboardItemType.TEXTURE -> {
				TODO()
			}
			ClipboardItemType.FILE_LIST -> TODO()

			else -> null
		} as T?
	}

	fun set(clipboardEvent: ClipboardEvent) {
		jsEvent = clipboardEvent
	}

	override fun clear() {
		super.clear()
		jsEvent = null
	}
}


private class JsCopyInteraction : InteractionEventBase(), CopyInteractionRo {

	private var jsEvent: ClipboardEvent? = null

	override fun <T : Any> addItem(type: ClipboardItemType<T>, value: T) {
		val jsEvent = jsEvent ?: return
		when (type) {
			ClipboardItemType.PLAIN_TEXT -> {
				jsEvent.clipboardData?.setData("text/plain", value as String)
			}
			ClipboardItemType.HTML -> {
				jsEvent.clipboardData?.setData("text/html", value as String)
			}
			ClipboardItemType.TEXTURE -> {
				TODO()
			}
			ClipboardItemType.FILE_LIST -> TODO()
		}
	}

	fun set(clipboardEvent: ClipboardEvent) {
		jsEvent = clipboardEvent
	}

	override fun clear() {
		super.clear()
		jsEvent = null
	}
}
