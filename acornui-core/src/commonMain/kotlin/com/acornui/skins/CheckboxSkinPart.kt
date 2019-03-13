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

import com.acornui.component.Labelable
import com.acornui.component.UiComponent
import com.acornui.component.layout.VAlign
import com.acornui.component.layout.algorithm.HorizontalLayoutContainer
import com.acornui.component.text.TextField
import com.acornui.component.text.text
import com.acornui.core.di.Owned

/**
 * A typical implementation of a skin part for a labelable button state.
 */
open class CheckboxSkinPart(
		owner: Owned,
		val box: UiComponent
) : HorizontalLayoutContainer(owner), Labelable {

	val textField: TextField

	init {
		style.verticalAlign = VAlign.MIDDLE
		+box
		textField = +text("") {
			includeInLayout = false
		} layout {
			widthPercent = 1f
		}
	}

	override var label: String
		get() = textField.label
		set(value) {
			textField.includeInLayout = value.isNotEmpty()
			textField.text = value
		}
}
