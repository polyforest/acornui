/*
 * Copyright 2018 PolyForest
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

import com.acornui._assert
import com.acornui.component.*
import com.acornui.component.layout.*
import com.acornui.component.layout.algorithm.LineInfoRo
import com.acornui.component.style.*
import com.acornui.core.Disposable
import com.acornui.core.cursor.RollOverCursor
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.input.interaction.DragAttachment
import com.acornui.core.input.interaction.DragInteractionRo
import com.acornui.core.selection.Selectable
import com.acornui.core.selection.SelectableComponent
import com.acornui.core.selection.SelectionManager
import com.acornui.core.selection.SelectionRange
import com.acornui.gl.core.GlState
import com.acornui.graphic.Color
import com.acornui.math.*
import com.acornui.signal.Signal

interface TextField : Labelable, SelectableComponent, Styleable {

	/**
	 * The style object for text flow layout.
	 */
	val flowStyle: TextFlowStyle

	/**
	 * The style object for glyph decoration.
	 */
	val charStyle: CharStyle

	/**
	 * The Selectable target to use for the selection range.
	 */
	var selectionTarget: Selectable

	/**
	 * The TextField contents.
	 */
	val contents: TextNodeRo
	/**
	 * Sets this text field's contents to a simple text flow.
	 */
	var text: String

	/**
	 * If true (default), the contents will be clipped to the explicit size of this text field.
	 */
	var allowClipping: Boolean

	/**
	 * Sets the contents of this text field.
	 * This will remove the existing contents, but does not dispose.
	 */
	fun <T : TextNode> contents(value: T): T

	/**
	 * Replaces the given range with the provided text.
	 * This is functionally the same as:
	 * text.substring(0, startIndex) + newText + text.substring(endIndex, text.length)
	 *
	 * @param startIndex The starting character index for the replacement. (Inclusive)
	 * @param endIndex The ending character index for the replacement. (Exclusive)
	 *
	 * E.g.
	 * +text("Hello World") {
	 *   replaceTextRange(1, 5, "i") // Hi World
	 * }
	 */
	fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String)

	companion object : StyleTag
}

/**
 * A component that displays text.
 * @author nbilyk
 */
@Suppress("LeakingThis", "UNUSED_PARAMETER")
open class TextFieldImpl(owner: Owned) : UiComponentImpl(owner), TextField {

	override val flowStyle = bind(TextFlowStyle())
	override val charStyle = bind(CharStyle())

	private val selectionManager = inject(SelectionManager)

	private var _selectionCursor: RollOverCursor? = null

	/**
	 * The Selectable target to use for the selection range.
	 */
	override var selectionTarget: Selectable = this

	private val _textSpan = span()
	private val _textContents = textFlow { +_textSpan }
	private var _contents: TextNode = addChild(_textContents)

	private var _allowClipping: Boolean = true

	/**
	 * If true (default), the contents will be clipped to the explicit size of this text field.
	 */
	override var allowClipping: Boolean
		get() = _allowClipping
		set(value) {
			if (_allowClipping == value) return
			_allowClipping = value
			_contents.allowClipping = value
			invalidateLayout()
		}

	/**
	 * The TextField contents.
	 */
	override val contents: TextNodeRo
		get() = _contents

	/**
	 * Sets the contents of this text field.
	 * This will remove the existing contents, but does not dispose.
	 */
	override fun <T : TextNode> contents(value: T): T {
		if (_contents == value) return value
		removeChild(_contents)
		_contents = value
		_contents.allowClipping = _allowClipping
		addChild(value)
		return value
	}

	private fun removeChild(node: TextNode) {
		node.textField = null
		node.invalidate(cascadingFlags)
		node.invalidated.remove(this::childInvalidatedHandler)
		node.disposed.remove(this::childDisposedHandler)
		if (!isValidatingLayout)
			invalidateLayout()
	}

	private fun addChild(child: TextNode): TextNode {
		_assert(!isDisposed, "This TextField is disposed.")
		_assert(!child.isDisposed, "Added text node is disposed.")
		child.textField = this
		child.invalidated.add(this::childInvalidatedHandler)
		child.disposed.remove(this::childDisposedHandler)
		child.invalidate(cascadingFlags)
		if (!isValidatingLayout)
			invalidateLayout()
		return child
	}

	private fun childDisposedHandler(child: TextNode) {
		removeChild(child)
	}

	private val isValidatingLayout: Boolean
		get() = validation.currentFlag == ValidationFlags.LAYOUT

	private fun childInvalidatedHandler(child: TextNodeRo, flagsInvalidated: Int) {
		if (!isValidatingLayout && flagsInvalidated and ValidationFlags.LAYOUT > 0) invalidateLayout()
	}

