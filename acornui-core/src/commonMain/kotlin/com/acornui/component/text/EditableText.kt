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

import com.acornui.component.*
import com.acornui.component.layout.algorithm.LineInfoRo
import com.acornui.di.own
import com.acornui.focus.blurredSelf
import com.acornui.focus.focusedSelf
import com.acornui.focus.isFocused
import com.acornui.focus.isFocusedSelf
import com.acornui.function.as1
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.input.*
import com.acornui.input.interaction.ClipboardItemType
import com.acornui.input.interaction.KeyInteractionRo
import com.acornui.input.interaction.commandPlat
import com.acornui.math.Bounds
import com.acornui.math.MathUtils.clamp
import com.acornui.math.Vector3
import com.acornui.properties.afterChange
import com.acornui.repeat2
import com.acornui.selection.SelectionManager
import com.acornui.selection.SelectionRange
import com.acornui.selection.selectAll
import com.acornui.selection.unselect
import com.acornui.signal.Signal1
import com.acornui.string.isLetterOrDigit2
import com.acornui.substringInRange
import com.acornui.time.onTick

/**
 * Encapsulates the common functionality of editable text.
 * @suppress
 */
@Suppress("LeakingThis")
class EditableText(private val host: TextInput) : ContainerImpl(host) {

	private val _input = own(Signal1<TextInput>())
	val input = _input.asRo()

	private val _changed = own(Signal1<TextInput>())
	val changed = _changed.asRo()

	var editable by afterChange(true) {
		textCursor.visible = it
	}

	var maxLength: Int? = null

	private val softKeyboard: SoftKeyboard? = softKeyboard()

	val textField = addChild(TextFieldImpl(this).apply { selectionTarget = host })

	var pageHeight: Float = 400f

	private val textCursor = addChild(rect {
		interactivityMode = InteractivityMode.NONE
		style.backgroundColor = Color.WHITE
		layoutInvalidatingFlags = ValidationFlags.LAYOUT // Allows us to toggle visibility on this cursor and not affect layout.
		setOrigin(1f, 0f)
		size(2f, 2f)
		colorTint = Color.CLEAR
	})

	var cursorColorOne: ColorRo = Color.BLACK
	var cursorColorTwo: ColorRo = Color.WHITE
	var cursorBlinkSpeed: Float = 0.5f

	private var usingCursorColorOne = true
	private var cursorTimer: Float = 0f

	private var _text: String = ""
	var text: String
		get() = _text
		set(value) {
			softKeyboard?.text = value
			setTextInternal(value)
		}

	private fun setTextInternal(value: String) {
		if (_text == value) return
		_text = if (_restrictPattern == null) value else value.replace(_restrictPattern!!, "")
		_text = _text.replace("\r", "")
		refreshText()
	}

	var placeholder: String = ""

	private var _restrictPattern: Regex? = null

	var restrictPattern: Regex?
		get() = _restrictPattern
		set(value) {
			if (_restrictPattern == value) return
			_restrictPattern = value
			if (value != null) {
				_text = _text.replace(_restrictPattern!!, "")
			}
			refreshText()
		}

	private fun refreshText() {
		column = -1
		textField.text = if (_password) _text.toPassword() else _text
	}

	val charStyle: CharStyle
		get() = textField.charStyle

	val flowStyle: TextFlowStyle
		get() = textField.flowStyle

	/**
	 * The mask to use as replacement characters when [password] is set to true.
	 */
	var passwordMask = "*"

	private var _password = false
	var password: Boolean
		get() = _password
		set(value) {
			if (value == _password) return
			_password = value
			refreshText()
		}

	private val selectionManager = inject(SelectionManager)

	/**
	 * If true, pressing TAB will create a \t instead of navigating focus.
	 */
	var allowTab: Boolean = false

	/**
	 * When this component is blurred, only dispatch a changed event if the value has changed.
	 * It will start as true so that the first value commit, even if the value is still "", will be counted.
	 */
	private var pendingChange = true

