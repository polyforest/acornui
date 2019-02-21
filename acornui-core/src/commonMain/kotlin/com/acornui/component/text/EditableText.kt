package com.acornui.component.text

import com.acornui.async.then
import com.acornui.component.ContainerImpl
import com.acornui.component.ValidationFlags
import com.acornui.component.layout.algorithm.LineInfoRo
import com.acornui.component.rect
import com.acornui.core.di.inject
import com.acornui.core.di.own
import com.acornui.core.focus.blurredSelf
import com.acornui.core.focus.focusedSelf
import com.acornui.core.focus.isFocusedSelf
import com.acornui.core.input.*
import com.acornui.core.input.interaction.*
import com.acornui.core.isWhitespace2
import com.acornui.core.mvc.CommandGroup
import com.acornui.core.mvc.commander
import com.acornui.core.mvc.invokeCommand
import com.acornui.core.repeat2
import com.acornui.core.selection.SelectionManager
import com.acornui.core.selection.SelectionRange
import com.acornui.core.selection.selectAll
import com.acornui.core.selection.unselect
import com.acornui.core.time.delayedCallback
import com.acornui.core.time.onTick
import com.acornui.function.as1
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Bounds
import com.acornui.math.MathUtils
import com.acornui.reflect.observable
import com.acornui.signal.Signal0
import com.acornui.string.isLetterOrDigit2

/**
 * Encapsulates the common functionality of editable text.
 * @suppress
 */
@Suppress("LeakingThis")
class EditableText(private val host: TextInput) : ContainerImpl(host) {

	private val _input = own(Signal0())
	val input = _input.asRo()

	private val _changed = own(Signal0())
	val changed = _changed.asRo()

	var editable by observable(true) {
		textCursor.visible = it
	}

	var maxLength: Int? = null

	val textField = addChild(TextFieldImpl(this).apply { selectionTarget = host })

	var pageHeight: Float = 400f

