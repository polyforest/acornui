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

package com.acornui.component.layout.algorithm

import com.acornui.collection.*
import com.acornui.component.ComponentInit
import com.acornui.component.layout.*
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.styleTag
import com.acornui.component.text.TextField
import com.acornui.component.text.text
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo

class GridLayout : LayoutAlgorithm<GridLayoutStyle, GridLayoutData> {

	override val style = GridLayoutStyle()

	private val _measuredColWidths = ArrayList<Float>()

	/**
	 * The measured column widths. This is accurate after a layout.
	 */
	val measuredColWidths: List<Float>
		get() = _measuredColWidths

	private val _lines = ArrayList<LineInfo>()

	/**
	 * The measured rows. This is accurate after a layout.
	 */
	val lines: List<LineInfoRo> = _lines

	private val rowOccupancy = ArrayList<Int>()

	private val orderedElements = ArrayList<LayoutElement>()

	override fun calculateSizeConstraints(elements: List<LayoutElementRo>, out: SizeConstraints) {
		var minWidth = 0f
		for (i in 0..style.columns.lastIndex) {
			val c = style.columns[i]
			if (c.minWidth != null) minWidth += c.minWidth
		}
		out.width.min = minWidth
	}

	// TODO: can be private once unit tests support private methods
	internal fun cellWalk(elements: List<LayoutElement>, props: GridLayoutStyle, callback: CellFilter) {
		var colIndex = 0
		var rowIndex = 0

		rowOccupancy.clear()
		rowOccupancy.fill(props.columns.size) { 0 }

		for (i in 0..elements.lastIndex) {
			val e = elements[i]
			callback(e, rowIndex, colIndex)
			val layoutData = e.layoutDataCast
			val colSpan = layoutData?.colSpan ?: 1
			val rowSpan = layoutData?.rowSpan ?: 1

			// Pass through the columns where a previous cell in this row had a colSpan of > 1
			for (j in colIndex..minOf(colIndex + colSpan - 1, props.columns.lastIndex)) {
				rowOccupancy[j] = maxOf(rowOccupancy[j], rowSpan)
			}

			// Find the next unoccupied cell.
			while (true) {
				if (rowOccupancy[colIndex] > 0) {
					rowOccupancy[colIndex]--
					colIndex++
					if (colIndex >= props.columns.size) {
						colIndex = 0
						rowIndex++
					}
				} else {
					break
				}
			}
		}
	}

	private val elementComparator = compareBy<GridLayoutData>(
			{ -it.priority },
			{ it.widthPercent == null },
			{ it.heightPercent == null },
			{ it.colSpan > 1 },
			{ it.rowSpan > 1 }
	)