	init {
		host.focusedSelf().add {
			softKeyboard?.position(localToCanvas(Vector3()))
			softKeyboard?.focus()
			if (charStyle.selectable)
				host.selectAll()
		}

		host.blurredSelf().add {
			host.unselect()
			softKeyboard?.blur()
			if (isActive)
				dispatchChanged()
		}

		host.char().add {
			if (editable && !it.defaultPrevented()) {
				if (it.char != '\n' && it.char != '\r') {
					it.handled = true
					replaceSelection(it.char.toString())
					dispatchInput()
				}
			}
		}

		host.clipboardPaste().add {
			if (editable && !it.defaultPrevented()) {
				it.handled = true
				val str = it.getItemByType(ClipboardItemType.PLAIN_TEXT)
				if (str != null) {
					replaceSelection(str.filter { char -> char != '\n' && char != '\r' })
					dispatchInput()
				}
			}
		}

		host.clipboardCopy().add {
			if (!it.defaultPrevented()) {
				it.handled = true
				val sel = firstSelection
				if (sel != null) {
					val text = this.text
					val subStr = text.substringInRange(sel.min, sel.max)
					it.addItem(ClipboardItemType.PLAIN_TEXT, subStr)
				}
			}
		}

		host.clipboardCut().add {
			if (!it.defaultPrevented()) {
				it.handled = true
				val sel = firstSelection
				if (sel != null) {
					val text = this.text
					val subStr = text.substringInRange(sel.min, sel.max)
					if (editable)
						replaceSelection("")
					it.addItem(ClipboardItemType.PLAIN_TEXT, subStr)
					dispatchInput()
				}
			}
		}

		host.keyDown().add(::keyDownHandler)
		host.touchStart().add { column = -1; resetCursorBlink() }
		host.mouseDown().add { column = -1; resetCursorBlink() }

		validation.addNode(TEXT_CURSOR, ValidationFlags.LAYOUT, ::updateTextCursor)

		selectionManager.selectionChanged.add(::selectionChangedHandler)

		// Handle the cursor blink
		onTick {
			if (window.isActive) {
				cursorTimer -= it
				if (cursorTimer <= 0f) {
					cursorTimer = cursorBlinkSpeed
					usingCursorColorOne = !usingCursorColorOne
					textCursor.colorTint = if (usingCursorColorOne) cursorColorOne else cursorColorTwo
				}
			}
		}

		//host.enableUndoRedo()

		// TODO: Support undo/redo
//		host.undo().add {
//			if (!it.defaultPrevented()) {
//				dispatchInput()
//				dispatchChanged()
//			}
//		}
//
//		host.redo().add {
//			if (!it.defaultPrevented()) {
//				dispatchInput()
//				dispatchChanged()
//			}
//		}

		softKeyboard?.apply {
			selectionChanged.add {
				println("Setting selection $selectionStart $selectionEnd")
				selectionManager.selection = listOf(SelectionRange(host, selectionStart, selectionEnd))
			}
			input.add {
				setTextInternal(text)
				dispatchInput()
			}
		}
	}

	var softKeyboardType: String = SoftKeyboardType.DEFAULT
		set(value) {
			field = value
			softKeyboard?.type = value
		}

	private fun getFontAtSelection(): BitmapFont? {
		val firstSelection = firstSelection ?: return null
		return textField.getLoadedFontAtIndex(firstSelection.endIndex)
	}

	override fun updateStyles() {
		super.updateStyles()
		// This class's styles are delegated from the textField styles.
		textField.validate(ValidationFlags.STYLES)
	}

	override fun onActivated() {
		super.onActivated()
		window.isActiveChanged.add(::windowActiveChangedHandler.as1)
		windowActiveChangedHandler()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		window.isActiveChanged.remove(::windowActiveChangedHandler.as1)
	}

	private fun windowActiveChangedHandler() {
		invalidate(TEXT_CURSOR)
	}

	private var column = -1

