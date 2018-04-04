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

package com.acornui.js.dom.component

import com.acornui.component.ElementContainerImpl
import com.acornui.component.UiComponent
import com.acornui.component.scroll.ScrollRect
import com.acornui.core.di.Owned
import com.acornui.math.*
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.browser.document

class DomScrollRect(
		owner: Owned,
		private val element: HTMLElement = document.createElement("div") as HTMLDivElement
) : ElementContainerImpl<UiComponent>(owner, DomContainer(element)), ScrollRect {

	private val _contentBounds = Rectangle()

	private val _borderRadius = Corners()
	override var borderRadius: CornersRo
		get() = _borderRadius
		set(value) {
			_borderRadius.set(value)
			element.style.apply {
				borderTopLeftRadius = "${value.topLeft.x}px ${value.topLeft.y}px"
				borderTopRightRadius = "${value.topRight.x}px ${value.topRight.y}px"
				borderBottomRightRadius = "${value.bottomRight.x}px ${value.bottomRight.y}px"
				borderBottomLeftRadius = "${value.bottomLeft.x}px ${value.bottomLeft.y}px"
			}
		}

	override val contentBounds: RectangleRo
		get() = _contentBounds

	override fun scrollTo(x: Float, y: Float) {
		element.scrollLeft = x.toDouble()
		element.scrollTop = y.toDouble()
		_contentBounds.x = -x
		_contentBounds.y = -y
	}

	init {
		element.style.apply {
			overflowX = "hidden"
			overflowY = "hidden"
		}
	}

	override fun updateColorTransform() {
		if (_colorTint.a == 1f) _colorTint.a = 0.99999f // A workaround to a webkit bug http://stackoverflow.com/questions/5736503/how-to-make-css3-rounded-corners-hide-overflow-in-chrome-opera
		super.updateColorTransform()
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val w = explicitWidth ?: 100f
		val h = explicitHeight ?: 100f
		super.updateLayout(explicitWidth, explicitHeight, out)
		_contentBounds.width = out.width
		_contentBounds.height = out.height
		out.set(w, h)
	}
}

