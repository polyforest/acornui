/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.math

import com.acornui.graphic.Color
import com.acornui.graphic.ColorRo

interface ColorTransformationRo {

	/**
	 * Returns true if this color transformation doesn't modify a color and therefore can be no-oped.
	 */
	val isIdentity: Boolean
		get() = matrix.mode == MatrixMode.IDENTITY && offset == Color.CLEAR

	val matrix: Matrix4Ro
	val offset: ColorRo
}

/**
 * Shaders may support a color transformation matrix.
 */
class ColorTransformation : ColorTransformationRo {

	private val _matrix = Matrix4()
	private val _offset = Color()

	override var matrix: Matrix4Ro
		get() = _matrix
		set(value) {
			_matrix.set(value)
		}

	override var offset: ColorRo
		get() = _offset
		set(value) {
			_offset.set(value)
		}

	fun offset(r: Float = 0f, g: Float = 0f, b: Float = 0f, a: Float = 0f): ColorTransformation {
		_offset.set(r, g, b, a)
		return this
	}

	/**
	 * Sets the transformation matrix values.
	 */
	fun setTransformValues(values: FloatArray) {
		_matrix.set(values)
	}

	/**
	 * Sets the transformation matrix values.
	 */
	fun setTransformValues(values: List<Float>) {
		_matrix.set(values)
	}

	/**
	 * Multiplies the tint by the given color.
	 */
	fun mul(value: ColorRo): ColorTransformation = mul(value.r, value.g, value.b, value.a)

	/**
	 * Multiplies the tint by the given color.
	 */
	fun mul(r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f): ColorTransformation {
		_matrix[Matrix4.M00] *= r
		_matrix[Matrix4.M11] *= g
		_matrix[Matrix4.M22] *= b
		_matrix[Matrix4.M33] *= a
		return this
	}

	fun mul(value: ColorTransformationRo): ColorTransformation {
		_matrix.mul(value.matrix)
		_offset.add(value.offset)
		return this
	}

	/**
	 * Sets the tint to the given color.
	 */
	fun tint(value: ColorRo): ColorTransformation = tint(value.r, value.g, value.b, value.a)

	/**
	 * Sets the tint to the given color.
	 */
	fun tint(r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f): ColorTransformation {
		_matrix[Matrix4.M00] = r
		_matrix[Matrix4.M11] = g
		_matrix[Matrix4.M22] = b
		_matrix[Matrix4.M33] = a
		return this
	}

	fun idt(): ColorTransformation {
		_matrix.idt()
		_offset.clear()
		return this
	}

	fun set(other: ColorTransformationRo): ColorTransformation {
		_matrix.set(other.matrix)
		_offset.set(other.offset)
		return this
	}

	companion object {

		val IDENTITY: ColorTransformationRo = ColorTransformation()
	}

}

/**
 * Sets this color transformation to a sepia transform.
 */
fun ColorTransformation.sepia(): ColorTransformation {
	setTransformValues(floatArrayOf(
			0.769f, 0.686f, 0.534f, 0.0f,
			0.393f, 0.349f, 0.272f, 0.0f,
			0.189f, 0.168f, 0.131f, 0.0f,
			0.000f, 0.000f, 0.000f, 1.0f
	))
	offset = Color.CLEAR
	return this
}

/**
 * Sets this color transformation to a grayscale transform.
 */
fun ColorTransformation.grayscale(): ColorTransformation {
	setTransformValues(floatArrayOf(
			0.33f, 0.33f, 0.33f, 0.0f,
			0.59f, 0.59f, 0.59f, 0.0f,
			0.11f, 0.11f, 0.11f, 0.0f,
			0.00f, 0.00f, 0.00f, 1.0f
	))
	offset = Color.CLEAR
	return this
}

/**
 * Sets this color transformation to an invert transform.
 */
fun ColorTransformation.invert(): ColorTransformation {
	setTransformValues(floatArrayOf(
			-1.0f, 0.0f, 0.0f, 0.0f,
			0.0f, -1.0f, 0.0f, 0.0f,
			0.0f, 0.0f, -1.0f, 0.0f,
			0.0f, 0.0f, 0.0f, 1.0f
	))
	offset = Color(1.0f, 1.0f, 1.0f, 0.0f)
	return this
}

fun colorTransformation(init: ColorTransformation.() -> Unit = {}): ColorTransformation {
	val c = ColorTransformation()
	c.init()
	return c
}