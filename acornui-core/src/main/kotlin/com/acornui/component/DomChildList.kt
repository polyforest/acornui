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

import com.acornui.dom.add
import com.acornui.dom.remove
import com.acornui.dom.removeAt
import org.w3c.dom.Node

/**
 * DomChildList provides a way to mutate dom children in a [MutableList] manner.
 */
class DomChildList(private val dom: Node) : AbstractMutableList<WithNode>() {

	override fun add(index: Int, element: WithNode) {
		check(index in 0..size) { "index is out of bounds." }
		if (element.parent === dom) {
			// Reorder child.
			val oldIndex = indexOf(element)
			val newIndex = if (index > oldIndex) index - 1 else index
			dom.remove(element.dom)
			dom.add(newIndex, element.dom)
		} else {
			check(element.parent == null) {
				"Attempted adding child <${element}> to $dom but was already a child of <${element.parent}>. Remove child first."
			}
			dom.add(index, element.dom)
		}
	}

	override fun removeAt(index: Int): WithNode {
		return dom.removeAt(index).asWithNode()
	}

	override val size: Int
		get() = dom.childNodes.length

	override fun get(index: Int): WithNode = dom.childNodes.item(index)!!.asWithNode()

	override fun set(index: Int, element: WithNode): WithNode {
		val old = get(index)
		if (old == element) return element
		dom.removeAt(index)
		dom.add(index, element.dom)
		return old
	}
}