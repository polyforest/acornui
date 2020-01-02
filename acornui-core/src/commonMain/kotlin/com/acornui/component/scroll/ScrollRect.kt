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
import com.acornui.di.Owned
import com.acornui.graphic.CameraRo
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
		owner: Owned
) : ElementContainerImpl<UiComponent>(owner), ScrollRect {

	override val style = bind(ScrollRectStyle())

	private var scroll = Vector2()

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
		get() {
			_contentBounds.set(contents.x, contents.y, contents.width, contents.height)
			return _contentBounds
		}

	private val clipRegion = MinMax(0f, 0f, 0f, 0f)

	init {
		watch(style) {
			maskClip.style.borderRadii = it.borderRadii
			maskClip.style.margin = it.padding
		}
	}

	override var cameraOverride: CameraRo?
		get() = null
		set(_) { throw UnsupportedOperationException("Cannot override the camera on ScrollRect.") }

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
//		contents.moveTo(-x, -y)
		scroll.set(x, y)
		_bounds.x = x
		_bounds.y = y
		clipRegionLocal = clipRegion.set(_bounds)
		invalidateViewProjection()
	}

//	override fun onSizeSet(oldWidth: Float?, oldHeight: Float?, newWidth: Float?, newHeight: Float?) {
//		// TODO:
////		clipRegionLocal = clipRegion.set(0f, 0f, newWidth ?: 0f, newHeight ?: 0f)
//	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		maskClip.setSize(explicitWidth, explicitHeight)
		out.set(scroll.x, scroll.y, maskClip.width, maskClip.height, maskClip.baseline)
		clipRegionLocal = clipRegion.set(out)
	}

	override fun getChildrenUnderPoint(canvasX: Float, canvasY: Float, onlyInteractive: Boolean, returnAll: Boolean, out: MutableList<UiComponentRo>, rayCache: RayRo?): MutableList<UiComponentRo> {
		return super.getChildrenUnderPoint(canvasX, canvasY, onlyInteractive, returnAll, out, rayCache)
	}

	//	override fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean {
//		return true
////		return maskClip.intersectsGlobalRay(globalRay, intersection)
//	}

	override fun draw() {
		StencilUtil.mask(gl.batch, gl, {
			maskClip.render()
		}) {
			gl.uniforms.useCamera(this) {
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

inline fun Owned.scrollRect(init: ComponentInit<ScrollRectImpl> = {}): ScrollRectImpl {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	val s = ScrollRectImpl(this)
	s.init()
	return s
}
