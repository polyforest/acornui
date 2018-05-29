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

/**
 */
class BezierSegment(

		val a: Vector2Ro,

		val b: Vector2Ro,

		val c: Vector2Ro,

		val d: Vector2Ro

) {

	private val coeffA: Float
	private val coeffB: Float
	private val coeffC: Float
	private val coeffD: Float
	private val roots = arrayListOf<Float>()

	init {
		coeffA = -a.x + 3f * b.x - 3f * c.x + d.x
		coeffB = 3f * a.x - 6f * b.x + 3f * c.x
		coeffC = -3f * a.x + 3f * b.x
		coeffD = a.x
	}

	/**
	 */
	fun getValue(t: Float, out: Vector2) {
		val ax = a.x
		val x = (t * t * (d.x - ax) + 3f * (1f - t) * (t * (c.x - ax) + (1f - t) * (b.x - ax))) * t + ax
		val ay = a.y
		val y = (t * t * (d.y - ay) + 3f * (1f - t) * (t * (c.y - ay) + (1f - t) * (b.y - ay))) * t + ay
		out.set(x, y)
	}


	/**
	 */
	fun getY(x: Float): Float {
		if (a.x < d.x) {
			if (x <= a.x + 0.001f) return a.y
			if (x >= d.x - 0.001f) return d.y
		} else {
			if (x >= a.x + 0.001f) return a.y
			if (x <= d.x - 0.001f) return d.y
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

		val y: Float = getSingleValue(time, a.y, b.y, c.y, d.y)
		return y
	}


	companion object {

		private fun getSingleValue(t: Float, a: Float, b: Float, c: Float, d: Float): Float {
			return (t * t * (d - a) + 3f * (1f - t) * (t * (c - a) + (1f - t) * (b - a))) * t + a
		}

	}
}