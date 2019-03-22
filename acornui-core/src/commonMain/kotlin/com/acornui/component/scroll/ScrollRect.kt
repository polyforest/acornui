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

package com.acornui.component.scroll

import com.acornui.component.*
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.core.di.Owned
import com.acornui.graphic.Color
import com.acornui.math.*

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

	private val contents = addChild(container { interactivityMode = InteractivityMode.CHILDREN })
	private val maskClip = addChild(rect {
		style.backgroundColor = Color.WHITE
		interactivityMode = InteractivityMode.NONE
	})

	private val _contentBounds = Rectangle()
	override val contentBounds: RectangleRo
		get() {
			_contentBounds.set(contents.x, contents.y, contents.width, contents.height)
			return _contentBounds
		}

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

	override fun scrollTo(x: Float, y: Float) {
		contents.setPosition(-x, -y)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val w = explicitWidth ?: 100f
		val h = explicitHeight ?: 100f
		maskClip.setSize(w, h)
		maskClip.setScaling(1f, 1f)
		out.set(w, h)
	}

	override fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean {
		return maskClip.intersectsGlobalRay(globalRay, intersection)
	}

	private val contentsClip = MinMax()

	override fun draw(clip: MinMaxRo) {
		StencilUtil.mask(glState.batch, gl, {
			if (maskClip.visible) {
				maskClip.render(clip)
			}
		}) {
			localToCanvas(contentsClip.set(0f, 0f, _bounds.width, _bounds.height))
			contentsClip.intersection(clip)

			contents.render(contentsClip)
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

fun Owned.scrollRect(init: ComponentInit<ScrollRectImpl> = {}): ScrollRectImpl {
	val s = ScrollRectImpl(this)
	s.init()
	return s
}