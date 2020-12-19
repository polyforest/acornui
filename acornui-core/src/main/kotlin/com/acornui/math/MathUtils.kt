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

import kotlin.js.JsName
import kotlin.math.*
import kotlin.random.Random

const val PI = 3.1415927
const val PI2 = PI * 2.0
const val E = 2.7182818

/**
 * multiply by this to convert from radians to degrees
 */
const val TO_DEG = 180.0 / PI

/**
 * multiply by this to convert from degrees to radians
 */
const val TO_RAD = PI / 180.0

/**
 * A very small value used for floating point comparison.
 */
const val ROUNDING_ERROR: Double = 0.000001

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
fun lerp(fromValue: Double, toValue: Double, progress: Double): Double {
	return fromValue + (toValue - fromValue) * progress
}

// ---

/**
 * Returns true if the value is zero
 * @param tolerance represent an upper bound below which the value is considered zero.
 */
fun isZero(value: Double, tolerance: Double = ROUNDING_ERROR): Boolean {
	return abs(value) <= tolerance
}

/**
 * Returns true if a is nearly equal to b. The function uses the default floating error tolerance.
 * @param a the first value.
 * @param b the second value.
 */
fun isEqual(a: Double, b: Double): Boolean {
	return abs(a - b) <= ROUNDING_ERROR
}

/**
 * Returns true if a is nearly equal to b.
 * @param a the first value.
 * @param b the second value.
 * @param tolerance represent an upper bound below which the two values are considered equal.
 */
fun isEqual(a: Double, b: Double, tolerance: Double): Boolean {
	return abs(a - b) <= tolerance
}

@JsName("clampN")
inline fun <T : Comparable<T>> clamp(value: T?, min: T, max: T): T? {
	if (value == null) return null
	if (value <= min) return min
	if (value >= max) return max
	return value
}

/**
 * Given a comparable value, returns a value within the range of [min] and [max] (inclusive).
 * NB: [min] takes precedence over [max].
 * This is the same: `maxOf(min, minOf(max, value))`
 */
inline fun <T : Comparable<T>> clamp(value: T, min: T, max: T): T {
	if (value <= min) return min
	if (value >= max) return max
	return value
}

/**
 * Returns the signum function of the argument; zero if the argument
 * is zero, 1.0.0 if the argument is greater than zero, -1.0.0 if the
 * argument is less than zero.
 *
 * Special Cases:
 *
 * If the argument is NaN, then the result is NaN.
 *
 * @param v the floating-point value whose signum is to be returned
 * @return the signum function of the argument
 * @author Joseph D. Darcy
 */
fun signum(v: Double): Double {
	if (v > 0) return 1.0
	if (v < 0) return -1.0
	if (v.isNaN()) return Double.NaN
	return 0.0
}

/**
 * n must be positive.
 * mod( 5.0, 3.0) produces 2.0
 * mod(-5.0, 3.0) produces 1.0
 */
