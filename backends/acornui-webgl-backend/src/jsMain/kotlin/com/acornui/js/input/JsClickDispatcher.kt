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

import com.acornui.core.di.Injector
import com.acornui.core.input.interaction.ClickDispatcher
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

/**
 * An implementation of ClickDispatcher that doesn't fire the click event until the browser fires the click event.
 */
class JsClickDispatcher(
		private val rootElement: HTMLElement,
		injector: Injector
) : ClickDispatcher(injector) {

	private val contextMenuFireHandler = {
		event: Event ->
		if (fireClickEvent()) {
			if (clickEvent.defaultPrevented()) {
				event.preventDefault()
			}
		}
		Unit
	}

	init {
		rootElement.addEventListener("click", fireHandler, true)
		rootElement.addEventListener("contextmenu", contextMenuFireHandler, true)
	}

	override fun dispose() {
		super.dispose()
		rootElement.removeEventListener("click", fireHandler, true)
		rootElement.removeEventListener("contextmenu", contextMenuFireHandler, true)
	}
}
