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

@file:Suppress("ReplaceRangeToWithUntil")

package com.acornui.component.text

import com.acornui.collection.*
import com.acornui.component.*
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.layout.algorithm.FlowVAlign
import com.acornui.component.layout.algorithm.LineInfo
import com.acornui.component.layout.algorithm.LineInfoRo
import com.acornui.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.MathUtils.offsetRound
import com.acornui.math.Vector3
import com.acornui.selection.SelectionRange
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A Paragraph component is a container of styleable text spans, to be used inside of a TextField.
 */
class Paragraph(owner: Owned) : UiComponentImpl(owner), TextNode, ElementParent<TextSpanElement> {

	val flowStyle = bind(TextFlowStyle())

	override var textField: TextField? = null

	private val _lines = ArrayList<LineInfo>()
	private val _elements = ElementsList()

	private val _textElements = ArrayList<TextElement>()

	/**
	 * A list of all the text elements within the child spans.
	 */
	override val textElements: List<TextElementRo>
		get() {
			validate(TEXT_ELEMENTS)
			return _textElements
		}

	private val _placeholder = LastTextElement(this)
	override val placeholder: TextElementRo
		get() = _placeholder

	override val multiline: Boolean
		get() = flowStyle.multiline

	private var bubblingFlags =
			ValidationFlags.HIERARCHY_ASCENDING or
					ValidationFlags.LAYOUT

	override var allowClipping: Boolean by validationProp(true, VERTICES)

	init {
		validation.addNode(TEXT_ELEMENTS, dependencies = ValidationFlags.HIERARCHY_ASCENDING, dependents = ValidationFlags.LAYOUT, onValidate = ::updateTextElements)
		validation.addNode(VERTICES, dependencies = TEXT_ELEMENTS or ValidationFlags.LAYOUT or ValidationFlags.STYLES, dependents = 0, onValidate = ::updateVertices)
		validation.addNode(CHAR_STYLE, dependencies = TEXT_ELEMENTS or ValidationFlags.STYLES, dependents = 0, onValidate = ::updateCharStyle)
	}

	override val lines: List<LineInfoRo>
		get() {
			validate(ValidationFlags.LAYOUT)
			return _lines
		}

	//-------------------------------------------------------------------------------------------------
	// Element methods.
	//-------------------------------------------------------------------------------------------------

	override val elements: MutableList<TextSpanElement> = _elements

	override fun <S : TextSpanElement> addElement(index: Int, element: S): S {
		_elements.add(index, element)
		return element
	}

	override fun removeElement(index: Int): TextSpanElement = _elements.removeAt(index)

	override fun clearElements(dispose: Boolean) {
		for (i in 0.._elements.lastIndex) {
			_elements[i].textParent = null
		}
		_elements.clear()
		invalidate(bubblingFlags)
	}

