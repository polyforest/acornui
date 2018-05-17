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

package com.acornui.pathfinding

import com.acornui.collection.BinaryHeap
import com.acornui.collection.BinaryHeapNode
import com.acornui.math.MathUtils

open class AStar(
		val width: Int,
		val height: Int
) {

	private val open: BinaryHeap<PathNode>
	private val nodes: Array<PathNode?>
	internal var runID: Int = 0
	private var closestNode: PathNode? = null
	private var closestDistance: Int = 0

	protected var targetX: Int = 0
	protected var targetY: Int = 0

	init {
		open = BinaryHeap<PathNode>(width * 4, false)
		nodes = arrayOfNulls<PathNode>(width * height)
	}

	/**
	 * Populates the out parameter with x,y pairs that are the path from the target to the start.
	 */
	fun calculatePath(startX: Int, startY: Int, targetX: Int, targetY: Int, out: MutableList<Int>) {
		this.targetX = targetX
		this.targetY = targetY

		runID++
		if (runID < 0) runID = 1

		val index = startY * width + startX
		var root: PathNode? = nodes[index]
		if (root == null) {
			root = PathNode(0f)
			root.x = startX
			root.y = startY
			nodes[index] = root
		}
		closestNode = root
		closestDistance = MathUtils.abs(startX - targetX) + MathUtils.abs(startY - targetY)
		root.parent = null
		root.pathCost = 0
		open.add(root, 0f)

		val lastColumn = width - 1
		val lastRow = height - 1
		while (open.size > 0) {
			val node = open.pop()
			if (node.x == targetX && node.y == targetY) {
				break
			}
			node.closedID = runID
			val x = node.x
			val y = node.y
			if (x < lastColumn) {
				addNode(node, x + 1, y, 10)
				if (y < lastRow) addNode(node, x + 1, y + 1, 14) // Diagonals cost more, roughly equivalent to sqrt(2).
				if (y > 0) addNode(node, x + 1, y - 1, 14)
			}
			if (x > 0) {
				addNode(node, x - 1, y, 10)
				if (y < lastRow) addNode(node, x - 1, y + 1, 14)
				if (y > 0) addNode(node, x - 1, y - 1, 14)
			}
			if (y < lastRow) addNode(node, x, y + 1, 10)
			if (y > 0) addNode(node, x, y - 1, 10)
		}

		out.clear()
		open.clear()
		out.add(targetX)
		out.add(targetY)
		if (closestNode != null) {
			var node: PathNode = closestNode!!
			while (node !== root) {
				out.add(node.x)
				out.add(node.y)
				node = node.parent!!
			}
		}
	}

	private fun addNode(parent: PathNode, x: Int, y: Int, cost: Int) {
		if ((x != targetX || y != targetY) && !isValid(x, y)) return

		val pathCost = parent.pathCost + cost
		val distance = MathUtils.abs(x - targetX) + MathUtils.abs(y - targetY)
		if (distance > closestDistance * MAX_DEVIATION_PERCENT + MAX_DEVIATION) {
			return  // We've deviated too far from the closest node we've found so far.
		}
		val score = (pathCost + distance).toFloat()

		val index = y * width + x
		var node: PathNode? = nodes[index]
		if (node != null && node.runID == runID) { // Node already encountered for this run.
			if (node.closedID != runID && pathCost < node.pathCost) { // Node isn't closed and new cost is lower.
				// Update the existing node.
				open.setValue(node, score)
				node.parent = parent
				node.pathCost = pathCost
			} else {
				node = null
			}
		} else {
			// Use node from the cache or create a new one.
			if (node == null) {
				node = PathNode(0f)
				node.x = x
				node.y = y
				nodes[index] = node
			}
			open.add(node, score)
			node.runID = runID
			node.parent = parent
			node.pathCost = pathCost
		}
		if (node != null) {
			if (distance < closestDistance) {
				closestNode = node
				closestDistance = distance
			}
		}
	}

	protected open fun isValid(x: Int, y: Int): Boolean {
		return true
	}

	private class PathNode(value: Float) : BinaryHeapNode(value) {
		internal var runID: Int = 0
		internal var closedID: Int = 0
		internal var x: Int = 0
		internal var y: Int = 0
		internal var pathCost: Int = 0
		internal var parent: PathNode? = null
	}

	companion object {

		/**
		 * The maximum difference we can deviate from the closest node we've found so far.
		 */
		private val MAX_DEVIATION_PERCENT = 1.5f
		private val MAX_DEVIATION = 4.0f
	}
}