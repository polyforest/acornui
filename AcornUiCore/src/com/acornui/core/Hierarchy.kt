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

@file:Suppress("unused")

package com.acornui.core

import com.acornui.collection.*


/**
 * An abstract child to a single parent.
 */
interface ChildRo {

	val parent: ParentRo<ChildRo>?

}

fun <T : ChildRo> T.previousSibling(): T? {
	@Suppress("UNCHECKED_CAST")
	val p = parent as? ParentRo<T> ?: return null
	val c = p.children
	val index = c.indexOf(this)
	if (index == 0) return null
	return c[index - 1]
}

fun <T : ChildRo> T.nextSibling(): T? {
	@Suppress("UNCHECKED_CAST")
	val p = parent as? ParentRo<T> ?: return null
	val c = p.children
	val index = c.indexOf(this)
	if (index == c.lastIndex) return null
	return c[index + 1]
}

/**
 * An abstract parent to children relationship of a parameterized type.
 * Acorn hierarchies are conventionally
 * A : B -> B
 * Where B is a child of A, and A is a sub-type of B.
 * This means that when using recursion throughout the tree, all nodes can be considered to be a [ChildRo] of type B,
 * but it must be type checked against a [ParentRo] of type A before continuing the traversal.
 */
interface ParentRo<out T> : ChildRo {

	/**
	 * Returns a read-only list of the children.
	 */
	val children: List<T>

}

inline fun <T> ParentRo<T>.iterateChildren(reversed: Boolean = false, body: (child: T) -> Boolean) {
	if (reversed) {
		for (i in children.lastIndex downTo 0) {
			val shouldContinue = body(children[i])
			if (!shouldContinue) break
		}
	} else {
		for (i in 0..children.lastIndex) {
			val shouldContinue = body(children[i])
			if (!shouldContinue) break
		}
	}
}

/**
 * An interface to Parent that allows write access.
 */
interface Parent<T> : ParentRo<T> {

	fun <S : T> addChild(child: S): S = addChild(children.size, child)

	/**
	 * Adds the specified child to this container.
	 * @param index The index of where to insert the child. By default this is the end of the list.
	 * @return Returns the newly added child.
	 */
	fun <S : T> addChild(index: Int, child: S): S

	fun addAllChildren(children: Iterable<T>) = addAllChildren(this.children.size, children)

	fun addAllChildren(index: Int, children: Iterable<T>) {
		var i = index
		for (child in children) {
			addChild(i++, child)
		}
	}

	fun addAllChildren(children: Array<T>) = addAllChildren(this.children.size, children)

	fun addAllChildren(index: Int, children: Array<T>) {
		var i = index
		for (child in children) {
			addChild(i++, child)
		}
	}

	/**
	 * Adds a child after the provided element.
	 */
	fun addChildAfter(child: T, after: T): Int {
		val index = children.indexOf(after)
		if (index == -1) return -1
		addChild(index + 1, child)
		return index + 1
	}

	/**
	 * Adds a child after the provided element.
	 */
	fun addChildBefore(child: T, before: T): Int {
		val index = children.indexOf(before)
		if (index == -1) return -1
		addChild(index, child)
		return index
	}

	/**
	 * Removes a child from this container.
	 * @return true if the child was found and removed.
	 */
	fun removeChild(child: T?): Boolean {
		if (child == null) return false
		val index = children.indexOf(child)
		if (index == -1) return false
		removeChild(index)
		return true
	}

	/**
	 * Removes a child at the given index from this container.
	 * @return Returns the child if it was removed, or null if the index was out of range.
	 */
	fun removeChild(index: Int): T

	/**
	 * Removes all the children and optionally disposes them (default).
	 */
	fun clearChildren() {
		val c = children
		while (c.isNotEmpty()) {
			removeChild(children.size - 1)
		}
	}
}

abstract class ParentBase<T : ParentBase<T>> : Parent<T> {

	override var parent: ParentRo<ChildRo>? = null

	protected val _children: MutableList<T> = ArrayList()
	override val children: List<T>
		get() = _children

	override fun <S : T> addChild(index: Int, child: S): S {
		_children.add(index, child)
		child.parent = this
		return child
	}

