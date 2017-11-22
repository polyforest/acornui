package com.acornui.gl.component.text

import com.acornui.component.*
import com.acornui.component.layout.setSize
import com.acornui.component.scroll.ClampedScrollModel
import com.acornui.component.scroll.ScrollPolicy
import com.acornui.component.scroll.scrollArea
import com.acornui.component.scroll.scrollTo
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
import com.acornui.graphics.ColorRo
import com.acornui.math.Bounds
import com.acornui.math.MathUtils.clamp
import com.acornui.math.Rectangle
import com.acornui.signal.Signal
import com.acornui.signal.Signal0

open class GlTextInput(owner: Owned) : ContainerImpl(owner), TextInput {

	protected val background = addChild(rect())

	override var focusEnabled: Boolean = true
	override var focusOrder: Float = 0f
	override var highlight: UiComponent? by createSlot()

	override final val textInputStyle = bind(TextInputStyle())
	override final val boxStyle = bind(BoxStyle())
	protected val tF = addChild(EditableText(this))

	override val charStyle: CharStyle
		get() = tF.charStyle

	override val flowStyle: TextFlowStyle
		get() = tF.flowStyle

	override val input: Signal<() -> Unit>
		get() = tF.input

	override val changed: Signal<() -> Unit>
		get() = tF.changed

	override var editable: Boolean
		get() = tF.editable
		set(value) {
			tF.editable = value
		}

	override var maxLength: Int?
		get() = tF.maxLength
		set(value) {
			tF.maxLength = value
		}

	override var text: String
		get() = tF.text
		set(value) {
			tF.text = value
		}

	override var placeholder: String
		get() = tF.placeholder
		set(value) {
			tF.placeholder = value
		}

	override var restrictPattern: String?
		get() = tF.restrictPattern
		set(value) {
			tF.restrictPattern = value
		}

	override var password: Boolean
		get() = tF.password
		set(value) {
			tF.password = value
		}

	init {
		styleTags.add(TextInput)
		watch(boxStyle) {
			background.style.set(it)
		}
		watch(textInputStyle) {
			tF.textCursorColor = it.cursorColor
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
	}
}


open class GlTextArea(owner: Owned) : ContainerImpl(owner), TextArea {

	protected val background = addChild(rect())

	override var focusEnabled: Boolean = true
	override var focusOrder: Float = 0f
	override var highlight: UiComponent? by createSlot()

	override final val textInputStyle = bind(TextInputStyle())
	override final val boxStyle = bind(BoxStyle())

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
		selectionManager.selectionChanged.add(this::selectionChangedHandler)
	}

	private val b = Rectangle()

	private fun selectionChangedHandler(oldSelection: List<SelectionRange>, newSelection: List<SelectionRange>) {
		// Scroll to the selection position.
		// TODO: Determine direction.
		val selection = newSelection.firstOrNull { it.target == this } ?: return
		val contents = editableText.textField.contents
		val rangeEnd = contents.size
		val start = selection.startIndex
		val textElement = if (start >= rangeEnd) contents.placeholder else contents.getTextElementAt(start)

		b.set(textElement.textFieldX, textElement.textFieldY, textElement.width, textElement.lineHeight)
		b.inflate(flowStyle.padding)
		scroller.scrollTo(b)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val margin = boxStyle.margin
		val w = margin.reduceWidth2(explicitWidth ?: textInputStyle.defaultWidth)
		val h = margin.reduceHeight(explicitHeight)
		scroller.setSize(w, h)
		scroller.setPosition(margin.left, margin.top)
		out.set(explicitWidth ?: textInputStyle.defaultWidth, explicitHeight ?: margin.expandHeight2(scroller.height))
		background.setSize(margin.reduceWidth2(out.width), margin.reduceHeight(out.height))
		background.setPosition(margin.left, margin.top)
		highlight?.setSize(background.bounds)
		highlight?.setPosition(margin.left, margin.top)
	}

