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
import com.acornui.component.style.StyleTag
import com.acornui.component.style.Styleable
import com.acornui.component.style.addStyleRule
import com.acornui.core.cursor.RollOverCursor
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.input.clipboardCopy
import com.acornui.core.input.interaction.ClipboardItemType
import com.acornui.core.input.interaction.CopyInteractionRo
import com.acornui.core.input.interaction.DragAttachment
import com.acornui.core.input.interaction.DragInteractionRo
import com.acornui.core.selection.Selectable
import com.acornui.core.selection.SelectableComponent
import com.acornui.core.selection.SelectionManager
import com.acornui.core.selection.SelectionRange
import com.acornui.math.Bounds
import com.acornui.math.MinMaxRo

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
	 *
	 * @see replaceTextRange
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
	private val _textContents = p { +_textSpan }
	private var _contents: TextNode = watchNode(_textContents)

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
		if (_contents === value) return value
		unwatchNode(_contents)
		_contents = value
		_contents.allowClipping = _allowClipping
		watchNode(value)
		return value
	}

	private fun unwatchNode(node: TextNode) {
		node.textField = null
		node.invalidate(cascadingFlags)
		node.invalidated.remove(::childInvalidatedHandler)
		node.disposed.remove(::childDisposedHandler)
		if (!isValidatingLayout)
			invalidateLayout()
	}

	private fun watchNode(child: TextNode): TextNode {
		_assert(!isDisposed, "This TextField is disposed.")
		_assert(!child.isDisposed, "Added text node is disposed.")
		_assert(child.textField == null, "Added text node is already owned by a different text field.")
		child.textField = this
		child.invalidated.add(::childInvalidatedHandler)
		child.disposed.remove(::childDisposedHandler)
		child.invalidate(cascadingFlags)
		if (!isValidatingLayout)
			invalidateLayout()
		return child
	}

	private fun childDisposedHandler(child: TextNode) {
		unwatchNode(child)
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
		selectionManager.selectionChanged.remove(::selectionChangedHandler)
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
				val subStr = text.substring(sel.min, sel.max)
				e.addItem(ClipboardItemType.PLAIN_TEXT, subStr)
			}
		}
	}

	private val firstSelection: SelectionRange?
		get() = selectionManager.selection.firstOrNull { it.target == this }

	companion object {
		const val SELECTION = 1 shl 16
	}
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