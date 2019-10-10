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

import com.acornui.Renderable
import com.acornui.graphic.yDown
import com.acornui.gl.core.Framebuffer

import com.acornui.math.*

interface CanvasTransformableRo : ModelTransformableRo {

	/**
	 * The transformation of clip coordinates to global coordinates.
	 * The inverse of the combined [projectionTransform] and [viewTransform].
	 */
	val viewProjectionTransformInv: Matrix4Ro

	/**
	 * The transformation of global coordinates to clip coordinates.
	 * The combined [projectionTransform] and [viewTransform].
	 */
	val viewProjectionTransform: Matrix4Ro

	/**
	 * The transformation of global to view coordinates.
	 * View space (or eye space) is what the camera 'sees'.
	 */
	val viewTransform: Matrix4Ro

	/**
	 * The transformation of view space to clip space.
	 * Clip coordinates are in the range of -1 to 1 to determine which vertices will end up on the screen.
	 * -1, -1 will correspond to bottom left for the screen frame buffer, or top left for [Framebuffer] objects.
	 *
	 * @see yDown
	 */
	val projectionTransform: Matrix4Ro

	/**
	 * The affine transformation to convert clip space coordinates to canvas coordinates.
	 * The canvas coordinate space is the same coordinate space as input events.
	 * The top left of the canvas is 0,0 and the bottom right is the canvas width and height, in points, not pixels.
	 */
	val canvasTransform: RectangleRo
}

/**
 * Projects the given in global space to canvas coordinates. The canvas coordinate system has its
 * origin in the top left, with the y-axis pointing downwards and the x-axis pointing to the right.
 */
fun CanvasTransformableRo.globalToCanvas(globalCoords: Vector3): Vector3 {
	val cT = canvasTransform
	viewProjectionTransform.prj(globalCoords) // Global coords become clip coords.
	// Convert clip coords to canvas coords.
	globalCoords.x = cT.width * (globalCoords.x + 1f) * 0.5f + cT.x
	globalCoords.y = cT.height * (-globalCoords.y + 1f) * 0.5f + cT.y
	globalCoords.z = (globalCoords.z + 1f) * 0.5f
	return globalCoords
}

/**
 * Translates a point given in canvas coordinates to global space. The x- and y-coordinate of vec are assumed to be
 * in canvas coordinates (origin is the top left corner, y pointing down, x pointing to the right) as reported by
 * the canvas coordinates in input events. A z-coordinate of 0 will return a point on the near plane, a z-coordinate
 * of 1 will return a point on the far plane.
 * @param canvasCoords the point in canvas coordinates (origin top left). This will be mutated.
 */
fun CanvasTransformableRo.canvasToGlobal(canvasCoords: Vector3): Vector3 {
	val cT = canvasTransform
	canvasCoords.x = 2f * (canvasCoords.x - cT.x) / cT.width - 1f
	canvasCoords.y = -2f * (canvasCoords.y - cT.y) / cT.height + 1f
	canvasCoords.z = 2f * canvasCoords.z - 1f
	viewProjectionTransformInv.prj(canvasCoords)
	return canvasCoords
}


fun CanvasTransformableRo.getPickRay(canvasX: Float, canvasY: Float, out: Ray): Ray {
	canvasToGlobal(out.origin.set(canvasX, canvasY, -1f))
	canvasToGlobal(out.direction.set(canvasX, canvasY, 0f))
	out.direction.sub(out.origin)
	out.update()
	return out
}

private val rayTmp = Ray()

fun CanvasTransformableRo.canvasToLocal(canvasCoord: Vector2): Vector2 {
	globalToLocal(getPickRay(canvasCoord.x, canvasCoord.y, rayTmp))
	rayToPlane(rayTmp, canvasCoord)
	return canvasCoord
}

fun CanvasTransformableRo.localToCanvas(localCoord: Vector3): Vector3 {
	localToGlobal(localCoord)
	globalToCanvas(localCoord)
	return localCoord
}

/**
 * Converts a bounding rectangle from local to canvas coordinates.
 */
fun CanvasTransformableRo.localToCanvas(minMax: MinMax): MinMax {
	val tmp1 =  Vector3.obtain().set(minMax.xMin, minMax.yMin, 0f)
	val tmp2 =  Vector3.obtain().set(minMax.xMax, minMax.yMax, 0f)
	val tmp =  Vector3.obtain()
	minMax.clear()
	localToCanvas(tmp.set(tmp1))
	minMax.ext(tmp.x, tmp.y)
	localToCanvas(tmp.set(tmp2.x, tmp1.y, 0f))
	minMax.ext(tmp.x, tmp.y)
	localToCanvas(tmp.set(tmp2))
	minMax.ext(tmp.x, tmp.y)
	localToCanvas(tmp.set(tmp1.x, tmp2.y, 0f))
	minMax.ext(tmp.x, tmp.y)
	Vector3.free(tmp1)
	Vector3.free(tmp2)
	Vector3.free(tmp)
	return minMax
}

/**
 * Converts a bounding rectangle from canvas to local coordinates.
 * Warning: this does require a matrix inversion calculation, which is a fairly expensive operation.
 */
fun CanvasTransformableRo.canvasToLocal(minMax: MinMax): MinMax {
	val tmp1 =  Vector3.obtain().set(minMax.xMin, minMax.yMin, 0f)
	val tmp2 =  Vector3.obtain().set(minMax.xMax, minMax.yMax, 0f)
	val tmp = Vector2.obtain()
	minMax.clear()
	canvasToLocal(tmp.set(tmp1.x, tmp1.y))
	minMax.ext(tmp.x, tmp.y)
	canvasToLocal(tmp.set(tmp2.x, tmp1.y))
	minMax.ext(tmp.x, tmp.y)
	canvasToLocal(tmp.set(tmp2.x, tmp2.y))
	minMax.ext(tmp.x, tmp.y)
	canvasToLocal(tmp.set(tmp1.x, tmp2.y))
	minMax.ext(tmp.x, tmp.y)
	Vector3.free(tmp1)
	Vector3.free(tmp2)
	Vector3.free(tmp1)
	Vector2.free(tmp)
	return minMax
}

private val minMaxTmp = MinMax()
/**
 * Returns true if the local region intersects with the canvas region.
 */
fun CanvasTransformableRo.localIntersectsCanvas(localRegion: MinMaxRo, canvasRegion: MinMaxRo): Boolean {
	localToCanvas(minMaxTmp.set(localRegion))
	return minMaxTmp.intersects(canvasRegion)
}

/**
 * Returns true if the draw region intersects with the canvas region.
 */
fun CanvasTransformableRo.drawRegionIntersectsCanvas(element: Renderable, canvasRegion: MinMaxRo): Boolean {
	return localIntersectsCanvas(element.drawRegion, canvasRegion)
}

fun UiComponentRo.intersectsClipRegion(): Boolean {
	return localIntersectsCanvas(drawRegion, naturalRenderContext.clipRegion)
}