	private fun keyDownHandler(event: KeyInteractionRo) {
		val contents = textField.element ?: return
		if (event.defaultPrevented()) return
		resetCursorBlink()

		if (event.keyCode != Ascii.UP && event.keyCode != Ascii.DOWN && event.keyCode != Ascii.PAGE_UP && event.keyCode != Ascii.PAGE_DOWN) column = -1

		when (event.keyCode) {
			Ascii.LEFT -> cursorLeft(event)
			Ascii.RIGHT -> cursorRight(event)
			Ascii.UP -> cursorUp(event)
			Ascii.DOWN -> cursorDown(event)
			Ascii.BACKSPACE -> {
				event.preventDefault() // Otherwise backspace causes browser BACK in IE.
				event.handled = true
				backspace(event)
				dispatchInput()
			}
			Ascii.TAB -> if (allowTab) {
				event.preventDefault() // Prevent focus manager from tabbing.
				event.handled = true
				replaceSelection("\t")
				dispatchInput()
			}
			Ascii.DELETE -> {
				event.handled = true
				delete()
				dispatchInput()
			}
			Ascii.ENTER, Ascii.RETURN -> {
				val sel = firstSelection
				val multiline = if (sel == null || sel.min >= contents.textElements.size) null
				else contents.textElements[sel.min].parentSpan?.textParent?.multiline

				if (multiline ?: flowStyle.multiline) {
					event.handled = true
					replaceSelection("\n")
					dispatchInput()
				} else {
					if (_changed.isNotEmpty()) {
						// Only mark the key event as handled if there are change handlers and there was a change.
						if (dispatchChanged())
							event.handled = true
					}
				}
			}
			Ascii.HOME -> cursorHome(event)
			Ascii.END -> cursorEnd(event)
			Ascii.A -> {
				if (event.commandPlat) {
					event.handled = true
					val n = contents.textElements.size
					selectionManager.selection = listOf(SelectionRange(host, 0, n))
				}
			}
			Ascii.PAGE_UP -> cursorPageUp(event)
			Ascii.PAGE_DOWN -> cursorPageDown(event)
		}
	}

	private fun resetCursorBlink() {
		cursorTimer = cursorBlinkSpeed
		usingCursorColorOne = true
		textCursor.colorTint = cursorColorOne
	}

