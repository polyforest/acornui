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
import com.acornui.core.input.interaction.PasteInteractionRo
import com.acornui.core.input.interaction.ClipboardItemType
import com.acornui.js.html.ClipboardEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.DataTransfer
import org.w3c.dom.DataTransferItemList
import org.w3c.dom.get
import org.w3c.files.FileList
import org.w3c.files.get
import kotlin.browser.window


class JsClipboardDispatcher(
		private val rootElement: HTMLElement,
		override val injector: Injector
) : Scoped, Disposable {

	private val interactivity = inject(InteractivityManager)
	private val focus = inject(FocusManager)
	private val stage = inject(Stage)

	private val pasteEvent = JsPasteInteraction()

	private val pasteHandler: (Event) -> dynamic = {
		pasteEvent.set(it as ClipboardEvent)
		interactivity.dispatch(focus.focused() ?: stage, pasteEvent)
		Unit
	}

	init {
		pasteEvent.type = PasteInteractionRo.PASTE
		window.addEventListener("paste", pasteHandler, true)
	}

	override fun dispose() {
		window.removeEventListener("paste", pasteHandler, true)
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