	override fun dispose() {
		super.dispose()
		selectionManager.selectionChanged.remove(this::selectionChangedHandler)
	}
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

	private val textCursor = addChild(dynamicMeshC {
		buildMesh {
			lineStyle.isVisible = false
			fillStyle.colorTint.set(Color.WHITE)
			+quad(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
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

		host.keyDown().add(this::keyDownHandler)

		selectionManager.selectionChanged.add(this::selectionChangedHandler)
	}

	private fun keyDownHandler(event: KeyInteraction) {
		if (event.keyCode == Ascii.LEFT) {
			val sel = firstSelection
			if (sel != null) {
				val next = maxOf(0, sel.startIndex - 1)
				selectionManager.selection = listOf(SelectionRange(host, next, next))
			}
		} else if (event.keyCode == Ascii.RIGHT) {
			val sel = firstSelection
			if (sel != null) {
				val next = minOf(textField.contents.size, sel.endIndex + 1)
				selectionManager.selection = listOf(SelectionRange(host, next, next))
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
		get() = selectionManager.selection.firstOrNull { it.target == host }

	private fun backspace() {
		val sel = firstSelection ?: return
		if (sel.startIndex != sel.endIndex) {
			replaceTextRange(sel.startIndex, sel.endIndex, "")
			selectionManager.selection = listOf(SelectionRange(host, sel.startIndex, sel.startIndex))
		} else if (sel.startIndex > 0) {
			replaceTextRange(sel.startIndex - 1, sel.startIndex, "")
			selectionManager.selection = listOf(SelectionRange(host, sel.startIndex - 1, sel.startIndex - 1))
		}
	}

	private fun delete() {
		val sel = firstSelection ?: return
		if (sel.startIndex != sel.endIndex) {
			replaceTextRange(sel.startIndex, sel.endIndex, "")
			selectionManager.selection = listOf(SelectionRange(host, sel.startIndex, sel.startIndex))
		} else if (sel.startIndex < _text.length) {
			replaceTextRange(sel.startIndex, sel.startIndex + 1, "")
			selectionManager.selection = listOf(SelectionRange(host, sel.startIndex, sel.startIndex))
		}
	}

	private fun replaceSelection(str: String) {
		val str2 = if (_restrictPatternRegex == null) str else str.replace(_restrictPatternRegex!!, "")
		val sel = firstSelection ?: return
		replaceTextRange(sel.startIndex, sel.endIndex, str2)
		selectionManager.selection = listOf(SelectionRange(host, sel.startIndex + str2.length, sel.startIndex + str2.length))
	}

	fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String) {
		val text = this.text
		this.text = text.substring(0, clamp(startIndex, 0, text.length)) + newText + text.substring(clamp(endIndex, 0, text.length), text.length)
		validateLayout()
	}

	private fun String.toPassword(): String {
		return passwordMask.repeat2(length)
	}

	private fun selectionChangedHandler(oldSelection: List<SelectionRange>, newSelection: List<SelectionRange>) {
		if (oldSelection.filter { it.target == host } != newSelection.filter { it.target == host }) {
			invalidateLayout()
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		textField.setSize(explicitWidth, explicitHeight)
		out.set(textField.bounds)
		updateTextCursor()
	}

	private fun updateTextCursor() {
		textField.validate(ValidationFlags.LAYOUT)

		val textCursorVisible: Boolean
		val sel = firstSelection
		if (host.isFocused && sel != null) {
			val rangeEnd = textField.contents.size

			val start = clamp(sel.startIndex, 0, rangeEnd)
			val end = clamp(sel.endIndex, 0, rangeEnd)
			if (start == end) {
				val textElement = if (start >= rangeEnd) textField.contents.placeholder else textField.contents.getTextElementAt(start)

				textCursor.x = textElement.textFieldX
				textCursor.y = textElement.textFieldY

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

	override fun dispose() {
		super.dispose()
		selectionManager.selectionChanged.remove(this::selectionChangedHandler)
	}
}
