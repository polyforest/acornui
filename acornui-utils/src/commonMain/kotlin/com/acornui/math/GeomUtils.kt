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
 * Contains a few intersection calculation utilities.
 */
object GeomUtils {


	fun intersectPointTriangle(pt: Vector2, v1: Vector2, v2: Vector2, v3: Vector2): Boolean {
		val b1 = sign(pt, v1, v2) < 0.0f
		val b2 = sign(pt, v2, v3) < 0.0f
		val b3 = sign(pt, v3, v1) < 0.0f
		return (b1 == b2) && (b2 == b3)
	}

	private fun sign(v1: Vector2, v2: Vector2, v3: Vector2): Float {
		return (v1.x - v3.x) * (v2.y - v3.y) - (v2.x - v3.x) * (v1.y - v3.y)
	}

	/**
	 * To find orientation of ordered triplet (p, q, r).
	 * The function returns following values
	 * 0 --> p, q and r are collinear
	 * 1 --> Clockwise
	 * 2 --> Counter-clockwise
	 */
	private fun orientation(p: Vector2, q: Vector2, r: Vector2): Int {
		val i = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)
		if (i == 0f) return 0  // collinear

		return if (i > 0) 1 else 2 // clock or counter-clock wise
	}

	/**
	 * Calculates the closest point to [x], [y] that lies on the edge ([aX], [aY]) -> ([bX], [bY]).
	 */
	fun getClosestPointToEdge(x: Float, y: Float, aX: Float, aY: Float, bX: Float, bY: Float, out: Vector2) {
		val aBx = bX - aX
		val aBy = bY - aY
		val aPx = aX - x
		val aPy = aY - y

		val abAb = aBx * aBx + aBy * aBy
		val abAp = aPx * aBx + aPy * aBy

		val t = MathUtils.clamp(abAp / abAb, 0f, 1f)

		out.set(aBx, aBy).scl(t).add(x, y)
	}
}
