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
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.math.*
import kotlin.math.abs
import kotlin.math.roundToInt

object StencilUtil {

	var depth = -1

	inline fun mask(batch: ShaderBatch, gl: Gl20, renderMask: () -> Unit, renderContents: () -> Unit) {
		batch.flush()
		depth++
		if (depth >= 65535) throw IllegalStateException("There may not be more than 65535 nested masks.")
		if (depth == 0) {
			gl.enable(Gl20.STENCIL_TEST)
		}
		gl.colorMask(false, false, false, false)
		gl.stencilFunc(Gl20.ALWAYS, 0, 0.inv())
		gl.stencilOp(Gl20.INCR, Gl20.INCR, Gl20.INCR)
		renderMask()
		batch.flush()

		gl.colorMask(true, true, true, true)
		gl.stencilFunc(Gl20.EQUAL, depth + 1, 0.inv())
		gl.stencilOp(Gl20.KEEP, Gl20.KEEP, Gl20.KEEP)

		renderContents()

		batch.flush()
		gl.colorMask(false, false, false, false)
		gl.stencilFunc(Gl20.ALWAYS, 0, 0.inv())
		gl.stencilOp(Gl20.DECR, Gl20.DECR, Gl20.DECR)
		renderMask()

		batch.flush()
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
		val borderRadii = style.borderRadii
		if (borderRadii.isEmpty()) {
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
	var borderRadii: CornersRo by prop(Corners())

	companion object : StyleType<ScrollRectStyle>
}


/**
 * Calls scissorLocal with the default rectangle of 0, 0, width, height
 */
fun UiComponentRo.scissorLocal(inner: () -> Unit) {
	scissorLocal(0f, 0f, width, height, inner)
}

/**
 * Wraps the [inner] call in a scissor rectangle.
 * The local coordinates will be converted to gl window coordinates automatically.
 * Note that this will not work properly for rotated components.
 */
fun UiComponentRo.scissorLocal(x: Float, y: Float, width: Float, height: Float, inner: () -> Unit) {
	val tmp = Vector3.obtain()
	localToCanvas(tmp.set(x, y, 0f))
	val sX1 = tmp.x
	val sY1 = tmp.y
	localToCanvas(tmp.set(width, height, 0f))
	val sX2 = tmp.x
	val sY2 = tmp.y
	Vector3.free(tmp)

	val glState = inject(GlState)
	val intR = IntRectangle.obtain()
	glState.getViewport(intR)
	glState.setScissor(
			minOf(sX1, sX2).roundToInt(),
			(intR.height - maxOf(sY1, sY2)).roundToInt(),
			abs(sX2 - sX1).roundToInt(),
			abs(sY2 - sY1).roundToInt(),
			inner
	)
	IntRectangle.free(intR)
}