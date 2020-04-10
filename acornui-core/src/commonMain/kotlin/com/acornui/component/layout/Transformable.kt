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
	 * that would otherwise generate the local transformation matrix are no longer applicable.
	 */
	val transformOverride: Matrix4Ro?

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

	/**
	 * Sets the custom local transformation.
	 */
	override var transformOverride: Matrix4Ro?

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
	 * If true, then [size] and [position] will be snapped to the nearest pixel.
	 */
	val snapToPixel: Boolean

	@Deprecated("use setPosition", ReplaceWith("position(x, y, z)"), DeprecationLevel.ERROR)
	fun moveTo(x: Float, y: Float, z: Float = 0f) = position(x, y, z)

	/**
	 * Sets the position of this component, and if [snapToPixel] is true, the x and y coordinates will be rounded to
	 * the nearest pixel (accounting for pixel densities).
	 * This helps prevent texture blurring from placement on a fraction of a pixel.
	 *
	 * Pixel snapping is only guaranteed if all ancestors are both pixel snapped and have translation-only
	 * transformations.
	 */
	fun position(x: Float, y: Float, z: Float = 0f)

	@Deprecated("Use position", ReplaceWith("position(x, y, z)"))
	fun setPosition(x: Float, y: Float, z: Float = 0f) = position(x, y, z)

	companion object {

		/**
		 * Transformable components will use this value to set their initial [Transformable.snapToPixel] state.
		 */
		var defaultSnapToPixel = true
	}

}

@Deprecated("use setPosition", ReplaceWith("position(value)"))
fun Positionable.moveTo(value: Vector3Ro) = position(value.x, value.y, value.z)

@Deprecated("use setPosition", ReplaceWith("position(value)"))
fun Positionable.moveTo(value: Vector2Ro) = position(value.x, value.y )

@Deprecated("Use position", ReplaceWith("position(value)"))
fun Positionable.setPosition(value: Vector3Ro) = position(value.x, value.y, value.z)

fun Positionable.position(value: Vector3Ro) = position(value.x, value.y, value.z)

fun Positionable.position(value: Vector2Ro) = position(value.x, value.y, 0f)
