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

import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.ComponentInit
import com.acornui.component.UiComponent
import com.acornui.css.toLengthOrNull
import com.acornui.string.toHyphenCase
import org.intellij.lang.annotations.Language
import org.w3c.dom.*
import kotlin.browser.document
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <reified T : Element> createElement(localName: String, init: ComponentInit<T> = {}): T {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val ele = document.createElement(localName).unsafeCast<T>()
	ele.apply(init)
	return ele
}

inline fun styleElement(init: ComponentInit<HTMLStyleElement> = {}): HTMLStyleElement =
	createElement("style", init)

/**
 * Creates an [HTMLLinkElement]. This should be added to the [head].
 */
inline fun linkElement(href: String, rel: String, init: ComponentInit<HTMLLinkElement> = {}): HTMLLinkElement =
	createElement("link") {
		this.href = href
		this.rel = rel
		init()
	}

fun addCssToHead(@Language("CSS") css: String, priority: Double = 0.0): HTMLStyleElement {
	val ele = styleElement {
		asDynamic().priority = priority
		innerHTML = css
	}
	val priorities = head.childNodes.asList().map { (it.asDynamic().priority as Double?) ?: 0.0 }
	val index = priorities.sortedInsertionIndex(priority)
	head.add(index, ele)
	return ele
}

val head: HTMLHeadElement
	get() {
		if (document.head == null)
			document.appendChild(document.createElement("head").unsafeCast<HTMLHeadElement>())
		return document.head!!
	}

val body: HTMLElement
	get() {
		if (document.body == null)
			document.appendChild(document.createElement("body").unsafeCast<HTMLBodyElement>())
		return document.body!!
	}