	private val textCursor = addChild(rect {
		style.backgroundColor = Color.WHITE
		layoutInvalidatingFlags = ValidationFlags.LAYOUT // Allows us to toggle visibility on this cursor and not affect layout.
		setOrigin(1f, 0f)
		setSize(2f, 2f)
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
			if (_text == value) return
			_text = if (_restrictPatternRegex == null) value else value.replace(_restrictPatternRegex!!, "")
			_text = _text.replace("\r", "")
			refreshText()
		}

	var placeholder: String = ""

	private var _restrictPattern: String? = null
	private var _restrictPatternRegex: Regex? = null

	var restrictPattern: String?
		get() = _restrictPattern
		set(value) {
			if (_restrictPattern == value) return
			_restrictPattern = value
			_restrictPatternRegex = if (value == null) null else Regex(value)
			if (value != null) {
				_text = _text.replace(_restrictPatternRegex!!, "")
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

	private val cmd = own(commander())

	init {
		host.focusedSelf().add {
			if (charStyle.selectable)
				host.selectAll()
			// todo: open mobile keyboard
		}

		host.blurredSelf().add {
			host.unselect()
			if (isActive)
				_changed.dispatch()
		}

		host.char().add {
			if (editable && !it.defaultPrevented()) {
				val font = host.charStyle.font
				if (font?.glyphs?.containsKey(it.char) == true && it.char != '\n' && it.char != '\r') {
					it.handled = true
					replaceSelection(it.char.toString())
					_input.dispatch()
				}
			}
		}

		host.clipboardPaste().add {
			if (editable && !it.defaultPrevented()) {
				it.handled = true
				async {
					it.getItemByType(ClipboardItemType.PLAIN_TEXT)
				} then { str ->
					if (str != null) {
						replaceSelection(str.replace("\r", ""), CommandGroup())
						currentGroup = CommandGroup()
						_input.dispatch()
					}
				}
			}
		}

		host.clipboardCopy().add {
			if (!it.defaultPrevented()) {
				it.handled = true
				val sel = firstSelection
				if (sel != null) {
					val text = this.text
					val subStr = text.substring(sel.min, sel.max)
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
					val subStr = text.substring(sel.min, sel.max)
					if (editable)
						replaceSelection("", CommandGroup())
					it.addItem(ClipboardItemType.PLAIN_TEXT, subStr)
					currentGroup = CommandGroup()
					_input.dispatch()
				}
			}
		}

		host.keyDown().add(this::keyDownHandler)
		host.touchStart().add { column = -1; resetCursorBlink() }
		host.mouseDown().add { column = -1; resetCursorBlink() }

		validation.addNode(TEXT_CURSOR, ValidationFlags.LAYOUT, this::updateTextCursor)

		selectionManager.selectionChanged.add(this::selectionChangedHandler)

		// Handle the cursor blink
		onTick {
			cursorTimer -= it
			if (cursorTimer <= 0f) {
				cursorTimer = cursorBlinkSpeed
				usingCursorColorOne = !usingCursorColorOne
				textCursor.colorTint = if (usingCursorColorOne) cursorColorOne else cursorColorTwo
			}
		}

		//host.enableUndoRedo()

		cmd.onCommandInvoked(ReplaceTextRangeCommand, this::onReplaceTextRange)

		cmd.onCommandInvoked(ChangeSelectionCommand) {
			if (it.target == host)
				selectionManager.selection = it.newSelection
		}

		host.undo().add {
			if (!it.defaultPrevented()) {
				_input.dispatch()
				_changed.dispatch()
			}
		}

		host.redo().add {
			if (!it.defaultPrevented()) {
				_input.dispatch()
				_changed.dispatch()
			}
		}
	}

	override fun updateStyles() {
		super.updateStyles()
		// This class's styles are delegated from the textField styles.
		textField.validate(ValidationFlags.STYLES)
	}

	override fun onActivated() {
		super.onActivated()
		window.isActiveChanged.add(this::windowActiveChangedHandler.as1)
		windowActiveChangedHandler()
	}

	override fun onDeactivated() {
		super.onDeactivated()
		window.isActiveChanged.remove(this::windowActiveChangedHandler.as1)
	}

	private fun windowActiveChangedHandler() {
		invalidate(TEXT_CURSOR)
	}

	private val contents
		get() = textField.contents

	private var column = -1

	private fun keyDownHandler(event: KeyInteractionRo) {
		if (event.defaultPrevented()) return
		clearGroup()
		resetCursorBlink()

		if (event.keyCode != Ascii.UP && event.keyCode != Ascii.DOWN && event.keyCode != Ascii.PAGE_UP && event.keyCode != Ascii.PAGE_DOWN) column = -1

		when (event.keyCode) {
			Ascii.LEFT -> cursorLeft(event)
			Ascii.RIGHT -> cursorRight(event)
			Ascii.UP -> cursorUp(event)
			Ascii.DOWN -> cursorDown(event)
			Ascii.BACKSPACE -> {
				event.handled = true
				backspace()
				_input.dispatch()
			}
			Ascii.TAB -> if (allowTab) {
				event.preventDefault() // Prevent focus manager from tabbing.
				event.handled = true
				replaceSelection("\t")
				_input.dispatch()
			}
			Ascii.DELETE -> {
				event.handled = true
				delete()
				_input.dispatch()
			}
			Ascii.ENTER, Ascii.RETURN -> {
				val sel = firstSelection
				val multiline = if (sel == null || sel.min >= contents.textElementsCount) null else textField.contents.getTextElementAt(sel.min).textParent?.textParent?.multiline

				if (multiline ?: flowStyle.multiline) {
					event.handled = true
					replaceSelection("\n")
					_input.dispatch()
				} else {
					if (_changed.isNotEmpty()) {
						// Only mark the key event as handled if there are change handlers.
						event.handled = true
						_changed.dispatch()
					}
				}
			}
			Ascii.HOME -> cursorHome(event)
			Ascii.END -> cursorEnd(event)
			Ascii.A -> {
				if (event.commandPlat) {
					val n = contents.textElementsCount
					setSelection(listOf(SelectionRange(host, 0, n)))
				}
			}
			Ascii.PAGE_UP -> cursorPageUp(event)
			Ascii.PAGE_DOWN -> cursorPageDown(event)
		}
	}

	private val clearGroup = delayedCallback(0.4f) {
		currentGroup = CommandGroup()
	}

	private fun resetCursorBlink() {
		cursorTimer = cursorBlinkSpeed
		usingCursorColorOne = true
		textCursor.colorTint = cursorColorOne
	}

	private fun cursorLeft(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val n = contents.textElementsCount
		var i = MathUtils.clamp(sel.endIndex, 0, n)
		if (event.commandPlat) {
			while (i > 0 && charAt(i - 1).charType() == 0) {
				i--
			}
			val startType = charAt(--i).charType()
			while (i > 0 && charAt(i - 1).charType() == startType) {
				i--
			}
		} else --i
		if (i < 0) i = 0
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else i, i))
	}

	private fun cursorRight(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val n = contents.textElementsCount
		var i = MathUtils.clamp(sel.endIndex, 0, n)
		if (event.commandPlat) {
			val startType = charAt(i++).charType()
			while (i < n && charAt(i).charType() == startType) {
				i++
			}
			while (i < n && charAt(i).charType() == 0) {
				i++
			}
		} else ++i
		if (i > n) i = n
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else i, i))
	}

	private fun charAt(index: Int): Char? {
		return contents.getTextElementAt(index).char
	}

	private fun Char?.charType(): Int {
		if (this == null) return -1
		return when {
			isWhitespace2() -> 0
			isLetterOrDigit2() -> 1
			else -> 2
		}
	}

	private fun cursorUp(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val line = contents.getLineOrNullAt(minOf(contents.textElementsCount - 1, sel.endIndex)) ?: return
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
		val sel = firstSelection ?: return
		val line = contents.getLineOrNullAt(sel.endIndex) ?: return
		val nextLine = contents.getLineOrNullAt(line.endIndex)
		if (nextLine != null) {
			if (column == -1)
				column = sel.endIndex - line.startIndex
			val nextPos = minOf(lineEnd(nextLine), nextLine.startIndex + column)
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else nextPos, nextPos))
		} else {
			val n = contents.textElementsCount
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else n, n))
		}
	}

	private fun lineEnd(line: LineInfoRo): Int {
		val n = contents.textElementsCount
		return if (line.endIndex == n) n else line.endIndex - 1
	}

	private fun cursorHome(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val line = contents.getLineOrNullAt(minOf(contents.textElementsCount - 1, sel.endIndex)) ?: return
		val metaKey = event.commandPlat
		val toIndex = if (metaKey) 0 else line.startIndex
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else toIndex, toIndex))
	}

	private fun cursorEnd(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val line = contents.getLineOrNullAt(sel.endIndex) ?: return
		val pos = lineEnd(line)
		val metaKey = event.commandPlat
		val toIndex = if (metaKey) contents.textElementsCount else pos
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else toIndex, toIndex))
	}

	private fun cursorPageUp(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val currentLine = contents.getLineOrNullAt(minOf(sel.endIndex, contents.textElementsCount - 1)) ?: return
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
		val sel = firstSelection ?: return
		val currentLine = contents.getLineOrNullAt(sel.endIndex) ?: return
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
			val n = contents.textElementsCount
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else n, n))
		}
	}

	private val contentsSize: Int
		get() = textField.contents.textElementsCount

	private val firstSelection: SelectionRange?
		get() = selectionManager.selection.firstOrNull { it.target == host }

	/**
	 * If there is a non-empty selection, that selection is replaced with nothing. If there is an empty selection,
	 * the previous character is deleted.
	 */
	private fun backspace() {
		val sel = firstSelection ?: return
		if (sel.startIndex != sel.endIndex) {
			replaceTextRange(sel.min, sel.max, "")
			setSelection(listOf(SelectionRange(host, sel.min, sel.min)))
		} else if (sel.min > 0) {
			val i = minOf(contentsSize, sel.min)
			replaceTextRange(i - 1, i, "")
			setSelection(listOf(SelectionRange(host, sel.min - 1, sel.min - 1)))
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
			setSelection(listOf(SelectionRange(host, sel.min, sel.min)))
		} else if (sel.min < contentsSize) {
			replaceTextRange(sel.min, sel.max + 1, "")
			setSelection(listOf(SelectionRange(host, sel.min, sel.min)))
		}
	}

	private fun replaceSelection(str: String, group: CommandGroup = currentGroup) {
		var str2 = if (_restrictPatternRegex == null) str else str.replace(_restrictPatternRegex!!, "")
		val sel = firstSelection ?: return
		val maxLength = maxLength
		if (maxLength != null) {
			val newSize = contentsSize + str2.length - sel.size
			if (newSize > maxLength) {
				str2 = str2.substring(0, maxOf(0, str2.length - newSize + maxLength))
			}
		}
		replaceTextRange(sel.min, sel.max, str2, group)
		val p = sel.min + str2.length
		setSelection(listOf(SelectionRange(host, p, p)), group)
	}

	private var currentGroup = CommandGroup()

	/**
	 * Invokes the command to replace the given text range.
	 */
	private fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String, group: CommandGroup = currentGroup) = host.replaceTextRange(startIndex, endIndex, newText, group)

	/**
	 * Invokes the command to change the current selection.
	 */
	private fun setSelection(newSelection: List<SelectionRange>, group: CommandGroup = currentGroup) {
		invokeCommand(ChangeSelectionCommand(host, selectionManager.selection, newSelection, group))
	}

	private fun onReplaceTextRange(cmd: ReplaceTextRangeCommand) {
		if (cmd.target != host) return
		// TODO: Make this efficient.
		val text = this.text
		this.text = text.substring(0, MathUtils.clamp(cmd.startIndex, 0, text.length)) + cmd.newText + text.substring(MathUtils.clamp(cmd.endIndex, 0, text.length), text.length)
		validateLayout()
	}

	private fun String.toPassword(): String {
		return passwordMask.repeat2(length)
	}

	private fun selectionChangedHandler(oldSelection: List<SelectionRange>, newSelection: List<SelectionRange>) {
		if (oldSelection.filter { it.target == host } != newSelection.filter { it.target == host }) {
			invalidate(TEXT_CURSOR)
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		textField.setSize(explicitWidth, explicitHeight)
		out.set(textField.bounds)
	}

	private fun updateTextCursor() {
		textField.validate(ValidationFlags.LAYOUT)

		val textCursorVisible = if (!charStyle.selectable || !window.isActive) false else {
			val sel = firstSelection
			if (host.isFocusedSelf && sel != null) {
				val rangeEnd = contents.textElementsCount
				val end = MathUtils.clamp(sel.endIndex, 0, rangeEnd)
				val textElement = if (end >= rangeEnd) contents.placeholder else contents.getTextElementAt(end)
				textCursor.x = textElement.textFieldX
				textCursor.y = textElement.textFieldY
				textCursor.scaleY = textElement.lineHeight / textCursor.height
				true
			} else {
				false
			}
		}
		if (textCursor.visible != textCursorVisible) {
			if (textCursorVisible)
				resetCursorBlink()
			textCursor.visible = textCursorVisible
		}
	}

	override fun dispose() {
		super.dispose()
		selectionManager.selectionChanged.remove(this::selectionChangedHandler)
	}

	companion object {
		private const val TEXT_CURSOR = 1 shl 16
	}
}