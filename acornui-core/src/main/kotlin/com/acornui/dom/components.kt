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

import com.acornui.component.*
import com.acornui.component.style.CommonStyleTags
import com.acornui.di.Context
import com.acornui.signal.event
import org.w3c.dom.*
import kotlinx.browser.document
import org.w3c.dom.events.Event
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

inline fun Context.span(init: ComponentInit<UiComponentImpl<HTMLSpanElement>> = {}): UiComponentImpl<HTMLSpanElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("span", init)
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

open class Img(owner: Context) : UiComponentImpl<Image>(owner, Image()) {

	var alt: String
		get() = dom.alt
		set(value) {
			dom.alt = value
		}

	var src: String
		get() = dom.src
		set(value) {
			dom.src = value
		}

	val naturalWidth: Int
		get() = dom.naturalWidth

	val naturalHeight: Int
		get() = dom.naturalHeight
}

inline fun Context.img(src: String = "", alt: String = "", init: ComponentInit<Img> = {}): Img {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Img(this).apply {
		this.src = src
		this.alt = alt
		this.title = alt
		init()
	}
}

open class Form(owner: Context) : UiComponentImpl<HTMLFormElement>(owner, createElement("form")) {

	val submitted = event<Event>("submit")

	var acceptCharset: String
		get() = dom.acceptCharset
		set(value) {
			dom.acceptCharset = value
		}

	var action: String
		get() = dom.action
		set(value) {
			dom.action = value
		}

	var autocomplete: String
		get() = dom.autocomplete
		set(value) {
			dom.autocomplete = value
		}

	var enctype: String
		get() = dom.enctype
		set(value) {
			dom.enctype = value
		}

	var encoding: String
		get() = dom.encoding
		set(value) {
			dom.encoding = value
		}

	var method: String
		get() = dom.method
		set(value) {
			dom.method = value
		}

	var noValidate: Boolean
		get() = dom.noValidate
		set(value) {
			dom.noValidate = value
		}

	/**
	 * Sets [action] to `"javascript:void(0);"`, thus preventing a page redirect on form submission.
	 */
	fun preventAction() {
		action = "javascript:void(0);"
	}

	fun submit() = dom.submit()
	fun reset() = dom.reset()
	fun checkValidity(): Boolean = dom.checkValidity()
	fun reportValidity(): Boolean = dom.reportValidity()
}

inline fun Context.form(init: ComponentInit<Form> = {}): Form {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Form(this).apply(init)
}

/**
 * Returns an input element of type 'submit' styled to be invisible.
 */
fun Context.hiddenSubmit(init: ComponentInit<UiComponentImpl<HTMLInputElement>> = {}): UiComponentImpl<HTMLInputElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("input") {
		addClass(CommonStyleTags.hidden)
		dom.type = "submit"
		tabIndex = -1
	}
}

inline fun Context.footer(init: ComponentInit<UiComponentImpl<HTMLElement>> = {}): UiComponentImpl<HTMLElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return component("footer", init)
}

/**
 * Creates a [DocumentFragment].
 */
inline fun fragment(init: ComponentInit<WithNode> = {}): WithNode =
	document.createDocumentFragment().asWithNode().apply(init)