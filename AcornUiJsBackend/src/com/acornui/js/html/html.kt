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
import com.acornui.core.UserInfo
import com.acornui.core.childWalkLevelOrder
import com.acornui.core.di.inject
import com.acornui.core.i18n.Locale
import com.acornui.js.dom.component.DomComponent
import com.acornui.logging.Log
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
 * Sets various properties on the [UserInfo] object.
 */
@Suppress("UnsafeCastFromDynamic")
fun initializeUserInfo() {
	UserInfo.apply {
		isTouchDevice = js("""'ontouchstart' in window || !!navigator.maxTouchPoints;""")
		isBrowser = true
		isMobile = js("""
			var check = false;
  (function(a){if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4))) check = true;})(navigator.userAgent||navigator.vendor||window.opera);
  return check;
		""")

		val ua = window.navigator.userAgent

		if (ua.indexOf("MSIE ") >= 0) {
			// IE 10 or older
			isIe = true
		} else if (ua.indexOf("Trident/") >= 0) {
			// IE 11
			isIe = true
		}
		languages = if (window.navigator.languages == null) listOf(Locale(window.navigator.language)) else window.navigator.languages.map { Locale(it) }
		Log.info(this)
	}
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