	/**
	 * These flags, when invalidated, will cascade down to this TextField's contents.
	 */
	private val cascadingFlags = ContainerImpl.defaultCascadingFlags

	override fun onInvalidated(flagsInvalidated: Int) {
		val flagsToCascade = flagsInvalidated and cascadingFlags
		if (flagsToCascade > 0) {
			// This component has flags that have been invalidated that must cascade down to the children.
			_contents.invalidate(flagsToCascade)
		}
	}

	private var _drag: DragAttachment? = null

	private fun selectionChangedHandler(old: List<SelectionRange>, new: List<SelectionRange>) {
		invalidate(TextValidationFlags.SELECTION)
	}

	private fun dragHandler(event: DragInteractionRo) {
		if (!charStyle.selectable) return
		if (!event.handled) {
			event.handled = true
			selectionManager.selection = getNewSelection(event) ?: emptyList()
		}
	}

	private fun getNewSelection(event: DragInteractionRo): List<SelectionRange>? {
		val contents = _contents
		val p1 = event.startPositionLocal
		val p2 = event.positionLocal

		val p1A = contents.getSelectionIndex(p1.x, p1.y)
		val p2A = contents.getSelectionIndex(p2.x, p2.y)
		return listOf(SelectionRange(selectionTarget, p1A, p2A))
	}

	private fun refreshCursor() {
		if (charStyle.selectable) {
			if (_selectionCursor == null)
				_selectionCursor = RollOverCursor(this, StandardCursors.IBEAM)
		} else {
			_selectionCursor?.dispose()
			_selectionCursor = null
		}
	}

	/**
	 * Sets this text field's contents to a simple text flow.
	 */
	override var text: String
		get() {
			val builder = StringBuilder()
			_contents.toString(builder)
			return builder.toString()
		}
		set(value) {
			_textSpan.text = value
			contents(_textContents)
		}

	override fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String) {
		val text = text
		this.text = text.substring(0, maxOf(0, startIndex)) + newText + text.substring(minOf(text.length, endIndex), text.length)
	}

	override var label: String
		get() = text
		set(value) {
			text = value
		}

	private fun updateSelection() {
		_contents.setSelection(0, selectionManager.selection.filter { it.target == selectionTarget })
	}

	override fun update() {
		super.update()
		_contents.update()
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val contents = _contents
		contents.setSize(explicitWidth, explicitHeight)
		contents.setPosition(0f, 0f)
		out.set(contents.bounds)

		val font = charStyle.font
		val minHeight = flowStyle.padding.expandHeight(font?.data?.lineHeight?.toFloat()) ?: 0f
		if (out.height < minHeight) out.height = minHeight

		if (contents.allowClipping) {
			if (explicitWidth != null) out.width = explicitWidth
			if (explicitHeight != null) out.height = explicitHeight
		}
	}

	override fun draw(clip: MinMaxRo) {
		_contents.render(clip)
	}

	override fun dispose() {
		super.dispose()
		_selectionCursor?.dispose()
		_selectionCursor = null
		selectionManager.selectionChanged.remove(this::selectionChangedHandler)
		_drag?.dispose()
		_drag = null
	}

	init {
		addStyleRule(flowStyle)
		addStyleRule(charStyle)
		styleTags.add(TextField)

		watch(charStyle) { cS ->
			refreshCursor()
			if (cS.selectable) {
				if (_drag == null) {
					val d = DragAttachment(this, 0f)
					d.drag.add(this::dragHandler)
					_drag = d
				}
			} else {
				_drag?.dispose()
				_drag = null
			}
		}
		validation.addNode(TextValidationFlags.SELECTION, ValidationFlags.HIERARCHY_ASCENDING, this::updateSelection)
		selectionManager.selectionChanged.add(this::selectionChangedHandler)
	}
}

object TextValidationFlags {
	const val SELECTION = 1 shl 16
}

/**
 * A [TextSpanElement] will decorate the span's characters all the same way. This class is used to store those
 * calculated properties.
 */
class CharElementStyle {
	var font: BitmapFont? = null
	var underlined: Boolean = false
	var strikeThrough: Boolean = false
	var lineThickness: Float = 1f
	val selectedTextColorTint = Color()
	val selectedBackgroundColor = Color()
	val textColorTint = Color()
	val backgroundColor = Color()
}

/**
 * The smallest unit that can be inside of a TextField.
 * This can be a single character, or a more complex object.
 */
interface TextElementRo {

	/**
	 * Set by the TextSpanElement when this is part is added.
	 */
	val textParent: TextSpanElementRo<TextElementRo>?

	val char: Char?

	val x: Float
	val y: Float

	/**
	 * The natural amount of horizontal space to advance after this part.
	 * [explicitWidth] will override this value.
	 */
	val advanceX: Float

	/**
	 * If set, this part should be drawn to fit this width.
	 */
	val explicitWidth: Float?

