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

package com.acornui.component.layout.algorithm

import com.acornui.collection.forEach2
import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.ComponentInit
import com.acornui.component.UiComponent
import com.acornui.component.layout.ElementLayoutContainer
import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.LayoutElementRo
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.Context
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName
import kotlin.math.floor
import kotlin.math.round

/**
 * A layout where the elements are placed left to right, then wraps before reaching the explicit width of the container.
 */
class FlowLayout : LayoutAlgorithm<FlowLayoutStyle, FlowLayoutData>, SequencedLayout<FlowLayoutStyle, FlowLayoutData> {

	override val style = FlowLayoutStyle()

	private val _lines = ArrayList<LineInfo>()

	/**
	 * The list of current lines. This is valid after a layout.
	 */
	val lines: List<LineInfoRo> = _lines

	override fun layout(explicitWidth: Float?, explicitHeight: Float?, elements: List<LayoutElement>, out: Bounds) {
		val padding = style.padding
		val availableWidth: Float? = padding.reduceWidth(explicitWidth)
		val availableHeight: Float? = padding.reduceHeight(explicitHeight)

		var measuredW = 0f
		val lines = _lines
		lines.forEach2(action = LineInfo.Companion::free)
		lines.clear()
		if (elements.isEmpty()) return

		var line = LineInfo.obtain()
		line.y = padding.top
		var x = 0f
		var y = 0f
		var previousElement: LayoutElement? = null

		for (i in 0..elements.lastIndex) {
			val element = elements[i]
			val layoutData = element.layoutDataCast
			element.size(layoutData?.getPreferredWidth(availableWidth), layoutData?.getPreferredHeight(availableHeight))
			val w = element.width
			val h = element.height
			val doesOverhang = layoutData?.overhangs ?: false

			if (style.multiline && i > line.startIndex &&
					(previousElement!!.clearsLine() || element.startsNewLine() ||
							(availableWidth != null && !doesOverhang && x + w > availableWidth))) {
				line.endIndex = i
				line.x = calculateLineX(availableWidth, style, line.contentsWidth)
				positionElementsInLine(line, availableWidth, style, elements)
				lines.add(line)
				x = 0f
				y += line.height + style.verticalGap
				// New line
				line = LineInfo.obtain()
				line.startIndex = i
				line.y = y + padding.top
			}
			x += w

			line.width = x
			if (!doesOverhang) {
				line.contentsWidth = x
				if (x > measuredW) measuredW = x
			}
			x += style.horizontalGap
			if (layoutData?.verticalAlign ?: style.verticalAlign == FlowVAlign.BASELINE) {
				val baseline = element.baseline
				if (baseline > line.baseline)
					line.baseline = baseline
				val belowBaseline = h - baseline
				if (belowBaseline > belowBaseline)
					line.descender = belowBaseline
			}
			val elementH = element.height
			if (elementH > line.nonBaselineHeight) line.nonBaselineHeight = elementH
			previousElement = element
		}
		line.endIndex = elements.size
		if (line.isNotEmpty()) {
			line.x = calculateLineX(availableWidth, style, line.contentsWidth)
			positionElementsInLine(line, availableWidth, style, elements)
			lines.add(line)
			y += line.height
		} else {
			LineInfo.free(line)
			y -= style.verticalGap
		}
		measuredW += padding.left + padding.right
		if (measuredW > out.width) out.width = measuredW // Use the measured width if it is larger than the explicit.
		val measuredH = padding.expandHeight(y)
		if (measuredH > out.height) out.height = measuredH
		out.baseline = lines.firstOrNull()?.baseline ?: measuredH
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
				floor((props.horizontalGap + remainingSpace / (line.endIndex - line.startIndex - 1)))
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
				FlowVAlign.BASELINE -> line.baseline - element.baseline
			}
			element.position(line.x + x, line.y + yOffset)
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
		val lineIndex = _lines.sortedInsertionIndex(y, comparator = { yVal, line ->
			yVal.compareTo(line.bottom)
		})
		val line = _lines[lineIndex]
		return elements.sortedInsertionIndex(x, line.startIndex, line.endIndex) { xVal, element ->
			xVal.compareTo(element.right)
		}
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
	 * The height of the line.
	 */
	val height: Float
		get() = maxOf(nonBaselineHeight, baseline + descender)

	/**
	 * The height measuring the top to the baseline (where a line of text should 'sit').
	 */
	val baseline: Float

	/**
	 * The height below the baseline.
	 */
	val descender: Float

	/**
	 * The max height of the elements not sitting on the baseline.
	 */
	val nonBaselineHeight: Float

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
	override var endIndex: Int = 0
	override var x: Float = 0f
	override var y: Float = 0f

	override var contentsWidth: Float = 0f
	override var width: Float = 0f
	override var baseline: Float = 0f
	override var descender: Float = 0f
	override var nonBaselineHeight: Float = 0f

	fun set(other: LineInfoRo): LineInfo {
		startIndex = other.startIndex
		endIndex = other.endIndex
		contentsWidth = other.contentsWidth
		width = other.width
		nonBaselineHeight = other.nonBaselineHeight
		x = other.x
		y = other.y
		baseline = other.baseline
		descender = other.descender
		return this
	}

	override fun clear() {
		startIndex = 0
		endIndex = 0
		contentsWidth = 0f
		width = 0f
		nonBaselineHeight = 0f
		x = 0f
		y = 0f
		baseline = 0f
		descender = 0f
	}

	override fun toString(): String {
		return "LineInfo(startIndex=$startIndex, endIndex=$endIndex)"
	}

	companion object {

		private val pool = ClearableObjectPool { LineInfo() }

		fun obtain(): LineInfo = pool.obtain()
		fun free(obj: LineInfo) = pool.free(obj)
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
	var verticalAlign by prop(FlowVAlign.BASELINE)
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

open class FlowLayoutContainer<E : UiComponent>(owner: Context) : ElementLayoutContainer<FlowLayoutStyle, FlowLayoutData, E>(owner, FlowLayout())

@JvmName("flowT")
inline fun <E : UiComponent> Context.flow(init: ComponentInit<FlowLayoutContainer<E>> = {}): FlowLayoutContainer<E> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return FlowLayoutContainer<E>(this).apply(init)
}

inline fun Context.flow(init: ComponentInit<FlowLayoutContainer<UiComponent>> = {}): FlowLayoutContainer<UiComponent> {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return flow<UiComponent>(init)
}
