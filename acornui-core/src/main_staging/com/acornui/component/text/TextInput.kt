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

@file:Suppress("UNUSED_PARAMETER", "unused")

package com.acornui.component.text

import com.acornui.Disposable
import com.acornui.component.*
import com.acornui.component.scroll.ClampedScrollModel
import com.acornui.component.scroll.ScrollPolicy
import com.acornui.component.scroll.scrollArea
import com.acornui.component.style.Stylable
import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import com.acornui.focus.Focusable
import com.acornui.frameTimeS
import com.acornui.function.as2
import com.acornui.input.*
import com.acornui.input.interaction.KeyEventRo
import com.acornui.math.Bounds
import com.acornui.math.Rectangle
import com.acornui.math.minOf4
import com.acornui.math.vec2
import com.acornui.recycle.Clearable
import com.acornui.repeat2
import com.acornui.selection.SelectableComponent
import com.acornui.selection.SelectionManager
import com.acornui.selection.SelectionRange
import com.acornui.signal.Signal
import com.acornui.time.tick
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface TextInput : InputComponent<String>, Focusable, SelectableComponent, Stylable, Clearable {

	val charStyle: CharStyle
	val flowStyle: TextFlowStyle
	val textInputStyle: TextInputStyle

	/**
	 * Dispatched on each input character.
	 * Note - this does not invoke when the text is programmatically changed.
	 */
	val input: Signal<TextInput>

	/**
	 * Dispatched on value commit.
	 * This is only dispatched on a user interaction, such as pressing ENTER or TAB. It is not dispatched when
	 * the text is programmatically changed.
	 */
	override val changed: Signal<TextInput>

	var editable: Boolean
	var maxLength: Int?

	var text: String

	var placeholder: String

	/**
	 * A regular expression pattern to define what is NOT allowed in this text input.
	 * E.g. Regex("[a-z]") will prevent lowercase letters from being entered.
	 * Setting this will mutate the current [text] property.
	 */
	var restrictPattern: Regex?

	var password: Boolean

	/**
	 * If true, pressing TAB inserts a tab character as opposed to the default behavior (typically a focus change).
	 */
	var allowTab: Boolean

	/**
	 * The hinting to use for the virtual keyboard.
	 * @see SoftKeyboardType
	 */
	var softKeyboardType: String


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

class TextInputImpl(owner: Context) : ContainerImpl(owner), TextInput {

	private var background: UiComponent? = null

	override val textInputStyle = bind(TextInputStyle())
	private val editableText = addChild(EditableText(this))

	override val charStyle: CharStyle
		get() = editableText.charStyle

	override val flowStyle: TextFlowStyle
		get() = editableText.flowStyle

	override val input: Signal<TextInput>
		get() = editableText.input

	override val changed: Signal<TextInput>
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

	override var value: String
		get() = editableText.text
		set(value) {
			editableText.text = value
		}

	override var placeholder: String
		get() = editableText.placeholder
		set(value) {
			editableText.placeholder = value
		}

	override var restrictPattern: Regex?
		get() = editableText.restrictPattern
		set(value) {
			editableText.restrictPattern = value
		}

	override var password: Boolean
		get() = editableText.isPassword
		set(value) {
			editableText.isPassword = value
		}

	override var allowTab: Boolean
		get() = editableText.allowTab
		set(value) {
			editableText.allowTab = value
		}

	init {
		focusEnabled = true
		addClass(TextInput)
		watch(textInputStyle) {
			background?.dispose()
			background = addOptionalChild(0, it.background(this))

			editableText.cursorColorOne = it.cursorColorOne
			editableText.cursorColorTwo = it.cursorColorTwo
			editableText.cursorBlinkSpeed = it.cursorBlinkSpeed
		}
	}

	private var defaultWidthFromText: String? = null

	/**
	 * Sets this text input's default width to fit the character 'M' repeated [textLength] times.
	 * If this text input has been given either an explicit width, or a [defaultWidth], this will have no effect.
	 */
	fun setSizeToFit(textLength: Int) = setSizeToFit("M".repeat2(textLength))

	/**
	 * Sets this text input's default width to fit the given text line.
	 * If this text input has been given either an explicit width, or a [defaultWidth], this will have no effect.
	 */
	fun setSizeToFit(text: String?) {
		defaultWidthFromText = text
		invalidateLayout()
	}

	override var softKeyboardType = SoftKeyboardType.DEFAULT

	override fun updateStyles() {
		super.updateStyles()
		// This class's styles are delegated from the editableText styles.
		editableText.validate(ValidationFlags.STYLES)
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val pad = textInputStyle.padding
		val margin = textInputStyle.margin
		val h = margin.reduceHeight(pad.reduceHeight(explicitHeight))

		val w = if (explicitWidth == null && defaultWidthFromText != null) {
			editableText.validate(ValidationFlags.STYLES)
			val font = editableText.textField.font
			font?.data?.measureLineWidth(defaultWidthFromText!!)?.toDouble() ?: 0.0
		} else {
			margin.reduceWidth(pad.reduceWidth(explicitWidth ?: textInputStyle.defaultWidth))
		}

		editableText.size(w, h)
		editableText.position(margin.left + pad.left, margin.top + pad.top)
		out.set(
				explicitWidth ?: margin.expandHeight(pad.expandHeight(w)),
				explicitHeight ?: margin.expandHeight(pad.expandHeight(editableText.height)),
				baseline = editableText.baseline + editableText.y
		)
		background?.size(margin.reduceWidth(out.width), margin.reduceHeight(out.height))
		background?.position(margin.left, margin.top)
	}

	override fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String) {
		editableText.replaceTextRange(startIndex, endIndex, newText)
	}

	override fun clear() {
		text = ""
	}
}


