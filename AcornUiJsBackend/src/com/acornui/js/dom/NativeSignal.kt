package com.acornui.js.dom

import com.acornui.component.*
import com.acornui.core.input.InteractionEvent
import com.acornui.core.input.InteractionEventRo
import com.acornui.core.input.InteractionType
import com.acornui.core.input.interaction.KeyInteraction
import com.acornui.core.input.interaction.MouseInteraction
import com.acornui.js.dom.component.DomComponent
import com.acornui.js.html.findComponentFromDom
import com.acornui.signal.StoppableSignalImpl
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import kotlin.browser.window

class NativeSignal<T : InteractionEventRo>(
		private val host: UiComponentRo,
		private val jsType: String,
		private val isCapture: Boolean,
		private val type: InteractionType<InteractionEventRo>,
		private val event: InteractionEvent,
		private val handler: (Event) -> dynamic
) : StoppableSignalImpl<T>() {

	private val element: EventTarget

	@Suppress("UNCHECKED_CAST")
	private val wrappedHandler: (Event) -> dynamic = {
		if (host.interactivityEnabled) {
			val returnVal = handler(it)
			event.type = type
			event.target = findComponentFromDom(it.target, host)
			event.localize(host)
			dispatch(event as T)
			if (event.defaultPrevented())
				it.preventDefault()
			if (event.propagation.immediatePropagationStopped())
				it.stopImmediatePropagation()
			else if (event.propagation.propagationStopped())
				it.stopPropagation()

			returnVal
		} else {
			Unit
		}
	}

	private val windowEvents = arrayOf<InteractionType<InteractionEvent>>(MouseInteraction.MOUSE_DOWN, MouseInteraction.MOUSE_UP, MouseInteraction.MOUSE_MOVE, MouseInteraction.MOUSE_OUT, MouseInteraction.MOUSE_UP, KeyInteraction.KEY_DOWN, KeyInteraction.KEY_UP)

	init {
		if (host is Stage && windowEvents.contains(type)) {
			element = window
		} else {
			val native = (host as UiComponent).native as DomComponent
			element = native.element
		}
		element.addEventListener(jsType, wrappedHandler, isCapture)
	}

	override fun dispose() {
		super.dispose()
		element.removeEventListener(jsType, wrappedHandler, isCapture)
	}
}