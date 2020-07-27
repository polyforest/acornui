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

import com.acornui.component.ComponentInit
import com.acornui.component.UiComponentImpl
import com.acornui.component.WithNode
import com.acornui.component.asWithNode
import com.acornui.di.Context
import org.w3c.dom.*
import kotlin.browser.document
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <T : HTMLElement> Context.component(localName: String, init: ComponentInit<UiComponentImpl<T>> = {}): UiComponentImpl<T> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return UiComponentImpl(this, createElement<Element>(localName).unsafeCast<T>()).apply(init)
}

inline fun Context.div(init: ComponentInit<UiComponentImpl<HTMLDivElement>> = {}): UiComponentImpl<HTMLDivElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("div", init)
}

inline fun Context.a(href: String = "", target: String = "", init: ComponentInit<UiComponentImpl<HTMLAnchorElement>> = {}): UiComponentImpl<HTMLAnchorElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("a") {
		dom.href = href
		dom.target = target
		init()
	}
}

inline fun Context.br(init: ComponentInit<UiComponentImpl<HTMLBRElement>> = {}): UiComponentImpl<HTMLBRElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("br", init)
}

inline fun Context.hr(init: ComponentInit<UiComponentImpl<HTMLHRElement>> = {}): UiComponentImpl<HTMLHRElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("hr", init)
}

inline fun Context.ul(init: ComponentInit<UiComponentImpl<HTMLUListElement>> = {}): UiComponentImpl<HTMLUListElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("ul", init)
}

inline fun Context.ol(init: ComponentInit<UiComponentImpl<HTMLOListElement>> = {}): UiComponentImpl<HTMLOListElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("ol", init)
}

inline fun Context.li(init: ComponentInit<UiComponentImpl<HTMLLIElement>> = {}): UiComponentImpl<HTMLLIElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("li", init)
}

inline fun Context.form(init: ComponentInit<UiComponentImpl<HTMLFormElement>> = {}): UiComponentImpl<HTMLFormElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("form", init)
}

/**
 * Creates a [DocumentFragment].
 */
inline fun fragment(init: ComponentInit<WithNode> = {}): WithNode =
	document.createDocumentFragment().asWithNode().apply(init)