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

@file:Suppress(
	"INTERFACE_WITH_SUPERCLASS",
	"OVERRIDING_FINAL_MEMBER",
	"RETURN_TYPE_MISMATCH_ON_OVERRIDE",
	"CONFLICTING_OVERLOADS",
	"EXTERNAL_DELEGATION"
)

package com.acornui

import com.acornui.component.UiComponent
import com.acornui.function.as1
import com.acornui.signal.Signal
import com.acornui.signal.SignalSubscription
import org.w3c.dom.DOMRectReadOnly
import org.w3c.dom.Element

open external class ResizeObserver(callback: ResizeObserverCallback) {
	fun disconnect()
	fun observe(target: Element, options: ResizeObserverObserveOptions = definedExternally)
	fun unobserve(target: Element)
}

external interface ResizeObserverObserveOptions {
	var box: String? /* "content-box" | "border-box" */
		get() = definedExternally
		set(value) = definedExternally
}

@Suppress("NOTHING_TO_INLINE", "UnsafeCastFromDynamic")
inline fun resizeObserverObserveOptions(box: String = "content-box"): ResizeObserverObserveOptions {
	val o = js("({})")
	o["box"] = box
	return o
}

typealias ResizeObserverCallback = (entries: Array<ResizeObserverEntry>, observer: ResizeObserver) -> Unit

external interface ResizeObserverEntry {
	var borderBoxSize: ResizeObserverEntryBoxSize
	var contentBoxSize: ResizeObserverEntryBoxSize
	var contentRect: DOMRectReadOnly
	var target: Element
}

external interface ResizeObserverEntryBoxSize {
	var blockSize: Number
	var inlineSize: Number
}

/**
 * Creates a [ResizeObserver] and provides a [Signal] interface to it.
 * If this component is disposed, the observer will be disconnected.
 */
val UiComponent.resize: Signal<Array<ResizeObserverEntry>>
	get() = object : Signal<Array<ResizeObserverEntry>> {
		override fun listen(isOnce: Boolean, handler: (Array<ResizeObserverEntry>) -> Unit): SignalSubscription {
			return object : SignalSubscription {

				override val isOnce: Boolean = isOnce
				override var isPaused: Boolean = false
				private val disposedWatch: Disposable

				private val observer = ResizeObserver { entries, observer ->
					if (!isPaused)
						handler(entries)
					if (isOnce)
						observer.disconnect()
				}

				init {
					observer.observe(this@resize.dom)
					disposedWatch = disposed.listen(::dispose.as1)
				}

				override fun dispose() {
					disposedWatch.dispose()
					observer.disconnect()
				}
			}
		}
	}