	private fun cursorLeft(event: KeyInteractionRo) {
		val contents = textField.element ?: return
		val sel = firstSelection ?: return
		val n = contents.textElements.size
		var i = clamp(sel.endIndex, 0, n)
		if (event.commandPlat) i = previousWordIndex(i) else --i
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else i, i))
	}

	private fun previousWordIndex(index: Int): Int {
		var i = index
		if (i == 0) return 0
		while (i > 0 && charAt(i - 1).charType() == 0) {
			i--
		}
		val startType = charAt(--i).charType()
		while (i > 0 && charAt(i - 1).charType() == startType) {
			i--
		}
		return i
	}

	private fun cursorRight(event: KeyInteractionRo) {
		val contents = textField.element ?: return
		val sel = firstSelection ?: return
		val n = contents.textElements.size
		var i = clamp(sel.endIndex, 0, n)
		if (event.commandPlat) i = nextWordIndex(i) else ++i
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else i, i))
	}

	private fun nextWordIndex(index: Int): Int {
		val contents = textField.element ?: return -1
		var i = index
		val n = contents.textElements.size
		if (i == n) return n
		val startType = charAt(i++).charType()
		while (i < n && charAt(i).charType() == startType) {
			i++
		}
		while (i < n && charAt(i).charType() == 0) {
			i++
		}
		return i
	}

	private fun charAt(index: Int): Char? {
		val contents = textField.element ?: return null
		return contents.textElements[index].char
	}

	private fun Char?.charType(): Int {
		if (this == null) return -1
		return when {
			isWhitespace() -> 0
			isLetterOrDigit2() -> 1
			else -> 2
		}
	}

	private fun cursorUp(event: KeyInteractionRo) {
		val contents = textField.element ?: return
		val sel = firstSelection ?: return
		val line = contents.getLineOrNullAt(minOf(contents.textElements.size - 1, sel.endIndex)) ?: return
		val previousLine = contents.getLineOrNullAt(line.startIndex - 1)
		if (previousLine != null) {
			if (column == -1)
				column = sel.endIndex - line.startIndex
			val nextPos = minOf(previousLine.endIndex - 1, previousLine.startIndex + column)
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else nextPos, nextPos))
		} else {
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else 0, 0))
		}
	}

	private fun cursorDown(event: KeyInteractionRo) {
		val contents = textField.element ?: return
		val sel = firstSelection ?: return
		val line = contents.getLineOrNullAt(sel.endIndex) ?: return
		event.handled = true
		val nextLine = contents.getLineOrNullAt(line.endIndex)
		if (nextLine != null) {
			if (column == -1)
				column = sel.endIndex - line.startIndex
			val nextPos = minOf(lineEnd(nextLine), nextLine.startIndex + column)
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else nextPos, nextPos))
		} else {
			val n = contents.textElements.size
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else n, n))
		}
	}

	/**
	 *
	 */
	private fun lineEnd(line: LineInfoRo): Int {
		val contents = textField.element ?: return -1
		val n = contents.textElements.size
		return if (line.endIndex == n) n else line.endIndex - 1
	}

	private fun cursorHome(event: KeyInteractionRo) {
		val contents = textField.element ?: return
		val sel = firstSelection ?: return
		val line = contents.getLineOrNullAt(minOf(contents.textElements.size - 1, sel.endIndex)) ?: contents.lines.firstOrNull() ?: return
		event.handled = true
		val metaKey = event.commandPlat
		val toIndex = if (metaKey) 0 else line.startIndex
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else toIndex, toIndex))
	}

	private fun cursorEnd(event: KeyInteractionRo) {
		val contents = textField.element ?: return
		val sel = firstSelection ?: return
		val line = contents.getLineOrNullAt(sel.endIndex) ?: contents.lines.lastOrNull() ?: return
		event.handled = true
		val pos = lineEnd(line)
		val metaKey = event.commandPlat
		val toIndex = if (metaKey) contents.textElements.size else pos
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else toIndex, toIndex))
	}

	private fun cursorPageUp(event: KeyInteractionRo) {
		val contents = textField.element ?: return
		val sel = firstSelection ?: return
		val currentLine = contents.getLineOrNullAt(minOf(sel.endIndex, contents.textElements.size - 1)) ?: return
		event.handled = true
		var line: LineInfoRo? = currentLine
		var h = line?.height ?: 0f
		while (line != null && h < pageHeight) {
			line = contents.getLineOrNullAt(line.startIndex - 1)
			h += line?.height ?: 0f
		}
		if (line != null) {
			if (column == -1)
				column = sel.endIndex - currentLine.startIndex
			val nextPos = minOf(line.endIndex - 1, line.startIndex + column)
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else nextPos, nextPos))
		} else {
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else 0, 0))
		}
	}

	private fun cursorPageDown(event: KeyInteractionRo) {
		val contents = textField.element ?: return
		val sel = firstSelection ?: return
		val currentLine = contents.getLineOrNullAt(sel.endIndex) ?: return
		event.handled = true
		var line: LineInfoRo? = currentLine
		var h = line?.height ?: 0f
		while (line != null && h < pageHeight) {
			line = contents.getLineOrNullAt(line.endIndex)
			h += line?.height ?: 0f
		}
		if (line != null) {
			if (column == -1)
				column = sel.endIndex - currentLine.startIndex
			val nextPos = minOf(lineEnd(line), line.startIndex + column)
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else nextPos, nextPos))
		} else {
			val n = contents.textElements.size
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else n, n))
		}
	}

	private val contentsSize: Int
		get() = textField.element?.textElements?.size ?: 0

	private val firstSelection: SelectionRange?
		get() = selectionManager.selection.firstOrNull { it.target == host }

	/**
	 * If there is a non-empty selection, that selection is replaced with nothing. If there is an empty selection,
	 * the previous character is deleted.
	 */
	private fun backspace(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		if (sel.startIndex != sel.endIndex) {
			replaceTextRange(sel.min, sel.max, "")
			selectionManager.selection = listOf(SelectionRange(host, sel.min, sel.min))
		} else if (sel.min > 0) {
			val to = minOf(contentsSize, sel.min)
			val from = if (event.commandPlat) previousWordIndex(to) else (to - 1)
			replaceTextRange(from, to, "")
			selectionManager.selection = listOf(SelectionRange(host, from, from))
		}
	}

	/**
	 * If there is a non-empty selection, that selection is replaced with nothing. If there is an empty selection,
	 * the next character is deleted.
	 */
	private fun delete() {
		val sel = firstSelection ?: return
		if (sel.startIndex != sel.endIndex) {
			replaceTextRange(sel.min, sel.max, "")
			selectionManager.selection = listOf(SelectionRange(host, sel.min, sel.min))
		} else if (sel.min < contentsSize) {
			replaceTextRange(sel.min, sel.max + 1, "")
			selectionManager.selection = listOf(SelectionRange(host, sel.min, sel.min))
		}
	}

	private fun replaceSelection(str: String) {
		var str2 = if (_restrictPattern == null) str else str.replace(_restrictPattern!!, "")
		val sel = firstSelection ?: return
		val maxLength = maxLength
		if (maxLength != null) {
			val newSize = contentsSize + str2.length - sel.size
			if (newSize > maxLength) {
				str2 = str2.substring(0, maxOf(0, str2.length - newSize + maxLength))
			}
		}
		replaceTextRange(sel.min, sel.max, str2)
		val p = sel.min + str2.length
		selectionManager.selection = listOf(SelectionRange(host, p, p))
	}

	fun replaceTextRange(startIndex: Int,
						 endIndex: Int,
						 newText: String) {
		// TODO: Make this efficient.
		val oldText = this.text
		this.text = oldText.substring(0, clamp(startIndex, 0, oldText.length)) + newText + oldText.substring(clamp(endIndex, 0, oldText.length), oldText.length)
	}

	private fun String.toPassword(): String {
		return passwordMask.repeat2(length)
	}

	private fun selectionChangedHandler(oldSelection: List<SelectionRange>, newSelection: List<SelectionRange>) {
		if (oldSelection.filter { it.target == host } != newSelection.filter { it.target == host }) {
			resetCursorBlink()
			val firstSelection = firstSelection
			if (softKeyboard?.selectionChanged?.isDispatching != true)
				softKeyboard?.setSelectionRange(firstSelection?.startIndex ?: 0, firstSelection?.endIndex ?: 0)
			invalidate(TEXT_CURSOR)
		}
	}

	private fun dispatchInput() {
		pendingChange = true
		_input.dispatch(host)
	}

	private fun dispatchChanged(): Boolean {
		if (!pendingChange) return false
		pendingChange = false
		_changed.dispatch(host)
		return true
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		textField.size(explicitWidth, explicitHeight)
		out.set(textField.bounds)
	}

	private fun updateTextCursor() {
		val contents = textField.element ?: return

		val textElement = if (!charStyle.selectable || !window.isActive) null else {
			val sel = firstSelection
			if (host.isFocused && sel != null) {
				val rangeEnd = contents.textElements.size
				val end = clamp(sel.endIndex, 0, rangeEnd)
				if (end >= rangeEnd) contents.placeholder else contents.textElements[end]
			} else {
				null
			}
		}
		val textCursorVisible = textElement != null
		if (textElement != null) {
			textCursor.x = textElement.textFieldX
			textCursor.y = textElement.textFieldY
			textCursor.scaleY = textElement.lineHeight / textCursor.height
		}
		if (textCursor.visible != textCursorVisible) {
			if (textCursorVisible)
				resetCursorBlink()
			textCursor.visible = textCursorVisible
		}
	}

	override fun dispose() {
		super.dispose()
		selectionManager.selectionChanged.remove(::selectionChangedHandler)
	}

	companion object {
		private const val TEXT_CURSOR = 1 shl 16
	}
}