	private fun elementOrderComparator(o1: LayoutElement, o2: LayoutElement): Int {
		return elementComparator.compare(o1.layoutDataCast!!, o2.layoutDataCast!!)
	}

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, out: Bounds) {
		val childAvailableWidth: Float? = style.padding.reduceWidth(explicitWidth)

		val measuredColWidths = _measuredColWidths
		val lines = _lines
		val style = style
		val columns = style.columns
		val padding = style.padding

		measuredColWidths.clear()
		lines.forEach2(LineInfo.Companion::free)
		lines.clear()

		// The sum of the explicit width of all columns.
		val columnTotalWidth = if (childAvailableWidth == null) null else childAvailableWidth - style.horizontalGap * columns.lastIndex

		// Calculate initial column widths. The flexible columns will later be fit in the remaining space.
		for (i in 0..columns.lastIndex) {
			val col = columns[i]
			measuredColWidths.add(col.getPreferredWidth(columnTotalWidth) ?: col.minWidth ?: 0f)
		}

		// Set the column and row indices.
		var totalRows = 0
		cellWalk(elements, style) { element, rowIndex, colIndex ->
			if (element.layoutData == null) element.layoutData = GridLayoutData()
			val layoutData = element.layoutDataCast!!
			layoutData.colIndex = colIndex
			layoutData.rowIndex = rowIndex
			totalRows = maxOf(totalRows, rowIndex + layoutData.rowSpan)
		}
		lines.fill(totalRows) { LineInfo.obtain().apply { nonBaselineHeight = style.rowHeight ?: 0f } }

		elements.sortTo(orderedElements, true, ::elementOrderComparator)

		for (i in 0..orderedElements.lastIndex) {
			val element = orderedElements[i]
			val layoutData = element.layoutDataCast!!
			val rowSpan = layoutData.rowSpan
			val colSpan = layoutData.colSpan
			val colIndex = layoutData.colIndex
			val rowIndex = layoutData.rowIndex

			val hGapTotal = style.horizontalGap * (colSpan - 1)
			val availableSpanWidth = measuredColWidths.sum2(colIndex, colIndex + colSpan - 1) + hGapTotal
			val vGapTotal = style.verticalGap * (rowSpan - 1)
			var availableSpanHeight = vGapTotal
			for (j in rowIndex..rowIndex + rowSpan - 1) {
				availableSpanHeight += lines[j].height
			}

			val cellW = layoutData.getPreferredWidth(availableSpanWidth)
			val cellH = layoutData.getPreferredHeight(availableSpanHeight)
			element.setSize(cellW, cellH)

			if (layoutData.verticalAlign ?: style.verticalAlign == VAlign.BASELINE) {
				val line = lines[rowIndex]
				if (element.baseline > line.baseline)
					line.baseline = element.baseline
				if (element.descender > line.descender)
					line.descender = element.descender
			}

			val elementH = element.height
			if (elementH > availableSpanHeight) {
				// Increase the spanned line heights evenly.
				val incH = (elementH - availableSpanHeight) / rowSpan
				for (j in rowIndex..rowIndex + rowSpan - 1) {
					val line = lines[j]
					line.nonBaselineHeight += incH
				}
			}
			val elementW = element.width
			if (elementW > availableSpanWidth) {
				// Increase the spanned column widths evenly across the flexible columns.
				val incW = (elementW - availableSpanWidth) / colSpan
				for (j in colIndex..colIndex + colSpan - 1) {
					if (columns[j].getIsFlexible())
						measuredColWidths[j] += incW
				}
			}
		}

		// Position the elements.

		var x = padding.left
		var y = padding.top
		var lastRowIndex = 0

		cellWalk(elements, style) { element, rowIndex, colIndex ->
			if (rowIndex != lastRowIndex) {
				x = padding.left
				for (i in 0..colIndex - 1) {
					x += measuredColWidths[i] + style.horizontalGap
				}
				for (i in lastRowIndex..rowIndex - 1) {
					y += lines[i].height + style.verticalGap
				}
				lastRowIndex = rowIndex
			}

			val layoutData = element.layoutData as GridLayoutData?

			val colSpan = layoutData?.colSpan ?: 1
			var measuredSpanWidth = 0f
			for (i in colIndex..colIndex + colSpan - 1) {
				measuredSpanWidth += measuredColWidths[i] + style.horizontalGap
			}
			measuredSpanWidth -= style.horizontalGap
			val xOffset = when (layoutData?.horizontalAlign ?: columns[colIndex].hAlign) {
				HAlign.LEFT -> 0f
				HAlign.CENTER -> (measuredSpanWidth - element.width) * 0.5f
				HAlign.RIGHT -> measuredSpanWidth - element.width
			}

			val rowSpan = layoutData?.rowSpan ?: 1
			var measuredSpanHeight = 0f
			for (i in rowIndex..rowIndex + rowSpan - 1) {
				measuredSpanHeight += lines[i].height + style.verticalGap
			}
			measuredSpanHeight -= style.verticalGap
			val yOffset = when (layoutData?.verticalAlign ?: style.verticalAlign) {
				VAlign.TOP -> 0f
				VAlign.MIDDLE -> (measuredSpanHeight - element.height) * 0.5f
				VAlign.BOTTOM -> measuredSpanHeight - element.height
				VAlign.BASELINE -> lines[rowIndex].baseline - element.baseline
			}
			element.moveTo(x + xOffset, y + yOffset)
			x += measuredSpanWidth + style.horizontalGap
		}
		for (i in lastRowIndex..lines.lastIndex) {
			y += lines[i].height + style.verticalGap
		}
		y += padding.bottom - style.verticalGap
		var maxWidth = padding.left
		for (i in 0..columns.lastIndex) {
			maxWidth += measuredColWidths[i] + style.horizontalGap
		}
		maxWidth += padding.right - style.horizontalGap
		out.set(maxWidth, y, baseline = lines.firstOrNull()?.baseline ?: 0f)

		orderedElements.clear()
	}

	override fun createLayoutData(): GridLayoutData {
		return GridLayoutData()
	}
}

