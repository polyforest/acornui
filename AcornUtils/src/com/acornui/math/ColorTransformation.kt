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

import com.acornui.collection.FloatList
import com.acornui.collection.arrayCopy
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo

// TODO: I'd like to pull this out into a Filters api.

///**
// * Shaders may support a color transformation matrix.
// */
//class ColorTransformation {
//
//	private val _mat = Matrix4()
//	private val _offset = Color()
//
//	val transformValues: FloatList
//		get() = _mat.values
//
//	var offset: Color
//		get() = _offset
//		set(value) {
//			_offset.set(value)
//		}
//
//	fun offset(r: Float = 0f, g: Float = 0f, b: Float = 0f, a: Float = 0f): ColorTransformation {
//		_offset.set(r, g, b, a)
//		return this
//	}
//
//	fun offset(value: ColorRo): ColorTransformation {
//		_offset.set(value)
//		return this
//	}
//
//	/**
//	 * Multiplies the tint by the given color.
//	 */
//	fun mul(value: ColorRo): ColorTransformation {
//		val values = _mat.values
//		values[0] *= value.r
//		values[5] *= value.g
//		values[10] *= value.b
//		values[15] *= value.a
//		return this
//	}
//
//	fun mul(value: ColorTransformation): ColorTransformation {
//		_mat.mul(value._mat)
//		_offset.add(value._offset)
//		return this
//	}
//
//	/**
//	 * Sets the tint to the given color.
//	 */
//	fun tint(value: Color): ColorTransformation {
//		return tint(value.r, value.g, value.b, value.a)
//	}
//
//	/**
//	 * Sets the tint to the given color.
//	 */
//	fun tint(r: Float = 1f, g: Float = 1f, b: Float = 1f, a: Float = 1f): ColorTransformation {
//		val values = _mat.values
//		values[0] = r
//		values[5] = g
//		values[10] = b
//		values[15] = a
//		return this
//	}
//
//	fun idt(): ColorTransformation {
//		_mat.idt()
//		_offset.clear()
//		return this
//	}
//
//	fun set(other: ColorTransformation): ColorTransformation {
//		_mat.set(other._mat)
//		_offset.set(other._offset)
//		return this
//	}
//
//	companion object {
//
//		/**
//		 * Do NOT modify this color transformation, it represents the untransformed matrix and offset.
//		 */
//		val IDENTITY = ColorTransformation()
//	}
//
//}
//
///**
// * Sets this color transformation to a sepia transform.
// */
//fun ColorTransformation.sepia(): ColorTransformation {
//	arrayCopy(floatArrayOf(
//			0.769f, 0.686f, 0.534f, 0.0f,
//			0.393f, 0.349f, 0.272f, 0.0f,
//			0.189f, 0.168f, 0.131f, 0.0f,
//			0.000f, 0.000f, 0.000f, 1.0f
//	), 0, transformValues)
//	offset.clear()
//	return this
//}
//
///**
// * Sets this color transformation to a grayscale transform.
// */
//fun ColorTransformation.grayscale(): ColorTransformation {
//	arrayCopy(floatArrayOf(
//			0.33f, 0.33f, 0.33f, 0.0f,
//			0.59f, 0.59f, 0.59f, 0.0f,
//			0.11f, 0.11f, 0.11f, 0.0f,
//			0.00f, 0.00f, 0.00f, 1.0f
//	), 0, transformValues)
//	offset.clear()
//	return this
//}
//
///**
// * Sets this color transformation to an invert transform.
// */
//fun ColorTransformation.invert(): ColorTransformation {
//	arrayCopy(floatArrayOf(
//			-1.0f, 0.0f, 0.0f, 0.0f,
//			0.0f, -1.0f, 0.0f, 0.0f,
//			0.0f, 0.0f, -1.0f, 0.0f,
//			0.0f, 0.0f, 0.0f, 1.0f
//	), 0, transformValues)
//	offset.set(1.0f, 1.0f, 1.0f, 0.0f)
//	return this
//}