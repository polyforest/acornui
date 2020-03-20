/*
 * Copyright 2020 Poly Forest, LLC
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

import com.acornui.gl.core.Uniforms
import com.acornui.gl.core.setCamera
import com.acornui.gl.core.useCamera
import com.acornui.math.*

interface CanvasTransformableRo : CameraTransformableRo, ModelTransformableRo {
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
 * Sets the camera uniforms using the given [component].
 */
fun Uniforms.setCamera(component: CanvasTransformableRo, useModel: Boolean = false) {
	if (useModel) setCamera(component.viewProjectionTransform, component.viewTransform, component.transformGlobal)
	else setCamera(component.viewProjectionTransform, component.viewTransform, Matrix4.IDENTITY)
}

/**
 * Sets the camera uniforms using the given [component], calls [inner], then sets the camera back to what it
 * previously was.
 *
 * @param component The component whose view projection, view, and model transforms to use.
 * @param useModel If true, the vertices are expected to be in local space and [CanvasTransformableRo.transformGlobal]
 * will be set as the model matrix on the shader. If the shader doesn't have a uniform for
 * [com.acornui.gl.core.CommonShaderUniforms.U_MODEL_TRANS], the transform global matrix will be multiplied into
 * [com.acornui.gl.core.CommonShaderUniforms.U_PROJ_TRANS]
 */
fun Uniforms.useCamera(component: CanvasTransformableRo, useModel: Boolean = false, inner: () -> Unit) {
	if (useModel) useCamera(component.viewProjectionTransform, component.viewTransform, component.transformGlobal, inner)
	else useCamera(component.viewProjectionTransform, component.viewTransform, Matrix4.IDENTITY, inner)
}