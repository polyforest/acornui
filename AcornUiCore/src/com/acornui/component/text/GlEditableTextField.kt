/*
 * Copyright 2018 Poly Forest
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
import com.acornui.component.ContainerImpl
import com.acornui.component.UiComponent
import com.acornui.component.scroll.ScrollModelImpl
import com.acornui.component.scroll.ScrollPolicy
import com.acornui.core.di.Owned
import com.acornui.graphics.Color
import com.acornui.math.Bounds

@Suppress("LeakingThis")
open class GlEditableTextField(owner: Owned) : ContainerImpl(owner), EditableTextField {

	override var focusEnabled = true
	override var focusOrder = 0f
	override var highlight: UiComponent? by createSlot()

	override val boxStyle: BoxStyle = bind(BoxStyle())
	override val hScrollModel = ScrollModelImpl()
	override val vScrollModel = ScrollModelImpl()
	override var hScrollPolicy = ScrollPolicy.AUTO
	override var vScrollPolicy = ScrollPolicy.AUTO

	override var editable: Boolean = true

	override val textCommander: TextCommander = GlTextCommander(this)

	private val textField = GlTextField(this)

	override val charStyle: CharStyle
		get() = textField.charStyle
	override val flowStyle: TextFlowStyle
		get() = textField.flowStyle

	override var text: String
		get() = textField.text
		set(value) {
			textField.text = value
		}

//	override var contents: UiComponent
//		get() = textField.contents
//		set(value) {
//			textField.contents = value
//		}

	init {
		styleTags.add(EditableTextField)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		textField.setSize(explicitWidth, explicitHeight)
		out.set(textField.bounds)
		highlight?.setSize(out.width, out.height)
	}

	override fun dispose() {
		super.dispose()
		hScrollModel.dispose()
		vScrollModel.dispose()
	}
}


class GlTextCommander(private val textField: GlEditableTextField) : TextCommander {
	override fun exec(commandName: String, value: String): Boolean {
		return false
	}

	override fun queryBool(commandId: String): Boolean {
		return false
	}

	override fun queryString(commandId: String): String {
		return ""
	}

	override fun queryColor(commandId: String): Color {
		return Color()
	}
}