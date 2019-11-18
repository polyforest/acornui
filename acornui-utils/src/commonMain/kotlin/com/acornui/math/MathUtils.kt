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

@file:Suppress("NOTHING_TO_INLINE")

package com.acornui.math

import kotlin.jvm.JvmName
import kotlin.math.*
import kotlin.random.Random

const val PI: Float = 3.1415927f
const val PI2: Float = PI * 2f
const val E: Float = 2.7182818f
const val TO_DEG = 180f / PI
const val TO_RAD = PI / 180f

/**
 * Utility and fast math functions.
 * @author Nathan Sweet
 */
@Suppress("NOTHING_TO_INLINE")
object MathUtils {

	const val nanoToSec: Float = 1f / 1000000000f

	// ---
	const val FLOAT_ROUNDING_ERROR: Float = 0.000001f // 32 bits

	/**
	 * multiply by this to convert from radians to degrees
	 */
	const val radDeg: Float = 180f / PI

	/**
	 * multiply by this to convert from degrees to radians
	 */
	const val degRad: Float = PI / 180f

	// ---

	/**
	 * Returns the next power of two. Returns the specified value if the value is already a power of two.
	 */
	fun nextPowerOfTwo(value: Int): Int {
		var v = value
		if (v == 0) return 1
		v--
		v = v or (v shr 1)
		v = v or (v shr 2)
		v = v or (v shr 4)
		v = v or (v shr 8)
		v = v or (v shr 16)
		return v + 1
	}

	fun isPowerOfTwo(value: Int): Boolean {
		return value != 0 && (value and value - 1) == 0
	}

	// ---

	/**
	 * Linearly interpolates between fromValue to toValue on progress position.
	 */
	fun lerp(fromValue: Float, toValue: Float, progress: Float): Float {
		return fromValue + (toValue - fromValue) * progress
	}

	// ---

	/**
	 * Returns true if the value is zero
	 * @param tolerance represent an upper bound below which the value is considered zero.
	 */
	fun isZero(value: Float, tolerance: Float = FLOAT_ROUNDING_ERROR): Boolean {
		return abs(value.toDouble()) <= tolerance
	}

	/**
	 * Returns true if the value is zero
	 * @param tolerance represent an upper bound below which the value is considered zero.
	 */
	fun isZero(value: Double, tolerance: Float = FLOAT_ROUNDING_ERROR): Boolean {
		return abs(value) <= tolerance
	}

	/**
	 * Returns true if a is nearly equal to b. The function uses the default floating error tolerance.
	 * @param a the first value.
	 * @param b the second value.
	 */
	fun isEqual(a: Float, b: Float): Boolean {
		return abs(a - b) <= FLOAT_ROUNDING_ERROR
	}

	/**
	 * Returns true if a is nearly equal to b.
	 * @param a the first value.
	 * @param b the second value.
	 * @param tolerance represent an upper bound below which the two values are considered equal.
	 */
	fun isEqual(a: Float, b: Float, tolerance: Float): Boolean {
		return abs(a - b) <= tolerance
	}

	/**
	 * @return the logarithm of x with base a
	 */
	@Deprecated("Use kotlin.math.log", ReplaceWith("log(x, base)", imports = arrayOf("kotlin.math.log")))
	fun log(x: Float, base: Float): Float {
		return kotlin.math.log(x, base)
	}

	@JvmName("clampN")
	inline fun <T : Comparable<T>> clamp(value: T?, min: T, max: T): T? {
		if (value == null) return null
		if (value <= min) return min
		if (value >= max) return max
		return value
	}

	inline fun <T : Comparable<T>> clamp(value: T, min: T, max: T): T {
		if (value <= min) return min
		if (value >= max) return max
		return value
	}

	@Deprecated("Use minOf", ReplaceWith("minOf(x, y)"))
	inline fun <T : Comparable<T>> min(x: T, y: T): T {
		return minOf(x, y)
	}

