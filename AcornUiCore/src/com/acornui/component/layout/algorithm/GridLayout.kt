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

import com.acornui.collection.ActiveList
import com.acornui.collection.addAll
import com.acornui.collection.addOrSet
import com.acornui.collection.fill
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
import com.acornui.math.MathUtils.clamp
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.observe.Observable
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class GridLayout : LayoutAlgorithm<GridLayoutStyle, GridLayoutData> {

	private val _measuredColWidths = ArrayList<Float>()

	/**
	 * The measured column widths. This is accurate after a layout.
	 */
	val measuredColWidths: List<Float>
		get() = _measuredColWidths

	private val _measuredRowHeights = ArrayList<Float>()

	/**
	 * The measured row heights. This is accurate after a layout.
	 */
	val measuredRowHeights: List<Float>
		get() = _measuredRowHeights

	private val columnPreferredWidths = ArrayList<Float?>()

	private val rowOccupancy = ArrayList<Int>()

	override fun calculateSizeConstraints(elements: List<LayoutElementRo>, props: GridLayoutStyle, out: SizeConstraints) {
		var minWidth = 0f
		for (i in 0..props.columns.lastIndex) {
			val c = props.columns[i]
			if (c.minWidth != null) minWidth += c.minWidth!!
		}
		out.width.min = minWidth
	}

	internal fun cellWalk(elements: List<BasicLayoutElement>, props: GridLayoutStyle, callback: CellFilter) {
		var colIndex = 0
		var rowIndex = 0

		rowOccupancy.clear()
		rowOccupancy.fill(props.columns.size) { 0 }

		for (i in 0..elements.lastIndex) {
			val e = elements[i]
			callback(e, rowIndex, colIndex)

			val layoutData = e.layoutData as GridLayoutData?
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

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, props: GridLayoutStyle, out: Bounds) {
		val childAvailableWidth: Float? = props.padding.reduceWidth(explicitWidth)

		_measuredColWidths.clear()
		_measuredRowHeights.clear()
		columnPreferredWidths.clear()
		// Calculate initial preferred column widths. The flexible columns will later be fit in the remaining space.
		for (i in 0..props.columns.lastIndex) {
			val col = props.columns[i]
			columnPreferredWidths.add(col.getPreferredWidth(childAvailableWidth))

			// Preset all the measured column widths so that inflexible columns will always have a measured size of
			// at least the preferred width.
			val w = if (childAvailableWidth != null && col.getIsFlexible()) 0f
			else columnPreferredWidths[i] ?: 0f
			_measuredColWidths.addOrSet(i, maxOf(col.minWidth ?: 0f, w))
		}

		// Size inflexible cells.
		cellWalk(elements, props) {
			element, rowIndex, colIndex ->
			val layoutData = element.layoutData as GridLayoutData?
			val colSpan = layoutData?.colSpan ?: 1
			var notFlexible = true
			var availableSpanWidth: Float? = 0f
			// If any column an element spans across is percent-based, it is considered flexible.
			for (i in colIndex..colIndex + colSpan - 1) {
				val col = props.columns[i]
				if (childAvailableWidth != null && col.getIsFlexible()) {
					notFlexible = false
					break
				} else {
					availableSpanWidth = if (columnPreferredWidths[colIndex] == null) null
					else availableSpanWidth!! + columnPreferredWidths[colIndex]!! + props.horizontalGap
				}
			}
			if (notFlexible) {
				if (availableSpanWidth != null) availableSpanWidth -= props.horizontalGap
				val cellW = layoutData?.getPreferredWidth(availableSpanWidth)
				val cellH = layoutData?.getPreferredHeight(props.rowHeight)
				element.setSize(cellW, cellH)

				val rowSpan = layoutData?.rowSpan ?: 1
				val h = (element.height - (rowSpan - 1) * props.verticalGap) / rowSpan
				_measuredRowHeights.fill(rowIndex + rowSpan, { props.rowHeight ?: 0f })
				for (i in rowIndex..rowIndex + rowSpan - 1) {
					_measuredRowHeights[i] = maxOf(_measuredRowHeights[i], h)
				}
				fitMeasuredSizeIntoColumns(colIndex, colSpan, element.width, props)
			}
		}

		// Set the preferred width on inflexible columns to their newly measured widths.
		for (i in 0..props.columns.lastIndex) {
			val col = props.columns[i]
			if (childAvailableWidth == null || !col.getIsFlexible())
				columnPreferredWidths[i] = _measuredColWidths[i]
		}

		// Size flexible columns to fit the remaining available width
		if (childAvailableWidth != null) {
			var inflexibleWidth = 0f
			var flexibleWidth = 0f
			for (i in 0..props.columns.lastIndex) {
				val col = props.columns[i]
				val preferredWidth = columnPreferredWidths[i]!!
				if (col.getIsFlexible()) {
					flexibleWidth += preferredWidth
				} else {
					inflexibleWidth += preferredWidth
				}
				inflexibleWidth += props.horizontalGap
			}

			inflexibleWidth -= props.horizontalGap
			val colScale = if (flexibleWidth > 0f)
				clamp((childAvailableWidth - inflexibleWidth) / flexibleWidth, 0f, if (props.allowScaleUp) 10000f else 1f)
			else 1f

			for (i in 0..props.columns.lastIndex) {
				val col = props.columns[i]
				if (col.getIsFlexible()) {
					columnPreferredWidths[i] = colScale * col.getPreferredWidth(childAvailableWidth)!! // getPreferredWidth may not return null in a flexible column.
					_measuredColWidths[i] = maxOf(_measuredColWidths[i], columnPreferredWidths[i]!!)
				}
			}
		}

		// Size flexible cells.
		// All columns are guaranteed to have preferred widths set at this point.
		if (childAvailableWidth != null) {
			cellWalk(elements, props) {
				element, rowIndex, colIndex ->
				val layoutData = element.layoutData as GridLayoutData?
				val colSpan = layoutData?.colSpan ?: 1
				var flexible = false
				var availableSpanWidth = 0f
				for (i in colIndex..colIndex + colSpan - 1) {
					val col = props.columns[i]
					if (col.getIsFlexible())
						flexible = true
					availableSpanWidth += columnPreferredWidths[i]!! + props.horizontalGap
				}
				availableSpanWidth -= props.horizontalGap
				if (flexible) {
					val cellW = layoutData?.getPreferredWidth(availableSpanWidth)
					val cellH = layoutData?.getPreferredHeight(props.rowHeight)
					element.setSize(cellW, cellH)

					val rowSpan = layoutData?.rowSpan ?: 1
					val h = (element.height - (rowSpan - 1) * props.verticalGap) / rowSpan
					_measuredRowHeights.fill(rowIndex + rowSpan, { props.rowHeight ?: 0f })
					for (i in rowIndex..rowIndex + rowSpan - 1) {
						_measuredRowHeights[i] = maxOf(_measuredRowHeights[i], h)
					}
					fitMeasuredSizeIntoColumns(colIndex, colSpan, element.width, props)
				}
			}
		}

		// Finally, position the elements.

		var x = props.padding.left
		var y = props.padding.top
		var lastRowIndex = 0

		cellWalk(elements, props) {
			element, rowIndex, colIndex ->

			if (rowIndex != lastRowIndex) {
				x = props.padding.left
				for (i in 0..colIndex - 1) {
					x += _measuredColWidths[i] + props.horizontalGap
				}
				for (i in lastRowIndex..rowIndex - 1) {
					y += _measuredRowHeights[i] + props.verticalGap
				}
				lastRowIndex = rowIndex
			}

			val layoutData = element.layoutData as GridLayoutData?

			val colSpan = layoutData?.colSpan ?: 1
			var measuredSpanWidth = 0f
			for (i in colIndex..colIndex + colSpan - 1) {
				measuredSpanWidth += _measuredColWidths[i] + props.horizontalGap
			}
			measuredSpanWidth -= props.horizontalGap
			val xOffset = when (layoutData?.horizontalAlign ?: props.columns[colIndex].hAlign) {
				HAlign.LEFT -> 0f
				HAlign.CENTER -> (measuredSpanWidth - element.width) * 0.5f
				HAlign.RIGHT -> measuredSpanWidth - element.width
			}

			val rowSpan = layoutData?.rowSpan ?: 1
			var measuredSpanHeight = 0f
			for (i in rowIndex..rowIndex + rowSpan - 1) {
				measuredSpanHeight += _measuredRowHeights[i] + props.verticalGap
			}
			measuredSpanHeight -= props.verticalGap
			val yOffset = when (layoutData?.verticalAlign ?: props.verticalAlign) {
				VAlign.TOP -> 0f
				VAlign.MIDDLE -> (measuredSpanHeight - element.height) * 0.5f
				VAlign.BOTTOM -> measuredSpanHeight - element.height
			}
			element.moveTo(x + xOffset, y + yOffset)
			x += measuredSpanWidth + props.horizontalGap
		}
		for (i in lastRowIndex.._measuredRowHeights.lastIndex) {
			y += _measuredRowHeights[i] + props.verticalGap
		}
		y += props.padding.bottom - props.verticalGap
		var maxWidth = props.padding.left
		for (i in 0..props.columns.lastIndex) {
			maxWidth += _measuredColWidths[i] + props.horizontalGap
		}
		maxWidth += props.padding.right - props.horizontalGap
		out.set(maxWidth, y)
	}

	private fun fitMeasuredSizeIntoColumns(colIndex: Int, colSpan: Int, measuredWidth: Float, props: GridLayoutStyle) {
		if (colSpan == 1) {
			_measuredColWidths[colIndex] = maxOf(_measuredColWidths[colIndex], measuredWidth)
			return
		}
		val totalActual = measuredWidth - props.horizontalGap * (colSpan - 1)
		val evenSplit = totalActual / colSpan

		var totalPreferred = 0f
		for (i in colIndex..colIndex + colSpan - 1) {
			totalPreferred += columnPreferredWidths[i] ?: evenSplit
		}

		for (i in colIndex..colIndex + colSpan - 1) {
			val p = if (totalPreferred <= 0f) {
				1f / colSpan
			} else {
				(columnPreferredWidths[i] ?: evenSplit) / totalPreferred
			}
			_measuredColWidths[i] = maxOf(_measuredColWidths[i], totalActual * p)
		}
	}

	override fun createLayoutData(): GridLayoutData {
		return GridLayoutData()
	}
}

private typealias CellFilter = (element: BasicLayoutElement, rowIndex: Int, colIndex: Int) -> Unit


/**
 * A GridColumn contains column properties for the [GridLayout]
 */
class GridColumn : Observable {

	private val _changed = Signal1<Observable>()
	override val changed: Signal<(Observable) -> Unit>
		get() = _changed

	var width: Float? by bindable(null)
	var widthPercent: Float? by bindable(null)
	var minWidth: Float? by bindable(null)
	var hAlign: HAlign by bindable(HAlign.LEFT)

	/**
	 * @see getIsFlexible
	 */
	var flexible: Boolean? by bindable(null)

	/**
	 * A flexible column will flex its size to fit within the available bounds of the container.
	 * A column is considered flexible first considering the [flexible] flag, and if that is not set,
	 * then it goes by if [widthPercent] is set.
	 */
	fun getIsFlexible(): Boolean {
		return (width != null || widthPercent != null) && flexible ?: (widthPercent != null)
	}

	fun getPreferredWidth(availableWidth: Float?): Float? {
		var w = if (availableWidth == null || widthPercent == null) width else widthPercent!! * availableWidth
		if (minWidth != null && w != null && minWidth!! > w) w = minWidth
		return w
	}

	private fun <T> bindable(initialValue: T): ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
		override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
			_changed.dispatch(this@GridColumn)
		}
	}
}

