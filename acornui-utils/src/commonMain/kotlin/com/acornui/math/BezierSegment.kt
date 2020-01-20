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

package com.acornui.math

/**
 */
class BezierSegment(
		val aX: Float,
		val aY: Float,
		val bX: Float,
		val bY: Float,
		val cX: Float,
		val cY: Float,
		val dX: Float,
		val dY: Float
) {

	constructor(source: FloatArray,
				index: Int) : this(
			aX = source[index],
			aY = source[index + 1],
			bX = source[index + 2],
			bY = source[index + 3],
			cX = source[index + 4],
			cY = source[index + 5],
			dX = source[index + 6],
			dY = source[index + 7]
	)

	private val coeffA: Float = -aX + 3f * bX - 3f * cX + dX
	private val coeffB: Float = 3f * aX - 6f * bX + 3f * cX
	private val coeffC: Float = -3f * aX + 3f * bX
	private val coeffD: Float = aX
	private val roots = arrayListOf<Float>()

	/**
	 */
	fun getValue(t: Float, out: Vector2) {
		val ax = aX
		val x = (t * t * (dX - ax) + 3f * (1f - t) * (t * (cX - ax) + (1f - t) * (bX - ax))) * t + ax
		val ay = aY
		val y = (t * t * (dY - ay) + 3f * (1f - t) * (t * (cY - ay) + (1f - t) * (bY - ay))) * t + ay
		out.set(x, y)
	}


	/**
	 */
	fun getY(x: Float): Float {
		if (aX < dX) {
			if (x <= aX + 0.001f) return aY
			if (x >= dX - 0.001f) return dY
		} else {
			if (x >= aX + 0.001f) return aY
			if (x <= dX - 0.001f) return dY
		}

		MathUtils.getCubicRoots(coeffA, coeffB, coeffC, coeffD - x, roots)
		var time: Float? = null
		if (roots.isEmpty())
			time = 0f
		else if (roots.size == 1)
			time = roots[0]
		else {
			for (i in 0..roots.lastIndex) {
				val root = roots[i]
				if (root > -0.01f && root < 1.01f) {
					time = root
					break
				}
			}
		}

		if (time == null)
			return 0f // Cubic root within range not found.

		val y: Float = getSingleValue(time, aY, bY, cY, dY)
		return y
	}


	companion object {

		private fun getSingleValue(t: Float, a: Float, b: Float, c: Float, d: Float): Float {
			return (t * t * (d - a) + 3f * (1f - t) * (t * (c - a) + (1f - t) * (b - a))) * t + a
		}

	}
}
