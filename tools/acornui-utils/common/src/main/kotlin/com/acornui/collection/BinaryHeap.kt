/*
 * Derived from LibGDX by Nicholas Bilyk
 * https://github.com/libgdx
 * Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
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

/**
 * @author Nathan Sweet
 */
class BinaryHeap<T : BinaryHeapNode>(
		initialCapacity: Int,
		private val isMaxHeap: Boolean = false
) : Clearable {

	private val nodes = ArrayList<T>(initialCapacity)

	val size: Int
		get() = nodes.size

	init {
	}

	fun add(node: T): T {
		// Insert at end and bubble up.
		node.index = size
		nodes[size] = node
		up(size)
		return node
	}

	fun add(node: T, value: Float): T {
		node.value = value
		return add(node)
	}

	fun peek(): T {
		if (size == 0) throw IllegalStateException("The heap is empty.")
		return nodes[0]
	}

	fun pop(): T {
		return remove(0)
	}

	fun remove(node: T): T {
		return remove(node.index)
	}

	private fun remove(index: Int): T {
		val nodes = this.nodes
		val removed = nodes[index]
		nodes[index] = nodes.last()
		nodes.pop()
		if (size > 0 && index < size) down(index)
		return removed
	}

	override fun clear() {
		nodes.clear()
	}

	fun setValue(node: T, value: Float) {
		val oldValue = node.value
		node.value = value
		if ((value < oldValue) xor isMaxHeap)
			up(node.index)
		else
			down(node.index)
	}

	private fun up(index: Int) {
		var i = index
		val nodes = this.nodes
		val node = nodes[i]
		val value = node.value
		while (i > 0) {
			val parentIndex = i - 1 shr 1
			val parent = nodes[parentIndex]
			if ((value < parent.value) xor isMaxHeap) {
				nodes[i] = parent
				parent.index = i
				i = parentIndex
			} else
				break
		}
		nodes[i] = node
		node.index = i
	}

	private fun down(index: Int) {
		var i = index
		val nodes = this.nodes
		val size = this.size

		val node = nodes[i]
		val value = node.value

		while (true) {
			val leftIndex = 1 + (i shl 1)
			if (leftIndex >= size) break
			val rightIndex = leftIndex + 1

			// Always have a left child.
			val leftNode = nodes[leftIndex]
			val leftValue = leftNode.value

			// May have a right child.
			val rightNode: T?
			val rightValue: Float
			if (rightIndex >= size) {
				rightNode = null
				rightValue = if (isMaxHeap) Float.MIN_VALUE else Float.MAX_VALUE
			} else {
				rightNode = nodes[rightIndex]
				rightValue = rightNode.value
			}

			// The smallest of the three values is the parent.
			if ((leftValue < rightValue) xor isMaxHeap) {
				if (leftValue == value || (leftValue > value) xor isMaxHeap) break
				nodes[i] = leftNode
				leftNode.index = i
				i = leftIndex
			} else {
				if (rightValue == value || (rightValue > value) xor isMaxHeap) break
				nodes[i] = rightNode!!
				rightNode.index = i
				i = rightIndex
			}
		}

		nodes[i] = node
		node.index = i
	}

	override fun toString(): String {
		if (size == 0) return "[]"
		val nodes = this.nodes
		val buffer = StringBuilder(32)
		buffer.append('[')
		buffer.append(nodes[0].value)
		for (i in 1..size - 1) {
			buffer.append(", ")
			buffer.append(nodes[i].value)
		}
		buffer.append(']')
		return buffer.toString()
	}

}

/**
 * @author Nathan Sweet
 */
open class BinaryHeapNode(value: Float) {

	var value: Float = 0f
		internal set

	internal var index: Int = 0

	init {
		this.value = value
	}

	override fun toString(): String {
		return value.toString()
	}
}
