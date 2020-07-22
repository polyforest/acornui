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

package com.acornui.signal

import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

class EventSignal<T : Event>(private val host: EventTarget, private val eventName: String) : Signal<T> {

	override fun listen(isOnce: Boolean, handler: (T) -> Unit): SignalSubscription {
		return listen(EventOptions(isOnce = isOnce), handler = handler)
	}

	fun listen(eventOptions: EventOptions, handler: (T) -> Unit): SignalSubscription =
		EventSignalSubscriptionImpl(host, eventName, eventOptions, handler)
}

private class EventSignalSubscriptionImpl<T : Event>(
	private val host: EventTarget,
	private val eventName: String,
	private val eventOptions: EventOptions,
	private val handler: (T) -> Unit
) : SignalSubscription {

	override var isPaused: Boolean = false
	override val isOnce: Boolean
		get() = eventOptions.isOnce

	private val handlerOuter = { event: T ->
		if (!isPaused) {
			if (isOnce)
				dispose()
			handler.invoke(event)
		}
	}

	fun invoke(data: T) {
		handlerOuter(data)
	}

	private fun listen() {
		@Suppress("UNCHECKED_CAST")
		host.addEventListener(eventName, handlerOuter as ((Event) -> Unit), eventOptions)
	}

	private fun unlisten() {
		@Suppress("UNCHECKED_CAST")
		host.removeEventListener(eventName, handlerOuter as ((Event) -> Unit), eventOptions)
	}

	init {
		listen()
	}

	override fun dispose() {
		unlisten()
	}
}

data class EventOptions(

	@JsName("passive")
	val isPassive: Boolean = true,

	@JsName("capture")
	val isCapture: Boolean = false,

	@JsName("once")
	val isOnce: Boolean = false
)

interface WithEventTarget {

	/**
	 * The dom element this component controls.
	 */
	val eventTarget: EventTarget
}

fun EventTarget.asWithEventTarget(): WithEventTarget = object : WithEventTarget {
	override val eventTarget: EventTarget = this@asWithEventTarget
}

fun <T : Event> WithEventTarget.event(name: String) = EventSignal<T>(eventTarget, name)
fun <T : Event> EventTarget.event(name: String) = EventSignal<T>(this, name)