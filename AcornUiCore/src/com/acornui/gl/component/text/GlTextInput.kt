package com.acornui.gl.component.text

import com.acornui.component.*
import com.acornui.component.layout.setSize
import com.acornui.component.style.set
import com.acornui.component.text.*
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.focus.blurred
import com.acornui.core.focus.focused
import com.acornui.core.input.Ascii
import com.acornui.core.input.char
import com.acornui.core.input.interaction.KeyInteraction
import com.acornui.core.input.keyDown
import com.acornui.core.repeat2
import com.acornui.core.selection.SelectionManager
import com.acornui.core.selection.SelectionRange
import com.acornui.core.selection.selectAll
import com.acornui.core.selection.unselect
import com.acornui.gl.component.drawing.dynamicMeshC
import com.acornui.gl.component.drawing.fillStyle
import com.acornui.gl.component.drawing.lineStyle
import com.acornui.gl.component.drawing.quad
import com.acornui.graphics.Color
import com.acornui.math.Bounds
import com.acornui.math.MathUtils.clamp
import com.acornui.signal.Signal
import com.acornui.signal.Signal0

@Suppress("LeakingThis")
open class GlTextInput(owner: Owned) : ContainerImpl(owner), TextInput {

	private val _input = Signal0()
	override val input: Signal<() -> Unit>
		get() = _input
	private val _changed = Signal0()
	override val changed: Signal<() -> Unit>
		get() = _changed

	override var editable: Boolean = true

	override var focusEnabled: Boolean = true
	override var focusOrder: Float = 0f
	override var highlight: UiComponent? by createSlot()

	override var maxLength: Int? = null

	protected val background = addChild(rect())
	protected val tF = addChild(GlTextField(this).apply { selectionTarget = this@GlTextInput })
	protected val textCursor = addChild(dynamicMeshC {
		buildMesh {
			lineStyle.isVisible = false
			fillStyle.colorTint.set(Color.WHITE)
			+quad(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
		}
	})

	private var _text: String = ""
	override var text: String
		get() = _text
		set(value) {
			if (_text == value) return
			_text = if (_restrictPatternRegex == null) value else value.replace(_restrictPatternRegex!!, "")
			refreshText()
		}

	override var placeholder: String = ""

	private var _restrictPattern: String? = null
	private var _restrictPatternRegex: Regex? = null

	override var restrictPattern: String?
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
		tF.text = if (_password) _text.toPassword() else _text
	}

	override final val charStyle: CharStyle = tF.charStyle
	override final val boxStyle: BoxStyle = bind(BoxStyle())
	override final val flowStyle: TextFlowStyle = tF.flowStyle
	override final val textInputStyle = bind(TextInputStyle())

	/**
	 * The mask to use as replacement characters when [password] is set to true.
	 */
	var passwordMask = "*"

	private var _password = false
	override var password: Boolean
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
		styleTags.add(TextInput)
		watch(boxStyle) {
			background.style.set(it)
		}
		watch(textInputStyle) {
			textCursor.colorTint = it.cursorColor
			invalidateLayout()
		}
		focused().add {
			selectAll()
			// todo: open mobile keyboard
		}
		blurred().add {
			unselect()
			if (isActive)
				_changed.dispatch()
		}
		char().add {
			if (it.char != '\r') {
				it.handled = true
				replaceSelection(it.char.toString())
				_input.dispatch()
			}
		}

		keyDown().add(this::keyDownHandler)

