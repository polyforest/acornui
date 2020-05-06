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

package com.acornui.gl.core

import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.graphic.TextureRo
import com.acornui.math.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

fun Gl20.scissor(x: Float, y: Float, width: Float, height: Float) {
	scissor(round(x).toInt(), round(y).toInt(), round(width).toInt(), round(height).toInt())
}

/**
 * Clears the current frame buffer with the given color and mask, then resets the clear color to the Window's clear
 * color.
 */
fun Gl20.clearAndReset(color: ColorRo = Color.CLEAR, stencil: Int = 0, depth: Float = 1f, mask: Int = Gl20.COLOR_BUFFER_BIT or Gl20.DEPTH_BUFFER_BIT or Gl20.STENCIL_BUFFER_BIT) {
	if (mask == 0) return
	val previousColor = getParameterfv(Gl20.COLOR_CLEAR_VALUE, FloatArray(4))
	val previousStencil = getParameteri(Gl20.STENCIL_CLEAR_VALUE)
	val previousDepth = getParameterf(Gl20.DEPTH_CLEAR_VALUE)
	clearColor(color)
	clearStencil(stencil)
	clearDepth(depth)
	clear(mask)
	clearColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3])
	clearStencil(previousStencil)
	clearDepth(previousDepth)
}

/**
 * Temporarily uses a scissor rectangle, resetting to the old scissor rectangle after [inner].
 * @see Gl20.scissor
 */
inline fun Gl20.useScissor(x: Int, y: Int, width: Int, height: Int, inner: () -> Unit) {
	val oldScissor = getParameteriv(Gl20.SCISSOR_BOX, IntArray(4))
	val oldEnabled = isEnabled(Gl20.SCISSOR_TEST)
	enable(Gl20.SCISSOR_TEST)
	scissor(x, y, width, height)
	inner()
	if (!oldEnabled) disable(Gl20.SCISSOR_TEST)
	scissor(oldScissor[0], oldScissor[1], oldScissor[2], oldScissor[3])
}

/**
 * Temporarily uses a scissor region, resetting to the old scissor after [inner].
 */
inline fun Gl20.useScissor(region: IntRectangleRo, inner: () -> Unit) = useScissor(region.x, region.y, region.width, region.height, inner)

/**
 * Temporarily uses a viewport, resetting to the old viewport after [inner].
 * @see Gl20.viewport
 */
inline fun Gl20.useViewport(x: Int, y: Int, width: Int, height: Int, inner: () -> Unit) {
	val oldViewport = getParameteriv(Gl20.VIEWPORT, IntArray(4))
	viewport(x, y, width, height)
	inner()
	viewport(oldViewport)
}

inline fun Gl20.useViewport(region: IntRectangleRo, inner: () -> Unit) = useViewport(region.x, region.y, region.width, region.height, inner)

private val viewportTmp = IntArray(4)

/**
 * Temporarily uses a viewport rectangle, resetting to the old viewport after [inner].
 * @see Gl20.viewport
 */
fun Gl20.useViewport(viewport: RectangleRo, scaleX: Float, scaleY: Float, inner: () -> Unit) {
	getParameteriv(Gl20.VIEWPORT, viewportTmp)
	useViewport(
			floor(viewport.x * scaleX).toInt(),
			floor((viewportTmp[3] - viewport.bottom * scaleY)).toInt(),
			ceil(viewport.width * scaleX).toInt(),
			ceil(viewport.height * scaleY).toInt(),
			inner
	)
}

/**
 * Temporarily uses a shader, resetting to the old shader after [inner].
 */
inline fun CachedGl20.useProgram(program: GlProgramRef, inner: () -> Unit) {
	val previousProgram = this.program
	useProgram(program)
	inner()
	useProgram(previousProgram)
}

/**
 * Temporarily uses a batch, resetting to the old batch after [inner].
 */
inline fun CachedGl20.useBatch(b: ShaderBatch, inner: () -> Unit) {
	val previousBatch = this.batch
	batch = b
	inner()
	batch = previousBatch
}

private val tmpIntArray4 = IntArray(4)

fun Gl20.getViewport(out: IntRectangle = IntRectangle()): IntRectangle = out.set(getParameteriv(Gl20.VIEWPORT, tmpIntArray4))
fun Gl20.viewport(value: IntArray) = viewport(value[0], value[1], value[2], value[3])
fun Gl20.viewport(value: IntRectangleRo) = viewport(value.x, value.y, value.width, value.height)

fun Gl20.getScissor(out: IntRectangle = IntRectangle()): IntRectangle = out.set(getParameteriv(Gl20.SCISSOR_BOX, tmpIntArray4))
fun Gl20.scissor(value: IntArray) = scissor(value[0], value[1], value[2], value[3])
fun Gl20.scissor(value: IntRectangleRo) = scissor(value.x, value.y, value.width, value.height)

fun Gl20.bindTexture(texture: TextureRo, unit: Int = 0) {
	activeTexture(Gl20.TEXTURE0 + unit)
	bindTexture(texture.target.value, texture.textureHandle)
}