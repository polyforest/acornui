package com.acornui.gl.component.text

import com.acornui.async.launch
import com.acornui.component.*
import com.acornui.component.layout.algorithm.LineInfoRo
import com.acornui.component.layout.setSize
import com.acornui.component.scroll.ClampedScrollModel
import com.acornui.component.scroll.ScrollPolicy
import com.acornui.component.scroll.scrollArea
import com.acornui.component.scroll.scrollTo
import com.acornui.component.style.set
import com.acornui.component.text.*
import com.acornui.core.Disposable
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.focus.blurred
import com.acornui.core.focus.focused
import com.acornui.core.input.*
import com.acornui.core.input.interaction.ClipboardItemType
import com.acornui.core.input.interaction.KeyInteractionRo
import com.acornui.core.repeat2
import com.acornui.core.selection.SelectionManager
import com.acornui.core.selection.SelectionRange
import com.acornui.core.selection.selectAll
import com.acornui.core.selection.unselect
import com.acornui.core.time.enterFrame
import com.acornui.gl.component.drawing.dynamicMeshC
import com.acornui.gl.component.drawing.fillStyle
import com.acornui.gl.component.drawing.lineStyle
import com.acornui.gl.component.drawing.quad
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.Bounds
import com.acornui.math.MathUtils.clamp
import com.acornui.math.Rectangle
import com.acornui.math.Vector2
import com.acornui.math.minOf4
import com.acornui.signal.Signal
import com.acornui.signal.Signal0

open class GlTextInput(owner: Owned) : ContainerImpl(owner), TextInput {

	protected val background = addChild(rect())

	override var focusEnabled: Boolean = true
	override var focusOrder: Float = 0f
	override var highlight: UiComponent? by createSlot()

	final override val textInputStyle = bind(TextInputStyle())
	final override val boxStyle = bind(BoxStyle())
	protected val editableText = addChild(EditableText(this))

	override val charStyle: CharStyle
		get() = editableText.charStyle

	override val flowStyle: TextFlowStyle
		get() = editableText.flowStyle

	override val input: Signal<() -> Unit>
		get() = editableText.input

	override val changed: Signal<() -> Unit>
		get() = editableText.changed

	override var editable: Boolean
		get() = editableText.editable
		set(value) {
			editableText.editable = value
		}

	override var maxLength: Int?
		get() = editableText.maxLength
		set(value) {
			editableText.maxLength = value
		}

	override var text: String
		get() = editableText.text
		set(value) {
			editableText.text = value
		}

	override var placeholder: String
		get() = editableText.placeholder
		set(value) {
			editableText.placeholder = value
		}

	override var restrictPattern: String?
		get() = editableText.restrictPattern
		set(value) {
			editableText.restrictPattern = value
		}

	override var password: Boolean
		get() = editableText.password
		set(value) {
			editableText.password = value
		}

	override var allowTab: Boolean
		get() = editableText.allowTab
		set(value) {
			editableText.allowTab = value
		}

	init {
		styleTags.add(TextInput)
		watch(boxStyle) {
			background.style.set(it)
		}
		watch(textInputStyle) {
			editableText.textCursorColor = it.cursorColor
			invalidateLayout()
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val pad = boxStyle.padding
		val margin = boxStyle.margin
		val w = margin.reduceWidth2(pad.reduceWidth2(explicitWidth ?: textInputStyle.defaultWidth))
		val h = margin.reduceHeight(pad.reduceHeight(explicitHeight))
		editableText.setSize(w, h)
		editableText.setPosition(margin.left + pad.left, margin.top + pad.top)
		out.set(explicitWidth ?: textInputStyle.defaultWidth, explicitHeight ?: margin.expandHeight2(pad.expandHeight2(editableText.height)))
		background.setSize(margin.reduceWidth2(out.width), margin.reduceHeight(out.height))
		background.setPosition(margin.left, margin.top)
		highlight?.setSize(background.bounds)
		highlight?.setPosition(margin.left, margin.top)
	}
}


open class GlTextArea(owner: Owned) : ContainerImpl(owner), TextArea {

	protected val background = addChild(rect())

	override var focusEnabled: Boolean = true
	override var focusOrder: Float = 0f
	override var highlight: UiComponent? by createSlot()

