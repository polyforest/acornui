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

package com.acornui.component.layout.algorithm

import com.acornui.collection.Clearable
import com.acornui.collection.ClearableObjectPool
import com.acornui.collection.freeTo
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.ComponentInit
import com.acornui.component.layout.LayoutContainerImpl
import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.LayoutElementRo
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.core.di.Owned
import com.acornui.core.floor
import com.acornui.math.*
import kotlin.math.round

/**
 * A layout where the elements are placed left to right, then wraps before reaching the explicit width of the container.
 */
class FlowLayout : LayoutAlgorithm<FlowLayoutStyle, FlowLayoutData>, SequencedLayout<FlowLayoutStyle, FlowLayoutData> {

	private val _lines = ArrayList<LineInfo>()

	/**
	 * The list of current lines. This is valid after a layout.
	 */
	val lines: List<LineInfoRo>
		get() = _lines

	override fun calculateSizeConstraints(elements: List<LayoutElementRo>, props: FlowLayoutStyle, out: SizeConstraints) {
		val padding = props.padding
		var minWidth = 0f
		for (i in 0..elements.lastIndex) {
			minWidth = maxOf(minWidth, elements[i].minWidth ?: 0f)
		}
		out.width.min = minWidth + padding.left + padding.right
	}

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, props: FlowLayoutStyle, out: Bounds) {
		val padding = props.padding
		val availableWidth: Float? = padding.reduceWidth(explicitWidth)
		val availableHeight: Float? = padding.reduceHeight(explicitHeight)

		var measuredW = 0f
		_lines.freeTo(linesPool)
		var line = linesPool.obtain()
		line.y = padding.top
		var x = 0f
		var y = 0f
		var previousElement: LayoutElement? = null

		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutDataCast
			element.setSize(layoutData?.getPreferredWidth(availableWidth), layoutData?.getPreferredHeight(availableHeight))
			if (availableWidth != null && element.width > availableWidth) {
				element.width(availableWidth)
			}
			val w = element.width
			val h = element.height
			val doesOverhang = layoutData?.overhangs ?: false

			if (props.multiline && i > line.startIndex &&
					(previousElement!!.clearsLine() || element.startsNewLine() ||
							(availableWidth != null && !doesOverhang && x + w > availableWidth))) {
				line.endIndex = i
				line.x = calculateLineX(availableWidth, props, line.contentsWidth)
				positionElementsInLine(line, availableWidth, props, elements)
				_lines.add(line)
				x = 0f
				y += line.height + props.verticalGap
				// New line
				line = linesPool.obtain()
				line.startIndex = i
				line.y = y + padding.top
			}
			x += w

			line.width = x
			if (!doesOverhang) {
				line.contentsWidth = x
				if (x > measuredW) measuredW = x
			}
			x += props.horizontalGap
			val baseline = layoutData?.baseline ?: h
			if (baseline > line.baseline) line.baseline = baseline
			val belowBaseline = h - baseline
			if (belowBaseline > belowBaseline) line.belowBaseline = belowBaseline
			previousElement = element
		}
		line.endIndex = elements.size
		if (line.isNotEmpty()) {
			line.x = calculateLineX(availableWidth, props, line.contentsWidth)
			positionElementsInLine(line, availableWidth, props, elements)
			_lines.add(line)
			y += line.height
		} else {
			linesPool.free(line)
			y -= props.verticalGap
		}
		measuredW += padding.left + padding.right
		if (measuredW > out.width) out.width = measuredW // Use the measured width if it is larger than the explicit.
		val measuredH = padding.expandHeight2(y)
		if (measuredH > out.height) out.height = measuredH
	}

	private fun calculateLineX(availableWidth: Float?, props: FlowLayoutStyle, lineWidth: Float): Float {
		return if (availableWidth != null) {
			val remainingSpace = availableWidth - lineWidth
			props.padding.left + when (props.horizontalAlign) {
				FlowHAlign.LEFT -> 0f
				FlowHAlign.CENTER -> round(remainingSpace * 0.5f)
				FlowHAlign.RIGHT -> remainingSpace
				FlowHAlign.JUSTIFY -> 0f
			}
		} else {
			props.padding.left
		}
	}

	/**
	 * Adjusts the elements within a line to apply the horizontal and vertical alignment.
	 */
	private fun positionElementsInLine(line: LineInfoRo, availableWidth: Float?, props: FlowLayoutStyle, elements: List<LayoutElement>) {
		val hGap = if (availableWidth != null) {
			val remainingSpace = availableWidth - line.contentsWidth
			if (props.horizontalAlign == FlowHAlign.JUSTIFY &&
					line.size > 1 &&
					line.endIndex != elements.size &&
					!elements[line.endIndex - 1].clearsLine() &&
					!elements[line.endIndex].startsNewLine()) {
				// Apply JUSTIFY spacing if this is not the last line, and there are more than one elements.
				(props.horizontalGap + remainingSpace / (line.endIndex - line.startIndex - 1)).floor()
			} else {
				props.horizontalGap
			}
		} else {
			props.horizontalGap
		}

		var x = 0f
		for (j in line.startIndex..line.endIndex - 1) {
			val element = elements[j]

			val layoutData = element.layoutDataCast
			val yOffset = when (layoutData?.verticalAlign ?: props.verticalAlign) {
				FlowVAlign.TOP -> 0f
				FlowVAlign.MIDDLE -> round((line.height - element.height) * 0.5f)
				FlowVAlign.BOTTOM -> (line.height - element.height)
				FlowVAlign.BASELINE -> (line.baseline - (layoutData?.baseline ?: element.height))
			}
			element.moveTo(line.x + x, line.y + yOffset)
			x += element.width + hGap
		}
	}

	override fun createLayoutData(): FlowLayoutData {
		return FlowLayoutData()
	}

	private fun LayoutElementRo.clearsLine(): Boolean {
		return (layoutData as FlowLayoutData?)?.clearsLine ?: false
	}

	private fun LayoutElementRo.startsNewLine(): Boolean {
		return (layoutData as FlowLayoutData?)?.startsNewLine ?: false
	}

	override fun getElementInsertionIndex(x: Float, y: Float, elements: List<LayoutElement>, props: FlowLayoutStyle): Int {
		if (lines.isEmpty()) return 0
		if (y < lines.first().y) return 0
		if (y >= lines.last().bottom) return elements.size
		val lineIndex = _lines.sortedInsertionIndex(y, comparator = { y, line ->
			y.compareTo(line.bottom)
		})
		val line = _lines[lineIndex]
		return elements.sortedInsertionIndex(x, line.startIndex, line.endIndex) {
			x, element ->
			x.compareTo(element.right)
		}
	}

	companion object {
		private val linesPool = ClearableObjectPool { LineInfo() }
	}

}

