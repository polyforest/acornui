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

package com.acornui.component.layout

import com.acornui.component.ModelTransformableRo
import com.acornui.math.MathUtils
import com.acornui.math.Matrix4Ro
import com.acornui.math.Vector2Ro
import com.acornui.math.Vector3Ro

interface TransformableRo : PositionableRo, ModelTransformableRo {

	/**
	 * This component's local transformation matrix.
	 * Responsible for 3d positioning, scaling, rotation, etc.
	 */
	val transform: Matrix4Ro

	/**
	 * If this is not null, this custom transformation matrix will be used. Note that if this is set, all properties
	 * that would otherwise generate the transformation matrix are no longer applicable.
	 */
	val customTransform: Matrix4Ro?

	val rotationX: Float

	val rotationY: Float

	/**
	 * Rotation around the Z axis
	 */
	val rotation: Float

	val scaleX: Float

	val scaleY: Float

	val scaleZ: Float

	val originX: Float

	val originY: Float

	val originZ: Float

}

/**
 * The API for reading and modifying a component's 3d transformation.
 * @author nbilyk
 */
interface Transformable : TransformableRo, Positionable {

	override var customTransform: Matrix4Ro?

	override var rotationX: Float

	override var rotationY: Float

	/**
	 * Rotation around the Z axis
	 */
	override var rotation: Float

	fun setRotation(x: Float, y: Float, z: Float)

	//---------------------------------------------------------------------------------------
	// Transformation and translation methods
	//---------------------------------------------------------------------------------------

	override var scaleX: Float

	override var scaleY: Float

	override var scaleZ: Float

	fun setScaling(x: Float, y: Float, z: Float = 1f)

	override var originX: Float

	override var originY: Float

	override var originZ: Float

	fun setOrigin(x: Float, y: Float, z: Float = 0f)

}

interface PositionableRo {

	val x: Float
	val y: Float
	val z: Float

	val position: Vector3Ro
}

interface Positionable : PositionableRo {

	override var x: Float
	override var y: Float
	override var z: Float

	/**
	 * If true, then [moveTo] will snap the position to the nearest pixel.
	 */
	val snapToPixel: Boolean

	/**
	 * Sets the position of this component, and if [snapToPixel] is true,
	 * The x and y coordinates will be rounded to the nearest pixel.
	 * The rounding by default will use [MathUtils.offsetRound].
	 */
	fun moveTo(x: Float, y: Float, z: Float = 0f) {
		if (snapToPixel)
			setPosition(MathUtils.offsetRound(x), MathUtils.offsetRound(y), z)
		else
			setPosition(x, y, z)
	}

	/**
	 * Sets the position of this component. (Without rounding)
	 */
	fun setPosition(x: Float, y: Float, z: Float = 0f)

	companion object {

		/**
		 * Transformable components will use this value to set their initial [Transformable.snapToPixel] state.
		 */
		var defaultSnapToPixel = true
	}

}

fun Positionable.moveTo(value: Vector3Ro) = moveTo(value.x, value.y, value.z)
fun Positionable.moveTo(value: Vector2Ro) = moveTo(value.x, value.y)
fun Positionable.setPosition(value: Vector3Ro) = setPosition(value.x, value.y, value.z)
