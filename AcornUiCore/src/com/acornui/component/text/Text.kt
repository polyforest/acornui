/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui.component.BoxStyle
import com.acornui.component.ComponentInit
import com.acornui.component.Labelable
import com.acornui.component.UiComponent
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.layout.algorithm.FlowVAlign
import com.acornui.component.scroll.ClampedScrollModel
import com.acornui.component.scroll.ScrollPolicy
import com.acornui.component.style.*
import com.acornui.core.di.Owned
import com.acornui.core.di.dKey
import com.acornui.core.focus.Focusable
import com.acornui.core.selection.SelectableComponent
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.graphics.color
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.serialization.*
import com.acornui.signal.Signal

interface TextField : UiComponent, Labelable, SelectableComponent, Styleable {

	val charStyle: CharStyle
	val flowStyle: TextFlowStyle

	/**
	 *
	 */
	var text: String

//	/**
//	 *
//	 */
//	var contents: TextNodeRo

	@Deprecated("Will create text component builders.")
	var htmlText: String?
		get() = ""
		set(value) {
		}

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
	fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String) {
		val text = text
		this.text = text.substring(0, maxOf(0, startIndex)) + newText + text.substring(minOf(text.length, endIndex), text.length)
	}

	override var label: String
		get() = text
		set(value) {
			text = value
		}

	companion object : StyleTag {
		val FACTORY_KEY = dKey<(owner: Owned) -> TextField>()
	}
}

fun Owned.editableText(init: ComponentInit<EditableTextField> = {}): EditableTextField {
	val t = injector.inject(EditableTextField.FACTORY_KEY)(this)
	t.init()
	return t
}

var TextField.selectable: Boolean
	get(): Boolean = charStyle.selectable
	set(value) {
		charStyle.selectable = value
	}

/**
 * Creates a [TextField] implementation with the provided text content.
 */
fun Owned.text(text: String, init: ComponentInit<TextField> = {}): TextField {
	val t = injector.inject(TextField.FACTORY_KEY)(this)
	t.text = text
	t.init()
	return t
}

/**
 * Creates a [TextField] implementation.
 */
fun Owned.text(init: ComponentInit<TextField> = {}): TextField {
	val t = injector.inject(TextField.FACTORY_KEY)(this)
	t.init()
	return t
}

/**
 * The glyph characteristics.
 */
class CharStyle : StyleBase() {

	override val type: StyleType<CharStyle> = CharStyle

	/**
	 * The name of the font face. (case sensitive)
	 */
	var face by prop("_sans")

	/**
	 * The size of the font.
	 */
	var size by prop(14)

	var bold by prop(false)

	var italic by prop(false)

	var underlined by prop(false)

	var colorTint: ColorRo by prop(Color(1f, 1f, 1f, 1f))
	var backgroundColor: ColorRo by prop(Color())

	var selectedColorTint: ColorRo by prop(Color(1f, 1f, 1f, 1f))
	var selectedBackgroundColor: ColorRo by prop(Color(0.12f, 0.25f, 0.5f, 1f))

	/**
	 * The text is selectable by the user. This does not affect whether or not the text can be selected
	 * programmatically, nor does setting this to false de-select the text if it's already selected.
	 */
	var selectable by prop(true)

	companion object : StyleType<CharStyle>
}

object CharStyleSerializer : To<CharStyle>, From<CharStyle> {

	override fun CharStyle.write(writer: Writer) {
		writer.styleProperty(this, "face")?.string(face)
		writer.styleProperty(this, "size")?.int(size)
		writer.styleProperty(this, "bold")?.bool(bold)
		writer.styleProperty(this, "italic")?.bool(italic)
		writer.styleProperty(this, "underlined")?.bool(underlined)
		writer.styleProperty(this, "colorTint")?.color(colorTint)
		writer.styleProperty(this, "backgroundColor")?.color(backgroundColor)
		writer.styleProperty(this, "selectedColorTint")?.color(selectedColorTint)
		writer.styleProperty(this, "selectedBackgroundColor")?.color(selectedBackgroundColor)
		writer.styleProperty(this, "selectable")?.bool(selectable)
	}

