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

import com.acornui.gl.core.Framebuffer
import com.acornui.gl.core.Uniforms
import com.acornui.gl.core.setCamera
import com.acornui.gl.core.useCamera
import com.acornui.graphic.yDown
import com.acornui.math.*

interface CameraTransformableRo {

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

interface CanvasTransformableRo : CameraTransformableRo, ModelTransformableRo {
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

private val originTmp = Vector3()
private val directionTmp = Vector3()

fun CanvasTransformableRo.getPickRay(canvasX: Float, canvasY: Float, out: Ray): Ray {
	canvasToGlobal(originTmp.set(canvasX, canvasY, -1f))
	canvasToGlobal(directionTmp.set(canvasX, canvasY, 0f))
	directionTmp.sub(originTmp)
	out.set(originTmp, directionTmp)
	return out
}

private val rayTmp = Ray()

/**
 * Converts a canvas coordinate to a local coordinate.
 */
fun CanvasTransformableRo.canvasToLocal(canvasCoord: Vector2): Vector2 {
	globalToLocal(getPickRay(canvasCoord.x, canvasCoord.y, rayTmp))
	rayToPlane(rayTmp, canvasCoord)
	return canvasCoord
}

/**
 * Converts a local coordinate to a canvas coordinate.
 * @see localToGlobal
 * @see globalToCanvas
 */
fun CanvasTransformableRo.localToCanvas(localCoord: Vector3): Vector3 {
	localToGlobal(localCoord)
	globalToCanvas(localCoord)
	return localCoord
}

private val tmp1 = Vector3()
private val tmp2 = Vector3()
private val tmp3 = Vector3()

/**
 * Converts a bounding rectangle from local to canvas coordinates.
 */
fun CanvasTransformableRo.localToCanvas(minMax: MinMax): MinMax {
	val minTmp =  tmp1.set(minMax.xMin, minMax.yMin, 0f)
	val maxTmp =  tmp2.set(minMax.xMax, minMax.yMax, 0f)
	minMax.clear()
	localToCanvas(tmp3.set(minTmp))
	minMax.ext(tmp3.x, tmp3.y)
	localToCanvas(tmp3.set(maxTmp.x, minTmp.y, 0f))
	minMax.ext(tmp3.x, tmp3.y)
	localToCanvas(tmp3.set(maxTmp))
	minMax.ext(tmp3.x, tmp3.y)
	localToCanvas(tmp3.set(minTmp.x, maxTmp.y, 0f))
	minMax.ext(tmp3.x, tmp3.y)
	return minMax
}

private val tmpVec3 = Vector3()
private val tmpBox = Box()

/**
 * Converts a bounding box from local to canvas coordinates.
 * @return Returns the [localBox] after conversion.
 */
fun CanvasTransformableRo.localToCanvas(localBox: Box): Box {
	val v = tmpVec3
	tmpBox.inf()
	tmpBox.ext(localToCanvas(localBox.getCorner000(v)))
	tmpBox.ext(localToCanvas(localBox.getCorner100(v)))
	tmpBox.ext(localToCanvas(localBox.getCorner110(v)))
	tmpBox.ext(localToCanvas(localBox.getCorner010(v)))
	if (localBox.depth != 0f) {
		tmpBox.ext(localToCanvas(localBox.getCorner001(v)))
		tmpBox.ext(localToCanvas(localBox.getCorner101(v)))
		tmpBox.ext(localToCanvas(localBox.getCorner111(v)))
		tmpBox.ext(localToCanvas(localBox.getCorner011(v)))
	}
	localBox.set(tmpBox)
	return localBox
}

/**
 * Converts a bounding rectangle from local to canvas coordinates.
 * @return Returns the [localRect] after conversion.
 */
fun CanvasTransformableRo.localToCanvas(localRect: Rectangle): Rectangle {
	val v = tmpVec3
	tmpBox.inf()
	tmpBox.ext(localToCanvas(v.set(localRect.x, localRect.y, 0f)))
	tmpBox.ext(localToCanvas(v.set(localRect.right, localRect.y, 0f)))
	tmpBox.ext(localToCanvas(v.set(localRect.right, localRect.bottom, 0f)))
	tmpBox.ext(localToCanvas(v.set(localRect.x, localRect.bottom, 0f)))
	localRect.set(tmpBox)
	return localRect
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

/**
 * Returns true if the camera and viewport match.
 */
fun CanvasTransformableRo.cameraEquals(renderContext: CanvasTransformableRo): Boolean {
	return viewProjectionTransform == renderContext.viewProjectionTransform && canvasTransform == renderContext.canvasTransform
}

/**
 * Sets the camera uniforms using the given [component].
 */
fun Uniforms.setCamera(component: CanvasTransformableRo, useModel: Boolean = false) {
	if (useModel) setCamera(component.viewProjectionTransform, component.viewTransform, component.transformGlobal)
	else setCamera(component.viewProjectionTransform, component.viewTransform, Matrix4.IDENTITY)
}

/**
 * Sets the camera uniforms using the given [component], calls [inner], then sets the camera back to what it
 * previously was.
 */
fun Uniforms.useCamera(component: CanvasTransformableRo, useModel: Boolean = false, inner: () -> Unit) {
	if (useModel) useCamera(component.viewProjectionTransform, component.viewTransform, component.transformGlobal, inner)
	else useCamera(component.viewProjectionTransform, component.viewTransform, Matrix4.IDENTITY, inner)
}