fun gridColumn(init: GridColumn.() -> Unit = {}): GridColumn {
	val c = GridColumn()
	c.init()
	return c
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
	var verticalAlign by prop(VAlign.BOTTOM)

	/**
	 * If set, the height of each row will be fixed to this value.
	 */
	var rowHeight: Float? by prop(null)

	/**
	 * If true, flexible columns may be proportionally given more space in order to fit the available width.
	 */
	var allowScaleUp: Boolean by prop(false)

	// TODO: Make this a styleable property...
	/**
	 * The columns for the grid to use.
	 */
	val columns: MutableList<GridColumn> = ActiveList()

	companion object : StyleType<GridLayoutStyle>
}

open class GridLayoutData : BasicLayoutData() {

	var colSpan by bindable(1)
	var rowSpan by bindable(1)
	var horizontalAlign: HAlign? by bindable(null)
	var verticalAlign: VAlign? by bindable(null)

}

fun gridLayoutData(init: GridLayoutData.() -> Unit): GridLayoutData {
	val g = GridLayoutData()
	g.init()
	return g
}

open class GridLayoutContainer(owner: Owned) : LayoutContainerImpl<GridLayoutStyle, GridLayoutData>(owner, GridLayout(), GridLayoutStyle())

fun Owned.grid(init: ComponentInit<GridLayoutContainer> = {}): GridLayoutContainer {
	val c = GridLayoutContainer(this)
	c.init()
	return c
}

open class FormContainer(owner: Owned) : GridLayoutContainer(owner) {
	init {
		styleTags.add(FormContainer)
		style.apply {
			columns.addAll(
					gridColumn {
						hAlign = HAlign.RIGHT
						widthPercent = 0.4f
					},
					gridColumn {
						widthPercent = 0.6f
					}
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