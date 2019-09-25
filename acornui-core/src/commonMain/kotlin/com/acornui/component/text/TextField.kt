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

import com.acornui.async.getCompletedOrNull
import com.acornui.async.then
import com.acornui.component.*
import com.acornui.component.style.StyleTag
import com.acornui.component.style.Styleable
import com.acornui.component.style.addStyleRule
import com.acornui.cursor.RollOverCursor
import com.acornui.cursor.StandardCursors
import com.acornui.di.Owned
import com.acornui.di.inject
import com.acornui.input.Ascii
import com.acornui.input.KeyState
import com.acornui.input.clipboardCopy
import com.acornui.input.interaction.ClipboardItemType
import com.acornui.input.interaction.CopyInteractionRo
import com.acornui.input.interaction.DragAttachment
import com.acornui.input.interaction.DragInteractionRo
import com.acornui.math.Bounds
import com.acornui.selection.Selectable
import com.acornui.selection.SelectableComponent
import com.acornui.selection.SelectionManager
import com.acornui.selection.SelectionRange
import com.acornui.substringInRange
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface TextField : SingleElementContainer<TextNode>, Labelable, SelectableComponent, Styleable {

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
	 * Sets this text field's contents to a simple text flow.
	 *
	 * @see replaceTextRange
	 */
	var text: String

	/**
	 * If true (default), the contents will be clipped to the explicit size of this text field.
	 */
	var allowClipping: Boolean

	/**
	 * Replaces the given range with the provided text. This method doesn't destroy text node structure (therefore
	 * styling) outside of the range. Nodes entirely contained within this range, however, will be removed.
	 *
	 * E.g.
	 * ```
	 * +text("Hello World") {
	 *   replaceTextRange(1, 5, "i") // Hi World
	 * }
	 * ```
	 *
	 * @param startIndex The starting text element index for the replacement. (Inclusive)
	 * @param endIndex The ending text element index for the replacement. (Exclusive)
	 * (Max is `contents.textElements.size`)
	 */
	fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String)

//	fun <T : TextNode> replaceTextRange(startIndex: Int, endIndex: Int, newContent: T): T {
//
//	}

	companion object : StyleTag
}

/**
 * A component that displays text.
 * @author nbilyk
 */
@Suppress("LeakingThis", "UNUSED_PARAMETER")
open class TextFieldImpl(owner: Owned) : SingleElementContainerImpl<TextNode>(owner), TextField {

	override val flowStyle = bind(TextFlowStyle())
	override val charStyle = bind(CharStyle())

	private val selectionManager = inject(SelectionManager)

	private var _selectionCursor: RollOverCursor? = null

	/**
	 * The Selectable target to use for the selection range.
	 */
	override var selectionTarget: Selectable = this

	private val keyState by KeyState

	private val _textSpan = span()
	private val _textContents = p { +_textSpan }

	init {
		element = _textContents
		// Add the styles as rules so that they cascade down into the text spans:
		addStyleRule(flowStyle)
		addStyleRule(charStyle)
		styleTags.add(TextField)

		watch(charStyle) { cS ->
			cS.getFont()?.then {
				if (!isDisposed) invalidate(ValidationFlags.LAYOUT)
			}
			refreshCursor()
			if (cS.selectable) {
				if (_drag == null) {
					val d = DragAttachment(this, 0f)
					d.drag.add(::dragHandler)
					_drag = d
				}
			} else {
				_drag?.dispose()
				_drag = null
			}
		}
		validation.addNode(SELECTION, dependencies = ValidationFlags.HIERARCHY_ASCENDING, onValidate = ::updateSelection)
		selectionManager.selectionChanged.add(::selectionChangedHandler)
	}

	/**
	 * If true (default), the contents will be clipped to the explicit size of this text field.
	 */
	override var allowClipping: Boolean = true
		set(value) {
			if (field == value) return
			field = value
			_textContents.allowClipping = value
			element?.allowClipping = value
			invalidateLayout()
		}

	override fun onElementChanged(oldElement: TextNode?, newElement: TextNode?) {
		super.onElementChanged(oldElement, newElement)
		oldElement?.textField = null
		newElement?.textField = this
	}

	private var _drag: DragAttachment? = null

	private fun selectionChangedHandler(old: List<SelectionRange>, new: List<SelectionRange>) {
		invalidate(SELECTION)
	}

	private fun dragHandler(event: DragInteractionRo) {
		if (!charStyle.selectable) return
		if (!event.handled) {
			event.handled = true
			selectionManager.selection = getNewSelection(event) ?: emptyList()
		}
	}

	private fun getNewSelection(event: DragInteractionRo): List<SelectionRange>? {
		val contents = element ?: return emptyList()
		val p1 = event.startPositionLocal
		val p2 = event.positionLocal

		val p1A = if (keyState.keyIsDown(Ascii.SHIFT)) firstSelection?.startIndex ?: 0
		else contents.getSelectionIndex(p1.x, p1.y)
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
			element?.toString(builder)
			return builder.toString()
		}
		set(value) {
			_textSpan.text = value
			element = _textContents
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
		element?.setSelection(0, selectionManager.selection.filter { it.target == selectionTarget })
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val contents = element ?: return
		contents.setSize(explicitWidth, explicitHeight)
		contents.setPosition(0f, 0f)
		out.set(contents.bounds)

		// Handle sizing if the content is blank:
		if (contents.textElements.isEmpty()) {
			val font = charStyle.getFont()
			val fontData = font?.getCompletedOrNull()?.data
			val padding = flowStyle.padding
			val lineHeight: Float = (fontData?.lineHeight?.toFloat() ?: 0f) / charStyle.scaleY
			out.height = padding.expandHeight2(lineHeight)
			out.baseline = padding.top + (fontData?.baseline?.toFloat() ?: 0f) / charStyle.scaleY
		}

		if (contents.allowClipping) {
			if (explicitWidth != null) out.width = explicitWidth
			if (explicitHeight != null) {
				out.height = explicitHeight
				out.baseline = minOf(explicitHeight, out.baseline)
			}
		}
	}

	override fun dispose() {
		super.dispose()
		_selectionCursor?.dispose()
		_selectionCursor = null
		selectionManager.selectionChanged.remove(::selectionChangedHandler)
		_drag?.dispose()
		_drag = null
	}

	override fun onActivated() {
		super.onActivated()
		stage.clipboardCopy().add(::copyHandler)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		stage.clipboardCopy().remove(::copyHandler)
	}

	private fun copyHandler(e: CopyInteractionRo) {
		if (!e.defaultPrevented() && selectable && isRendered) {
			e.handled = true
			val sel = firstSelection
			if (sel != null) {
				val text = this.text
				val subStr = text.substringInRange(sel.min, sel.max)
				e.addItem(ClipboardItemType.PLAIN_TEXT, subStr)
			}
		}
	}

	private val firstSelection: SelectionRange?
		get() = selectionManager.selection.firstOrNull { it.target == selectionTarget }

	companion object {
		const val SELECTION = 1 shl 16
	}
}

/**
 * Creates a [TextField] implementation with the provided text content.
 */
inline fun Owned.text(text: String, init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.text = text
	t.init()
	return t
}

/**
 * Creates a [TextField] implementation.
 */
inline fun Owned.text(init: ComponentInit<TextField> = {}): TextField  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextFieldImpl(this)
	t.init()
	return t
}