	override fun removeChild(index: Int): T {
		val c = _children.removeAt(index)
		c.parent = null
		return c
	}
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
 * Traverses this parent's hierarchy from top to bottom, breadth first, invoking a callback on each ChildRo element
 * (including this receiver object).
 *
 * @param reversed If true, the last child will be added to the queue first.
 * @param callback The callback to invoke on each child.
 */
inline fun <reified T : ChildRo> T.childWalkLevelOrder(callback: (T) -> TreeWalk, reversed: Boolean) {
	val openList = arrayListObtain<Any?>()
	openList.add(this)
	loop@ while (openList.isNotEmpty()) {
		val next = openList.shift()
		if (next is T) {
			val treeWalk = callback(next)
			when (treeWalk) {
				TreeWalk.HALT -> break@loop
				TreeWalk.SKIP -> continue@loop
				TreeWalk.ISOLATE -> {
					openList.clear()
				}
				else -> {
				}
			}
			(next as? ParentRo<*>)?.iterateChildren(reversed) {
				openList.add(it)
				true
			}
		}
	}
	arrayListPool.free(openList)
}

inline fun <reified T : ChildRo> T.childWalkLevelOrder(callback: (T) -> TreeWalk) {
	childWalkLevelOrder(callback, false)
}

inline fun <reified T : ChildRo> T.childWalkLevelOrderReversed(callback: (T) -> TreeWalk) {
	childWalkLevelOrder(callback, true)
}

/**
 * Given a callback that returns true if the descendant is found, this method will return the first descendant with
 * the matching condition.
 * The tree traversal will be level-order.
 */
inline fun <reified T : ChildRo> T.findChildLevelOrder(callback: (T) -> Boolean, reversed: Boolean): T? {
	var foundItem: T? = null
	childWalkLevelOrder({
		if (callback(it)) {
			foundItem = it
			TreeWalk.HALT
		} else {
			TreeWalk.CONTINUE
		}
	}, reversed)
	return foundItem
}

inline fun <reified T : ChildRo> T.findChildLevelOrder(callback: (T) -> Boolean): T? {
	return findChildLevelOrder(callback, reversed = false)
}

inline fun <reified T : ChildRo> T.findLastChildLevelOrder(callback: (T) -> Boolean): T? {
	return findChildLevelOrder(callback, reversed = true)
}

//-------------------------------------------------
// Pre-order
//-------------------------------------------------

/**
 * A pre-order child walk.
 *
 * @param callback The callback to invoke on each child.
 */
inline fun <reified T: ChildRo> T.childWalkPreOrder(callback: (T) -> TreeWalk, reversed: Boolean) {
	val openList = arrayListObtain<Any?>()
	openList.add(this)
	loop@ while (openList.isNotEmpty()) {
		val next = openList.pop()
		if (next is T) {
			val treeWalk = callback(next)
			when (treeWalk) {
				TreeWalk.HALT -> break@loop
				TreeWalk.SKIP -> continue@loop
				TreeWalk.ISOLATE -> {
					openList.clear()
				}
				else -> {
				}
			}
			(next as? ParentRo<*>)?.iterateChildren(!reversed) {
				openList.add(it)
				true
			}
		}
	}
	arrayListPool.free(openList)
}

inline fun <reified T: ChildRo> T.childWalkPreOrder(callback: (T) -> TreeWalk) {
	childWalkPreOrder(callback, false)
}

inline fun <reified T: ChildRo> T.childWalkPreOrderReversed(callback: (T) -> TreeWalk) {
	childWalkPreOrder(callback, true)
}

/**
 * Given a callback that returns true if the descendant is found, this method will return the first descendant with
 * the matching condition.
 * The tree traversal will be pre-order.
 */
inline fun <reified T: ChildRo> T.findChildPreOrder(callback: (T) -> Boolean, reversed: Boolean): T? {
	var foundItem: T? = null
	childWalkPreOrder({
		if (callback(it)) {
			foundItem = it
			TreeWalk.HALT
		} else {
			TreeWalk.CONTINUE
		}
	}, reversed)
	return foundItem
}

inline fun <reified T: ChildRo> T.findChildPreOrder(callback: (T) -> Boolean): T? {
	return findChildPreOrder(callback, reversed = false)
}

inline fun <reified T: ChildRo> T.findLastChildPreOrder(callback: (T) -> Boolean): T? {
	return findChildPreOrder(callback, reversed = true)
}

/**
 * Returns the lineage count. 0 if this child is the root, 1 if the root is the parent,
 * 2 if the root is the grandparent, 3 if the root is the great grandparent, and so on.
 */
fun ChildRo.ancestryCount(): Int {
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
fun ChildRo.maxDepth(): Int {
	@Suppress("UNCHECKED_CAST")
	val calculatedMap = mapPool.obtain() as MutableMap<ChildRo, Int>
	var max = 0
	childWalkPreOrder {
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
fun <T : ChildRo> T.leftDescendant(): T {
	if (this is ParentRo<*>) {
		if (children.isEmpty()) return this
		@Suppress("UNCHECKED_CAST")
		return (children.first() as T).leftDescendant()
	} else {
		return this
	}
}

/**
 * Starting from this Node as the root, walks down the right side until the end, returning that child.
 */
fun <T : ChildRo> T.rightDescendant(): T {
	if (this is ParentRo<*>) {
		if (children.isEmpty()) return this
		@Suppress("UNCHECKED_CAST")
		return (children.last() as T).rightDescendant()
	} else {
		return this
	}
}

inline fun ChildRo.parentWalk(callback: (ChildRo) -> Boolean): ChildRo? {
	var p: ChildRo? = this
	while (p != null) {
		val shouldContinue = callback(p)
		if (!shouldContinue) return p
		p = p.parent
	}
	return null
}

/**
 * Populates an ArrayList with a ChildRo's ancestry.
 * @return Returns the [out] ArrayList
 */
fun ChildRo.ancestry(out: MutableList<ChildRo>): MutableList<ChildRo> {
	out.clear()
	parentWalk {
		out.add(it)
		true
	}
	return out
}

fun ChildRo.root(): ChildRo {
	var root: ChildRo = this
	var p: ChildRo? = this
	while (p != null) {
		root = p
		p = p.parent
	}
	return root
}

/**
 * Returns true if this ChildRo is before the [other] ChildRo. This considers the parent to come before the child.
 * @throws Exception If [other] does not have a common ancestor.
 */
fun ChildRo.isBefore(other: ChildRo): Boolean {
	if (this === other) throw Exception("this === other")
	var a = this
	parentWalk { parentA ->
		var b = this
		other.parentWalk { parentB ->
			if (parentA === parentB) {
				val children = (parentA as ParentRo<*>).children
				val indexA = children.indexOf(a)
				val indexB = children.indexOf(b)
				return indexA < indexB
			}
			b = parentB
			true
		}
		a = parentA
		true
	}
	throw Exception("No common withAncestor")
}

/**
 * Returns true if this ChildRo is after the [other] ChildRo. This considers the parent to come before the child.
 * @throws Exception If [other] does not have a common ancestor.
 */
fun ChildRo.isAfter(other: ChildRo): Boolean {
	if (this === other) throw Exception("this === other")
	var a = this
	parentWalk { parentA ->
		var b = this
		other.parentWalk { parentB ->
			if (parentA === parentB) {
				val children = (parentA as ParentRo<*>).children
				val indexA = children.indexOf(a)
				val indexB = children.indexOf(b)
				return indexA > indexB
			}
			b = parentB
			true
		}
		a = parentA
		true
	}
	throw Exception("No common withAncestor")
}

/**
 * Returns true if this [ChildRo] is the ancestor of the given [child].
 * X is considered to be an ancestor of Y if doing a parent walk starting from Y, X is then reached.
 * This will return true if X === Y
 */
fun ChildRo.isAncestorOf(child: ChildRo): Boolean {
	var isAncestor = false
	child.parentWalk {
		isAncestor = it === this
		!isAncestor
	}
	return isAncestor
}

fun ChildRo.isDescendantOf(ancestor: ChildRo): Boolean = ancestor.isAncestorOf(this)