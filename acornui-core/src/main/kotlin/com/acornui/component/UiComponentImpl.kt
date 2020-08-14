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

@file:Suppress("PropertyName", "unused", "MemberVisibilityCanBePrivate")

package com.acornui.component

import com.acornui.Disposable
import com.acornui.UidUtil
import com.acornui.collection.addAfter
import com.acornui.collection.addBefore
import com.acornui.component.layout.Transform
import com.acornui.component.layout.TransformOrigin
import com.acornui.component.layout.toTransformOrigin
import com.acornui.component.style.CssClass
import com.acornui.css.Length
import com.acornui.css.px
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.dom.createElement
import com.acornui.isDebug
import com.acornui.properties.afterChange
import kotlinx.browser.document
import org.intellij.lang.annotations.Language
import org.w3c.dom.DOMStringMap
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.events.EventTarget
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * The base for every AcornUi component.
 * UiComponent wraps an [HTMLElement], providing dsl utility, behavior attachments, and ownership hierarchy.
 *
 * @param owner The creator of this component. This is used for dependency injection, disposal propagation, and
 * coroutine job hierarchy.
 */
open class UiComponentImpl<T : HTMLElement>(
	owner: Context,
	final override val dom: T
) : UiComponent, ContextImpl(owner) {

	final override val eventTarget: EventTarget
		get() = dom

	override val attachments: MutableMap<Any, Any> = HashMap<Any, Any>()

	// Node properties

	override val parent: WithNode?
		get() = dom.parentNode?.asWithNode()

	init {
		@Suppress("LeakingThis")
		dom.host = this@UiComponentImpl
		if (dom.id.isEmpty())
			if (isDebug)
				dom.id = this::class.simpleName + "_" + UidUtil.createUid() // class.simpleName is slow, but informative.
			else
				dom.id = "_" + UidUtil.createUid()
	}

	//-----------------------------------------------
	// UiComponent
	//-----------------------------------------------

	final override var id: String
		get() = dom.id
		set(value) {
			dom.id = value
		}

	final override var title: String
		get() = dom.title
		set(value) {
			dom.title = value
		}

	@Language("html")
	var innerHtml: String
		get() = dom.innerHTML
		set(value) {
			dom.innerHTML = value
		}

	var text: String
		get() = dom.innerText
		set(value) {
			dom.innerText = value
		}

	override var label: String
		get() = text
		set(value) {
			text = value
		}

	final override val dataset: DOMStringMap
		get() = dom.dataset

	final override fun removeDataAttribute(name: String) {
		dom.removeAttribute("data-$name")
	}

	//-----------------------------------------------

	var tabIndex: Int?
		get() = if (dom.hasAttribute("tabindex")) dom.tabIndex else null
		set(value) {
			if (value == null)
				dom.removeAttribute("tabindex")
			else
				dom.tabIndex = value
		}

	//-----------------------------------------------
	// Layout properties
	//-----------------------------------------------

	override val width: Double
		get() = dom.offsetWidth.toDouble()

	override val height: Double
		get() = dom.offsetHeight.toDouble()

	override fun width(value: Length?) {
		if (value == null)
			style.removeProperty("width")
		else
			style.width = value.toString()
	}

	override fun height(value: Length?) {
		if (value == null)
			style.removeProperty("height")
		else
			style.height = value.toString()
	}

	override fun size(width: Length?, height: Length?) {
		width(width)
		height(height)
	}

	final override fun size(width: Double?, height: Double?) {
		width(width?.px)
		height(height?.px)
	}

	//-----------------------------------------------
	// Style properties
	//-----------------------------------------------

	final override fun addClass(styleTag: CssClass) {
		dom.classList.add(styleTag.className)
	}

	final override fun removeClass(styleTag: CssClass) {
		dom.classList.remove(styleTag.className)
	}

	final override fun toggleClass(styleTag: CssClass) {
		dom.classList.toggle(styleTag.className)
	}

	final override fun containsClass(styleTag: CssClass): Boolean {
		return dom.classList.contains(styleTag.className)
	}

	//-----------------------------------------------
	// Transformation and translation methods
	//-----------------------------------------------

	final override var transformOrigin: TransformOrigin?
		get() = style.transformOrigin.toTransformOrigin()
		set(value) {
			if (value == null) style.removeProperty("transform-origin")
			else style.transformOrigin = value.toString()
		}

	final override var transform by afterChange<Transform?>(null) {
		dom.style.transform = "${it ?: ""}"
	}

	override val x: Double
		get() = dom.offsetLeft.toDouble()

	override val y: Double
		get() = dom.offsetTop.toDouble()

	override fun x(value: Length?) {
		if (value == null)
			dom.style.removeProperty("left")
		else
			dom.style.left = value.toString()
	}

	final override fun x(value: Double?) = x(value?.px)

	override fun y(value: Length?) {
		if (value == null)
			dom.style.removeProperty("top")
		else
			dom.style.top = value.toString()
	}

	final override fun y(value: Double?) = y(value?.px)

	final override fun position(x: Length?, y: Length?) {
		x(x)
		y(y)
	}

	final override fun position(x: Double, y: Double) = position(x.px, y.px)

	//-----------------------------------------------
	// Children
	//-----------------------------------------------

	protected val childrenMutable: MutableList<WithNode> by lazy { DomChildList(dom) }
	override val children: List<WithNode>
		get() = childrenMutable

	/**
	 * Appends a child to the display children.
	 */
	protected fun <T : WithNode?> addChild(child: T): T {
		return addChild(childrenMutable.size, child)
	}

	/**
	 * Adds or reorders the specified child to this container at the specified index.
	 * If the child is already added to a different container, an error will be thrown.
	 * @param index The index of where to insert the child.
	 */
	protected fun <T : WithNode?> addChild(index: Int, child: T): T {
		if (child == null) return child
		childrenMutable.add(index, child)
		return child
	}

	/**
	 * Removes a child from the display children.
	 */
	protected fun removeChild(child: WithNode?): Boolean {
		if (child == null) return false
		return childrenMutable.remove(child)
	}

	/**
	 * Removes a child at the given index from this container.
	 * @return Returns true if a child was removed, or false if the index was out of range.
	 */
	protected fun removeChild(index: Int): WithNode {
		return childrenMutable.removeAt(index)
	}

	/**
	 * Removes all children, optionally disposing them.
	 */
	protected fun clearChildren(dispose: Boolean = true) {
		val c = children
		while (c.isNotEmpty()) {
			val child = removeChild(c.lastIndex)
			if (dispose)
				(child as? Disposable)?.dispose()
		}
	}

	//-----------------------------------------------
	// Elements
	//-----------------------------------------------

	final override val elements: MutableList<WithNode> = ElementsList(::onElementAdded, ::onElementRemoved)

	override fun <S : WithNode?> addElement(index: Int, element: S): S {
		if (element == null) return element
		elements.add(index, element)
		return element
	}

	override fun removeElement(index: Int): WithNode {
		return elements.removeAt(index)
	}

	/**
	 * Invoked when an external element has been added or reordered.
	 *
	 *
	 * If this is overridden and the [addChild] is delegated, the [onElementRemoved] should mirror the delegation.
	 *
	 * Example:
	 *```
	 * private val otherContainer = addChild(scrollArea())
	 *
	 * override fun onElementAdded(oldIndex: Int, newIndex: Int, element: T) {
	 *     otherContainer.addElement(newIndex, child)
	 * }
	 * override fun onElementRemoved(index: Int, element: T) {
	 *     otherContainer.removeElement(index)
	 * }
	 * ```
	 *
	 * @param oldIndex The previous index of the element, or -1 if the element was not previously in the elements list.
	 * @param newIndex The new index of the element.
	 */
	protected open fun onElementAdded(oldIndex: Int, newIndex: Int, element: WithNode) {
		when (newIndex) {
			elements.lastIndex -> {
				addChild(element)
			}
			0 -> {
				val nextElement = elements[newIndex + 1]
				childrenMutable.addBefore(element, nextElement)
			}
			else -> {
				val previousElement = elements[newIndex - 1]
				childrenMutable.addAfter(element, previousElement)
			}
		}
	}

	/**
	 * Invoked when an element has been removed.
	 * This will not be invoked if an element has been reordered.
	 */
	protected open fun onElementRemoved(index: Int, element: WithNode) {
		removeChild(element)
	}

	override fun clearElements(dispose: Boolean) {
		val c = elements
		while (c.isNotEmpty()) {
			val element = removeElement(c.lastIndex)
			if (dispose)
				element.dispose()
		}
	}

	/**
	 * Syntax sugar for adding a text node.
	 */
	operator fun String.unaryPlus(): WithNode =
		+WithNodeImpl(document.createTextNode(this))

	//-----------------------------------------------
	// Style
	//-----------------------------------------------

	val style: CSSStyleDeclaration
		get() = dom.style

	//-----------------------------------------------
	// Disposable
	//-----------------------------------------------

	override fun dispose() {
		checkDisposed()
		super.dispose()
		dom.remove()
		attachments.clear()
	}

	override fun toString(): String {
		return id
	}
}

/**
 * The most common type of UiComponent base class.
 */
open class Div(
	owner: Context
) : UiComponentImpl<HTMLDivElement>(owner, createElement("div"))

open class Span(
	owner: Context
) : UiComponentImpl<HTMLSpanElement>(owner, createElement("span"))

inline fun Context.html(@Language("html") html: String = "", init: ComponentInit<UiComponentImpl<HTMLDivElement>> = {}): UiComponentImpl<HTMLDivElement> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return Div(this).apply {
		this.innerHtml = html
		init()
	}
}