	final override val textInputStyle = bind(TextInputStyle())
	final override val boxStyle = bind(BoxStyle())

	protected val editableText = EditableText(this)

	private val scroller = addChild(scrollArea {
		hScrollPolicy = ScrollPolicy.OFF
		+editableText layout { widthPercent = 1f }
	})

	override val charStyle: CharStyle
		get() = editableText.charStyle

	override val flowStyle: TextFlowStyle
		get() = editableText.flowStyle

	override val input: Signal<() -> Unit>
		get() = editableText.input

	override val changed: Signal<() -> Unit>
		get() = editableText.changed

	override var editable: Boolean
		get() = editableText.editable
		set(value) {
			editableText.editable = value
		}

	override var maxLength: Int?
		get() = editableText.maxLength
		set(value) {
			editableText.maxLength = value
		}

	override var text: String
		get() = editableText.text
		set(value) {
			editableText.text = value
		}

	override var placeholder: String
		get() = editableText.placeholder
		set(value) {
			editableText.placeholder = value
		}

	override var restrictPattern: String?
		get() = editableText.restrictPattern
		set(value) {
			editableText.restrictPattern = value
		}

	override var password: Boolean
		get() = editableText.password
		set(value) {
			editableText.password = value
		}

	override var allowTab: Boolean
		get() = editableText.allowTab
		set(value) {
			editableText.allowTab = value
		}

	override val hScrollModel: ClampedScrollModel
		get() = scroller.hScrollModel

	override val vScrollModel: ClampedScrollModel
		get() = scroller.vScrollModel

	override var hScrollPolicy: ScrollPolicy
		get() = scroller.hScrollPolicy
		set(value) {
			scroller.hScrollPolicy = value
		}

	override var vScrollPolicy: ScrollPolicy
		get() = scroller.vScrollPolicy
		set(value) {
			scroller.vScrollPolicy = value
		}

	override val contentsWidth: Float
		get() = scroller.contentsWidth

	override val contentsHeight: Float
		get() = scroller.contentsHeight

	private val selectionManager = inject(SelectionManager)
	private val rect = Rectangle()

	init {
		styleTags.add(TextInput)
		styleTags.add(TextArea)
		watch(boxStyle) {
			scroller.stackStyle.padding = it.padding
			scroller.style.borderRadius = it.borderRadius
			background.style.set(it)
		}
		watch(textInputStyle) {
			editableText.textCursorColor = it.cursorColor
			invalidateLayout()
		}

		mouseDown().add(this::startScrollWatch)
		touchStart().add(this::startScrollWatch)
		keyDown().add(this::scrollToSelected)
	}

	private fun scrollToSelected(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val line = contents.getLineAt(minOf(contents.size - 1, sel.endIndex)) ?: return
		rect.set(line.x, line.y, line.width, line.height)
		rect.inflate(flowStyle.padding)
		scroller.scrollTo(rect)
	}

	private val contents
		get() = editableText.textField.contents

	private val maxScrollSpeed = 20f
	private val bufferP = 0.2f
	private val innerBufferMax = 80f
	private val outerBufferMax = 200f
	private val tmpVec = Vector2()
	private var startMouseY = 0f
	private var _frameWatch: Disposable? = null

	private fun startScrollWatch(event: Any) {
		mousePosition(tmpVec)
		startMouseY = tmpVec.y
		_frameWatch?.dispose()
		_frameWatch = enterFrame(-1, this::scrollWatcher)
		stage.mouseUp().add(this::endScrollWatch)
		stage.touchEnd().add(this::endScrollWatch)
	}

	private fun endScrollWatch(event: Any) {
		_frameWatch?.dispose()
		_frameWatch = null
		stage.mouseUp().remove(this::endScrollWatch)
		stage.touchEnd().remove(this::endScrollWatch)
	}