	/**
	 * The explicit width, if it's set, or the xAdvance.
	 */
	val width: Float
		get() = explicitWidth ?: advanceX

	/**
	 * The kerning offset between this element and the next.
	 */
	val kerning: Float

	/**
	 * Returns the amount of horizontal space to offset this part from the next part.
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
		return textParent?.lineHeight ?: 0f
	}

val TextElementRo.baseline: Float
	get() = (textParent?.baseline ?: 0f)

val TextElementRo.textFieldX: Float
	get() {
		return x + (textParent?.textFieldX ?: 0f)
	}

val TextElementRo.textFieldY: Float
	get() {
		return y + (textParent?.textFieldY ?: 0f)
	}

interface TextElement : TextElementRo, Disposable {

	/**
	 * Set by the TextSpanElement when this is part is added.
	 */
	override var textParent: TextSpanElementRo<TextElementRo>?

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
	 */
	fun validateVertices(transform: Matrix4Ro, leftClip: Float, topClip: Float, rightClip: Float, bottomClip: Float)

	/**
	 * Draws this element.
	 */
	fun render(glState: GlState)

}

interface TextNodeRo : Validatable, StyleableRo, BasicLayoutElementRo, Owned {

	override val disposed: Signal<(TextNodeRo) -> Unit>
	override val invalidated: Signal<(TextNodeRo, Int) -> Unit>

	val textNodeParent: TextNodeRo?
	val textField: TextField?

	/**
	 * The transformation matrix this node uses.
	 */
	val concatenatedTransform: Matrix4Ro

	fun localToGlobal(localCoord: Vector3): Vector3 {
		concatenatedTransform.prj(localCoord)
		return localCoord
	}

	/**
	 * The total number of text elements this node contains (deep/hierarchical).
	 */
	val textElementsCount: Int

	/**
	 * A virtual text element to indicate the position of the next element within this node.
	 */
	val placeholder: TextElementRo

	/**
	 * True if this node allows \n characters.
	 */
	val multiline: Boolean

	/**
	 * Returns the text element at the given index.
	 * @param index The text element index between 0 and [textElementsCount] - 1.
	 */
	fun getTextElementAt(index: Int): TextElementRo

	/**
	 * The number of lines this node has (deep/hierarchical).
	 */
	val linesCount: Int

	/**
	 * Returns the line at the given text element index (deep/hierarchical).
	 * @param index The text element index.
	 */
	fun getLineAt(index: Int): LineInfoRo

	/**
	 * Returns the line at the given index, or null if the index is < 0 or >= [textElementsCount].
	 */
	fun getLineOrNullAt(index: Int): LineInfoRo? {
		val n = linesCount
		if (n == 0 || index < 0 || index >= textElementsCount) return null
		return getLineAt(index)
	}

	/**
	 * @param x The relative x coordinate
	 * @param y The relative y coordinate
	 * @return Returns the relative index of the text element nearest (x, y). The text element index will be separated
	 * at the half-width of the element. This range will be between 0 and [textElementsCount] (inclusive)
	 */
	fun getSelectionIndex(x: Float, y: Float): Int

	/**
	 * Writes this node's contents to a string builder.
	 */
	fun toString(builder: StringBuilder)
}

interface TextNode : TextNodeRo, Styleable, BasicLayoutElement, Disposable {

	override val disposed: Signal<(TextNode) -> Unit>
	override val invalidated: Signal<(TextNode, Int) -> Unit>

	override var textNodeParent: TextNodeRo?
	override var textField: TextField?

	/**
	 * If true, this component's vertices will be clipped to the explicit size.
	 */
	var allowClipping: Boolean

	/**
	 * Sets the text selection.
	 * @param rangeStart The starting index of this leaf.
	 * @param selection A list of ranges that are selected.
	 */
	fun setSelection(rangeStart: Int, selection: List<SelectionRange>)

	/**
	 * Updates this component, validating it and its children.
	 */
	fun update()

	/**
	 * Renders any graphics.
	 * @see UiComponent.render
	 */
	fun render(clip: MinMaxRo)

}

private fun Owned.textFlow(init: ComponentInit<TextFlow>): TextFlow {
	val f = TextFlow(this)
	f.init()
	return f
}

/**
 * A placeholder for the last text element in a span. This is useful for calculating the text cursor placement.
 */
class LastTextElement(private val flow: TextFlow) : TextElementRo {

	override val textParent: TextSpanElement?
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

/**
 * Creates a [TextField] implementation with the provided text content.
 */
fun Owned.text(text: String, init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.text = text
	t.init()
	return t
}

/**
 * Creates a [TextField] implementation.
 */
fun Owned.text(init: ComponentInit<TextField> = {}): TextField {
	val t = TextFieldImpl(this)
	t.init()
	return t
}