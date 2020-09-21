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

package com.acornui.dom

import com.acornui.component.UiComponent
import com.acornui.signal.Signal
import com.acornui.signal.SignalImpl
import com.acornui.signal.SignalSubscription
import com.acornui.signal.filtered
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.Event

object ComputedStyleChangedSignal : SignalImpl<Unit>() {

	/**
	 * If a style element changes its content.
	 */
	private val styleContentsObserver = MutationObserver { mutations, _ ->
		dispatch(Unit)
	}

	private val targetObserver = MutationObserver { mutations, _ ->

		if (mutations.any { mutationRecord ->
			when (mutationRecord.type) {
				"attributes" -> true
				"childList" -> {
					var shouldDispatch = false

					mutationRecord.addedNodes.asList().forEach { addedNode ->
						if (addedNode.nodeName == "STYLE") {
							observeStyleElement(addedNode.unsafeCast<HTMLStyleElement>())
							shouldDispatch = true
						} else if (addedNode.nodeName == "LINK") {
							observeLinkElement(addedNode.unsafeCast<HTMLLinkElement>())
							shouldDispatch = true
						}
					}
					shouldDispatch
				}
				else -> false
			}
		}) {
			dispatch(Unit)
		}
	}

	private fun observeStyleElement(it: HTMLStyleElement) {
		styleContentsObserver.observe(it, MutationObserverInit(
			childList = true,
			subtree = true,
			attributes = true,
			characterData = true
		))
	}

	private fun observeLinkElement(it: HTMLLinkElement) {
		it.addEventListener("load", ::loadedHandler)
	}

	private fun loadedHandler(e: Event) {
		dispatch(Unit)
	}

	override fun removeSubscription(subscription: SignalSubscription) {
		super.removeSubscription(subscription)
		if (isEmpty())
			disconnect()
	}

	override fun listen(isOnce: Boolean, handler: (Unit) -> Unit): SignalSubscription {
		if (isEmpty())
			connect()
		return super.listen(isOnce, handler)
	}

	private fun connect() {
		document.getElementsByTagName("STYLE").asList().forEach {
			observeStyleElement(it.unsafeCast<HTMLStyleElement>())
		}
		document.getElementsByTagName("LINK").asList().forEach {
			observeLinkElement(it.unsafeCast<HTMLLinkElement>())
		}
		targetObserver.observe(document, MutationObserverInit(
			childList = true,
			subtree = true,
			attributes = true,
			attributeFilter = arrayOf("style", "class", "src")
		))
	}

	private fun disconnect() {
		targetObserver.disconnect()
		styleContentsObserver.disconnect()
		document.getElementsByTagName("LINK").asList().forEach {
			it.removeEventListener("load", ::loadedHandler)
		}
	}

}

/**
 * Dispatched when the computed style of this component with the given name has changed.
 */
fun UiComponent.computedStyleChanged(property: String): Signal<Unit> {
	val computed = window.getComputedStyle(dom)
	var value = computed.getPropertyValue(property)
	return ComputedStyleChangedSignal.filtered {
		val newValue = computed.getPropertyValue(property)
		if (newValue != value) {
			value = newValue
			true
		} else {
			false
		}
	}
}