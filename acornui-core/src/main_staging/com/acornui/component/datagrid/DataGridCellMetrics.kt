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

package com.acornui.component.datagrid

import com.acornui.collection.sortedInsertionIndex
import com.acornui.math.Bounds
import com.acornui.math.BoundsRo
import com.acornui.recycle.Clearable

interface DataGridCellMetricsRo {

	/**
	 * The start position these metrics represent.
	 */
	val startPosition: Double

	/**
	 * The end position these metrics represent.
	 */
	val endPosition: Double

	/**
	 * The total number of visible rows. (i.e. [endPosition] - [startPosition])
	 * This includes header and footer rows.
	 */
	val visibleRows: Double
		get() = endPosition - startPosition

	/**
	 * Returns true if the row index is within the position range.
	 */
	fun rowIsVisible(rowIndex: Int): Boolean = rowIndex in startPosition.toInt() .. endPosition.toInt()

	/**
	 * The heights of each visible row.  (Rows startPosition.toInt() through endPosition.toInt())
	 * This includes header and footer rows.
	 */
	val rowHeights: List<Double>

	/**
	 * The y positions of each visible row. (Rows startPosition.toInt() through endPosition.toInt())
	 * This includes header and footer rows.
	 */
	val rowPositions: List<Double>

	/**
	 * The total width and height of the cell area.
	 */
	val bounds: BoundsRo

	fun isEmpty(): Boolean = endPosition <= startPosition

}

class DataGridCellMetrics : DataGridCellMetricsRo, Clearable {

	override val rowHeights: MutableList<Double> = ArrayList()
	override val rowPositions: MutableList<Double> = ArrayList()
	override val bounds = Bounds()

	override var startPosition: Double = -1.0
	override var endPosition: Double = -1.0

	override fun clear() {
		rowHeights.clear()
		rowPositions.clear()
		bounds.clear()
		startPosition = -1.0
		endPosition = -1.0
	}
}

/**
 * Returns the data position at the given yValue. This will be fractional to represent the y position being partially
 * into a row. Use [Double.toInt] to determine the index.
 * @param yValue The y value, relative to the cells container.
 * @return Returns -1.0 if the yValue is not within the rows y range (yValue < 0.0 or >= bounds.height).
 */
fun DataGridCellMetricsRo.getPositionAtY(yValue: Double): Double {
	if (rowPositions.isEmpty() || yValue < 0.0 || yValue >= bounds.height) return -1.0
	val rowIndex = rowPositions.sortedInsertionIndex(yValue, matchForwards = true) - 1
	val rowPosition = rowPositions[rowIndex]
	val rowHeight = rowHeights[rowIndex]
	return startPosition.toInt() + rowIndex.toDouble() + (yValue - rowPosition) / rowHeight
}
