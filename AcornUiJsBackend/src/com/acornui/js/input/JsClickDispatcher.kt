package com.acornui.js.input

import com.acornui.core.di.Injector
import com.acornui.core.input.interaction.ClickDispatcher
import org.w3c.dom.HTMLElement

/**
 * An implementation of ClickDispatcher that doesn't fire the click event until the browser fires the click event.
 */
class JsClickDispatcher(
		private val rootElement: HTMLElement,
		injector: Injector
) : ClickDispatcher(injector) {

	init {
		rootElement.addEventListener("click", fireHandler, true)
	}

	override fun dispose() {
		super.dispose()
		rootElement.removeEventListener("click", fireHandler, true)
	}
}