	@Deprecated("Use minOf", ReplaceWith("minOf(x, y, z)"))
	inline fun <T : Comparable<T>> min(x: T, y: T, z: T): T {
		return minOf(x, y, z)
	}

	@Deprecated("Use minOf", ReplaceWith("minOf(w, x, minOf(y, z))"))
	inline fun <T : Comparable<T>> min(w: T, x: T, y: T, z: T): T {
		return minOf(w, x, minOf(y, z))
	}

	@Deprecated("Use maxOf", ReplaceWith("maxOf(x, y)"))
	inline fun <T : Comparable<T>> max(x: T, y: T): T {
		return maxOf(x, y)
	}

	@Deprecated("Use maxOf", ReplaceWith("maxOf(x, y, z)"))
	inline fun <T : Comparable<T>> max(x: T, y: T, z: T): T {
		return maxOf(x, y, z)
	}

	@Deprecated("Use maxOf", ReplaceWith("maxOf(w, x, maxOf(y, z))"))
	inline fun <T : Comparable<T>> max(w: T, x: T, y: T, z: T): T {
		return maxOf(w, x, maxOf(y, z))
	}

	// TODO: deprecate what's now in kotlin native math

	/**
	 * Returns the signum function of the argument; zero if the argument
	 * is zero, 1.0f if the argument is greater than zero, -1.0f if the
	 * argument is less than zero.
	 *
	 * <p>Special Cases:
	 * <ul>
	 * <li> If the argument is NaN, then the result is NaN.
	 * <li> If the argument is positive zero or negative zero, then the
	 *      result is the same as the argument.
	 * </ul>
	 *
	 * @param f the floating-point value whose signum is to be returned
	 * @return the signum function of the argument
	 * @author Joseph D. Darcy
	 * @since 1.5
	 */
	fun signum(v: Float): Float {
		if (v > 0) return 1f
		if (v < 0) return -1f
		if (v.isNaN()) return Float.NaN
		return 0f
	}

	/**
	 * n must be positive.
	 * mod( 5f, 3f) produces 2f
	 * mod(-5f, 3f) produces 1f
	 */
	fun mod(a: Float, n: Float): Float {
		return if (a < 0f) (a % n + n) % n else a % n
	}

	/**
	 * n must be positive.
	 * mod( 5, 3) produces 2
	 * mod(-5, 3) produces 1
	 */
	fun mod(a: Int, n: Int): Int {
		return if (a < 0) (a % n + n) % n else a % n
	}

	/**
	 * Finds the difference between two angles.
	 * The returned difference will always be >= -PI and < PI
	 */
	fun angleDiff(a: Float, b: Float): Float {
		var diff = b - a
		if (diff < -PI) diff = PI2 - diff
		if (diff > PI2) diff %= PI2
		if (diff >= PI) diff -= PI2
		return diff
	}

	/**
	 * Given a quadratic equation of the form: y = ax^2 + bx + c, returns the solutions to x where y == 0f
	 * Uses the quadratic formula: x = (-b += sqrt(b^2 - 4ac)) / 2a
	 * @param a The a coefficient.
	 * @param b The b coefficient.
	 * @param c The c coefficient.
	 * @param out The list to populate with the solutions.
	 * @return Returns the x values where y == 0f. This may have 0, 1, or 2 values.
	 */
	fun getQuadraticRoots(a: Float, b: Float, c: Float, out: MutableList<Float>) {
		out.clear()
		if (a == 0f) {
			// Not a quadratic equation.
			if (b == 0f) return
			out.add(-c / b)
		}

		val q = b * b - 4f * a * c
		val signQ = if (q > 0f) 1 else if (q < 0f) -1 else 0

		if (signQ < 0) {
			// No solution
		} else if (signQ == 0) {
			out.add(-b / (2f * a))
		} else {
			val aa = -b / (2f * a)
			val tmp = sqrt(q) / (2f * a)
			out.add(aa - tmp)
			out.add(aa + tmp)
		}
	}

