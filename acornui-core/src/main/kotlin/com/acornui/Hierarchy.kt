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

@file:Suppress("unused")

package com.acornui

import com.acornui.collection.*

/**
 * An abstract tree node.
 */
interface Node {

	/**
	 * Returns a read-only list of the children.
	 */
	val children: List<Node>

}

//------------------------------------------------
// Hierarchy traversal methods
//------------------------------------------------

/**
 * Flags for what relative nodes to skip when traversing a hierarchy.
 */
enum class TreeWalk {

	/**
	 * Continues iteration normally.
	 */
	CONTINUE,

	/**
	 * Skips all remaining nodes, halting iteration.
	 */
	HALT,

	/**
	 * Skips current node's children.
	 */
	SKIP,

	/**
	 * Discards nodes not descending from current node.
	 * This will have no effect in post-order traversal.
	 */
	ISOLATE

}

//-------------------------------------------------
// Level order
//-------------------------------------------------

/**
 * A level-order child walk.
 * Traverses this parent's hierarchy from top to bottom, breadth first, invoking a callback on each node.
 * (including `this` receiver object).
 *
 * @param reversed If true, the last node will be added to the queue first.
 * @param callback The callback to invoke on each node.
 * @return Returns the element, if any, where [callback] returned [TreeWalk.HALT].
 */
fun Node.childWalkLevelOrder(reversed: Boolean, callback: (Node) -> TreeWalk): Node? {
	val openList = ArrayList<Node>()
	openList.add(this)
	var found: Node? = null
	loop@ while (openList.isNotEmpty()) {
		val next = openList.shift()
		when (callback(next)) {
			TreeWalk.HALT -> {
				found = next
				break@loop
			}
			TreeWalk.SKIP -> continue@loop
			TreeWalk.ISOLATE -> {
				openList.clear()
			}
			else -> {
			}
		}
		if (reversed) {
			for (i in next.children.lastIndex downTo 0)
				openList.add(next.children[i])
		} else
			openList.addAll(next.children)
	}
	return found
}

fun Node.childWalkLevelOrder(callback: (Node) -> TreeWalk) {
	childWalkLevelOrder(false, callback)
}

fun Node.childWalkLevelOrderReversed(callback: (Node) -> TreeWalk) {
	childWalkLevelOrder(true, callback)
}

/**
 * Given a callback that returns true if the descendant is found, this method will return the first descendant with
 * the matching condition.
 * The tree traversal will be level-order.
 */
fun Node.findChildLevelOrder(reversed: Boolean, callback: Filter<Node>): Node? {
	return childWalkLevelOrder(reversed) {
		if (callback(it)) TreeWalk.HALT else TreeWalk.CONTINUE
	}
}

fun Node.findChildLevelOrder(callback: Filter<Node>): Node? {
	return findChildLevelOrder(reversed = false, callback = callback)
}

fun Node.findLastChildLevelOrder(callback: Filter<Node>): Node? {
	return findChildLevelOrder(reversed = true, callback = callback)
}

//-------------------------------------------------
// Pre-order
//-------------------------------------------------

/**
 * A pre-order child walk.
 *
 * @param callback The callback to invoke on each child.
 * @return Returns the node, if any, where [callback] returned [TreeWalk.HALT]
 */
fun Node.childWalkPreorder(reversed: Boolean, callback: (Node) -> TreeWalk): Node? {
	val openList = ArrayList<Node>()
	openList.add(this)
	var found: Node? = null
	loop@ while (openList.isNotEmpty()) {
		val next = openList.pop()
		when (callback(next)) {
			TreeWalk.HALT -> {
				found = next
				break@loop
			}
			TreeWalk.SKIP -> continue@loop
			TreeWalk.ISOLATE -> {
				openList.clear()
			}
			else -> {
			}
		}
		if (reversed) {
			openList.addAll(next.children)
		} else
			for (i in next.children.lastIndex downTo 0)
				openList.add(next.children[i])
	}
	return found
}

fun Node.childWalkPreorder(callback: (Node) -> TreeWalk) {
	childWalkPreorder(false, callback)
}

fun Node.childWalkPreorderReversed(callback: (Node) -> TreeWalk) {
	childWalkPreorder(true, callback)
}

/**
 * Given a callback that returns true if the descendant is found, this method will return the first descendant with
 * the matching condition.
 * The tree traversal will be pre-order.
 */
fun Node.findChildPreOrder(reversed: Boolean, callback: Filter<Node>): Node? {
	return childWalkPreorder(reversed) {
		if (callback(it)) TreeWalk.HALT else TreeWalk.CONTINUE
	}
}

fun Node.findChildPreOrder(callback: Filter<Node>): Node? {
	return findChildPreOrder(reversed = false, callback = callback)
}

fun Node.findLastChildPreOrder(callback: Filter<Node>): Node? {
	return findChildPreOrder(reversed = true, callback = callback)
}


/**
 * Starting from this Node as the root, walks down the left side until the end, returning that child.
 */