	private fun scrollWatcher() {
		mousePosition(tmpVec)
		val height = height
		val b = maxOf(0f, minOf4(innerBufferMax, height * bufferP, startMouseY, height - startMouseY))
		val speed = if (tmpVec.y < b) {
			-(1f - (tmpVec.y + outerBufferMax) / (b + outerBufferMax))
		} else if (tmpVec.y > height - b) {
			(tmpVec.y - height + b) / (b + outerBufferMax)
		} else {
			0f
		}
		vScrollModel.value += speed * maxScrollSpeed

	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val margin = boxStyle.margin
		val w = margin.reduceWidth2(explicitWidth ?: textInputStyle.defaultWidth)
		val h = margin.reduceHeight(explicitHeight)
		scroller.setSize(w, h)
		scroller.setPosition(margin.left, margin.top)
		editableText.pageHeight = h ?: 400f
		out.set(explicitWidth ?: textInputStyle.defaultWidth, explicitHeight ?: margin.expandHeight2(scroller.height))
		background.setSize(margin.reduceWidth2(out.width), margin.reduceHeight(out.height))
		background.setPosition(margin.left, margin.top)
		highlight?.setSize(background.bounds)
		highlight?.setPosition(margin.left, margin.top)
	}

	private val firstSelection: SelectionRange?
		get() = selectionManager.selection.firstOrNull { it.target == this }

}

@Suppress("LeakingThis")
class EditableText(private val host: TextInput) : ContainerImpl(host) {

	private val _input = Signal0()
	val input: Signal<() -> Unit>
		get() = _input

	private val _changed = Signal0()
	val changed: Signal<() -> Unit>
		get() = _changed

	var editable: Boolean = true

	var maxLength: Int? = null

	val textField = addChild(GlTextField(this).apply { selectionTarget = host })

	var pageHeight: Float = 400f

	private val textCursor = addChild(dynamicMeshC {
		buildMesh {
			lineStyle.isVisible = false
			fillStyle.colorTint.set(Color.WHITE)
			+quad(-1f, 0f, 1f, 0f, 1f, 1f, -1f, 1f)
		}
	})

	var textCursorColor: ColorRo
		get() = textCursor.colorTint
		set(value) {
			textCursor.colorTint = value
		}

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

	val charStyle: CharStyle = textField.charStyle
	val flowStyle: TextFlowStyle = textField.flowStyle

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

	init {
		host.focused().add {
			host.selectAll()
			// todo: open mobile keyboard
		}
		host.blurred().add {
			host.unselect()
			if (isActive)
				_changed.dispatch()
		}

		host.char().add {
			if (it.char != '\r') {
				it.handled = true
				replaceSelection(it.char.toString())
				_input.dispatch()
			}
		}

		host.clipboardPaste().add {
			launch {
				var str = it.getItemByType(ClipboardItemType.PLAIN_TEXT)
				if (str != null) {
					str = str.replace("\r", "")
					replaceSelection(str)
				}
			}
		}

		host.keyDown().add(this::keyDownHandler)
		host.touchStart().add { column = -1 }
		host.mouseDown().add { column = -1 }

		validation.addNode(TEXT_CURSOR, ValidationFlags.LAYOUT, this::updateTextCursor)

		selectionManager.selectionChanged.add(this::selectionChangedHandler)
	}

	private val contents
		get() = textField.contents

	private var column = -1

	private fun keyDownHandler(event: KeyInteractionRo) {
		if (event.keyCode != Ascii.UP && event.keyCode != Ascii.DOWN && event.keyCode != Ascii.PAGE_UP && event.keyCode != Ascii.PAGE_DOWN) column = -1

		when (event.keyCode) {
			Ascii.LEFT -> cursorDelta(event, -1)
			Ascii.RIGHT -> cursorDelta(event, 1)
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
				event.handled = true
				if (flowStyle.multiline) {
					replaceSelection("\n")
					_input.dispatch()
				} else {
					_changed.dispatch()
				}
			}
			Ascii.HOME -> cursorHome(event)
			Ascii.END -> cursorEnd(event)
			Ascii.A -> {
				if (event.ctrlKey) {
					val n = contents.size
					selectionManager.selection = listOf(SelectionRange(host, 0, n))
				}
			}
			Ascii.PAGE_UP -> cursorPageUp(event)
			Ascii.PAGE_DOWN -> cursorPageDown(event)
		}
	}

