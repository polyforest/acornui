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

package com.acornui.component.text

import com.acornui.collection.sortedInsertionIndex
import com.acornui.component.UiComponent
import com.acornui.component.UiComponentRo
import com.acornui.component.layout.SizableRo
import com.acornui.component.layout.algorithm.LineInfoRo
import com.acornui.Disposable
import com.acornui.selection.SelectionRange
import com.acornui.graphic.ColorRo
import com.acornui.math.Bounds
import com.acornui.math.BoundsRo
import com.acornui.math.Matrix4Ro
import com.acornui.math.MinMaxRo

/**
 * The smallest unit that can be inside of a TextField.
 * This can be a single character, or a more complex object.
 */
interface TextElementRo: SizableRo {

	/**
	 * Set by the TextSpanElement when this part is added.
	 */
	val parentSpan: TextSpanElementRo<TextElementRo>?

	/**
	 * The character, if any, that this text element represents.
	 */
	val char: Char?

	val x: Float
	val y: Float

	/**
	 * The natural amount of horizontal space to advance after this part.
	 * [explicitWidth] will override this value.
	 * In points, not pixels.
	 */
	val advanceX: Float

	override val explicitHeight: Float?
		get() = null

	/**
	 * The kerning offset between this element and the next.
	 * In points, not pixels.
	 */
	val kerning: Float

	/**
	 * Returns the amount of horizontal space to offset this part from the next part.
	 * In points, not pixels.
	 */
	fun getKerning(next: TextElementRo): Float

	/**
	 * If true, this element will cause the line to break after this element.
	 */
	val clearsLine: Boolean

	/**
	 * If true, the tabstop should be cleared after placing this element.
	 */
	val clearsTabstop: Boolean

	/**
	 * If true, this part is a good word break. (The part after this part may be placed on the next line).
	 */
	val isBreaking: Boolean

	/**
	 * If true, this part will not cause a wrap.
	 */
	val overhangs: Boolean

}

val TextElementRo.right: Float
	get() = x + width

val TextElementRo.bottom: Float
	get() = y + lineHeight

val TextElementRo.lineHeight: Float
	get() {
		return parentSpan?.lineHeight ?: 0f
	}

val TextElementRo.textFieldX: Float
	get() {
		return x + (parentSpan?.textFieldX ?: 0f)
	}

val TextElementRo.textFieldY: Float
	get() {
		return y + (parentSpan?.textFieldY ?: 0f)
	}

interface TextElement : TextElementRo, Disposable {

	/**
	 * Set by the TextSpanElement when this is part is added.
	 */
	override var parentSpan: TextSpanElementRo<TextElementRo>?

	override var x: Float
	override var y: Float

	/**
	 * If set, this element should be drawn to fit this width.
	 */
	override var explicitWidth: Float?

	override var kerning: Float

	/**
	 * If set to true, this part will be rendered using the selected styling.
	 */
	fun setSelected(value: Boolean)

	/**
	 * Finalizes the vertices for rendering.
	 * @param leftClip The x position in canvas coordinate points for clipping.
	 * @param topClip The y position in canvas coordinate points for clipping.
	 * @param rightClip The right position in canvas coordinate points for clipping.
	 * @param bottomClip The bottom position in canvas coordinate points for clipping.
	 */
	fun validateVertices(leftClip: Float, topClip: Float, rightClip: Float, bottomClip: Float)

	fun render(clip: MinMaxRo, transform: Matrix4Ro, tint: ColorRo)

}

interface TextNodeRo : UiComponentRo {

	val textField: TextField?

	/**
	 * A virtual text element to indicate the position of the next element within this node.
	 */
	val placeholder: TextElementRo?

	/**
	 * True if this node allows \n characters.
	 */
	val multiline: Boolean

	/**
	 * A list of the text elements this node contains.  (deep/hierarchical)
	 */
	val textElements: List<TextElementRo>

	/**
	 * The lines in this node (deep/hierarchical).
	 */
	val lines: List<LineInfoRo>

	/**
	 * Returns the line at the given text element index (deep/hierarchical).
	 * @param index The text element index.
	 */
	fun getLineAt(index: Int): LineInfoRo {
		val lines = lines
		val lineIndex = lines.sortedInsertionIndex(index) { _, line ->
			index.compareTo(line.startIndex)
		} - 1
		return lines[lineIndex]
	}

	/**
	 * Returns the line at the given index, or null if the index is `< 0` or `>= textElements.size`.
	 */
	fun getLineOrNullAt(index: Int): LineInfoRo? {
		val n = lines.size
		if (n == 0 || index < 0 || index >= textElements.size) return null
		return getLineAt(index)
	}

	/**
	 * Returns the element index at the given local coordinate.
	 *
	 * Implementation note:
	 * The default implementation assumes the lines are vertically stacked and uses a binary search for the relevant
	 * line range, but if the lines are not in a vertical arrangement, this should be overriden.
	 *
	 * @param x The relative x coordinate
	 * @param y The relative y coordinate
	 * @return Returns the relative index of the text element nearest the local coordinate (x, y). The text element
	 * index will be separated at the half-width of the element.
	 * This range will be between `0` and `textElements.size` (inclusive)
	 */
	fun getSelectionIndex(x: Float, y: Float): Int {
		val multiline = multiline
		val lines = lines
		if (lines.isEmpty()) return 0
		if (y < lines.first().y) return 0
		if (y >= lines.last().bottom) return textElements.size
		val lineIndex = lines.sortedInsertionIndex(y) { yVal, line ->
			yVal.compareTo(line.bottom)
		}
		val line = lines[lineIndex]
		return textElements.sortedInsertionIndex(x, line.startIndex, line.endIndex) { xVal, textElement ->
			// If the current element clears the line, end the recursion before the newline character, not after.
			// Otherwise, separate at the half-width of the element.
			if (textElement.clearsLine && multiline) -1 else xVal.compareTo(textElement.x + textElement.width * 0.5f)
		}
	}

	/**
	 * Writes this node's contents to a string builder.
	 */
	fun toString(builder: StringBuilder)

}

interface TextNode : TextNodeRo, UiComponent {

	override var textField: TextField?

	/**
	 * If true, this component's vertices will be clipped to the explicit size.
	 */
	var allowClipping: Boolean

	/**
	 * Sets the text selection.
	 * @param rangeStart The starting index of this node. The selection range indices are relative to the text field
	 * and text nodes themselves don't know their starting index. Therefore it is provided.
	 * @param selection A list of ranges that are selected.
	 */
	fun setSelection(rangeStart: Int, selection: List<SelectionRange>)

}

/**
 * A placeholder for the last text element in a span. This is useful for calculating the text cursor placement.
 */
class LastTextElement(private val flow: Paragraph) : TextElementRo {

	override val bounds: BoundsRo = Bounds.EMPTY_BOUNDS

	override val parentSpan: TextSpanElement?
		get() = flow.elements.lastOrNull()

	override val char: Char? = null
	override var x = 0f
	override var y = 0f

	override val advanceX = 0f

	override val explicitWidth = 0f
	override val kerning: Float = 0f

	override fun getKerning(next: TextElementRo) = 0f

	override val clearsLine = false
	override val clearsTabstop = false
	override val isBreaking = false
	override val overhangs = false
}
