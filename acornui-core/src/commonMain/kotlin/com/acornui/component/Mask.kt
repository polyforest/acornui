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

package com.acornui.component

import com.acornui.di.inject
import com.acornui.gl.core.*
import com.acornui.graphic.Window
import com.acornui.math.IntRectangle
import com.acornui.math.Vector3
import kotlin.math.abs
import kotlin.math.roundToInt

object StencilUtil {

	var depth = 0 // Start at depth 0 instead of -1 because the stage reserves bit 1.
	private var maxDepth = -1

	fun mask(batch: ShaderBatch, gl: Gl20, renderMask: () -> Unit, renderContents: () -> Unit) {
		batch.flush()
		depth++
		if (maxDepth == -1) {
			val bitPlanes = gl.getParameteri(Gl20.STENCIL_BITS)
			maxDepth = 1 shl bitPlanes - 1
		}
		if (depth >= maxDepth) throw IllegalStateException("There may not be more than $maxDepth nested masks.")
		gl.colorMask(false, false, false, false)
		gl.stencilFunc(Gl20.ALWAYS, 0, -1)
		gl.stencilOp(Gl20.INCR, Gl20.INCR, Gl20.INCR)
		renderMask()
		batch.flush()

		updateMask(gl)
		renderContents()

		batch.flush()
		gl.colorMask(false, false, false, false)
		gl.stencilFunc(Gl20.ALWAYS, 0, -1)
		gl.stencilOp(Gl20.DECR, Gl20.DECR, Gl20.DECR)
		renderMask()
		batch.flush()

		depth--
		updateMask(gl)
	}

	private fun updateMask(gl: Gl20) {
		gl.colorMask(true, true, true, true)
		gl.stencilFunc(Gl20.EQUAL, depth + 1, -1)
		gl.stencilOp(Gl20.KEEP, Gl20.KEEP, Gl20.KEEP)
	}
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
	val tmp = Vector3()
	localToCanvas(tmp.set(x, y, 0f))
	val sX1 = tmp.x
	val sY1 = tmp.y
	localToCanvas(tmp.set(width, height, 0f))
	val sX2 = tmp.x
	val sY2 = tmp.y

	val gl = inject(CachedGl20)
	val window = inject(Window)
	val viewport = gl.getParameteriv(Gl20.VIEWPORT, IntArray(4))

	val sX = window.scaleX
	val sY = window.scaleY
	gl.useScissor(
			(minOf(sX1, sX2) * sX).roundToInt(),
			(viewport[3] - maxOf(sY1, sY2) * sY).roundToInt(),
			(abs(sX2 - sX1) * sX).roundToInt(),
			(abs(sY2 - sY1) * sY).roundToInt(),
			inner
	)
}
