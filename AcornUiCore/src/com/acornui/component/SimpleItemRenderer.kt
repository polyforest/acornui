package com.acornui.component

import com.acornui.component.layout.ListItemRenderer
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.style.StyleTag
import com.acornui.component.style.styleTag
import com.acornui.component.text.text
import com.acornui.core.di.Owned
import com.acornui.core.text.StringFormatter
import com.acornui.core.text.ToStringFormatter
import com.acornui.math.Bounds

/**
 * A SimpleItemRenderer is a [ListItemRenderer] implementation that displays data as text using a formatter.
 */
class SimpleItemRenderer<E : Any>(
		owner: Owned,
		private val formatter: StringFormatter<E>
) : ContainerImpl(owner), ListItemRenderer<E> {

	private val textField = addChild(text { interactivityMode = InteractivityMode.NONE })
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

	override fun updateSizeConstraints(out: SizeConstraints) {
		out.bound(textField.sizeConstraints)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		super.updateLayout(explicitWidth, explicitHeight, out)
		textField.setSize(explicitWidth, explicitHeight)
		out.set(textField.bounds)
	}

	companion object : StyleTag {
		val EVEN_STYLE = styleTag()
		val ODD_STYLE = styleTag()
	}
}

fun <E : Any> Owned.simpleItemRenderer(formatter: StringFormatter<E> = ToStringFormatter, init: ComponentInit<SimpleItemRenderer<E>> = {}): SimpleItemRenderer<E> {
	val renderer = SimpleItemRenderer(this, formatter)
	renderer.init()
	return renderer
}