/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui.component.NativeComponent
import com.acornui.core.userInfo
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.js.time.setTimeout
import com.acornui.math.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.css.CSSStyleDeclaration
import kotlin.browser.document

fun DomComponent(localName: String) = DomComponent(document.createElement(localName) as HTMLElement)

open class DomComponent(
		val element: HTMLElement = document.createElement("div") as HTMLElement
) : NativeComponent {

	val padding: Pad = Pad(0f)
	val border: Pad = Pad(0f)
	val margin: Pad = Pad(0f)

	private var _interactivityEnabled: Boolean = true
	override var interactivityEnabled: Boolean
		get() = _interactivityEnabled
		set(value) {
			_interactivityEnabled = value
			element.style.setProperty("pointer-events", if (value) "auto" else "none")
		}

	private val _bounds = Bounds()

	init {
		element.draggable = false
		element.className = "acornComponent"
	}

	private var _visible: Boolean = true

	override var visible: Boolean
		get() = _visible
		set(value) {
			if (_visible == value) return
			_visible = value
			refreshDisplayStyle()
		}

	protected open fun refreshDisplayStyle() {
		if (_visible) {
			element.style.display = "inline-block"
		} else {
			// We use display for visibility so that it no longer affects layout or scrolling.
			element.style.display = "none"
		}
	}

	private var explicitWidth: Float? = null
	private var explicitHeight: Float? = null

	override val bounds: BoundsRo
		get() {
			if (explicitWidth == null) {
				_bounds.width = element.offsetWidth.toFloat() + marginW
			} else {
				_bounds.width = explicitWidth!!
			}

			if (explicitHeight == null) {
				_bounds.height = element.offsetHeight.toFloat() + marginH
			} else {
				_bounds.height = explicitHeight!!
			}
			return _bounds
		}

	private var _width: String? = null
	private var _height: String? = null

	override fun setSize(width: Float?, height: Float?) {
		if (explicitWidth == width && explicitHeight == height) return // no-op
		explicitWidth = width
		explicitHeight = height
		val newW = if (width == null) "auto" else "${maxOf(0f, width - paddingW - borderW - marginW)}px"
		val newH = if (height == null) "auto" else "${maxOf(0f, height - paddingH - borderH - marginH)}px"
		if (newW != _width) {
			_width = newW
			element.style.width = newW
		}
		if (newH != _height) {
			_height = newH
			element.style.height = newH
		}
	}

	protected open val paddingW: Float
		get() = padding.left + padding.right

	protected open val paddingH: Float
		get() = padding.top + padding.bottom


	protected open val borderW: Float
		get() = border.left + border.right

	protected open val borderH: Float
		get() = border.top + border.bottom

	protected open val marginW: Float
		get() = margin.left + margin.right

	protected open val marginH: Float
		get() = margin.top + margin.bottom

	private var wasSimpleTranslate: Boolean = true

	override fun setTransform(value: Matrix4Ro) {
		if (wasSimpleTranslate) {
			element.style.removeProperty("left")
			element.style.removeProperty("top")
			wasSimpleTranslate = false
		}
		element.style.transform = "matrix3d(${value.values.joinToString(",")})"
	}

	override fun setSimpleTranslate(x: Float, y: Float) {
		if (!wasSimpleTranslate) {
			element.style.removeProperty("transform")
			wasSimpleTranslate = true
		}
		element.style.top = "${y}px"
		element.style.left = "${x}px"
	}

	override fun setConcatenatedTransform(value: Matrix4Ro) {
	}

	override fun setColorTint(value: ColorRo) {
		element.style.opacity = value.a.toString()
	}

	override fun setConcatenatedColorTint(value: ColorRo) {
	}

	open fun blur() {
		element.removeAttribute("tabindex") // Must be all lowercase.
		if (userInfo.isIe) {
			setTimeout({element.blur()}, 100)
		} else {
			element.blur()
		}
	}

	open fun focus() {
		element.setAttribute("tabindex", "0")
		element.focus()
		if (userInfo.isIe) {
			// Because.. IE
			setTimeout({element.focus()}, 100)
		}
	}

	override fun dispose() {
	}
}

fun CSSStyleDeclaration.userSelect(value: Boolean) {
	val v = if (value) "text" else "none"
	setProperty("user-select", v)
	setProperty("-webkit-user-select", v)
	setProperty("-moz-user-select", v)
	setProperty("-ms-user-select", v)
}