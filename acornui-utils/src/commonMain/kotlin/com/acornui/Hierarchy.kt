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
 *
 * It is expected that any implementations of [NodeRo] define parent and children as their self type.
 */
interface NodeRo {

	/**
	 * The parent of this node.
	 */
	val parent: NodeRo?

	/**
	 * Returns a read-only list of the children.
	 */
	val children: List<NodeRo>

}

fun NodeRo.previousSibling(): NodeRo? {
	val p = parent ?: return null
	val c = p.children
	val index = c.indexOf(this)
	if (index == 0) return null
	return c[index - 1]
}

fun NodeRo.nextSibling(): NodeRo? {
	val p = parent ?: return null
	val c = p.children
	val index = c.indexOf(this)
	if (index == c.lastIndex) return null
	return c[index + 1]
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
 * (including this receiver object).
 *
 * @param reversed If true, the last node will be added to the queue first.
 * @param callback The callback to invoke on each node.
 * @return Returns the element, if any, where [callback] returned [TreeWalk.HALT].
 */
fun NodeRo.childWalkLevelOrder(callback: (NodeRo) -> TreeWalk, reversed: Boolean): NodeRo? {
	val openList = arrayListObtain<NodeRo>()
	openList.add(this)
	var found: NodeRo? = null
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
	arrayListPool.free(openList)
	return found
}

fun NodeRo.childWalkLevelOrder(callback: (NodeRo) -> TreeWalk) {
	childWalkLevelOrder(callback, false)
}

fun NodeRo.childWalkLevelOrderReversed(callback: (NodeRo) -> TreeWalk) {
	childWalkLevelOrder(callback, true)
}

/**
 * Given a callback that returns true if the descendant is found, this method will return the first descendant with
 * the matching condition.
 * The tree traversal will be level-order.
 */
fun NodeRo.findChildLevelOrder(callback: Filter<NodeRo>, reversed: Boolean): NodeRo? {
	return childWalkLevelOrder({
		if (callback(it)) TreeWalk.HALT else TreeWalk.CONTINUE
	}, reversed)
}

fun NodeRo.findChildLevelOrder(callback: Filter<NodeRo>): NodeRo? {
	return findChildLevelOrder(callback, reversed = false)
}

fun NodeRo.findLastChildLevelOrder(callback: Filter<NodeRo>): NodeRo? {
	return findChildLevelOrder(callback, reversed = true)
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
fun NodeRo.childWalkPreorder(callback: (NodeRo) -> TreeWalk, reversed: Boolean): NodeRo? {
	val openList = arrayListObtain<NodeRo>()
	openList.add(this)
	var found: NodeRo? = null
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
	arrayListPool.free(openList)
	return found
}

fun NodeRo.childWalkPreorder(callback: (NodeRo) -> TreeWalk) {
	childWalkPreorder(callback, false)
}

fun NodeRo.childWalkPreorderReversed(callback: (NodeRo) -> TreeWalk) {
	childWalkPreorder(callback, true)
}

/**
 * Given a callback that returns true if the descendant is found, this method will return the first descendant with
 * the matching condition.
 * The tree traversal will be pre-order.
 */
fun NodeRo.findChildPreOrder(callback: Filter<NodeRo>, reversed: Boolean): NodeRo? {
	return childWalkPreorder({
		if (callback(it)) TreeWalk.HALT else TreeWalk.CONTINUE
	}, reversed)
}

fun NodeRo.findChildPreOrder(callback: Filter<NodeRo>): NodeRo? {
	return findChildPreOrder(callback, reversed = false)
}

fun NodeRo.findLastChildPreOrder(callback: Filter<NodeRo>): NodeRo? {
	return findChildPreOrder(callback, reversed = true)
}

/**
 * Returns the lineage count. 0 if this child is the root, 1 if the root is the parent,
 * 2 if the root is the grandparent, 3 if the root is the great grandparent, and so on.
 */
fun NodeRo.ancestryCount(): Int {
	var count = 0
	var p = parent
	while (p != null) {
		count++
		p = p.parent
	}
	return count
}

/**
 * Returns the maximum [ancestryCount] for any descendant node.
 */
fun NodeRo.maxDepth(): Int {
	@Suppress("UNCHECKED_CAST")
	val calculatedMap = mapPool.obtain() as MutableMap<NodeRo, Int>
	var max = 0
	childWalkPreorder {
		val p = it.parent
		if (p == null) {
			calculatedMap[it] = 0
		} else {
			val v = calculatedMap[p]!! + 1
			calculatedMap[it] = v
			if (v > max) {
				max = v
			}
		}
		TreeWalk.CONTINUE
	}
	mapPool.free(calculatedMap)
	return max
}

/**
 * Starting from this Node as the root, walks down the left side until the end, returning that child.
 */
fun NodeRo.leftDescendant(): NodeRo {
	if (children.isEmpty()) return this
	return children.first().leftDescendant()
}

/**
 * Starting from this Node as the root, walks down the right side until the end, returning that child.
 */
fun NodeRo.rightDescendant(): NodeRo {
	if (children.isEmpty()) return this
	return children.last().rightDescendant()
}

inline fun NodeRo.ancestorWalk(callback: (NodeRo) -> Unit): NodeRo? {
	var p: NodeRo? = this
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
inline fun NodeRo.findAncestor(predicate: Filter<NodeRo>): NodeRo? {
	var p: NodeRo? = this
	while (p != null) {
		val found = predicate(p)
		if (found) return p
		p = p.parent
	}
	return null
}

/**
 * Populates an ArrayList with a ChildRo's ancestry.
 * @return Returns the [out] ArrayList
 */
fun NodeRo.ancestry(out: MutableList<NodeRo>): MutableList<NodeRo> {
	out.clear()
	ancestorWalk {
		out.add(it)
	}
	return out
}

fun NodeRo.root(): NodeRo {
	var root: NodeRo = this
	var p: NodeRo? = this
	while (p != null) {
		root = p
		p = p.parent
	}
	return root
}

private val ancestry1 = ArrayList<NodeRo>()
private val ancestry2 = ArrayList<NodeRo>()

/**
 * Returns the lowest common ancestor if there is one between the two children.
 */
fun NodeRo.lowestCommonAncestor(other: NodeRo): NodeRo? {
	ancestry(ancestry1)
	other.ancestry(ancestry2)

	val element = ancestry1.firstOrNull { ancestry2.contains(it) }
	ancestry1.clear()
	ancestry2.clear()
	return element
}

/**
 * Returns true if this node is before the [other] node. This considers the parent to come before the child.
 * Returns null if there is no common ancestor.
 */
fun NodeRo.isBefore(other: NodeRo): Boolean? {
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
fun NodeRo.isAfter(other: NodeRo): Boolean {
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
 * Returns true if this [NodeRo] is the ancestor of the given [child].
 * X is considered to be an ancestor of Y if doing a parent walk starting from Y, X is then reached.
 * This will return true if X === Y
 */
fun NodeRo.isAncestorOf(child: NodeRo): Boolean {
	return child.findAncestor {
		it === this
	} != null
}

fun NodeRo.isDescendantOf(ancestor: NodeRo): Boolean = ancestor.isAncestorOf(this)
