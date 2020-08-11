/*
 * Copyright 2019 Poly Forest, LLC
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
	"UNUSED_PARAMETER",
	"RedundantLambdaArrow",
	"ObjectPropertyName",
	"MemberVisibilityCanBePrivate",
	"PropertyName"
)

package com.acornui.component

import com.acornui.*
import com.acornui.component.layout.LayoutElement
import com.acornui.component.style.CssClass
import com.acornui.di.Context
import com.acornui.dom.remove
import com.acornui.signal.WithEventTarget
import org.intellij.lang.annotations.Language
import org.w3c.dom.DOMStringMap
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.EventTarget

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ComponentDslMarker

typealias ComponentInit<T> = (@ComponentDslMarker T).() -> Unit

interface UiComponent : LayoutElement, AttachmentHolder, Context, WithNode,
	NodeWithParent, ElementParent<WithNode>, Disposable {

	fun addClass(styleTag: CssClass)

	fun removeClass(styleTag: CssClass)

	fun toggleClass(styleTag: CssClass)

	fun containsClass(styleTag: CssClass): Boolean

	/**
	 * A unique string id for this component.
	 */
	var id: String

	/**
	 * Text representing advisory information.
	 */
	var title: String

	override val dom: HTMLElement

	/**
	 * The parent on the display graph.
	 */
	override val parent: WithNode?

	val dataset: DOMStringMap

	/**
	 * Sets the text on this component. This may be overridden to set the label on a child component.
	 */
	var label: String

	/**
	 * Removes a property from [dataset].
	 * This is equivalent to:
	 * `dom.removeAttribute("data-$name")`
	 */
	fun removeDataAttribute(name: String)
}

private val cssPropertyRegex = Regex("""([a-zA-Z\-]+):(.*);""")

/**
 * Applies the given CSS to the component.
 */
fun UiComponent.applyCss(@Language("CSS", prefix = "#component {\n", suffix = "\n}") css: String) {
	cssPropertyRegex.findAll(css).forEach {
		val propName = it.groups[1]!!.value
		val propValue = it.groups[2]!!.value.trim()
		dom.style.setProperty(propName, propValue)
	}
}

interface WithNode : WithEventTarget, NodeWithParent, Owner, Disposable {

	/**
	 * The dom element this component controls.
	 */
	val dom: Node

	override val parent: WithNode?
}

@Suppress("UnsafeCastFromDynamic")
var Node.host: WithNode?
	get() = asDynamic().__host
	set(value) {
		asDynamic().__host = value
	}

@Suppress("UnsafeCastFromDynamic")
fun Node.asWithNode(): WithNode {
	val h = asDynamic().__host
	if (h != null) return h
	asDynamic().__host = WithNodeImpl(this)
	return asDynamic().__host
}

class WithNodeImpl(override val dom: Node) : WithNode, DisposableBase() {

	init {
		dom.asDynamic().__host = this
	}

	override val parent: WithNode?
		get() = dom.parentNode?.asWithNode()

	override val eventTarget: EventTarget
		get() = dom

	override val children: List<WithNode> = DomChildList(dom)

	override fun dispose() {
		super.dispose()
		dom.parentNode?.remove(dom)
	}
}