interface TextArea : TextInput {

	val hScrollModel: ClampedScrollModel
	val vScrollModel: ClampedScrollModel

	/**
	 * The horizontal scrolling policy.
	 * Default: ScrollPolicy.OFF
	 */
	var hScrollPolicy: ScrollPolicy

	/**
	 * The vertical scrolling policy.
	 * Default: ScrollPolicy.AUTO
	 */
	var vScrollPolicy: ScrollPolicy

	/**
	 * The unclipped width of the contents.
	 * This will perform a layout validation.
	 */
	val contentsWidth: Double

	/**
	 * The unclipped height of the contents.
	 * This will perform a layout validation.
	 */
	val contentsHeight: Double

	companion object : StyleTag
}

class TextAreaImpl(owner: Context) : ContainerImpl(owner), TextArea {

	private var background: UiComponent? = null

	override val textInputStyle = bind(TextInputStyle())

	private val editableText = EditableText(this).apply {
		textField.flowStyle.allowClipping = false // TODO: Clipping within a text area
	}

	private val scroller = addChild(scrollArea {
		hScrollPolicy = ScrollPolicy.OFF
		+editableText layout { widthPercent = 1.0 }
	})

	override val charStyle: CharStyle
		get() = editableText.charStyle

	override val flowStyle: TextFlowStyle
		get() = editableText.flowStyle

	override val input: Signal<TextInput>
		get() = editableText.input

	override val changed: Signal<TextInput>
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

	override var value: String
		get() = editableText.text
		set(value) {
			editableText.text = value
		}

	override var placeholder: String
		get() = editableText.placeholder
		set(value) {
			editableText.placeholder = value
		}

	override var restrictPattern: Regex?
		get() = editableText.restrictPattern
		set(value) {
			editableText.restrictPattern = value
		}

	override var password: Boolean
		get() = editableText.isPassword
		set(value) {
			editableText.isPassword = value
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

	override val contentsWidth: Double
		get() = scroller.contentsWidth

	override val contentsHeight: Double
		get() = scroller.contentsHeight

	/**
	 * If the explicit height is not set, the height will be set to what it would be if there were [rows] number of
	 * lines. Only the root [TextField.element] node's font will be considered.
	 */
	var rows by validationProp<Double?>(3.0, ValidationFlags.LAYOUT)

	private val selectionManager = inject(SelectionManager)
	private val rect = Rectangle()

	init {
		focusEnabled = true
		addClass(TextInput)
		addClass(TextArea)
		watch(textInputStyle) {
			background?.dispose()
			background = addOptionalChild(0, it.background(this))

			scroller.stackStyle.padding = it.padding
			scroller.style.borderRadii = it.borderRadii
			editableText.cursorColorOne = it.cursorColorOne
			editableText.cursorColorTwo = it.cursorColorTwo
			editableText.cursorBlinkSpeed = it.cursorBlinkSpeed
			invalidateLayout()
		}

		mouseDown().add(::startScrollWatch)
		touchStart().add(::startScrollWatch)
		keyDown().add(::scrollToSelected)
	}

	override fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String) {
		editableText.replaceTextRange(startIndex, endIndex, newText)
	}

	override var softKeyboardType = SoftKeyboardType.DEFAULT