interface LineInfoRo {

	/**
	 * The line's start index, inclusive.
	 */
	val startIndex: Int

	/**
	 * The line's end index, exclusive.
	 */
	val endIndex: Int
	val x: Float
	val y: Float

	/**
	 * The width of the line, counting overhanging whitespace.
	 */
	val width: Float

	/**
	 * The width of the line, not counting overhanging whitespace.
	 */
	val contentsWidth: Float

	/**
	 * The [baseline] + [belowBaseline]
	 */
	val height: Float
		get() = baseline + belowBaseline
	val baseline: Float
	val belowBaseline: Float

	val bottom: Float
		get() = y + height

	fun isEmpty(): Boolean {
		return size <= 0
	}

	fun isNotEmpty(): Boolean = !isEmpty()

	val size: Int
		get() = endIndex - startIndex
}

class LineInfo : Clearable, LineInfoRo {

	override var startIndex: Int = 0

	/**
	 * The end index of the line. (exclusive)
	 */
	override var endIndex: Int = 0
	override var x: Float = 0f
	override var y: Float = 0f

	override var contentsWidth: Float = 0f
	override var width: Float = 0f
	override var baseline: Float = 0f
	override var belowBaseline: Float = 0f

	override fun clear() {
		startIndex = 0
		endIndex = 0
		contentsWidth = 0f
		width = 0f
		x = 0f
		y = 0f
		baseline = 0f
		belowBaseline = 0f
	}

	override fun toString(): String {
		return "LineInfo(startIndex=$startIndex, endIndex=$endIndex)"
	}


}

class FlowLayoutStyle : StyleBase() {

	override val type = Companion

	var horizontalGap by prop(5f)
	var verticalGap by prop(5f)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding: PadRo by prop(Pad())
	var horizontalAlign by prop(FlowHAlign.LEFT)
	var verticalAlign by prop(FlowVAlign.TOP)
	var multiline by prop(true)

	companion object : StyleType<FlowLayoutStyle>
}

class FlowLayoutData : BasicLayoutData() {

	/**
	 * If true, this element will cause the line to break after this element.
	 */
	var clearsLine by bindable(false)

	/**
	 * If true, this element will cause the line to break before this element.
	 */
	var startsNewLine by bindable(false)

	/**
	 * True if this layout element can overhang off the edge of the boundaries.
	 */
	var overhangs by bindable(false)

	/**
	 * If set, the vertical align of this element overrides the default of the flow layout algorithm.
	 */
	var verticalAlign: FlowVAlign? by bindable(null)

	/**
	 * A child whose vertical alignment is [FlowVAlign.BASELINE] will be positioned so that the baseline attribute
	 * will align with the baseline position on its line. If left null, the height will be used.
	 */
	var baseline by bindable<Float?>(null)

}

enum class FlowHAlign {
	LEFT,
	CENTER,
	RIGHT,

	/**
	 * The left edge will be at padding.left, the right edge will be at padding.right, and the horizontal gap will
	 * be the horizontal gap plus an even distribution of remaining space.
	 */
	JUSTIFY
}

enum class FlowVAlign {
	TOP,
	MIDDLE,
	BOTTOM,
	BASELINE
}

open class FlowLayoutContainer(owner: Owned) : LayoutContainerImpl<FlowLayoutStyle, FlowLayoutData>(owner, FlowLayout(), FlowLayoutStyle())

fun Owned.flow(init: ComponentInit<FlowLayoutContainer> = {}): FlowLayoutContainer {
	val flowContainer = FlowLayoutContainer(this)
	flowContainer.init()
	return flowContainer
}