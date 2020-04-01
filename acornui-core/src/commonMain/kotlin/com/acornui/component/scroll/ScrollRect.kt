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

package com.acornui.component.scroll

import com.acornui.component.*
import com.acornui.component.layout.Positionable
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.di.Context
import com.acornui.graphic.Color
import com.acornui.math.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface ScrollRect : ElementContainer<UiComponent> {

	val style: ScrollRectStyle

	val contentBounds: RectangleRo

	val contentsWidth: Float
		get() = contentBounds.width

	val contentsHeight: Float
		get() = contentBounds.height

	fun scrollTo(x: Float, y: Float)

}

class ScrollRectImpl(
		owner: Context
) : ElementContainerImpl<UiComponent>(owner), ScrollRect {

	override val style = bind(ScrollRectStyle())

	private var scroll = vec2()

	override val useMvpTransforms: Boolean = true

	private val contents = addChild(container { interactivityMode = InteractivityMode.CHILDREN })
	private val maskClip = addChild(rect {
		style.backgroundColor = Color.WHITE
		interactivityMode = InteractivityMode.NONE
	})

	/**
	 * If true, when the contents scroll, the position will be snapped to the nearest pixel.
	 * Default is [Positionable.defaultSnapToPixel]
	 */
	var contentsSnapToPixel: Boolean
		get() = contents.snapToPixel
		set(value) {
			contents.snapToPixel = value
		}

	private val _contentBounds = Rectangle()
	override val contentBounds: RectangleRo
		get() = _contentBounds.set(contents.left, contents.top, contents.width, contents.height)

	private val clipRegion = MinMax(0f, 0f, 0f, 0f)

	init {
		watch(style) {
			maskClip.style.borderRadii = it.borderRadii
			maskClip.style.margin = it.padding
		}
	}
	
	override fun onElementAdded(oldIndex: Int, newIndex: Int, element: UiComponent) {
		contents.addElement(newIndex, element)
	}

	override fun onElementRemoved(index: Int, element: UiComponent) {
		contents.removeElement(element)
	}

	private val _viewTransform = Matrix4()
	override val viewTransform: Matrix4Ro by validationProp(ValidationFlags.VIEW_PROJECTION) {
		_viewTransform.set(parent?.viewTransform ?: Matrix4.IDENTITY).translate(-_bounds.x, -_bounds.y)
	}

	override fun scrollTo(x: Float, y: Float) {
		if (contentsSnapToPixel)
			scroll.set(MathUtils.offsetRound(x), MathUtils.offsetRound(y))
		else
			scroll.set(x, y)
		_bounds.x = scroll.x
		_bounds.y = scroll.y
		clipRegionLocal = clipRegion.set(_bounds)
		invalidate(ValidationFlags.VIEW_PROJECTION)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		maskClip.setSize(explicitWidth, explicitHeight)
		out.set(scroll.x, scroll.y, maskClip.width, maskClip.height, maskClip.baseline)
		clipRegionLocal = clipRegion.set(out)
	}

	override fun updateViewProjection() {
		check(cameraOverride == null) { "Cannot override the camera on ScrollRect." }
		super.updateViewProjection()
	}

	override fun render() {
		if (parent != null && visible && colorTint.a > 0f) {
			draw()
		}
	}

	override fun draw() {
		StencilUtil.mask(gl.batch, gl, {
			gl.uniforms.useCamera(parent!!, useModel = true) {
				maskClip.render()
			}
		}) {
			gl.uniforms.useCamera(this, useModel = true) {
				contents.render()
			}
		}
	}
}

class ScrollRectStyle : StyleBase() {

	override val type: StyleType<ScrollRectStyle> = Companion

	/**
	 * The border radii for clipping.
	 */
	var borderRadii: CornersRo by prop(Corners())

	/**
	 * Pads the mask.  This will not affect the layout of the Scroll Rect's elements.
	 */
	var padding: PadRo by prop(Pad())

	companion object : StyleType<ScrollRectStyle>
}

inline fun Context.scrollRect(init: ComponentInit<ScrollRectImpl> = {}): ScrollRectImpl {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val s = ScrollRectImpl(this)
	s.init()
	return s
}
