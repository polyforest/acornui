package com.acornui.js.dom

import com.acornui.component.Stage
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.core.input.InteractionEvent
import com.acornui.core.input.InteractionEventRo
import com.acornui.core.input.InteractionType
import com.acornui.core.input.interaction.KeyInteractionRo
import com.acornui.core.input.interaction.MouseInteractionRo
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
			val target = findComponentFromDom(it.target, host)
			if (target != null) {
				val returnVal = handler(it)
				event.type = type
				event.target = target
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
		} else {
			Unit
		}
	}

	private val windowEvents = arrayOf(MouseInteractionRo.MOUSE_DOWN, MouseInteractionRo.MOUSE_UP, MouseInteractionRo.MOUSE_MOVE, MouseInteractionRo.MOUSE_OUT, MouseInteractionRo.MOUSE_UP, KeyInteractionRo.KEY_DOWN, KeyInteractionRo.KEY_UP)

	init {
		element = if (host is Stage && windowEvents.contains(type)) {
			window
		} else {
			val native = (host as UiComponent).native as DomComponent
			native.element
		}
		element.addEventListener(jsType, wrappedHandler, isCapture)
	}

	override fun dispose() {
		super.dispose()
		element.removeEventListener(jsType, wrappedHandler, isCapture)
	}
}