	/**
	 * Given a cubic equation of the form: y = ax^3 + bx^2 + cx + d, returns the solutions to x where y == 0f
	 * @param a The a coefficient.
	 * @param b The b coefficient.
	 * @param c The c coefficient.
	 * @param c The d coefficient.
	 * @param out The list to populate with the solutions.
	 * @return Returns the x values where y == 0f. This may have 0, 1, 2 or 3 values.
	 */
	fun getCubicRoots(a: Float = 0f, b: Float = 0f, c: Float = 0f, d: Float = 0f, out: MutableList<Float>) {
		if (a == 0f) {
			// Not a cubic equation
			return getQuadraticRoots(b, c, d, out)
		}
		out.clear()

		var b = b
		var c = c
		var d = d
		// normalize the coefficients so the cubed term is 1 and we can ignore it hereafter
		if (a != 1f) {
			b /= a
			c /= a
			d /= a
		}

		val q = (b * b - 3f * c) / 9f
		val q3 = q * q * q
		val r = (2f * b * b * b - 9f * b * c + 27f * d) / 54f
		val diff: Float = q3 - r * r
		if (diff >= 0) {
			if (q == 0f) {
				// avoid division by zero
				out.add(0f)
			} else {
				// three real roots
				val theta: Float = acos(r / sqrt(q3))
				val qSqrt: Float = sqrt(q)

				out.add(-2f * qSqrt * cos(theta / 3f) - b / 3f)
				out.add(-2f * qSqrt * cos((theta + 2f * PI) / 3f) - b / 3f)
				out.add(-2f * qSqrt * cos((theta + 4f * PI) / 3f) - b / 3f)
			}
		} else {
			// one real root
			val tmp: Float = (sqrt(-diff) + abs(r)).pow(1f / 3f)
			val rSign = if (r > 0f) 1f else if (r < 0f) -1f else 0f
			out.add(-rSign * (tmp + q / tmp) - b / 3f)
		}
	}

	// TODO: Document

	/**
	 * Snaps a value to the nearest interval.
	 */
	fun roundToNearest(value: Float, snap: Float, offset: Float = 0f): Float {
		if (snap <= 0) return value
		var v = value - offset
		v /= snap
		v = round(v)
		v *= snap
		return v + offset
	}

	fun floorToNearest(value: Float, snap: Float, offset: Float = 0f): Float {
		if (snap <= 0) return value
		var v = value - offset
		v /= snap
		v = floor(v)
		v *= snap
		return v + offset
	}

	fun ceilToNearest(value: Float, snap: Float, offset: Float = 0f): Float {
		if (snap <= 0) return value
		var v = value - offset
		v /= snap
		v = ceil(v)
		v *= snap
		return v + offset
	}

	/**
	 * Round after a small, but obscure offset, to avoid flip-flopping around the common case of 0.5f
 	 */
	inline fun offsetRound(x: Float, offset: Float = -0.0136f): Float {
		return round(x + offset)
	}
}

/**
 * Returns the fraction of this float.
 */
inline fun Float.fpart(): Float {
	return this - floor(this)
}

/**
 * Gets the number of fraction digits for this value.
 * E.g.  if this value is 1f, returns 0, if this value is 3.1f, returns 1.
 * The max return value is 10.
 */
val Float.fractionDigits: Int
	get() {
		var m = 1f
		for (i in 0..10) {
			if ((this * m).fpart() == 0f) {
				return i
			}
			m *= 10f
		}
		return 10
	}


inline fun <T : Comparable<T>> maxOf4(a: T, b: T, c: T, d: T): T {
	return maxOf(maxOf(a, b), maxOf(c, d))
}

inline fun <T : Comparable<T>> minOf4(a: T, b: T, c: T, d: T): T {
	return minOf(minOf(a, b), minOf(c, d))
}

fun Random.nextFloat(until: Float): Float = nextDouble(until.toDouble()).toFloat()
fun Random.nextFloat(from: Float, until: Float): Float = nextDouble(from.toDouble(), until.toDouble()).toFloat()
