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

package com.acornui.component

import com.acornui.dom.visible
import com.acornui.signal.Signal
import com.acornui.signal.SignalImpl
import com.acornui.signal.filtered
import com.acornui.signal.map
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import kotlinx.browser.document

/**
 * Dispatched when this component's [UiComponent.dom] element has been added to the document.
 * This will create a Mutation observer for the document, which will be disconnected when this component is disposed.
 */
val UiComponent.connected: Signal<Unit>
	get() = createOrReuseAttachment("connected") {
		isConnectedChanged.filtered { it }.map { Unit }
	}

/**
 * Dispatched when this component's [UiComponent.dom] element has been removed from the document.
 * This will create a Mutation observer for the document, which will be disconnected when this component is disposed.
 */
val UiComponent.disconnected: Signal<Unit>
	get() = createOrReuseAttachment("disconnected") {
		isConnectedChanged.filtered { !it }.map { Unit }
	}

/**
 * Dispatched when this component's [UiComponent.dom] element's [org.w3c.dom.Node.isConnected] property has changed.
 *
 * The isConnected read-only property of the Node interface returns a boolean indicating whether the node is connected
 * (directly or indirectly) to the context object, for example the Document object in the case of the normal DOM, or the
 * ShadowRoot in the case of a shadow DOM.
 * [https://developer.mozilla.org/en-US/docs/Web/API/Node/isConnected]
 */
val UiComponent.isConnectedChanged: Signal<Boolean>
	get() {
		return createOrReuseAttachment<Signal<Boolean>>("isConnectedChanged") {
			object : SignalImpl<Boolean>() {

				private val observer: MutationObserver
				private var currentIsConnected = dom.isConnected

				init {
					observer = MutationObserver { _, _ ->
						if (currentIsConnected != dom.isConnected) {
							currentIsConnected = dom.isConnected
							dispatch(currentIsConnected)
						}
					}
					observer.observe(document, MutationObserverInit(childList = true, subtree = true))
				}

				override fun dispose() {
					super.dispose()
					observer.disconnect()
				}
			}
		}
	}

/**
 * Sets the style.display to "none" if visible is false, otherwise sets the display to [visibleDisplay].
 */
fun UiComponent.visible(value: Boolean, visibleDisplay: String? =  null) {
	dom.visible(value, visibleDisplay)
}

/**
 * Sets `visible(false)`
 * @see visible
 */
fun UiComponent.hide() = visible(false)

/**
 * Sets `visible(true)`
 * @see visible
 */
fun UiComponent.show() = visible(true)