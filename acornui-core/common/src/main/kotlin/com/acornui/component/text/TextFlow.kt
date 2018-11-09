package com.acornui.component.text

import com.acornui.collection.*
import com.acornui.component.ComponentInit
import com.acornui.component.ElementParent
import com.acornui.component.ValidationFlags
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.layout.algorithm.FlowVAlign
import com.acornui.component.layout.algorithm.LineInfo
import com.acornui.component.layout.algorithm.LineInfoRo
import com.acornui.core.di.Owned
import com.acornui.core.floor
import com.acornui.core.selection.SelectionRange
import com.acornui.math.Bounds
import com.acornui.math.MathUtils.offsetRound
import com.acornui.math.MinMaxRo
import com.acornui.math.Vector3
import com.acornui.math.ceil

/**
 * A TextFlow component is a container of styleable text spans, to be used inside of a TextField.
 */
class TextFlow(owner: Owned) : TextNodeBase(owner), TextNode, ElementParent<TextSpanElement> {

	override var textNodeParent: TextNodeRo? = null

	val flowStyle = bind(TextFlowStyle())

	private val _lines = ArrayList<LineInfo>()

	private val _textElements = ArrayList<TextElement>()

	/**
	 * A list of all the text elements within the child spans.
	 */
	private val textElements: List<TextElement>
		get() {
			validate(TEXT_ELEMENTS)
			return _textElements
		}

	private val _placeholder = LastTextElement(this)
	override val placeholder: TextElementRo
		get() = _placeholder

	override val multiline: Boolean
		get() = flowStyle.multiline

	override val textElementsCount: Int
		get() = textElements.size

	init {
		validation.addNode(TEXT_ELEMENTS, dependencies = ValidationFlags.HIERARCHY_ASCENDING, dependants = ValidationFlags.LAYOUT, onValidate = this::updateTextElements)
		validation.addNode(VERTICES, dependencies = ValidationFlags.LAYOUT or ValidationFlags.STYLES or ValidationFlags.CONCATENATED_TRANSFORM, dependants = 0, onValidate = this::updateVertices)
		validation.addNode(ValidationFlags.CONCATENATED_COLOR_TRANSFORM) {}
		validation.addNode(CHAR_STYLE, dependencies = TEXT_ELEMENTS or ValidationFlags.CONCATENATED_COLOR_TRANSFORM or ValidationFlags.STYLES, dependants = 0, onValidate = this::updateCharStyle)
	}

	override fun getTextElementAt(index: Int): TextElementRo = textElements[index]

	override val linesCount: Int
		get() = _lines.size

	override fun getLineAt(index: Int): LineInfoRo {
		val lineIndex = _lines.sortedInsertionIndex(index) { _, line ->
			index.compareTo(line.startIndex)
		} - 1
		return _lines[lineIndex]
	}

	//-------------------------------------------------------------------------------------------------
	// Element methods.
	//-------------------------------------------------------------------------------------------------

	private val _elements = ArrayList<TextSpanElement>()
	override val elements: List<TextSpanElement>
		get() = _elements

	override fun <S : TextSpanElement> addElement(index: Int, element: S): S {
		if (element.textParent != null) throw Exception("Remove element first.")
		var newIndex = index
		val oldIndex = _elements.indexOf(element)
		if (oldIndex != -1) {
			if (newIndex == oldIndex) return element // Element was added in the same spot it previously was.
			// Handle the case where after the element is removed, the new index needs to decrement to compensate.
			if (oldIndex < newIndex)
				newIndex--
			removeElement(oldIndex)
		}
		_elements.add(newIndex, element)
		invalidate(TEXT_ELEMENTS)
		element.textParent = this
		return element
	}

	override fun removeElement(index: Int): TextSpanElement {
		val element = _elements.removeAt(index)
		element.textParent = null
		invalidate(TEXT_ELEMENTS)
		return element
	}

	override fun clearElements(dispose: Boolean) {
		for (i in 0.._elements.lastIndex) {
			_elements[i].textParent = null
		}
		_elements.clear()
		invalidate(TEXT_ELEMENTS)
	}

