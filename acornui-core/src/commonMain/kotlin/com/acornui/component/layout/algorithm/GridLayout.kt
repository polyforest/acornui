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

package com.acornui.component.layout.algorithm

import com.acornui.collection.*
import com.acornui.component.ComponentInit
import com.acornui.component.UiComponent
import com.acornui.component.layout.ElementLayoutContainer
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.VAlign
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.style.styleTag
import com.acornui.component.text.TextField
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

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
		lines.forEach2(action = LineInfo.Companion::free)
		lines.clear()

		// The sum of the explicit width of all columns.
		val totalColumnsExplicitWidth = if (childAvailableWidth == null) null else childAvailableWidth - style.horizontalGap * columns.lastIndex

		// Calculate initial column widths. The flexible columns will later be fit in the remaining space.
		var totalMeasuredColumnWidths = 0f
		for (i in 0..columns.lastIndex) {
			val col = columns[i]
			val measuredColWidth = col.getPreferredWidth(totalColumnsExplicitWidth) ?: col.minWidth ?: 0f
			measuredColWidths.add(measuredColWidth)
			totalMeasuredColumnWidths += measuredColWidth
		}
		if (totalColumnsExplicitWidth != null) {
			// Scale the measured columns down to fit if needed.
			if (totalMeasuredColumnWidths > totalColumnsExplicitWidth) {
				val scale = totalColumnsExplicitWidth / totalMeasuredColumnWidths
				for (i in 0..measuredColWidths.lastIndex) {
					measuredColWidths[i] *= scale
				}
			}
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
			val lastColIndex = colIndex + colSpan - 1
			val lastRowIndex = rowIndex + rowSpan - 1

			val measuredSpanWidth = measuredColWidths.sum2(colIndex, lastColIndex) + style.horizontalGap * (colSpan - 1)

			val cellW = if (layoutData.widthPercent != null) {
				layoutData.getPreferredWidth(measuredSpanWidth)
			} else {
				var explicitSpanWidth: Float? = 0f
				for (j in colIndex..lastColIndex) {
					val col = columns[j]
					val colW = col.getPreferredWidth(totalColumnsExplicitWidth) ?: col.minWidth
					if (colW == null) {
						explicitSpanWidth = null
						break
					} else {
						explicitSpanWidth = colW + explicitSpanWidth!!
					}
				}
				layoutData.getPreferredWidth(explicitSpanWidth)
			}

			val measuredSpanHeight = lines.sumByFloat2(rowIndex, lastRowIndex) { it.height } + style.verticalGap * (rowSpan - 1)
			val cellH = if (layoutData.heightPercent != null) {
				layoutData.getPreferredHeight(measuredSpanHeight)
			} else {
				val explicitSpanHeight: Float? = if (style.rowHeight == null) null else style.rowHeight!! * rowSpan + style.verticalGap * (rowSpan - 1)
				layoutData.getPreferredHeight(explicitSpanHeight)
			}
			element.setSize(cellW, cellH)

			if (layoutData.verticalAlign ?: style.verticalAlign == VAlign.BASELINE) {
				val line = lines[rowIndex]
				if (element.baseline > line.baseline)
					line.baseline = element.baseline
				if (element.descender > line.descender)
					line.descender = element.descender
			}

			val elementH = element.height
			if (elementH > measuredSpanHeight) {
				// Increase the spanned line heights evenly.
				val incH = (elementH - measuredSpanHeight) / rowSpan
				for (j in rowIndex..lastRowIndex) {
					val line = lines[j]
					line.nonBaselineHeight += incH
				}
			}
			val elementW = element.width
			if (elementW > measuredSpanWidth) {
				// Increase the spanned column widths evenly across the flexible columns.
				val numFlexibleColumns = columns.sumByInt2(colIndex, lastColIndex) { if (it.getIsFlexible()) 1 else 0 }
				if (numFlexibleColumns > 0) {
					val incW = (elementW - measuredSpanWidth) / numFlexibleColumns
					for (j in colIndex..lastColIndex) {
						if (columns[j].getIsFlexible())
							measuredColWidths[j] += incW
					}
				}
			}
		}

		// Position the elements. (Order doesn't matter here)

		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutDataCast!!
			val rowSpan = layoutData.rowSpan
			val colSpan = layoutData.colSpan
			val colIndex = layoutData.colIndex
			val rowIndex = layoutData.rowIndex
			val lastColIndex = colIndex + colSpan - 1
			val lastRowIndex = rowIndex + rowSpan - 1

			val measuredSpanWidth = measuredColWidths.sum2(colIndex, lastColIndex) + (colSpan - 1) * style.horizontalGap
			val xOffset = when (layoutData.horizontalAlign ?: columns[colIndex].hAlign) {
				HAlign.LEFT -> 0f
				HAlign.CENTER -> (measuredSpanWidth - element.width) * 0.5f
				HAlign.RIGHT -> measuredSpanWidth - element.width
			}

			var measuredSpanHeight = 0f
			for (j in rowIndex..lastRowIndex) {
				measuredSpanHeight += lines[j].height + style.verticalGap
			}
			measuredSpanHeight -= style.verticalGap
			val yOffset = when (layoutData.verticalAlign ?: style.verticalAlign) {
				VAlign.TOP -> 0f
				VAlign.MIDDLE -> (measuredSpanHeight - element.height) * 0.5f
				VAlign.BOTTOM -> measuredSpanHeight - element.height
				VAlign.BASELINE -> lines[rowIndex].baseline - element.baseline
			}
			val x = padding.left + measuredColWidths.sumByFloat2(0, colIndex - 1) { it } + style.horizontalGap * colIndex
			val y = padding.top + lines.sumByFloat2(0, rowIndex - 1) { it.height } + style.verticalGap * rowIndex
			element.moveTo(x + xOffset, y + yOffset)
		}
		val width = padding.expandWidth(measuredColWidths.sumByFloat2 { it } + style.horizontalGap * columns.lastIndex)
		val height = padding.expandHeight(lines.sumByFloat2 { it.height } + style.verticalGap * lines.lastIndex)
		out.set(width, height, baseline = lines.firstOrNull()?.baseline ?: 0f)

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

open class GridLayoutContainer<E : UiComponent>(owner: Context) : ElementLayoutContainer<GridLayoutStyle, GridLayoutData, E>(owner, GridLayout())

@JvmName("gridT")
inline fun <E : UiComponent> Context.grid(init: ComponentInit<GridLayoutContainer<E>> = {}): GridLayoutContainer<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return GridLayoutContainer<E>(this).apply(init)
}

inline fun Context.grid(init: ComponentInit<GridLayoutContainer<UiComponent>> = {}): GridLayoutContainer<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return grid<UiComponent>(init)
}

open class FormContainer<E : UiComponent>(owner: Context) : GridLayoutContainer<UiComponent>(owner) {
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

inline fun Context.form(init: ComponentInit<FormContainer<UiComponent>> = {}): FormContainer<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return FormContainer<UiComponent>(this).apply(init)
}

val formLabelStyle = styleTag()

inline fun Context.formLabel(text: String = "", init: ComponentInit<TextField> = {}): TextField {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return text(text) {
		styleTags.add(formLabelStyle)
	}.apply(init)
}
