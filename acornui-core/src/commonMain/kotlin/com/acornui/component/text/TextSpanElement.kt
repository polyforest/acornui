package com.acornui.component.text

import com.acornui.collection.forEach2
import com.acornui.component.ComponentInit
import com.acornui.component.ElementParent
import com.acornui.component.ElementParentRo
import com.acornui.component.ValidationFlags
import com.acornui.component.style.*
import com.acornui.core.Disposable
import com.acornui.graphic.ColorRo

interface TextSpanElementRo<out T : TextElementRo> : ElementParentRo<T> {

	/**
	 * The text node this span element was added to.
	 */
	val textParent: TextNodeRo?

	/**
	 * The height of the text line.
	 */
	val lineHeight: Float

	/**
	 * If the [TextFlowStyle] vertical alignment is BASELINE, this property will be used to vertically align the
	 * elements.
	 */
	val baseline: Float

	/**
	 * The size of a space.
	 */
	val spaceSize: Float

}

val TextSpanElementRo<TextElementRo>.textFieldX: Float
	get() {
		var textFieldX = 0f
		var p: TextNodeRo? = textParent
		while (p != null) {
			textFieldX += p.x
			p = p.parentTextNode
		}
		return textFieldX
	}

val TextSpanElementRo<TextElementRo>.textFieldY: Float
	get() {
		var textFieldY = 0f
		var p: TextNodeRo? = textParent
		while (p != null) {
			textFieldY += p.y
			p = p.parentTextNode
		}
		return textFieldY
	}


interface TextSpanElement : TextSpanElementRo<TextElement>, Disposable {

	override var textParent: TextNodeRo?

	fun validateStyles()
	fun validateCharStyle(concatenatedColorTint: ColorRo)
}

@Suppress("LeakingThis")
open class TextSpanElementImpl : TextSpanElement, ElementParent<TextElement>, Styleable {

	override var textParent: TextNodeRo? = null

	override val styleParent: StyleableRo?
		get() = textParent

	private val _elements = ArrayList<TextElement>()

	override val elements: List<TextElement>
		get() = _elements

	protected val styles = Styles(this)

	val charStyle = styles.bind(CharStyle())

	private val tfCharStyle = CharElementStyle()

	init {
	}

	//-------------------------------------------------------
	// Styleable
	//-------------------------------------------------------

	final override val styleTags: MutableList<StyleTag>
		get() = styles.styleTags

	final override val styleRules: MutableList<StyleRule<*>>
		get() = styles.styleRules

	override fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) = styles.getRulesByType(type, out)

	override fun invalidateStyles() {
		textParent?.invalidate(ValidationFlags.STYLES)
	}

	override fun validateStyles() {
		styles.validateStyles()
		tfCharStyle.font = charStyle.font
		tfCharStyle.underlined = charStyle.underlined
		tfCharStyle.strikeThrough = charStyle.strikeThrough
		tfCharStyle.lineThickness = charStyle.lineThickness
	}

	private val font: BitmapFont?
		get() = tfCharStyle.font

	override val lineHeight: Float
		get() = (font?.data?.lineHeight?.toFloat() ?: 0f)

	override val baseline: Float
		get() = (font?.data?.baseline?.toFloat() ?: 0f)

	override val spaceSize: Float
		get() {
			val font = font ?: return 6f
			return (font.data.glyphs[' ']?.advanceX?.toFloat() ?: 6f)
		}

	operator fun Char?.unaryPlus() {
		if (this == null) return
		addElement(char(this))
	}

	protected var bubblingFlags =
			ValidationFlags.HIERARCHY_ASCENDING or
					ValidationFlags.LAYOUT

	override fun <S : TextElement> addElement(index: Int, element: S): S {
		element.parentSpan = this
		_elements.add(index, element)
		textParent?.invalidate(bubblingFlags)
		return element
	}

	override fun removeElement(index: Int): TextElement {
		val element = _elements.removeAt(index)
		element.parentSpan = null
		textParent?.invalidate(bubblingFlags)
		return element
	}

	/**
	 * Clears all elements in this span.
	 * @param dispose If dispose is true, the elements will be disposed.
	 */
	override fun clearElements(dispose: Boolean) {
		for (i in 0.._elements.lastIndex) {
			if (dispose)
				_elements[i].dispose()
		}
		_elements.clear()
		textParent?.invalidate(bubblingFlags)
	}

	fun char(char: Char): TextElement {
		return CharElement.obtain(char, tfCharStyle)
	}

	operator fun String?.unaryPlus() {
		if (this == null) return
		for (i in 0..length - 1) {
			addElement(char(this[i]))
		}
	}

	/**
	 * A utility variable that when set, clears/disposes the current elements and replaces them with the new text.
	 */
	var text: String
		get() {
			val elements = elements
			val builder = StringBuilder()
			for (i in 0..elements.lastIndex) {
				val char = elements[i].char
				if (char != null)
					builder.append(char)
			}
			return builder.toString()
		}
		set(value) {
			clearElements(true)
			+value
		}

	override fun validateCharStyle(concatenatedColorTint: ColorRo) {
		tfCharStyle.selectedTextColorTint.set(concatenatedColorTint).mul(charStyle.selectedColorTint)
		tfCharStyle.selectedBackgroundColor.set(concatenatedColorTint).mul(charStyle.selectedBackgroundColor)
		tfCharStyle.textColorTint.set(concatenatedColorTint).mul(charStyle.colorTint)
		tfCharStyle.backgroundColor.set(concatenatedColorTint).mul(charStyle.backgroundColor)
	}

	override fun dispose() {
		clearElements(true)
	}
}

fun span(init: ComponentInit<TextSpanElement> = {}): TextSpanElementImpl {
	val s = TextSpanElementImpl()
	s.init()
	return s
}