private typealias CellFilter = GridLayout.(element: LayoutElement, rowIndex: Int, colIndex: Int) -> Unit


/**
 * A GridColumn contains column properties for the [GridLayout]
 */
data class GridColumn(

		val width: Float? = null,

		val widthPercent: Float? = null,

		val minWidth: Float? = null,

		/**
		 * The horizontal alignment of the column.
		 */
		val hAlign: HAlign = HAlign.LEFT,

		/**
		 * @see getIsFlexible
		 */
		val flexible: Boolean? = null
) {

	/**
	 * A flexible column will flex its size to fit within the available bounds of the container.
	 * A column is considered flexible first considering the [flexible] flag, and if that is not set,
	 * then it goes by if [widthPercent] is set.
	 */
	fun getIsFlexible(): Boolean {
		return (width != null || widthPercent != null) && flexible ?: (widthPercent != null)
	}

	/**
	 * Calculates the preferred column width given the total available column widths (minus padding and gaps).
	 */
	fun getPreferredWidth(availableWidth: Float?): Float? {
		var w = if (availableWidth == null || widthPercent == null) width else widthPercent * availableWidth
		if (minWidth != null && (w == null || minWidth > w)) w = minWidth
		return w
	}
}

open class GridLayoutStyle : StyleBase() {

	override val type = Companion

	/**
	 * The gap between rows.
	 */
	var verticalGap by prop(5f)

	/**
	 * The gap between columns.
	 */
	var horizontalGap by prop(5f)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding: PadRo by prop(Pad())

	/**
	 * The default vertical alignment of the cells relative to their rows.
	 * May be overriden on the individual cell via [GridLayoutData]
	 */
	var verticalAlign by prop(VAlign.BASELINE)

	/**
	 * If set, the height of each row will be fixed to this value.
	 */
	var rowHeight: Float? by prop(null)

	/**
	 * If true, flexible columns may be proportionally given more space in order to fit the available width.
	 */
	var allowScaleUp: Boolean by prop(false)

	/**
	 * The columns for the grid to use.
	 */
	var columns: List<GridColumn> by prop(emptyList())

	companion object : StyleType<GridLayoutStyle>
}

open class GridLayoutData : BasicLayoutData() {

	var colSpan by bindable(1)
	var rowSpan by bindable(1)
	var horizontalAlign: HAlign? by bindable(null)
	var verticalAlign: VAlign? by bindable(null)

	/**
	 * After a layout, this will be set based on the row position the element was placed.
	 */
	var rowIndex: Int = -1
		internal set

	/**
	 * After a layout, this will be set based on the column position the element was placed.
	 */
	var colIndex: Int = -1
		internal set

	/**
	 * The order of sizing precedence is as follows:
	 * - widthPercent null (inflexible width before flexible width)
	 * 		If the column has no preferred width, all elements in that column are considered to have inflexible width.
	 * - priority value (higher values before lower values)
	 * - heightPercent null (inflexible height before flexible height)
	 */
	var priority: Float by bindable(0f)

}

fun gridLayoutData(init: GridLayoutData.() -> Unit): GridLayoutData {
	val g = GridLayoutData()
	g.init()
	return g
}

open class GridLayoutContainer(owner: Owned) : ElementLayoutContainerImpl<GridLayoutStyle, GridLayoutData>(owner, GridLayout())

fun Owned.grid(init: ComponentInit<GridLayoutContainer> = {}): GridLayoutContainer {
	val c = GridLayoutContainer(this)
	c.init()
	return c
}

open class FormContainer(owner: Owned) : GridLayoutContainer(owner) {
	init {
		styleTags.add(FormContainer)
		style.apply {
			columns = listOf(
					GridColumn(
							hAlign = HAlign.RIGHT,
							widthPercent = 0.4f
					),
					GridColumn(
							widthPercent = 0.6f
					)
			)
		}
	}

	companion object : StyleTag
}

fun Owned.form(init: ComponentInit<FormContainer> = {}): FormContainer {
	val c = FormContainer(this)
	c.init()
	return c
}

val formLabelStyle = styleTag()

fun Owned.formLabel(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	return text(text) {
		styleTags.add(formLabelStyle)
		init()
	}
}