	private fun scrollToSelected(event: KeyEventRo) {
		val contents = contents ?: return
		val sel = firstSelection ?: return
		val e = (if (sel.endIndex >= contents.textElements.size) contents.placeholder else contents.textElements[sel.endIndex]) ?: return
		rect.set(e.x, e.y, e.width, e.lineHeight)
		rect.inflate(flowStyle.padding)
		scroller.scrollTo(rect)
	}

	private val contents
		get() = editableText.textField.element

	private val maxScrollSpeed = 1000.0 * frameTimeS
	private val bufferP = 0.2
	private val innerBufferMax = 80.0
	private val outerBufferMax = 200.0
	private var startMouse = vec2()
	private val currentMouse = vec2()
	private var _frameWatch: Disposable? = null

	private fun startScrollWatch(event: Any) {
		mousePosition(startMouse)
		_frameWatch?.dispose()
		_frameWatch = tick(-1, callback = ::scrollWatcher.as2)
		stage.mouseUp().add(::endScrollWatch)
		stage.touchEnd().add(::endScrollWatch)
	}

	private fun endScrollWatch(event: Any) {
		_frameWatch?.dispose()
		_frameWatch = null
		stage.mouseUp().remove(::endScrollWatch)
		stage.touchEnd().remove(::endScrollWatch)
	}

	private fun scrollWatcher() {
		mousePosition(currentMouse)
		if (hScrollPolicy != ScrollPolicy.OFF) {
			val width = width
			val b = maxOf(0.0, minOf4(innerBufferMax, width * bufferP, startMouse.x, width - startMouse.x))
			val speed = when {
				currentMouse.x < b -> -(1.0 - (currentMouse.x + outerBufferMax) / (b + outerBufferMax))
				currentMouse.x > width - b -> (currentMouse.x - width + b) / (b + outerBufferMax)
				else -> 0.0
			}
			hScrollModel.value += speed * maxScrollSpeed
		}
		if (vScrollPolicy != ScrollPolicy.OFF) {
			val height = height
			val b = maxOf(0.0, minOf4(innerBufferMax, height * bufferP, startMouse.y, height - startMouse.y))
			val speed = when {
				currentMouse.y < b -> -(1.0 - (currentMouse.y + outerBufferMax) / (b + outerBufferMax))
				currentMouse.y > height - b -> (currentMouse.y - height + b) / (b + outerBufferMax)
				else -> 0.0
			}
			vScrollModel.value += speed * maxScrollSpeed
		}
	}

	override fun updateStyles() {
		super.updateStyles()
		// This class's styles are delegated from the editableText styles.
		editableText.validate(ValidationFlags.STYLES)
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val margin = textInputStyle.margin
		val w = margin.reduceWidth(explicitWidth ?: textInputStyle.defaultWidth)
		val rows = rows
		val h = if (explicitHeight == null && rows != null) {
			val font = editableText.textField.font
			val fontData = font?.data
			val lineHeight: Double = (fontData?.lineHeight?.toDouble() ?: 0.0) / charStyle.scaleY
			textInputStyle.padding.expandHeight(lineHeight * rows)
		} else {
			margin.reduceHeight(explicitHeight)
		}
		scroller.size(w, h)
		scroller.position(margin.left, margin.top)
		editableText.pageHeight = h ?: 400.0
		out.set(
				explicitWidth ?: textInputStyle.defaultWidth,
				explicitHeight ?: margin.expandHeight(scroller.height),
				editableText.baselineY
		)
		background?.size(margin.reduceWidth(out.width), margin.reduceHeight(out.height))
		background?.position(margin.left, margin.top)
	}

	override fun clear() {
		text = ""
	}

	private val firstSelection: SelectionRange?
		get() = selectionManager.selection.firstOrNull { it.target == this }

}

var TextInput.selectable: Boolean
	get(): Boolean = charStyle.selectable
	set(value) {
		charStyle.selectable = value
	}


var TextArea.selectable: Boolean
	get(): Boolean = charStyle.selectable
	set(value) {
		charStyle.selectable = value
	}


inline fun Context.textInput(init: ComponentInit<TextInputImpl> = {}): TextInputImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextInputImpl(this)
	t.init()
	return t
}

inline fun Context.textArea(init: ComponentInit<TextAreaImpl> = {}): TextAreaImpl  {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val t = TextAreaImpl(this)
	t.init()
	return t
}

/**
 * Common text restrict patterns.
 * These shouldn't be used as validation patterns; they are meant to restrict the types of characters that can be
 * typed into an input text.
 */
object RestrictPatterns {

	val INTEGER = Regex("[^0-9+-]")
	val FLOAT = Regex("[^0-9+-.]")
	val COLOR = Regex("[^0-9a-fA-F#x]")
}