	private fun cursorDelta(event: KeyInteractionRo, offset: Int) {
		val sel = firstSelection ?: return
		val n = contents.size
		val next = clamp(clamp(sel.endIndex, 0, n) + offset, 0, n)
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else next, next))
	}

	private fun cursorUp(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val line = contents.getLineAt(minOf(contents.size - 1, sel.endIndex)) ?: return
		val previousLine = contents.getLineAt(line.startIndex - 1)
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
		val line = contents.getLineAt(sel.endIndex) ?: return
		val nextLine = contents.getLineAt(line.endIndex)
		if (nextLine != null) {
			if (column == -1)
				column = sel.endIndex - line.startIndex
			val nextPos = minOf(lineEnd(nextLine), nextLine.startIndex + column)
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else nextPos, nextPos))
		} else {
			val n = contents.size
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else n, n))
		}
	}

	private fun lineEnd(line: LineInfoRo): Int {
		val n = contents.size
		return if (line.endIndex == n) n else line.endIndex - 1
	}

	private fun cursorHome(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val line = contents.getLineAt(minOf(contents.size - 1, sel.endIndex)) ?: return
		val metaKey = event.ctrlKey || event.metaKey
		val toIndex = if (metaKey) 0 else line.startIndex
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else toIndex, toIndex))
	}

	private fun cursorEnd(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val line = contents.getLineAt(sel.endIndex) ?: return
		val pos = lineEnd(line)
		val metaKey = event.ctrlKey || event.metaKey
		val toIndex = if (metaKey) contents.size else pos
		selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else toIndex, toIndex))
	}

	private fun cursorPageUp(event: KeyInteractionRo) {
		val sel = firstSelection ?: return
		val currentLine = contents.getLineAt(minOf(sel.endIndex, contents.size - 1)) ?: return
		var line: LineInfoRo? = currentLine
		var h = line?.height ?: 0f
		while (line != null && h < pageHeight) {
			line = contents.getLineAt(line.startIndex - 1)
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
		val currentLine = contents.getLineAt(sel.endIndex) ?: return
		var line: LineInfoRo? = currentLine
		var h = line?.height ?: 0f
		while (line != null && h < pageHeight) {
			line = contents.getLineAt(line.endIndex)
			h += line?.height ?: 0f
		}
		if (line != null) {
			if (column == -1)
				column = sel.endIndex - currentLine.startIndex
			val nextPos = minOf(lineEnd(line), line.startIndex + column)
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else nextPos, nextPos))
		} else {
			val n = contents.size
			selectionManager.selection = listOf(SelectionRange(host, if (event.shiftKey) sel.startIndex else n, n))
		}
	}

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
			selectionManager.selection = listOf(SelectionRange(host, sel.min, sel.min))
		} else if (sel.min > 0) {
			replaceTextRange(sel.min - 1, sel.min, "")
			selectionManager.selection = listOf(SelectionRange(host, sel.min - 1, sel.min - 1))
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
		} else if (sel.min < _text.length) {
			replaceTextRange(sel.min, sel.max + 1, "")
			selectionManager.selection = listOf(SelectionRange(host, sel.min, sel.min))
		}
	}

	private fun replaceSelection(str: String) {
		val str2 = if (_restrictPatternRegex == null) str else str.replace(_restrictPatternRegex!!, "")
		val sel = firstSelection ?: return
		replaceTextRange(sel.min, sel.max, str2)
		val p = sel.min + str2.length
		selectionManager.selection = listOf(SelectionRange(host, p, p))
	}

	private fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String) {
		val text = this.text
		this.text = text.substring(0, clamp(startIndex, 0, text.length)) + newText + text.substring(clamp(endIndex, 0, text.length), text.length)
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

		val textCursorVisible: Boolean
		val sel = firstSelection
		if (host.isFocused && sel != null) {
			val rangeEnd = contents.size
			val end = clamp(sel.endIndex, 0, rangeEnd)
			val textElement = if (end >= rangeEnd) contents.placeholder else contents.getTextElementAt(end)
			textCursor.x = textElement.textFieldX
			textCursor.y = textElement.textFieldY
			textCursor.scaleY = textElement.lineHeight / textCursor.height
			textCursorVisible = true
		} else {
			textCursorVisible = false
		}
		textCursor.visible = textCursorVisible
	}

	override fun dispose() {
		super.dispose()
		selectionManager.selectionChanged.remove(this::selectionChangedHandler)
	}

	companion object {
		private const val TEXT_CURSOR = 1 shl 16
	}
}
