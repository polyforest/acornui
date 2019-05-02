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

package com.acornui.skins

import com.acornui.component.ElementContainerImpl
import com.acornui.component.Labelable
import com.acornui.component.UiComponent
import com.acornui.component.layout.algorithm.FlowHAlign
import com.acornui.component.text.TextField
import com.acornui.component.text.selectable
import com.acornui.component.text.text
import com.acornui.core.di.Owned
import com.acornui.math.Bounds
import com.acornui.math.PadRo

/**
 * A typical implementation of a skin part for a labelable button state.
 */
open class LabelButtonSkinPart(
		owner: Owned,
		val texture: UiComponent,
		val padding: PadRo
) : ElementContainerImpl<UiComponent>(owner), Labelable {

	private val textField: TextField = text()

	init {
		+texture
		+textField

		textField.selectable = false
		textField.flowStyle.horizontalAlign = FlowHAlign.CENTER
	}

	override var label: String
		get() = textField.label
		set(value) {
			textField.label = value
		}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		textField.setSize(padding.reduceWidth(explicitWidth), null)
		var h = maxOf(minHeight ?: 0f, padding.expandHeight2(textField.height))
		val w = maxOf(h, explicitWidth ?: maxOf(minWidth ?: 0f, padding.expandWidth2(textField.width)))
		if (explicitHeight != null && explicitHeight > h) h = explicitHeight
		texture.setSize(w, h)
		textField.moveTo((padding.reduceWidth2(w) - textField.width) * 0.5f + padding.left, (padding.reduceHeight2(h) - textField.height) * 0.5f + padding.top)
		out.set(texture.width, texture.height, textField.baselineY)
	}
}