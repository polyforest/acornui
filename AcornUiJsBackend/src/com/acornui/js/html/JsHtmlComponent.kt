/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.js.html

import com.acornui.component.*
import com.acornui.component.text.TextField
import com.acornui.core.di.Owned
import com.acornui.js.dom.component.DomComponent
import com.acornui.js.dom.component.applyBox
import com.acornui.js.dom.component.applyCss
import com.acornui.math.Bounds
import org.w3c.dom.HTMLElement

class JsHtmlComponent(owner: Owned, private val rootElement: HTMLElement) : UiComponentImpl(owner), HtmlComponent {

	private val component = DomComponent()
	override val boxStyle = bind(BoxStyle())

	init {
		styleTags.add(TextField)

		watch(boxStyle) {
			it.applyCss(component.element)
			it.applyBox(component)
			invalidate(ValidationFlags.LAYOUT)
		}

		component.element.style.setProperty("position", "absolute")
	}

	override fun onActivated() {
		super.onActivated()
		rootElement.appendChild(component.element)
	}

	override fun onDeactivated() {
		super.onDeactivated()
		rootElement.removeChild(component.element)
	}

	override var html: String
		get() = component.element.innerHTML
		set(value) {
			component.element.innerHTML = value
		}

	override fun updateLayoutEnabled() {
		super.updateLayoutEnabled()
		var v = true
		parentWalk {
			if (!it.visible) {
				v = false
				false
			} else {
				true
			}
		}
		component.visible = v
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		component.setSize(explicitWidth, explicitHeight)
		out.set(component.bounds)
	}

	override fun updateConcatenatedColorTransform() {
		super.updateConcatenatedColorTransform()
		component.setColorTint(colorTint)
	}

	override fun updateConcatenatedTransform() {
		super.updateConcatenatedTransform()
		component.setTransform(concatenatedTransform)
	}


}