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

package com.acornui.component.drawing

import com.acornui._assert
import com.acornui.gl.core.*
import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo
import com.acornui.math.Vector2
import com.acornui.math.Vector2Ro

private val v1 = Vector2()
private val v2 = Vector2()
private val v3 = Vector2()
private val v4 = Vector2()


fun ShaderBatch.triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, colorTint: ColorRo = Color.WHITE) {
	v1.set(x1, y1)
	v2.set(x2, y2)
	v3.set(x3, y3)
	triangle(v1, v2, v3, colorTint)
}

fun ShaderBatch.triangle(v1: Vector2Ro, v2: Vector2Ro, v3: Vector2Ro, fillColor: ColorRo = Color.WHITE) {
	putVertex(v1, colorTint = fillColor)
	putVertex(v2, colorTint = fillColor)
	putVertex(v3, colorTint = fillColor)
	putTriangleIndices()
}


fun ShaderBatch.rect(x: Float, y: Float, width: Float, height: Float, colorTint: ColorRo = Color.WHITE) {
	val x2 = x + width
	val y2 = y + height
	putVertex(x, y, 0f, colorTint = colorTint)
	putVertex(x2, y, 0f, colorTint = colorTint)
	putVertex(x2, y2, 0f, colorTint = colorTint)
	putVertex(x, y2, 0f, colorTint = colorTint)
	putQuadIndices()
}

/**
 * Adds the vertices and indices for a quadrilateral to this batch.
 */
fun ShaderBatch.quad(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, fillColor: ColorRo = Color.WHITE) {
	putVertex(x1, y1, 0f, colorTint = fillColor)
	putVertex(x2, y2, 0f, colorTint = fillColor)
	putVertex(x3, y3, 0f, colorTint = fillColor)
	putVertex(x4, y4, 0f, colorTint = fillColor)
	putQuadIndices()
}

/**
 * Adds the vertices and indices for a quadrilateral to this batch.
 */
fun ShaderBatch.quad(v1: Vector2Ro, v2: Vector2Ro, v3: Vector2Ro, v4: Vector2Ro, fillColor: ColorRo = Color.WHITE) {
	putVertex(v1, colorTint = fillColor)
	putVertex(v2, colorTint = fillColor)
	putVertex(v3, colorTint = fillColor)
	putVertex(v4, colorTint = fillColor)
	putQuadIndices()
}

fun ShaderBatch.line(x1: Float, y1: Float, x2: Float, y2: Float, lineStyle: LineStyleRo = LineStyle(), controlA: Vector2Ro? = null, controlB: Vector2Ro? = null, controlAThickness: Float = lineStyle.thickness, controlBThickness: Float = lineStyle.thickness, init: ShaderBatch.() -> Unit = {}) {
	val p1 = Vector2.obtain().set(x1, y1)
	val p2 = Vector2.obtain().set(x2, y2)

	line(p1, p2, lineStyle, controlA, controlB, controlAThickness, controlBThickness, init)
	Vector2.free(p1)
	Vector2.free(p2)
}

fun ShaderBatch.line(p1: Vector2Ro, p2: Vector2Ro, lineStyle: LineStyleRo = LineStyle(), controlA: Vector2Ro? = null, controlB: Vector2Ro? = null, controlAThickness: Float = lineStyle.thickness, controlBThickness: Float = lineStyle.thickness, init: ShaderBatch.() -> Unit = {}) {
	val startVertexPosition = vertexComponentsCount
	val capBuilder = CapStyle.getCapBuilder(lineStyle.capStyle)
			?: throw Exception("No cap builder defined for: ${lineStyle.capStyle}")
	capBuilder.createCap(p1, p2, controlA, this, lineStyle, controlAThickness, clockwise = true)
	val vertexSize = vertexAttributes.vertexSize
	val indexA = vertexComponentsCount / vertexSize - 2
	_assert(indexA >= startVertexPosition / vertexSize, "Cap builder did not create at least two vertices")
	val indexB = indexA + 1
	capBuilder.createCap(p2, p1, controlB, this, lineStyle, controlBThickness, clockwise = false)
	val indexC = vertexComponentsCount / vertexSize - 2
	_assert(indexC >= indexA + 2, "Cap builder did not create at least two vertices")
	val indexD = indexC + 1

	// Span a rectangle from the ends the caps create.
	putIndex(indexA)
	putIndex(indexC)
	putIndex(indexB)
	putIndex(indexB)
	putIndex(indexC)
	putIndex(indexD)

	init()
}

fun ShaderBatch.triangleLine(v1: Vector2Ro, v2: Vector2Ro, v3: Vector2Ro, lineStyle: LineStyleRo) {
	line(v1, v2, lineStyle, v3, v3)
	line(v2, v3, lineStyle, v1, v1)
	line(v3, v1, lineStyle, v2, v2)
}

fun ShaderBatch.quadLine(v1: Vector2Ro, v2: Vector2Ro, v3: Vector2Ro, v4: Vector2Ro, lineStyle: LineStyleRo) {
	line(v1, v2, lineStyle, v4, v3)
	line(v2, v3, lineStyle, v1, v4)
	line(v3, v4, lineStyle, v2, v1)
	line(v4, v1, lineStyle, v3, v2)
}

fun ShaderBatch.putIdtQuad() {
	putVertex(-1f, -1f, 0f)
	putVertex(1f, -1f, 0f)
	putVertex(1f, 1f, 0f)
	putVertex(-1f, 1f, 0f)
	putQuadIndices()
}
