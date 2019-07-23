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

package com.acornui.js.html

import com.acornui.component.*
import com.acornui.core.di.Owned
import com.acornui.core.focus.Focusable
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.*
import com.acornui.reflect.observable
import com.acornui.signal.Cancel
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.css.CSSStyleDeclaration
import kotlin.browser.document
import kotlin.properties.Delegates

class JsHtmlComponent(
		owner: Owned,
		rootElement: HTMLElement,
		element: HTMLElement = document.createElement("div") as HTMLElement
) : UiComponentImpl(owner), HtmlComponent {

	private val component = DomComponent(element)
	override val boxStyle = bind(BoxStyle())

	private val focusedChangingHandler = {
		oldFocusable: Focusable?, newFocusable: Focusable?, cancel: Cancel ->
		if (oldFocusable == this) {
			cancel.cancel()
		}
	}

	private var parentElement: Element

	init {
		styleTags.add(HtmlComponent)

		watch(boxStyle) {
			it.applyCss(element)
			it.applyBox(component)
		}

		parentElement = element.parentElement ?: rootElement
		if (parentElement.contains(element))
			parentElement.removeChild(element)
		element.style.display = "block"
		element.style.opacity = "0"
		element.style.setProperty("position", "absolute")
	}

	override fun onActivated() {
		super.onActivated()
		focusManager.focusedChanging.add(focusedChangingHandler)
		parentElement.appendChild(component.element)
	}

	override fun onDeactivated() {
		focusManager.focusedChanging.remove(focusedChangingHandler)
		super.onDeactivated()
		parentElement.removeChild(component.element)
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

	override fun draw(renderContext: RenderContextRo) {
		component.concatenatedTransform = renderContext.modelTransform
		component.concatenatedColorTint = renderContext.colorTint
	}
}


class DomComponent(
		val element: HTMLElement = document.createElement("div") as HTMLElement
) {

	val padding: Pad = Pad(0f)
	val border: Pad = Pad(0f)
	val margin: Pad = Pad(0f)

	private var _interactivityEnabled: Boolean = true
	var interactivityEnabled: Boolean
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

	var visible: Boolean
		get() = _visible
		set(value) {
			if (_visible == value) return
			_visible = value
			refreshDisplayStyle()
		}

	private fun refreshDisplayStyle() {
		if (_visible) {
			element.style.display = "inline-block"
		} else {
			// We use display for visibility so that it no longer affects layout or scrolling.
			element.style.display = "none"
		}
	}

	private var explicitWidth: Float? = null
	private var explicitHeight: Float? = null

	val bounds: BoundsRo
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

	fun setSize(width: Float?, height: Float?) {
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

	private val paddingW: Float
		get() = padding.left + padding.right

	private val paddingH: Float
		get() = padding.top + padding.bottom

	private val borderW: Float
		get() = border.left + border.right

	private val borderH: Float
		get() = border.top + border.bottom

	private val marginW: Float
		get() = margin.left + margin.right

	private val marginH: Float
		get() = margin.top + margin.bottom

	var concatenatedTransform: Matrix4Ro by observable(Matrix4.IDENTITY) { value ->
		element.style.transform = "matrix3d(${value.values.joinToString(",")})"
	}

	var concatenatedColorTint: ColorRo by observable(Color.WHITE) { value ->
		val str = value.a.toString()
		if (element.style.opacity != str)
			element.style.opacity = str
	}
}

fun CSSStyleDeclaration.userSelect(value: Boolean) {
	val v = if (value) "text" else "none"
	setProperty("user-select", v)
	setProperty("-webkit-user-select", v)
	setProperty("-moz-user-select", v)
	setProperty("-ms-user-select", v)
}


private fun BoxStyle.applyBox(native: DomComponent) {
	native.margin.set(margin)
	native.border.set(borderThicknesses)
	native.padding.set(padding)
}

private fun BoxStyle.applyCss(element: HTMLElement) {
	val it = this
	element.style.apply {
		val gradient = it.linearGradient
		if (gradient == null) {
			removeProperty("background")
			backgroundColor = it.backgroundColor.toCssString()
		} else {
			removeProperty("background-color")
			background = gradient.toCssString()
		}
		val bC = it.borderColors
		borderTopColor = bC.top.toCssString()
		borderRightColor = bC.right.toCssString()
		borderBottomColor = bC.bottom.toCssString()
		borderLeftColor = bC.left.toCssString()

		val b = it.borderThicknesses
		borderLeftWidth = "${b.left}px"
		borderTopWidth = "${b.top}px"
		borderRightWidth = "${b.right}px"
		borderBottomWidth = "${b.bottom}px"

		val c = it.borderRadii
		borderTopLeftRadius = "${c.topLeft.x}px ${c.topLeft.y}px"
		borderTopRightRadius = "${c.topRight.x}px ${c.topRight.y}px"
		borderBottomRightRadius = "${c.bottomRight.x}px ${c.bottomRight.y}px"
		borderBottomLeftRadius = "${c.bottomLeft.x}px ${c.bottomLeft.y}px"

		borderStyle = "solid"
		val m = it.margin
		marginLeft = "${m.left}px"
		marginTop = "${m.top}px"
		marginRight = "${m.right}px"
		marginBottom = "${m.bottom}px"

		val p = it.padding
		paddingLeft = "${p.left}px"
		paddingTop = "${p.top}px"
		paddingRight = "${p.right}px"
		paddingBottom = "${p.bottom}px"
	}
}
