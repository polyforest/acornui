/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.js.html

import com.acornui.collection.cyclicListObtain
import com.acornui.collection.cyclicListPool
import com.acornui.component.InteractiveElementRo
import com.acornui.component.Stage
import com.acornui.component.UiComponent
import com.acornui.core.TreeWalk
import com.acornui.core.childWalkLevelOrder
import com.acornui.core.di.inject
import com.acornui.js.dom.component.DomComponent
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import kotlin.browser.window

/**
 * cut/copy/paste events are type ClipboardEvent, except in IE, which is a DragEvent
 */
external class ClipboardEvent(type: String, eventInitDict: EventInit) : Event {
	val clipboardData: DataTransfer?
}

val Window.clipboardData: DataTransfer?
	get() {
		val d: dynamic = this
		return d.clipboardData
	}

fun Node.owns(element: Node): Boolean {
	var p: Node? = element
	while (p != null) {
		if (p == this) return true
		p = p.parentNode
	}
	return false
}

/**
 * Traverses the root interactive element until it finds the acorn component that wraps the target html element.
 * Only the branches of the tree that are part of the target's ancestry are traversed.
 */
fun findComponentFromDom(target: EventTarget?, root: InteractiveElementRo): InteractiveElementRo? {
	if (target == window) return root.inject(Stage)
	if (target !is HTMLElement) return null

	var found: InteractiveElementRo? = null
	root.childWalkLevelOrder {
		val native = (it as UiComponent).native as DomComponent
		val element = native.element

		if (element.owns(target)) {
			found = it
			TreeWalk.ISOLATE
		} else {
			TreeWalk.CONTINUE
		}

	}
	return found
}

/**
 * Starting from this Node as the root, walks down the left side until the end, returning that child.
 */
fun Node.leftDescendant(): Node {
	if (hasChildNodes()) {
		return childNodes[0]!!.leftDescendant()
	} else {
		return this
	}
}

/**
 * Starting from this Node as the root, walks down the right side until the end, returning that child.
 */
fun Node.rightDescendant(): Node {
	if (hasChildNodes()) {
		return childNodes[childNodes.length - 1]!!.rightDescendant()
	} else {
		return this
	}
}

fun Node.insertAfter(node: Node, child: Node?): Node {
	if (child == null)
		appendChild(node)
	else
		insertBefore(node, child.nextSibling)
	return node
}

/**
 * Does a level order walk on the child nodes of this node.
 */
fun Node.walkChildrenLo(callback: (Node) -> TreeWalk) {
	walkChildrenLo(callback, reversed = false)
}

/**
 * Does a level order walk on the child nodes of this node.
 */
fun Node.walkChildrenLo(callback: (Node) -> TreeWalk, reversed: Boolean) {
	val openList = cyclicListObtain<Node>()
	openList.add(this)
	loop@ while (openList.isNotEmpty()) {
		val next = openList.shift()
		val treeWalk = callback(next)
		when (treeWalk) {
			TreeWalk.HALT -> return
			TreeWalk.SKIP -> continue@loop
			TreeWalk.ISOLATE -> {
				openList.clear()
			}
			else -> {
			}
		}
		if (reversed) {
			for (i in next.childNodes.length - 1 downTo 0) {
				val it = next.childNodes[i]!!
				openList.add(it)
			}
		} else {
			for (i in 0..next.childNodes.length - 1) {
				val it = next.childNodes[i]!!
				openList.add(it)
			}
		}
	}
	cyclicListPool.free(openList)
}


//---------------------------------------------
// Mutation observers
//---------------------------------------------

@Suppress("UnsafeCastFromDynamic")
val mutationObserversSupported: Boolean = js("var MutationObserver = window.MutationObserver || window.WebKitMutationObserver || window.MozMutationObserver; MutationObserver != null")

fun MutationObserver.observe(target: Node, options: MutationObserverInit2) {
	val d: dynamic = this
	d.observe(target, options)
}

external interface MutationObserverInit2

fun mutationObserverOptions(childList: Boolean = false,
							attributes: Boolean = false,
							characterData: Boolean = false,
							subtree: Boolean = false,
							attributeOldValue: Boolean = false,
							characterDataOldValue: Boolean = false,
							attributeFilter: Array<String>? = null): MutationObserverInit2 {

	val initOptions = js("({})")
	initOptions.childList = childList
	initOptions.attributes = attributes
	initOptions.characterData = characterData
	initOptions.subtree = subtree
	initOptions.attributeOldValue = attributeOldValue
	initOptions.characterDataOldValue = characterDataOldValue
	if (attributeFilter != null)
		initOptions.attributeFilter = attributeFilter
	return initOptions
}


@Suppress("UNCHECKED_CAST")
fun <T, R> T.unsafeCast(): R {
	return this as R
}

@Suppress("UnsafeCastFromDynamic")
fun Document.getSelection(): Selection {
	val d: dynamic = this
	return d.getSelection()
}

@Suppress("UnsafeCastFromDynamic")
fun Window.getSelection(): Selection {
	val d: dynamic = this
	return d.getSelection()
}

external interface Selection {
	val rangeCount: Int

	fun removeAllRanges()
	fun getRangeAt(i: Int): Range
	fun addRange(value: Range)
}