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

package com.acornui.component

import com.acornui.component.style.ObservableBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.text.StringFormatter
import com.acornui.text.ToStringFormatter

/**
 * A SimpleItemRenderer is a [ListItemRenderer] implementation that displays data as text using a formatter.
 */
open class SimpleItemRenderer<E : Any>(
		owner: Context,
		private val formatter: StringFormatter<E>
) : ContainerImpl(owner), ListItemRenderer<E> {

	protected val textField = addChild(text())

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
		addClassAll(listOf(Companion, ItemRenderer))
	}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		val pad = style.padding
		textField.size(pad.expandWidth(explicitWidth), pad.expandHeight(explicitHeight))
		textField.position(pad.left, pad.top)
		out.set(pad.expandWidth(textField.width), pad.expandHeight(textField.height))
	}

	companion object : StyleTag
}

class SimpleItemRendererStyle : ObservableBase() {

	override val type: StyleType<SimpleItemRendererStyle> = SimpleItemRendererStyle

	var padding by prop(Pad())

	companion object : StyleType<SimpleItemRendererStyle>
}

fun <E : Any> Context.simpleItemRenderer(formatter: StringFormatter<E> = ToStringFormatter, init: ComponentInit<SimpleItemRenderer<E>> = {}): SimpleItemRenderer<E> {
	val renderer = SimpleItemRenderer(this, formatter)
	renderer.init()
	return renderer
}