		selectionManager.selectionChanged.add(this::selectionChangedHandler)
	}

	private fun keyDownHandler(event: KeyInteraction) {
		if (event.keyCode == Ascii.LEFT) {
			val sel = firstSelection
			if (sel != null) {
				val next = maxOf(0, sel.startIndex - 1)
				selectionManager.selection = listOf(SelectionRange(this, next, next))
			}
		} else if (event.keyCode == Ascii.RIGHT) {
			val sel = firstSelection
			if (sel != null) {
				val next = minOf(tF.contents.size, sel.endIndex + 1)
				selectionManager.selection = listOf(SelectionRange(this, next, next))
			}
		} else if (event.keyCode == Ascii.UP) {
			val sel = firstSelection
			if (sel != null) {
//						val leafRangeStart
//						val index = minOf(tF.textElementsCount, sel.endIndex)
//
//
//						//tF.leaves.indexOfLast2 { it }
//						val next = minOf(tF.textElementsCount, sel.endIndex + 1)
//						selectionManager.selection = listOf(SelectionRange(this, next, next))
			}
		} else if (event.keyCode == Ascii.BACKSPACE) {
			event.handled = true
			backspace()
			_input.dispatch()
		} else if (event.keyCode == Ascii.TAB) {
			if (allowTab) {
				event.preventDefault() // Prevent focus manager from tabbing.
				event.handled = true
				if (flowStyle.multiline) {
					replaceSelection("\t")
					_input.dispatch()
				}
			}
		} else if (event.keyCode == Ascii.DELETE) {
			event.handled = true
			delete()
			_input.dispatch()
		} else if (event.keyCode == Ascii.ENTER || event.keyCode == Ascii.RETURN) {
			event.handled = true
			if (flowStyle.multiline) {
				replaceSelection("\n")
				_input.dispatch()
			} else {
				_changed.dispatch()
			}
		}
	}

	private val firstSelection: SelectionRange?
		get() = selectionManager.selection.firstOrNull { it.target == this }

	private fun backspace() {
		val sel = firstSelection ?: return
		if (sel.startIndex != sel.endIndex) {
			replaceTextRange(sel.startIndex, sel.endIndex, "")
			selectionManager.selection = listOf(SelectionRange(this, sel.startIndex, sel.startIndex))
		} else if (sel.startIndex > 0) {
			replaceTextRange(sel.startIndex - 1, sel.startIndex, "")
			selectionManager.selection = listOf(SelectionRange(this, sel.startIndex - 1, sel.startIndex - 1))
		}
	}

	private fun delete() {
		val sel = firstSelection ?: return
		if (sel.startIndex != sel.endIndex) {
			replaceTextRange(sel.startIndex, sel.endIndex, "")
			selectionManager.selection = listOf(SelectionRange(this, sel.startIndex, sel.startIndex))
		} else if (sel.startIndex < _text.length) {
			replaceTextRange(sel.startIndex, sel.startIndex + 1, "")
			selectionManager.selection = listOf(SelectionRange(this, sel.startIndex, sel.startIndex))
		}
	}

	private fun replaceSelection(str: String) {
		val str2 = if (_restrictPatternRegex == null) str else str.replace(_restrictPatternRegex!!, "")
		val sel = firstSelection ?: return
		replaceTextRange(sel.startIndex, sel.endIndex, str2)
		selectionManager.selection = listOf(SelectionRange(this, sel.startIndex + str2.length, sel.startIndex + str2.length))
	}

	override fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String) {
		val text = this.text
		this.text = text.substring(0, clamp(startIndex, 0, text.length)) + newText + text.substring(clamp(endIndex, 0, text.length), text.length)
	}

	private fun String.toPassword(): String {
		return passwordMask.repeat2(length)
	}

	private fun selectionChangedHandler(old: List<SelectionRange>, new: List<SelectionRange>) {
		if (old.filter { it.target == this } != new.filter { it.target == this }) {
			invalidateLayout()
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val pad = boxStyle.padding
		val margin = boxStyle.margin
		val w = margin.reduceWidth2(pad.reduceWidth2(explicitWidth ?: textInputStyle.defaultWidth))
		val h = margin.reduceHeight(pad.reduceHeight(explicitHeight))
		tF.setSize(w, h)
		tF.setPosition(margin.left + pad.left, margin.top + pad.top)
		out.set(explicitWidth ?: textInputStyle.defaultWidth, explicitHeight ?: margin.expandHeight2(pad.expandHeight2(tF.height)))
		background.setSize(margin.reduceWidth2(out.width), margin.reduceHeight(out.height))
		background.setPosition(margin.left, margin.top)
		highlight?.setSize(background.bounds)
		highlight?.setPosition(margin.left, margin.top)

		updateTextCursor()
	}

	private fun updateTextCursor() {
		tF.validate(ValidationFlags.LAYOUT)


		val textCursorVisible: Boolean
		val sel = firstSelection
		if (isFocused && sel != null) {
			val rangeEnd = tF.contents.size

			val start = clamp(sel.startIndex, 0, rangeEnd)
			val end = clamp(sel.endIndex, 0, rangeEnd)
			if (start == end) {
				val textElement = if (start >= rangeEnd) tF.contents.placeholder else tF.contents.getTextElementAt(start)

				textCursor.x = textElement.textFieldX + tF.x
				textCursor.y = textElement.textFieldY + tF.y

				textCursor.scaleY = textElement.lineHeight / textCursor.height

				textCursorVisible = true
			} else {
				textCursorVisible = false
			}
		} else {
			textCursorVisible = false
		}
		textCursor.visible = textCursorVisible
	}

	private val TextElementRo.lineHeight: Float
		get() {
			return textParent?.lineHeight ?: 0f
		}

	private val TextElementRo.textFieldX: Float
		get() {
			return x + (textParent?.textFieldX ?: 0f)
		}

	private val TextElementRo.textFieldY: Float
		get() {
			return y + (textParent?.textFieldY ?: 0f)
		}
//
	private val TextSpanElementRo<TextElementRo>.textFieldX: Float
		get() {
			var textFieldX = x
			var p: TextNodeRo? = textParent
			while (p != null && p != tF) {
				textFieldX += p.x
				p = p.textParent
			}
			return textFieldX
		}

	private val TextSpanElementRo<TextElementRo>.textFieldY: Float
		get() {
			var textFieldY = y
			var p: TextNodeRo? = textParent
			while (p != null && p != tF) {
				textFieldY += p.y
				p = p.textParent
			}
			return textFieldY
		}

	override fun dispose() {
		super.dispose()
		selectionManager.selectionChanged.remove(this::selectionChangedHandler)
	}
}

open class GlTextArea(owner: Owned) : GlTextInput(owner), TextArea {

	init {
		allowTab = true
		styleTags.add(TextArea)
	}
}