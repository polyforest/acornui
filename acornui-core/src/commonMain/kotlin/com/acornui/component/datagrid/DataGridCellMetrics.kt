package com.acornui.component.datagrid

import com.acornui.collection.sortedInsertionIndex
import com.acornui.math.Bounds
import com.acornui.math.BoundsRo
import com.acornui.recycle.Clearable

interface DataGridCellMetricsRo {

	/**
	 * The start position these metrics represent.
	 */
	val startPosition: Float

	/**
	 * The end position these metrics represent.
	 */
	val endPosition: Float

	/**
	 * The total number of visible rows. (i.e. [endPosition] - [startPosition])
	 * This includes header and footer rows.
	 */
	val visibleRows: Float
		get() = endPosition - startPosition

	/**
	 * The heights of each visible row.  (Rows startPosition.toInt() through endPosition.toInt())
	 * This includes header and footer rows.
	 */
	val rowHeights: List<Float>

	/**
	 * The y positions of each visible row. (Rows startPosition.toInt() through endPosition.toInt())
	 * This includes header and footer rows.
	 */
	val rowPositions: List<Float>

	/**
	 * The total width and height of the cell area.
	 */
	val bounds: BoundsRo

	fun isEmpty(): Boolean = endPosition <= startPosition

}

class DataGridCellMetrics : DataGridCellMetricsRo, Clearable {

	override val rowHeights: MutableList<Float> = ArrayList()
	override val rowPositions: MutableList<Float> = ArrayList()
	override val bounds = Bounds()

	override var startPosition: Float = -1f
	override var endPosition: Float = -1f

	override fun clear() {
		rowHeights.clear()
		rowPositions.clear()
		bounds.clear()
		startPosition = -1f
		endPosition = -1f
	}
}

/**
 * Returns the data position at the given yValue. This will be fractional to represent the y position being partially
 * into a row. Use [Float.toInt] to determine the index.
 * @param yValue The y value, relative to the cells container.
 * @return Returns -1f if the yValue is not within the rows y range (yValue < 0f or >= bounds.height).
 */
fun DataGridCellMetricsRo.getPositionAtY(yValue: Float): Float {
	if (rowPositions.isEmpty() || yValue < 0f || yValue >= bounds.height) return -1f
	val rowIndex = rowPositions.sortedInsertionIndex(yValue, matchForwards = true) - 1
	val rowPosition = rowPositions[rowIndex]
	val rowHeight = rowHeights[rowIndex]
	return startPosition.toInt() + rowIndex.toFloat() + (yValue - rowPosition) / rowHeight
}