	private fun updateTextElements() {
		_textElements.clear()
		for (i in 0.._elements.lastIndex) {
			_textElements.addAll(_elements[i].elements)
		}
	}

	override fun updateStyles() {
		super.updateStyles()
		for (i in 0.._elements.lastIndex) {
			_elements[i].validateStyles()
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val textElements = textElements
		val padding = flowStyle.padding
		val availableWidth: Float? = padding.reduceWidth(explicitWidth)

		_lines.freeTo(linesPool)

		// To keep tab sizes consistent across the whole text field, we only use the first span's space size.
		val spaceSize = _elements.firstOrNull()?.spaceSize ?: 6f
		val tabSize = spaceSize * flowStyle.tabSize

		// Calculate lines
		var x = 0f
		var currentLine = linesPool.obtain()

		var spanPartIndex = 0
		while (spanPartIndex < textElements.size) {
			val part = textElements[spanPartIndex]
			part.explicitWidth = null
			part.x = x

			if (part.clearsTabstop) {
				val tabIndex = kotlin.math.floor(x / tabSize).toInt() + 1
				var w = tabIndex * tabSize - x
				// I'm not sure what standard text flows do for this, but if the tab size is too small, skip to
				// the next tabstop.
				if (w < spaceSize * 0.9f) w += tabSize
				part.explicitWidth = w
			}

			val partW = part.width

			// If this is multiline text and we extend beyond the right edge,then push the current line and start a new one.
			val extendsEdge = flowStyle.multiline && (!part.overhangs && availableWidth != null && x + partW > availableWidth)
			val isFirst = spanPartIndex == currentLine.startIndex
			val isLast = spanPartIndex == textElements.lastIndex
			if (isLast || (part.clearsLine && flowStyle.multiline) || (extendsEdge && !isFirst)) {
				if (extendsEdge && !isFirst) {
					// Find the last good breaking point.
					var breakIndex = textElements.indexOfLast2(spanPartIndex, currentLine.startIndex) { it.isBreaking }
					if (breakIndex == -1) breakIndex = spanPartIndex - 1
					val endIndex = textElements.indexOfFirst2(breakIndex + 1, spanPartIndex) { !it.overhangs }
					currentLine.endIndex = if (endIndex == -1) spanPartIndex + 1
					else endIndex
					spanPartIndex = currentLine.endIndex
				} else {
					spanPartIndex++
					currentLine.endIndex = spanPartIndex
				}
				_lines.add(currentLine)
				currentLine = linesPool.obtain()
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
		linesPool.free(currentLine) // Current line was obtained, but not used / pushed to list.

		// We now have the elements per line; measure the line heights/widths and position the elements within the line.
		var y = padding.top
		var measuredWidth = 0f
		for (i in 0.._lines.lastIndex) {
			val line = _lines[i]
			line.y = y

			for (j in line.startIndex..line.endIndex - 1) {
				val part = textElements[j]
				val b = part.lineHeight - part.baseline
				if (b > line.belowBaseline) line.belowBaseline = b
				if (part.baseline > line.baseline) line.baseline = part.baseline
				if (!part.overhangs) line.contentsWidth = part.x + part.width
				line.width = part.x + part.width
			}
			if (line.contentsWidth > measuredWidth)
				measuredWidth = line.contentsWidth
			line.x = calculateLineX(availableWidth, line.contentsWidth)
			positionElementsInLine(line, availableWidth)
			y += line.height + flowStyle.verticalGap
		}
		val lastLine = _lines.lastOrNull()
		if (lastLine == null) {
			// No lines, the placeholder is where the first character will begin.
			_placeholder.x = calculateLineX(availableWidth, 0f) // Considers alignment.
			_placeholder.y = flowStyle.padding.top
		} else {
			if (lastLine.lastClearsLine) {
				// Where the next line will begin.
				_placeholder.x = calculateLineX(availableWidth, 0f) // Considers alignment.
				_placeholder.y = lastLine.y + lastLine.height + flowStyle.verticalGap
			} else {
				// At the end of the last line.
				_placeholder.x = lastLine.x + lastLine.width
				_placeholder.y = lastLine.y
			}
		}
		val measuredHeight = y - flowStyle.verticalGap + padding.bottom
		measuredWidth += padding.left + padding.right
		if (measuredWidth > out.width) out.width = measuredWidth
		if (measuredHeight > out.height) out.height = measuredHeight
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
		val textElements = textElements
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
						part.x = (part.x + justifyOffset).floor()
						if (i < lastIndex && part.char == ' ') {
							part.explicitWidth = part.advanceX + hGap.ceil()
							justifyOffset += hGap
						}
					}
				}
			}
		}

