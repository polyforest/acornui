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

import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.get
import kotlinx.browser.document

fun <T : Node> Node.add(index: Int, node: T): T {
	if (index == childNodes.length)
		appendChild(node)
	else
		insertBefore(node, childNodes[index])
	return node
}

/**
 * Appends a node as the last child of a node.
 * @return Returns the added node.
 */
fun <T : Node> Node.add(child: T): T {
	appendChild(child)
	return child
}

/**
 * Removes the given child node from this element.
 * @return The removed node, or null if the node does not exist.
 */
fun <T : Node> Node.remove(child: T): Boolean {
	if (!contains(child)) return false
	removeChild(child)
	return true
}

fun Node.removeAt(index: Int): Node {
	if (index < 0 || index >= childNodes.length) throw IndexOutOfBoundsException("$index is out of range.")
	return removeChild(childNodes[index]!!)
}

fun Node.add(data: String): Text =
	add(document.createTextNode(data))

/**
 * Walks the [Node.parentNode] ancestry until [targetAncestor] is found, returning the ancestor of this Node that is a
 * direct child of [targetAncestor]. Will return [this] if `parentNode == targetAncestor`.
 */
fun Node?.getParentBeforeAncestor(targetAncestor: Node): Node? {
	var p: Node? = this
	while (p != null && p.parentNode != targetAncestor) {
		p = p.parentNode
	}
	return p
}