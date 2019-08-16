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

import com.acornui.di.Injector
import com.acornui.input.WhichButton
import com.acornui.input.interaction.ClickDispatcher
import com.acornui.input.interaction.ClickInteractionRo
import com.acornui.time.nowMs
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

/**
 * An implementation of ClickDispatcher that doesn't fire the click event until the browser fires the click event.
 */
class JsClickDispatcher(
		private val rootElement: HTMLElement,
		injector: Injector
) : ClickDispatcher(injector) {

	init {
		rootElement.addEventListener("click", ::clickHandler, true)
		rootElement.addEventListener("contextmenu", ::contextMenuHandler, true)
	}

	override fun dispose() {
		super.dispose()
		rootElement.removeEventListener("click", ::clickHandler, true)
		rootElement.removeEventListener("contextmenu", ::contextMenuHandler, true)
	}

	private fun clickHandler(event: Event) {
		if (event.defaultPrevented)
			return

		if (fireClickEvent()) {
			if (clickEvent.defaultPrevented()) {
				event.preventDefault()
			}
		}
	}

	private fun contextMenuHandler(jsEvent: Event) {
		jsEvent as MouseEvent
		if (jsEvent.defaultPrevented)
			return

		val canvasX = jsEvent.pageX.toFloat() - rootElement.offsetLeft.toFloat()
		val canvasY = jsEvent.pageY.toFloat() - rootElement.offsetTop.toFloat()
		release(WhichButton.LEFT, canvasX, canvasY, nowMs(), true)
		clickEvent.button = WhichButton.RIGHT
		clickEvent.type = ClickInteractionRo.RIGHT_CLICK
		if (fireClickEvent()) {
			// If anything is listening to right-click, prevent default to stop the default context webgl menu.
			if (clickEvent.handled || clickEvent.defaultPrevented())
				jsEvent.preventDefault()
		}
	}
}