fun Node.leftDescendant(): Node {
	if (children.isEmpty()) return this
	return children.first().leftDescendant()
}

/**
 * Starting from this Node as the root, walks down the right side until the end, returning that child.
 */
fun Node.rightDescendant(): Node {
	if (children.isEmpty()) return this
	return children.last().rightDescendant()
}

interface NodeWithParent : Node {
	val parent: NodeWithParent?
}

fun NodeWithParent.previousSibling(): Node? {
	val p = parent ?: return null
	val c = p.children
	val index = c.indexOf(this)
	if (index == 0) return null
	return c[index - 1]
}

fun NodeWithParent.nextSibling(): Node? {
	val p = parent ?: return null
	val c = p.children
	val index = c.indexOf(this)
	if (index == c.lastIndex) return null
	return c[index + 1]
}

/**
 * Returns the lineage count. 0 if this child is the root, 1 if the root is the parent,
 * 2 if the root is the grandparent, 3 if the root is the great grandparent, and so on.
 */
fun NodeWithParent.ancestryCount(): Int {
	var count = 0
	var p = parent
	while (p != null) {
		count++
		p = p.parent
	}
	return count
}

inline fun NodeWithParent.ancestorWalk(callback: (NodeWithParent) -> Unit): Node? {
	var p: NodeWithParent? = this
	while (p != null) {
		callback(p)
		p = p.parent
	}
	return null
}

/**
 * Walks the ancestry chain on the display graph, returning the first element where the predicate returns true.
 *
 * @param predicate The ancestor (starting with `this`) is passed to this function, if true is returned, the ancestry
 * walk will stop and that ancestor will be returned.
 * @return Returns the first ancestor where [predicate] returns true.
 */
inline fun NodeWithParent.findAncestor(predicate: Filter<Node>): Node? {
	var p: NodeWithParent? = this
	while (p != null) {
		val found = predicate(p)
		if (found) return p
		p = p.parent
	}
	return null
}


/**
 * Walks the ancestry chain on the display graph, returning the child of the ancestor where [predicate] returned true.
 *
 * @param predicate The ancestor (starting with `parent`) is passed to this function, if true is returned, the ancestry
 * walk will stop and the child of that ancestor is returned.
 */
inline fun NodeWithParent.findAncestorBefore(predicate: Filter<Node>): Node? {
	var c = this
	var p: NodeWithParent? = parent
	while (p != null) {
		val found = predicate(p)
		if (found) return c
		c = p
		p = p.parent
	}
	return null
}

/**
 * Populates an ArrayList with a ChildRo's ancestry.
 * @return Returns the [out] ArrayList
 */
fun NodeWithParent.ancestry(): List<NodeWithParent> {
	val out = ArrayList<NodeWithParent>()
	ancestorWalk {
		out.add(it)
	}
	return out
}

fun NodeWithParent.root(): NodeWithParent {
	var root: NodeWithParent = this
	var p: NodeWithParent? = this
	while (p != null) {
		root = p
		p = p.parent
	}
	return root
}

/**
 * Returns the lowest common ancestor if there is one between the two children.
 */
fun NodeWithParent.lowestCommonAncestor(other: NodeWithParent): NodeWithParent? {
	val ancestry1 = ancestry()
	val ancestry2 = other.ancestry()
	return ancestry1.firstOrNull { ancestry2.contains(it) }
}

/**
 * Returns true if this node is before the [other] node. This considers the parent to come before the child.
 * Returns null if there is no common ancestor.
 */
fun NodeWithParent.isBefore(other: NodeWithParent): Boolean? {
	if (this === other) throw Exception("this === other")
	var a = this
	ancestorWalk { parentA ->
		var b = this
		other.ancestorWalk { parentB ->
			if (parentA === parentB) {
				val children = parentA.children
				val indexA = children.indexOf(a)
				val indexB = children.indexOf(b)
				return indexA < indexB
			}
			b = parentB
		}
		a = parentA
	}
	return null
}

/**
 * Returns true if this ChildRo is after the [other] ChildRo. This considers the parent to come before the child.
 * @throws Exception If [other] does not have a common ancestor.
 */
fun NodeWithParent.isAfter(other: NodeWithParent): Boolean {
	if (this === other) throw Exception("this === other")
	var a = this
	ancestorWalk { parentA ->
		var b = this
		other.ancestorWalk { parentB ->
			if (parentA === parentB) {
				val children = parentA.children
				val indexA = children.indexOf(a)
				val indexB = children.indexOf(b)
				return indexA > indexB
			}
			b = parentB
		}
		a = parentA
	}
	throw Exception("No common withAncestor")
}

/**
 * Returns true if this [Node] is the ancestor of the given [child].
 * X is considered to be an ancestor of Y if doing a parent walk starting from Y, X is then reached.
 * This will return true if X === Y
 */
fun NodeWithParent.isAncestorOf(child: NodeWithParent): Boolean {
	return child.findAncestor {
		it === this
	} != null
}

fun NodeWithParent.isDescendantOf(ancestor: NodeWithParent): Boolean = ancestor.isAncestorOf(this)
