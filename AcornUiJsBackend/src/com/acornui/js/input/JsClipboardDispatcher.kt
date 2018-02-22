/*
 * Copyright 2018 Nicholas Bilyk
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

import com.acornui.component.Stage
import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.core.focus.FocusManager
import com.acornui.core.input.InteractionEventBase
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.interaction.ClipboardItemType
import com.acornui.core.input.interaction.CopyInteractionRo
import com.acornui.core.input.interaction.PasteInteractionRo
import com.acornui.js.html.ClipboardEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.browser.window


class JsClipboardDispatcher(
		private val rootElement: HTMLElement,
		override val injector: Injector
) : Scoped, Disposable {

	private val interactivity = inject(InteractivityManager)
	private val focus = inject(FocusManager)
	private val stage = inject(Stage)

	private val pasteEvent = JsPasteInteraction()
	private val copyEvent = JsCopyInteraction()

	private val pasteHandler: (Event) -> dynamic = {
		pasteEvent.clear()
		pasteEvent.type = PasteInteractionRo.PASTE
		pasteEvent.set(it as ClipboardEvent)
		interactivity.dispatch(focus.focused() ?: stage, pasteEvent)
		it.preventDefault()
		Unit
	}

	private val copyHandler: (Event) -> dynamic = {
		copyEvent.clear()
		copyEvent.type = CopyInteractionRo.COPY
		copyEvent.set(it as ClipboardEvent)
		interactivity.dispatch(focus.focused() ?: stage, copyEvent)
		it.preventDefault()
		Unit
	}

	private val cutHandler: (Event) -> dynamic = {
		copyEvent.clear()
		copyEvent.type = CopyInteractionRo.CUT
		copyEvent.set(it as ClipboardEvent)
		interactivity.dispatch(focus.focused() ?: stage, copyEvent)
		it.preventDefault()
		Unit
	}

	init {
		window.addEventListener("paste", pasteHandler, true)
		window.addEventListener("cut", cutHandler, true)
		window.addEventListener("copy", copyHandler, true)
	}

	override fun dispose() {
		window.removeEventListener("paste", pasteHandler, true)
		window.removeEventListener("cut", cutHandler, true)
		window.removeEventListener("copy", copyHandler, true)
	}
}

private class JsPasteInteraction : InteractionEventBase(), PasteInteractionRo {

	private var jsEvent: ClipboardEvent? = null

	@Suppress("UNCHECKED_CAST")
	override suspend fun <T : Any> getItemByType(type: ClipboardItemType<T>): T? {
		val jsEvent = jsEvent ?: return null
		return when (type) {
			ClipboardItemType.PLAIN_TEXT -> {
				jsEvent.clipboardData?.getData("text/plain")
			}
			ClipboardItemType.HTML -> {
				jsEvent.clipboardData?.getData("text/html")
			}
			ClipboardItemType.TEXTURE ->  {
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
			ClipboardItemType.TEXTURE ->  {
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