	override fun read(reader: Reader): CharStyle {
		val c = CharStyle()
		reader.contains("face") { c.face = it.string()!! }
		reader.contains("size") { c.size = it.int()!! }
		reader.contains("bold") { c.bold = it.bool()!! }
		reader.contains("italic") { c.italic = it.bool()!! }
		reader.contains("underlined") { c.underlined = it.bool()!! }
		reader.contains("colorTint") { c.colorTint = it.color()!! }
		reader.contains("backgroundColor") { c.backgroundColor = it.color()!! }
		reader.contains("selectedColorTint") { c.selectedColorTint = it.color()!! }
		reader.contains("selectedBackgroundColor") { c.selectedBackgroundColor = it.color()!! }
		reader.contains("selectable") { c.selectable = it.bool()!! }
		return c
	}
}

fun charStyle(init: CharStyle.() -> Unit = {}): CharStyle {
	val c = CharStyle()
	c.init()
	return c
}

class TextFlowStyle : StyleBase() {

	override val type = Companion

	/**
	 * The vertical gap between lines.
	 */
	var verticalGap by prop(0f)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding: PadRo by prop(Pad())

	/**
	 * The number of space char widths a tab should occupy.
	 */
	var tabSize: Int by prop(4)

	var horizontalAlign by prop(FlowHAlign.LEFT)
	var verticalAlign by prop(FlowVAlign.BASELINE)
	var multiline by prop(true)

	companion object : StyleType<TextFlowStyle>
}

interface TextInput : Focusable, SelectableComponent, Styleable {

	val charStyle: CharStyle
	val flowStyle: TextFlowStyle
	val boxStyle: BoxStyle
	val textInputStyle: TextInputStyle

	/**
	 * Dispatched on each input character.
	 */
	val input: Signal<() -> Unit>

	/**
	 * Dispatched on value commit.
	 */
	val changed: Signal<() -> Unit>

	var editable: Boolean
	var maxLength: Int?

	var text: String

	var placeholder: String

	/**
	 * A regular expression pattern to define what is NOT allowed in this text input.
	 * E.g. "[a-z]" will prevent lowercase letters from being entered.
	 * Setting this will mutate the current [text] property.
	 *
	 * Note: In the future, this will be changed to restrict: Regex, currently KT-17851 prevents this.
	 * Note: The global flag will be used.
	 */
	var restrictPattern: String?

	var password: Boolean

	/**
	 * If true, pressing TAB inserts a tab character as opposed to the default behavior (typically a focus change).
	 */
	var allowTab: Boolean

	// TODO: Make this efficient in GlTextInput.
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
	fun replaceTextRange(startIndex: Int, endIndex: Int, newText: String) {
		val text = text
		this.text = text.substring(0, maxOf(0, startIndex)) + newText + text.substring(minOf(text.length, endIndex), text.length)
	}

	companion object : StyleTag {
		val FACTORY_KEY = dKey<(owner: Owned) -> TextInput>()
	}
}

var TextInput.selectable: Boolean
	get(): Boolean = charStyle.selectable
	set(value) {
		charStyle.selectable = value
	}

interface TextArea : TextInput {

	val hScrollModel: ClampedScrollModel
	val vScrollModel: ClampedScrollModel

	var hScrollPolicy: ScrollPolicy
	var vScrollPolicy: ScrollPolicy

	val contentsWidth: Float
	val contentsHeight: Float

	companion object : StyleTag {
		val FACTORY_KEY = dKey<(owner: Owned) -> TextArea>()
	}
}

var TextArea.selectable: Boolean
	get(): Boolean = charStyle.selectable
	set(value) {
		charStyle.selectable = value
	}


fun Owned.textInput(init: ComponentInit<TextInput> = {}): TextInput {
	val t = injector.inject(TextInput.FACTORY_KEY)(this)
	t.init()
	return t
}

fun Owned.textArea(init: ComponentInit<TextArea> = {}): TextArea {
	val t = injector.inject(TextArea.FACTORY_KEY)(this)
	t.init()
	return t
}

class TextInputStyle : StyleBase() {
	override val type = Companion

	var defaultWidth by prop(180f)
//	var cursorColor: ColorRo by prop(Color(0.1f, 0.1f, 0.1f, 1f))
	var cursorColor: ColorRo by prop(Color(1f, 0.1f, 0.1f, 1f))

	companion object : StyleType<TextInputStyle>
}

/**
 * Common text restrict patterns.
 */
object RestrictPatterns {

	val INTEGER = "[^0-9+-]"
	val FLOAT = "[^0-9+-.]"
}