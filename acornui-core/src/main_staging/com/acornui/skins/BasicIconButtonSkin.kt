/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.skins

import com.acornui.component.*
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.LayoutElement
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.HorizontalLayout
import com.acornui.component.layout.algorithm.HorizontalLayoutData
import com.acornui.component.layout.algorithm.LayoutDataProvider
import com.acornui.component.layout.size
import com.acornui.component.style.ObservableBase
import com.acornui.component.style.StyleTag
import com.acornui.component.style.StyleType
import com.acornui.component.text.TextField
import com.acornui.component.text.selectable
import com.acornui.component.text.text
import com.acornui.di.Context
import com.acornui.math.Bounds
import com.acornui.math.Pad
import com.acornui.math.PadRo
import com.acornui.properties.afterChangeWithInit
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface IconButtonSkin : ButtonSkin, SingleElementContainer<UiComponent>

class BasicIconButtonSkin(
		owner: Context,
		private val texture: ButtonSkin
) : SingleElementContainerImpl<UiComponent>(owner), IconButtonSkin, LayoutDataProvider<HorizontalLayoutData> {

	val layoutStyle by layoutProp(IconButtonLayoutStyle())
	private val layoutAlgorithm = HorizontalLayout()
	override fun createLayoutData() = layoutAlgorithm.createLayoutData()

	private val textField: TextField

	init {
		addClass(BasicIconButtonSkin)
		addChild(texture)
		textField = addChild(text {
			selectable = false
			visible = false
		}) layout { widthPercent = 1.0 }
	}

	override var label: String = ""
		set(value) {
			field = value
			textField.label = value
			texture.label = value
			textField.visible = value.isNotEmpty()
		}

	override var buttonState: ButtonState by afterChangeWithInit(ButtonState.UP) { value ->
		texture.buttonState = value
	}

	override fun updateStyles() {
		super.updateStyles()
		layoutAlgorithm.style.apply {
			gap = layoutStyle.gap
			padding = layoutStyle.padding
			verticalAlign = layoutStyle.verticalAlign
			horizontalAlign = layoutStyle.horizontalAlign
		}
	}

	private val _elementsToLayout = ArrayList<LayoutElement>()
	private val elementsToLayout: List<LayoutElement>
		get() {
			val icon = element
			_elementsToLayout.clear()
			if (icon != null && icon.shouldLayout)
				_elementsToLayout.add(icon)
			if (textField.shouldLayout)
				_elementsToLayout.add(if (layoutStyle.iconOnLeft) _elementsToLayout.size else 0, textField)
			return _elementsToLayout
		}

	override fun updateLayout(explicitBounds: ExplicitBounds): Bounds {
		layoutAlgorithm.layout(explicitWidth, explicitHeight, elementsToLayout, out)
		if (explicitWidth != null && explicitWidth > out.width) out.width = explicitWidth
		if (explicitHeight != null && explicitHeight > out.height) out.height = explicitHeight
		texture.size(out)
	}

	companion object : StyleTag
}

class IconButtonLayoutStyle : ObservableBase() {

	override val type: StyleType<IconButtonLayoutStyle> = Companion

	/**
	 * The horizontal gap between elements.
	 */
	var gap by prop(4.0)

	/**
	 * The Padding object with left, bottom, top, and right padding.
	 */
	var padding by prop<PadRo>(Pad(4.0))

	/**
	 * The horizontal alignment of the entire row within the explicit width.
	 * If the explicit width is null, this will have no effect.
	 */
	var horizontalAlign by prop(HAlign.CENTER)

	/**
	 * The vertical alignment of each element within the measured height.
	 * This can be overridden on the individual element with [HorizontalLayoutData.verticalAlign]
	 */
	var verticalAlign by prop(VAlign.MIDDLE)

	/**
	 * If false, the icon will be on the right instead of left.
	 */
	var iconOnLeft by prop(true)

	companion object : StyleType<IconButtonLayoutStyle>

}

inline fun Context.basicIconButtonSkin(texture: ButtonSkin, init: ComponentInit<BasicIconButtonSkin> = {}): BasicIconButtonSkin {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return BasicIconButtonSkin(this, texture).apply(init)
}

inline fun Context.basicIconButtonSkin(theme: Theme, init: ComponentInit<BasicIconButtonSkin> = {}): IconButtonSkin {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return BasicIconButtonSkin(this, basicButtonSkin(theme)).apply {
		layoutStyle.apply {
			padding = theme.buttonPad
			gap = theme.iconButtonGap
			verticalAlign = VAlign.MIDDLE
		}
		init()
	}
}