	override fun updateStyles() {
		super.updateStyles()
		_elements.forEach2 {
			it.validateStyles()
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val lines = _lines
		val textElements = _textElements
		val flowStyle = flowStyle
		val padding = flowStyle.padding
		val placeholder = _placeholder
		val availableWidth: Float? = padding.reduceWidth(explicitWidth)

		lines.forEach2(action = LineInfo.Companion::free)
		lines.clear()

		// To keep tab sizes consistent across the whole text field, we only use the first span's space size.
		val spaceSize = _elements.firstOrNull()?.spaceSize ?: 6f
		val tabSize = spaceSize * flowStyle.tabSize

		// Calculate lines
		var x = 0f
		var currentLine = LineInfo.obtain()

		var spanPartIndex = 0
		var allowWordBreak = true

		while (spanPartIndex < textElements.size) {
			val part = textElements[spanPartIndex]
			part.explicitWidth = null
			part.x = x

			if (part.clearsTabstop) {
				val tabIndex = floor(x / tabSize).toInt() + 1
				var w = tabIndex * tabSize - x
				// I'm not sure what standard text flows do for this, but if the tab size is too small, skip to
				// the next tabstop.
				if (w < spaceSize * 0.9f) w += tabSize
				part.explicitWidth = w
			}

			val partW = part.width

			// If this is multiline text and we extend beyond the right edge, then push the current line and start a new one.
			val extendsEdge = flowStyle.multiline && (!part.overhangs && availableWidth != null && x + partW > availableWidth)
			val isFirst = spanPartIndex == currentLine.startIndex
			val mustBreak = part.clearsLine && flowStyle.multiline
			val canBreak = flowStyle.multiline && !isFirst && allowWordBreak
			val isLast = spanPartIndex == textElements.lastIndex
			if (isLast || mustBreak || (extendsEdge && canBreak)) {
				if (extendsEdge && canBreak) {
					// Find the last good breaking point.
					var breakIndex = textElements.indexOfLast2(spanPartIndex, currentLine.startIndex) { it.isBreaking }
					if (breakIndex == -1) {
						allowWordBreak = false
						breakIndex = spanPartIndex - 1
					}
					val endIndex = textElements.indexOfFirst2(breakIndex + 1, spanPartIndex) { !it.overhangs }
					currentLine.endIndex = if (endIndex == -1) spanPartIndex + 1
					else endIndex
					spanPartIndex = currentLine.endIndex
				} else {
					spanPartIndex++
					currentLine.endIndex = spanPartIndex
					allowWordBreak = true
				}
				lines.add(currentLine)
				currentLine = LineInfo.obtain()
				currentLine.startIndex = spanPartIndex
				x = 0f
			} else {
				val nextPart = textElements.getOrNull(spanPartIndex + 1)
				val kerning = if (nextPart == null) 0f else part.getKerning(nextPart)
				part.kerning = kerning
				x += partW + kerning
				spanPartIndex++
			}
		}
		LineInfo.free(currentLine) // Current line was obtained, but not used / pushed to list.

		// We now have the elements per line; measure the line heights/widths and position the elements within the line.
		var y = padding.top
		var measuredWidth = 0f
		for (i in 0..lines.lastIndex) {
			val line = lines[i]
			line.y = y

			for (j in line.startIndex..line.endIndex - 1) {
				val part = textElements[j]
				if (part.parentSpan?.verticalAlign ?: flowStyle.verticalAlign == FlowVAlign.BASELINE) {
					val belowBaseline = part.lineHeight - part.baseline
					if (belowBaseline > line.descender) line.descender = belowBaseline
					if (part.baseline > line.baseline) line.baseline = part.baseline
				} else {
					if (part.height > line.nonBaselineHeight)
						line.nonBaselineHeight = part.height
				}
				if (!part.overhangs) line.contentsWidth = part.x + part.width
				line.width = part.x + part.width
			}
			if (line.contentsWidth > measuredWidth)
				measuredWidth = line.contentsWidth
			line.x = calculateLineX(availableWidth, line.contentsWidth)
			positionElementsInLine(line, availableWidth)
			y += line.height + flowStyle.verticalGap
		}
		y -= flowStyle.verticalGap

		val lastLine = lines.lastOrNull()
		if (lastLine == null) {
			// No lines, the placeholder is where the first character will begin.
			placeholder.x = calculateLineX(availableWidth, 0f) // Considers alignment.
			placeholder.y = flowStyle.padding.top
		} else {
			if (lastLine.lastClearsLine) {
				// Where the next line will begin.
				placeholder.x = calculateLineX(availableWidth, 0f) // Considers alignment.
				placeholder.y = lastLine.y + lastLine.height + flowStyle.verticalGap
			} else {
				// At the end of the last line.
				placeholder.x = lastLine.x + lastLine.width
				placeholder.y = lastLine.y
			}
		}
		val measuredHeight = y + padding.bottom
		measuredWidth += padding.left + padding.right
		if (measuredWidth > out.width) out.width = measuredWidth
		if (measuredHeight > out.height) out.height = measuredHeight
		out.baseline = padding.top + (lines.firstOrNull()?.baseline ?: 0f)
	}

	private val LineInfoRo.lastClearsLine: Boolean
		get() {
			if (!flowStyle.multiline) return false
			return textElements.getOrNull(endIndex - 1)?.clearsLine ?: false
		}

	private fun calculateLineX(availableWidth: Float?, lineWidth: Float): Float {
		return if (availableWidth != null) {
			val remainingSpace = availableWidth - lineWidth
			flowStyle.padding.left + when (flowStyle.horizontalAlign) {
				FlowHAlign.LEFT -> 0f
				FlowHAlign.CENTER -> offsetRound(remainingSpace * 0.5f)
				FlowHAlign.RIGHT -> remainingSpace
				FlowHAlign.JUSTIFY -> 0f
			}
		} else {
			flowStyle.padding.left
		}
	}

	private fun positionElementsInLine(line: LineInfoRo, availableWidth: Float?) {
		val textElements = _textElements
		val flowStyle = flowStyle
		if (availableWidth != null) {
			val remainingSpace = availableWidth - line.contentsWidth

			if (flowStyle.horizontalAlign == FlowHAlign.JUSTIFY &&
					line.size > 1 &&
					_lines.last() != line &&
					!(textElements[line.endIndex - 1].clearsLine && flowStyle.multiline)
			) {
				// Apply JUSTIFY spacing if this is not the last line, and there are more than one elements.
				val lastIndex = textElements.indexOfLast2(line.endIndex - 1, line.startIndex) { !it.overhangs }
				val numSpaces = textElements.count2(line.startIndex, lastIndex) { it.char == ' ' }
				if (numSpaces > 0) {
					val hGap = remainingSpace / numSpaces
					var justifyOffset = 0f
					for (i in line.startIndex..line.endIndex - 1) {
						val part = textElements[i]
						part.x = floor((part.x + justifyOffset))
						if (i < lastIndex && part.char == ' ') {
							part.explicitWidth = part.advanceX + ceil(hGap).toInt()
							justifyOffset += hGap
						}
					}
				}
			}
		}

		for (i in line.startIndex..line.endIndex - 1) {
			val part = textElements[i]

			val yOffset = when (part.parentSpan?.verticalAlign ?: flowStyle.verticalAlign) {
				FlowVAlign.TOP -> 0f
				FlowVAlign.MIDDLE -> offsetRound((line.height - part.lineHeight) * 0.5f)
				FlowVAlign.BOTTOM -> line.height - part.lineHeight
				FlowVAlign.BASELINE -> line.baseline - part.baseline
			}

			part.x += line.x
			part.y = line.y + yOffset
		}
	}

	private fun updateTextElements() {
		_textElements.clear()
		_elements.forEach2 {
			_textElements.addAll(it.elements)
		}
	}

	private fun updateVertices() {
		val textElements = _textElements
		val padding = flowStyle.padding
		val leftClip = padding.left
		val topClip = padding.top
		val w = (if (allowClipping) explicitWidth else null) ?: Float.MAX_VALUE
		val h = (if (allowClipping) explicitHeight else null) ?: Float.MAX_VALUE
		val rightClip = w - padding.right
		val bottomClip = h - padding.bottom
		for (i in 0..textElements.lastIndex) {
			textElements[i].validateVertices(leftClip, topClip, rightClip, bottomClip)
		}
	}

	private var selectionRangeStart = 0
	private var selection: List<SelectionRange> = emptyList()

	override fun setSelection(rangeStart: Int, selection: List<SelectionRange>) {
		selectionRangeStart = rangeStart
		this.selection = selection
		invalidate(CHAR_STYLE)
	}

	private fun updateCharStyle() {
		val textElements = _textElements
		for (i in 0..textElements.lastIndex) {
			val selected = selection.indexOfFirst2 { it.contains(i + selectionRangeStart) } != -1
			textElements[i].setSelected(selected)
		}
	}

	override fun toString(builder: StringBuilder) {
		val textElements = textElements
		for (i in 0..textElements.lastIndex) {
			val char = textElements[i].char
			if (char != null)
				builder.append(char)
		}
	}

	private val tL = Vector3()
	private val tR = Vector3()

	override fun draw() {
		val tint = renderContext.colorTint
		val clip = renderContext.clipRegion
		val transform = renderContext.modelTransform
		if (_lines.isEmpty() || tint.a <= 0f)
			return
		val textElements = _textElements
		localToCanvas(tL.set(0f, 0f, 0f))
		localToCanvas(tR.set(_bounds.width, 0f, 0f))

		if (tL.y == tR.y) {
			// This text field is axis aligned, we can check against the viewport without a matrix inversion.
			val y = tL.y
			if (tR.x < clip.xMin || tL.x > clip.xMax)
				return
			val scaleY = transform.getScaleY()
			val lineStart = _lines.sortedInsertionIndex(clip.yMin - y) { viewPortY, line ->
				viewPortY.compareTo(line.bottom / scaleY)
			}
			if (lineStart == -1)
				return
			val lineEnd = _lines.sortedInsertionIndex(clip.yMax - y) { viewPortBottom, line ->
				viewPortBottom.compareTo(line.y / scaleY)
			}
			if (lineEnd <= lineStart)
				return

			for (i in lineStart..lineEnd - 1) {
				val line = _lines[i]
				for (j in line.startIndex..line.endIndex - 1) {
					textElements[j].renderBackground(clip, transform, tint)
				}
			}
			for (i in lineStart..lineEnd - 1) {
				val line = _lines[i]
				for (j in line.startIndex..line.endIndex - 1) {
					textElements[j].renderForeground(clip, transform, tint)
				}
			}
		} else {
			for (i in 0..textElements.lastIndex) {
				textElements[i].renderBackground(clip, transform, tint)
			}
			for (i in 0..textElements.lastIndex) {
				textElements[i].renderForeground(clip, transform, tint)
			}
		}
	}

	private inner class ElementsList : MutableListBase<TextSpanElement>() {

		private val elements = ArrayList<TextSpanElement>()

		override fun add(index: Int, element: TextSpanElement) {
			if (element.textParent != null && element.textParent != this@Paragraph) throw Exception("Remove element first.")
			val oldIndex = elements.indexOf(element)
			if (oldIndex == -1) {
				element.textParent = this@Paragraph
				elements.add(index, element)
			} else {
				val newIndex = if (oldIndex < index) index - 1 else index
				if (index == oldIndex || index == oldIndex + 1) return
				elements.removeAt(oldIndex)
				elements.add(newIndex, element)
			}
			invalidate(bubblingFlags)
		}

		override fun removeAt(index: Int): TextSpanElement {
			val element = elements.removeAt(index)
			element.textParent = null
			return element
		}

		override val size: Int
			get() = elements.size

		override fun get(index: Int): TextSpanElement = elements[index]

		override fun set(index: Int, element: TextSpanElement): TextSpanElement {
			val oldElement = elements.set(index, element)
			if (oldElement == element) return element
			oldElement.textParent = null
			element.textParent = this@Paragraph
			invalidate(bubblingFlags)
			return oldElement
		}
	}

	companion object {
		private const val TEXT_ELEMENTS = 1 shl 16
		private const val VERTICES = 1 shl 17
		private const val CHAR_STYLE = 1 shl 18
	}
}

inline fun Owned.p(init: ComponentInit<Paragraph> = {}): Paragraph  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = Paragraph(this)
	t.init()
	return t
}
