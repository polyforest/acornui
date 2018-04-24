package com.acornui.component

import com.acornui.component.layout.ListItemRenderer
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.text.text
import com.acornui.core.di.Owned
import com.acornui.core.text.StringFormatter
import com.acornui.core.text.ToStringFormatter
import com.acornui.math.Bounds
import com.acornui.math.Pad

/**
 * A SimpleItemRenderer is a [ListItemRenderer] implementation that displays data as text using a formatter.
 */
class SimpleItemRenderer<E : Any>(
		owner: Owned,
		private val formatter: StringFormatter<E>
) : ContainerImpl(owner), ListItemRenderer<E> {

	private val textField = addChild(text())
	override var toggled: Boolean = false

	override var index: Int = -1

	private var _data: E? = null
	override var data: E?
		get() = _data
		set(value) {
			if (_data == value) return
			_data = value
			val text = if (value == null) "" else formatter.format(value)
			textField.text = text
		}

	val style = bind(SimpleItemRendererStyle())

	init {
		styleTags.add(Companion)
		interactivityMode = InteractivityMode.NONE
	}

	override fun updateSizeConstraints(out: SizeConstraints) {
		out.width.min = style.padding.expandWidth2(textField.minWidth ?: 0f)
		out.height.min = style.padding.expandHeight2(textField.minHeight?: 0f)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val pad = style.padding
		textField.setSize(pad.expandWidth(explicitWidth), pad.expandHeight(explicitHeight))
		textField.moveTo(pad.left, pad.top)
		out.set(pad.expandWidth2(textField.width), pad.expandHeight2(textField.height))
	}

	companion object : StyleTag
}

class SimpleItemRendererStyle : StyleBase() {

	override val type: StyleType<SimpleItemRendererStyle> = SimpleItemRendererStyle

	var padding by prop(Pad())

	companion object : StyleType<SimpleItemRendererStyle>
}

fun <E : Any> Owned.simpleItemRenderer(formatter: StringFormatter<E> = ToStringFormatter, init: ComponentInit<SimpleItemRenderer<E>> = {}): SimpleItemRenderer<E> {
	val renderer = SimpleItemRenderer(this, formatter)
	renderer.init()
	return renderer
}