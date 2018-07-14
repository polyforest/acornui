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

package com.acornui.component

import com.acornui.component.scroll.ScrollRect
import com.acornui.component.style.StyleBase
import com.acornui.component.style.StyleType
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.graphics.Window
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.gl.core.ShaderBatch
import com.acornui.gl.core.scissor
import com.acornui.graphics.Color
import com.acornui.math.*
import kotlin.math.abs
import kotlin.math.roundToInt

object StencilUtil {

	var depth = -1

	inline fun mask(batch: ShaderBatch, gl: Gl20, renderMask: () -> Unit, renderContents: () -> Unit) {
		batch.flush(true)
		depth++
		if (depth >= 65535) throw IllegalStateException("There may not be more than 65535 nested masks.")
		if (depth == 0) {
			gl.enable(Gl20.STENCIL_TEST)
		}
		gl.colorMask(false, false, false, false)
		gl.stencilFunc(Gl20.ALWAYS, 0, 0.inv())
		gl.stencilOp(Gl20.INCR, Gl20.INCR, Gl20.INCR)
		renderMask()
		batch.flush(true)

		gl.colorMask(true, true, true, true)
		gl.stencilFunc(Gl20.EQUAL, depth + 1, 0.inv())
		gl.stencilOp(Gl20.KEEP, Gl20.KEEP, Gl20.KEEP)

		renderContents()

		batch.flush(true)
		gl.colorMask(false, false, false, false)
		gl.stencilFunc(Gl20.ALWAYS, 0, 0.inv())
		gl.stencilOp(Gl20.DECR, Gl20.DECR, Gl20.DECR)
		renderMask()

		batch.flush(true)
		gl.colorMask(true, true, true, true)
		gl.stencilFunc(Gl20.EQUAL, depth, 0.inv())
		gl.stencilOp(Gl20.KEEP, Gl20.KEEP, Gl20.KEEP)

		if (depth == 0) {
			gl.disable(Gl20.STENCIL_TEST)
		}
		depth--
	}
}


class GlScrollRect(
		owner: Owned
) : ElementContainerImpl<UiComponent>(owner), ScrollRect {

	override val style = bind(ScrollRectStyle())

	private val contents = addChild(container())
	private val maskClip = addChild(rect {
		style.backgroundColor = Color.WHITE
		interactivityMode = InteractivityMode.NONE
	})

	private val gl = inject(Gl20)

	private val glState = inject(GlState)

	private val _contentBounds = Rectangle()
	override val contentBounds: RectangleRo
		get() {
			_contentBounds.set(contents.x, contents.y, contents.width, contents.height)
			return _contentBounds
		}

	init {
		watch(style) {
			maskClip.style.borderRadii = it.borderRadius
		}
	}

	override fun onElementAdded(index: Int, element: UiComponent) {
		contents.addElement(index, element)
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
		val borderRadius = style.borderRadius
		if (borderRadius.isEmpty()) {
			// An optimized case where we can just scale the mask instead of recreating the mesh.
			maskClip.setSize(1f, 1f)
			maskClip.setScaling(w, h)
		} else {
			// Our mask needs curved borders.
			maskClip.setSize(w, h)
			maskClip.setScaling(1f, 1f)
		}
		out.set(w, h)
	}

	override fun intersectsGlobalRay(globalRay: RayRo, intersection: Vector3): Boolean {
		return maskClip.intersectsGlobalRay(globalRay, intersection)
	}

	private val localViewport = MinMax()

	override fun draw(viewport: MinMaxRo) {
		StencilUtil.mask(glState.batch, gl, {
			if (maskClip.visible) {
				maskClip.render(viewport)
			}
		}) {
			localToWindow(localViewport.set(0f, 0f, _bounds.width, _bounds.height))
			localViewport.intersection(viewport)

			contents.render(localViewport)
		}
	}
}

class ScrollRectStyle : StyleBase() {

	override val type: StyleType<ScrollRectStyle> = Companion
	var borderRadius: CornersRo by prop(Corners())

	companion object : StyleType<ScrollRectStyle>
}


/**
 * Calls scissorLocal with the default rectangle of 0, 0, width, height
 */
fun UiComponent.scissorLocal(inner: () -> Unit) {
	scissorLocal(0f, 0f, width, height, inner)
}

/**
 * Wraps the [inner] call in a scissor rectangle.
 * The coordinates will be converted to global automatically.
 * Note that this will not work properly for rotated components.
 */
fun UiComponent.scissorLocal(x: Float, y: Float, width: Float, height: Float, inner: () -> Unit) {
	val tmp = Vector3.obtain()
	localToWindow(tmp.set(x, y, 0f))
	val sX1 = tmp.x
	val sY1 = tmp.y
	localToWindow(tmp.set(width, height, 0f))
	val sX2 = tmp.x
	val sY2 = tmp.y
	Vector3.free(tmp)

	val glState = inject(GlState)
	val window = inject(Window)
	glState.scissor(minOf(sX1, sX2).roundToInt(), (window.height - maxOf(sY1, sY2)).roundToInt(), abs(sX2 - sX1).roundToInt(), abs(sY2 - sY1).roundToInt(), inner)
}