fun mod(a: Double, n: Double): Double {
	return if (a < 0.0) (a % n + n) % n else a % n
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
fun angleDiff(a: Double, b: Double): Double {
	var diff = b - a
	if (diff < -PI) diff = PI2 - diff
	if (diff > PI2) diff %= PI2
	if (diff >= PI) diff -= PI2
	return diff
}

/**
 * Given a quadratic equation of the form: y = ax^2 + bx + c, returns the solutions to x where y == 0.0
 * Uses the quadratic formula: x = (-b += sqrt(b^2 - 4ac)) / 2a
 * @param a The a coefficient.
 * @param b The b coefficient.
 * @param c The c coefficient.
 * @param out The list to populate with the solutions.
 * @return Returns the x values where y == 0.0. This may have 0, 1, or 2 values.
 */
fun getQuadraticRoots(a: Double, b: Double, c: Double, out: MutableList<Double>) {
	out.clear()
	if (a == 0.0) {
		// Not a quadratic equation.
		if (b == 0.0) return
		out.add(-c / b)
	}

	val q = b * b - 4.0 * a * c
	val signQ = if (q > 0.0) 1 else if (q < 0.0) -1 else 0

	if (signQ < 0) {
		// No solution
	} else if (signQ == 0) {
		out.add(-b / (2.0 * a))
	} else {
		val aa = -b / (2.0 * a)
		val tmp = sqrt(q) / (2.0 * a)
		out.add(aa - tmp)
		out.add(aa + tmp)
	}
}

/**
 * Given a cubic equation of the form: y = ax^3 + bx^2 + cx + d, returns the solutions to x where y == 0.0
 * @param a The a coefficient.
 * @param b The b coefficient.
 * @param c The c coefficient.
 * @param c The d coefficient.
 * @param out The list to populate with the solutions.
 * @return Returns the x values where y == 0.0. This may have 0, 1, 2 or 3 values.
 */
fun getCubicRoots(a: Double = 0.0, b: Double = 0.0, c: Double = 0.0, d: Double = 0.0, out: MutableList<Double>) {
	if (a == 0.0) {
		// Not a cubic equation
		return getQuadraticRoots(b, c, d, out)
	}
	out.clear()

	var b = b
	var c = c
	var d = d
	// normalize the coefficients so the cubed term is 1 and we can ignore it hereafter
	if (a != 1.0) {
		b /= a
		c /= a
		d /= a
	}

	val q = (b * b - 3.0 * c) / 9.0
	val q3 = q * q * q
	val r = (2.0 * b * b * b - 9.0 * b * c + 27.0 * d) / 54.0
	val diff: Double = q3 - r * r
	if (diff >= 0) {
		if (q == 0.0) {
			// avoid division by zero
			out.add(0.0)
		} else {
			// three real roots
			val theta: Double = acos(r / sqrt(q3))
			val qSqrt: Double = sqrt(q)

			out.add(-2.0 * qSqrt * cos(theta / 3.0) - b / 3.0)
			out.add(-2.0 * qSqrt * cos((theta + 2.0 * PI) / 3.0) - b / 3.0)
			out.add(-2.0 * qSqrt * cos((theta + 4.0 * PI) / 3.0) - b / 3.0)
		}
	} else {
		// one real root
		val tmp: Double = (sqrt(-diff) + abs(r)).pow(1.0 / 3.0)
		val rSign = if (r > 0.0) 1.0 else if (r < 0.0) -1.0 else 0.0
		out.add(-rSign * (tmp + q / tmp) - b / 3.0)
	}
}

/**
 * Snaps a value to the nearest interval.
 */
fun roundToNearest(value: Double, snap: Double, offset: Double = 0.0): Double {
	if (snap <= 0) return value
	var v = value - offset
	v /= snap
	v = round(v)
	v *= snap
	return v + offset
}

/**
 * Floors a value to the nearest interval.
 */
fun floorToNearest(value: Double, snap: Double, offset: Double = 0.0): Double {
	if (snap <= 0) return value
	var v = value - offset
	v /= snap
	v = floor(v)
	v *= snap
	return v + offset
}

/**
 * Ceils a value to the nearest interval.
 */
fun ceilToNearest(value: Double, snap: Double, offset: Double = 0.0): Double {
	if (snap <= 0) return value
	var v = value - offset
	v /= snap
	v = ceil(v)
	v *= snap
	return v + offset
}

/**
 * Round after a small, but obscure offset, to avoid flip-flopping around the common case of 0.5
 */
fun offsetRound(x: Double, offset: Double = -0.0136): Double {
	return round(x + offset)
}

/**
 * Returns the fraction of this float.
 */
fun Double.fpart(): Double {
	return this - floor(this)
}

/**
 * Gets the number of fraction digits for this value.
 * E.g.  if this value is 1.0, returns 0, if this value is 3.1.0, returns 1.
 * The max return value is 10.
 */
val Double.fractionDigits: Int
	get() {
		var m = 1.0
		for (i in 0..10) {
			if ((this * m).fpart() == 0.0) {
				return i
			}
			m *= 10.0
		}
		return 10
	}


fun <T : Comparable<T>> maxOf4(a: T, b: T, c: T, d: T): T {
	return maxOf(maxOf(a, b), maxOf(c, d))
}

fun <T : Comparable<T>> minOf4(a: T, b: T, c: T, d: T): T {
	return minOf(minOf(a, b), minOf(c, d))
}

fun Random.nextFloat(until: Double): Double = nextDouble(until.toDouble()).toDouble()
fun Random.nextFloat(from: Double, until: Double): Double = nextDouble(from.toDouble(), until.toDouble()).toDouble()
