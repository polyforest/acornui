package com.acornui.component.text

import com.acornui.async.resultOrNull
import com.acornui.async.then
import com.acornui.component.*
import com.acornui.component.style.*
import com.acornui.core.Disposable
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import com.acornui.gl.core.GlState
import com.acornui.recycle.ObjectPool

interface TextSpanElementRo<out T : TextElementRo> : ElementParentRo<T> {

	/**
	 * The calculated character style.
	 */
	val charElementStyle: CharElementStyleRo

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
		val textField = textParent?.textField
		var p: UiComponentRo? = textParent
		while (p != null && p !== textField) {
			textFieldX += p.x
			p = p.parent
		}
		return textFieldX
	}

val TextSpanElementRo<TextElementRo>.textFieldY: Float
	get() {
		var textFieldY = 0f
		val textField = textParent?.textField
		var p: UiComponentRo? = textParent
		while (p != null && p !== textField) {
			textFieldY += p.y
			p = p.parent
		}
		return textFieldY
	}


interface TextSpanElement : ElementParent<TextElement>, TextSpanElementRo<TextElement>, Disposable {

	override var textParent: TextNodeRo?

	fun validateStyles()

}

@Suppress("LeakingThis")
open class TextSpanElementImpl private constructor() : TextSpanElement, Styleable {

	override var textParent: TextNodeRo? = null

	private lateinit var glState: GlState

	override val styleParent: StyleableRo?
		get() = textParent

	private val _elements = ArrayList<TextElement>()

	override val elements: List<TextElement>
		get() = _elements

	protected val styles = Styles(this)

	val charStyle = styles.bind(CharStyle())

	private val _charElementStyle = CharElementStyle()
	override val charElementStyle: CharElementStyleRo = _charElementStyle

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
		_charElementStyle.set(charStyle)
		_charElementStyle.font?.then { textParent?.invalidate(ValidationFlags.LAYOUT) }
	}

	private val font: BitmapFont?
		get() = _charElementStyle.font?.resultOrNull()

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
		return CharElement.obtain(char, glState)
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

	override fun dispose() {
		clearElements(true)
		pool.free(this)
	}

	companion object {
		private val pool = ObjectPool { TextSpanElementImpl() }

		fun obtain(glState: GlState): TextSpanElementImpl {
			val s = pool.obtain()
			s.glState = glState
			return s
		}
	}
}

fun Scoped.span(init: ComponentInit<TextSpanElementImpl> = {}): TextSpanElementImpl {
	val s = TextSpanElementImpl.obtain(inject(GlState))
	s.init()
	return s
}