		for (i in line.startIndex..line.endIndex - 1) {
			val part = textElements[i]

			val yOffset = when (flowStyle.verticalAlign) {
				FlowVAlign.TOP -> 0f
				FlowVAlign.MIDDLE -> offsetRound((line.height - part.lineHeight) * 0.5f)
				FlowVAlign.BOTTOM -> line.height - part.lineHeight
				FlowVAlign.BASELINE -> line.baseline - part.baseline
			}

			part.x += line.x
			part.y = line.y + yOffset
		}
	}

	override fun getSelectionIndex(x: Float, y: Float): Int {
		if (_lines.isEmpty()) return 0
		if (y < _lines.first().y) return 0
		if (y >= _lines.last().bottom) return textElements.size
		val lineIndex = _lines.sortedInsertionIndex(y) { yVal, line ->
			yVal.compareTo(line.bottom)
		}
		val line = _lines[lineIndex]
		return textElements.sortedInsertionIndex(x, line.startIndex, line.endIndex) { xVal, part ->
			if (part.clearsLine && flowStyle.multiline) -1 else xVal.compareTo(part.x + part.width * 0.5f)
		}
	}

	private fun updateVertices() {
		val textElements = textElements
		val padding = flowStyle.padding
		val leftClip = padding.left
		val topClip = padding.top
		val w = (if (allowClipping) explicitWidth else null) ?: Float.MAX_VALUE
		val h = (if (allowClipping) explicitHeight else null) ?: Float.MAX_VALUE
		val rightClip = w - padding.right
		val bottomClip = h - padding.bottom
		for (i in 0..textElements.lastIndex) {
			textElements[i].validateVertices(concatenatedTransform, leftClip, topClip, rightClip, bottomClip)
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
		val concatenatedColorTint = concatenatedColorTint
		for (i in 0.._elements.lastIndex) {
			_elements[i].validateCharStyle(concatenatedColorTint)
		}
		val textElements = textElements
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

	override fun render(clip: MinMaxRo) {
		if (_lines.isEmpty())
			return
		val textElements = textElements
		localToCanvas(tL.set(0f, 0f, 0f))
		localToCanvas(tR.set(_bounds.width, 0f, 0f))

		if (tL.y == tR.y) {
			// This text field is axis aligned, we can check against the viewport without a matrix inversion.
			val y = tL.y
			if (tR.x < clip.xMin || tL.x > clip.xMax) return
			val scaleY = concatenatedTransform.getScaleY()
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
			glState.setCamera(camera)
			for (i in lineStart..lineEnd - 1) {
				val line = _lines[i]
				for (j in line.startIndex..line.endIndex - 1) {
					textElements[j].render(glState)
				}
			}
		} else {
			glState.setCamera(camera)
			for (i in 0..textElements.lastIndex) {
				textElements[i].render(glState)
			}
		}
	}

	companion object {
		private const val TEXT_ELEMENTS = 1 shl 16
		private const val VERTICES = 1 shl 17
		private const val CHAR_STYLE = 1 shl 18
		private val linesPool = ClearableObjectPool { LineInfo() }
	}
}

fun Owned.p(init: ComponentInit<TextFlow>): TextFlow {
	val t = TextFlow(this)
	t.init()
	return t
}