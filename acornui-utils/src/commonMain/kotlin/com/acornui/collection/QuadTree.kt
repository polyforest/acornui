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

package com.acornui.collection

import com.acornui.recycle.Clearable

class QuadTree<K : Comparable<K>, V> : Clearable {

	private var root: Node? = null

	fun add(value: V, x: K, y: K) {
		val newNode = Node(x, y, value)
		add(newNode)
	}

	private fun add(newNode: Node?) {
		if (newNode == null) return
		if (root == null) {
			root = newNode
		} else {
			val p = find(root, newNode.x, newNode.y)!!
			newNode.parent = p
			p.addChild(newNode, newNode.x, newNode.y)
		}
	}

	fun remove(value: V, x: K, y: K): Boolean {
		if (root == null) return false
		val node = find(root, value, x, y) ?: return false
		if (node == root) {
			root = null
		} else {
			node.parent!!.removeChild(node)
		}
		add(node.nw)
		add(node.ne)
		add(node.se)
		add(node.sw)
		return true
	}

	/**
	 * Returns true if there is a node with the given [value], [x], and [y].
	 */
	fun contains(value: V, x: K, y: K): Boolean {
		return find(root, value, x, y) != null
	}

	fun update(value: V, oldX: K, oldY: K, newX: K, newY: K) {
		if (oldX == newX && oldY == newY) return
		val node = find(root, value, oldX, oldY) ?: return
		update(node, newX, newY)
	}

	private fun update(node: Node, newX: K, newY: K) {
		node.x = newX
		node.y = newY

		// Check if the change causes the need to move.
		val parent = node.parent
		if (parent != null) {
			val shouldMove = if (parent.nw == node) newX >= parent.x || newY >= parent.y
			else if (parent.ne == node) newX < parent.x || newY >= parent.y
			else if (parent.se == node) newX < parent.x || newY < parent.y
			else newX >= parent.x || newY < parent.y

			if (shouldMove) {
				parent.removeChild(node)
				add(node)
			}
		}

		val nw = node.nw
		if (nw != null && (nw.x >= newX || nw.y >= newY)) {
			node.nw = null
			add(nw)
		}
		val ne = node.ne
		if (ne != null && (ne.x < newX || ne.y >= newY)) {
			node.ne = null
			add(ne)
		}
		val se = node.se
		if (se != null && (se.x < newX || se.y < newY)) {
			node.se = null
			add(se)
		}
		val sw = node.sw
		if (sw != null && (sw.x >= newX || sw.y < newY)) {
			node.sw = null
			add(sw)
		}
	}

	/**
	 * Finds the node with the given [value], [x], and [y]. Returns null if there is no match.
	 */
	private fun find(parent: Node?, value: V, x: K, y: K): Node? {
		return find(root, x, y)!!.parentWalk { it.value != value }
	}

	/**
	 * Traverses the tree to finds the parent of x, y
	 */
	private fun find(parent: Node?, x: K, y: K): Node? {
		return if (parent == null) null
		else if (x < parent.x && y < parent.y) find(parent.nw, x, y) ?: parent
		else if (x >= parent.x && y < parent.y) find(parent.ne, x, y) ?: parent
		else if (x >= parent.x && y >= parent.y) find(parent.se, x, y) ?: parent
		else find(parent.sw, x, y) ?: parent
	}

	fun iterate(xMin: K, yMin: K, xMax: K, yMax: K, inner: (V) -> Boolean) {
		iterate(root, xMin, yMin, xMax, yMax, inner)
	}

	private fun iterate(h: Node?, xMin: K, yMin: K, xMax: K, yMax: K, inner: (V) -> Boolean) {
		if (h == null) return

		if (xMin <= h.x && xMax >= h.x && yMin <= h.y && yMax >= h.y) {
			val shouldContinue = inner(h.value)
			if (!shouldContinue) return
		}

		if (xMin < h.x && yMin < h.y) iterate(h.nw, xMin, yMin, xMax, yMax, inner)
		if (xMax >= h.x && yMin < h.y) iterate(h.ne, xMin, yMin, xMax, yMax, inner)
		if (xMax >= h.x && yMax >= h.y) iterate(h.se, xMin, yMin, xMax, yMax, inner)
		if (xMin < h.x && yMax >= h.y) iterate(h.sw, xMin, yMin, xMax, yMax, inner)
	}

	fun iterate(inner: (V) -> Boolean) {
		iterate(root, inner)
	}

	private fun iterate(h: Node?, inner: (V) -> Boolean) {
		if (h == null) return

		val shouldContinue = inner(h.value)
		if (!shouldContinue) return

		iterate(h.nw, inner)
		iterate(h.ne, inner)
		iterate(h.se, inner)
		iterate(h.sw, inner)
	}

	fun isEmpty(): Boolean {
		return root == null
	}

	fun isNotEmpty(): Boolean {
		return root != null
	}

	override fun clear() {
		root = null
	}

	private inner class Node(
			var x: K,
			var y: K,
			val value: V
	) {
		var parent: Node? = null
		var nw: Node? = null
		var ne: Node? = null
		var se: Node? = null
		var sw: Node? = null

		fun addChild(node: Node, newX: K, newY: K) {
			if (newX < x && newY < y) nw = node
			else if (newX >= x && newY < y) ne = node
			else if (newX >= x && newY >= y) se = node
			else if (newX < x && newY >= y) sw = node
			node.parent = this
		}

		fun removeChild(node: Node) {
			node.parent = null
			if (nw == node) nw = null
			else if (ne == node) ne = null
			else if (se == node) se = null
			else if (sw == node) sw = null
		}

		inline fun parentWalk(inner: (Node) -> Boolean): Node? {
			var p: Node? = this
			while (p != null) {
				val shouldContinue = inner(p)
				if (!shouldContinue) break
				p = p.parent
			}
			return p
		}
	}
}

