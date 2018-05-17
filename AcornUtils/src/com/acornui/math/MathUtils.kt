/*
 * Derived from LibGDX by Nicholas Bilyk
 * https://github.com/libgdx
 * Copyright 2011 See https://github.com/libgdx/libgdx/blob/master/AUTHORS
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

const val PI: Float = 3.1415927f
const val PI2: Float = PI * 2f
const val E: Float = 2.7182818f
const val TO_DEG = 180f / PI
const val TO_RAD = PI / 180f

internal object TrigLookup {
	const val SIN_BITS = 14 // 16KB. Adjust for accuracy.
	const val SIN_MASK = (-1 shl SIN_BITS).inv()
	const val SIN_COUNT = SIN_MASK + 1

	const val radFull = PI * 2

	const val radToIndex = SIN_COUNT.toFloat() / radFull

	val table = FloatArray(SIN_COUNT)

	init {

		for (i in 1..SIN_COUNT - 1) {
			table[i] = Math.sin(((i.toFloat() + 0.5f) / SIN_COUNT.toFloat() * radFull).toDouble()).toFloat()
		}

		for (i in 0..16) {
			val theta = i * PI2 / 16
			table[(theta * radToIndex).toInt() and SIN_MASK] = Math.sin(theta.toDouble()).toFloat()
		}
	}

	/**
	 * Returns the sine in radians from a lookup table.
	 */
	fun sin(radians: Float): Float {
		return table[(radians * radToIndex).toInt() and SIN_MASK]
	}

	/**
	 * Returns the cosine in radians from a lookup table.
	 */
	fun cos(radians: Float): Float {
		return table[((radians + PI / 2f) * radToIndex).toInt() and SIN_MASK]
	}

	/**
	 * Returns the tan in radians from a lookup table.
	 */
	fun tan(radians: Float): Float {
		return sin(radians) / cos(radians)
	}
}

internal object Atan2 {

	private val ATAN2_BITS = 7 // Adjust for accuracy.
	private val ATAN2_BITS2 = ATAN2_BITS shl 1
	private val ATAN2_MASK = (-1 shl ATAN2_BITS2).inv()
	private val ATAN2_COUNT = ATAN2_MASK + 1
	private val ATAN2_DIM = Math.sqrt(ATAN2_COUNT.toDouble()).toInt()
	private val INV_ATAN2_DIM_MINUS_1 = 1f / (ATAN2_DIM.toFloat() - 1f)

	internal val table = FloatArray(ATAN2_COUNT)

	init {
		for (i in 0..ATAN2_DIM - 1) {
			for (j in 0..ATAN2_DIM - 1) {
				val x0 = i.toFloat() / ATAN2_DIM.toFloat()
				val y0 = j.toFloat() / ATAN2_DIM.toFloat()
				table[j * ATAN2_DIM + i] = Math.atan2(y0.toDouble(), x0.toDouble()).toFloat()
			}
		}
	}

	/**
	 * Returns atan2 in radians from a lookup table.
	 */
	fun atan2(y: Float, x: Float): Float {
		var yVar = y
		var xVar = x
		val add: Float
		val mul: Float
		if (xVar < 0) {
			if (yVar < 0) {
				yVar = -yVar
				mul = 1.0f
			} else
				mul = -1f
			xVar = -xVar
			add = -PI
		} else {
			if (yVar < 0) {
				yVar = -yVar
				mul = -1f
			} else
				mul = 1f
			add = 0f
		}
		val invDiv = 1 / ((if (xVar < yVar) yVar else xVar) * INV_ATAN2_DIM_MINUS_1)

		val xi = (xVar * invDiv).toInt()
		val yi = (yVar * invDiv).toInt()
		return (Atan2.table[yi * ATAN2_DIM + xi] + add) * mul
	}
}

/**
 * Utility and fast math functions.
 *
 * Thanks to Riven on JavaGaming.org for the basis of sin/cos/atan2/floor/ceil.
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

	/**
	 * Returns the sine in radians from a lookup table.
	 */
	@Deprecated("Use native math", ReplaceWith("kotlin.math.sin(radians)"))
	fun sin(radians: Float): Float = TrigLookup.sin(radians)

	/**
	 * Returns the cosine in radians from a lookup table.
	 */
	@Deprecated("Use native math", ReplaceWith("kotlin.math.cos(radians)"))
	fun cos(radians: Float): Float = TrigLookup.cos(radians)

	/**
	 * Returns the tan in radians from a lookup table.
	 * Throws DivideByZero exception when cos(radians) == 0
	 */
	@Deprecated("Use native math", ReplaceWith("kotlin.math.tan(radians)"))
	fun tan(radians: Float): Float = TrigLookup.tan(radians)

	// ---

	@Deprecated("Use native math", ReplaceWith("kotlin.math.atan2(y, x)"))
	fun atan2(y: Float, x: Float): Float = Atan2.atan2(y, x)

	// ---

	val rng: Random = Random()

	/**
	 * Returns a random number between 0 (inclusive) and the specified value (inclusive).
	 */
	fun random(range: Int): Int {
		return rng.nextInt(range + 1)
	}

	/**
	 * Returns a random number between start (inclusive) and end (inclusive).
	 */
	fun random(start: Int, end: Int): Int {
		return start + rng.nextInt(end - start + 1)
	}

	/**
	 * Returns a random number between 0 (inclusive) and the specified value (inclusive).
	 */
	fun random(range: Long): Long {
		return (rng.nextDouble() * range.toDouble()).toLong()
	}

	/**
	 * Returns a random number between start (inclusive) and end (inclusive).
	 */
	fun random(start: Long, end: Long): Long {
		return start + (rng.nextDouble() * (end - start).toDouble()).toLong()
	}

	/**
	 * Returns a random boolean value.
	 */
	fun randomBoolean(): Boolean {
		return rng.nextBoolean()
	}

	/**
	 * Returns true if a random value between 0 and 1 is less than the specified value.
	 */
	fun randomBoolean(chance: Float): Boolean {
		return random() < chance
	}

	/**
	 * Returns random number between 0.0 (inclusive) and 1.0 (exclusive).
	 */
	fun random(): Float {
		return rng.nextFloat()
	}

	/**
	 * Returns a random number between 0 (inclusive) and the specified value (exclusive).
	 */
	fun random(range: Float): Float {
		return rng.nextFloat() * range
	}

	/**
	 * Returns a random number between start (inclusive) and end (exclusive).
	 */
	fun random(start: Float, end: Float): Float {
		return start + rng.nextFloat() * (end - start)
	}

	/**
	 * Returns -1 or 1, randomly.
	 */
	fun randomSign(): Int {
		return 1 or (rng.nextInt() shr 31)
	}

	/**
	 * Returns a triangularly distributed random number between -1.0 (exclusive) and 1.0 (exclusive), where values around zero are
	 * more likely.
	 * This is an optimized version of {@link #randomTriangular(float, float, float) randomTriangular(-1, 1, 0)}
	 */
	fun randomTriangular(): Float {
		return rng.nextFloat() - rng.nextFloat()
	}

	/**
	 * Returns a triangularly distributed random number between {@code -max} (exclusive) and {@code max} (exclusive), where values
	 * around zero are more likely.
	 * This is an optimized version of {@link #randomTriangular(float, float, float) randomTriangular(-max, max, 0)}
	 * @param max the upper limit
	 */
	fun randomTriangular(max: Float): Float {
		return (rng.nextFloat() - rng.nextFloat()) * max
	}

	/**
	 * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where the
	 * `mode` argument defaults to the midpoint between the bounds, giving a symmetric distribution.
	 *
	 * This method is equivalent of [randomTriangular(min, max, (max - min) * .5f)]
	 * @param min the lower limit
	 * @param max the upper limit
	 */
	fun randomTriangular(min: Float, max: Float): Float {
		return randomTriangular(min, max, (max - min) * 0.5f)
	}

	/**
	 * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where values
	 * around {@code mode} are more likely.
	 * @param min the lower limit
	 * @param max the upper limit
	 * @param mode the point around which the values are more likely
	 */
	fun randomTriangular(min: Float, max: Float, mode: Float): Float {
		val u = rng.nextFloat()
		val d = max - min
		if (u <= (mode - min) / d) return min + MathUtils.sqrt(u * d * (mode - min))
		return max - MathUtils.sqrt((1 - u) * d * (max - mode))
	}

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

	@Deprecated("Use native math", ReplaceWith("kotlin.math.abs(value)"))
	inline fun abs(value: Float): Float {
		return if (value < 0f) -value else value
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.abs(value)"))
	inline fun abs(value: Double): Double {
		return if (value < 0f) -value else value
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.abs(value)"))
	inline fun abs(value: Int): Int {
		return if (value < 0f) -value else value
	}

	@Deprecated("Use native math", ReplaceWith("kotlin.math.abs(value)"))
	inline fun abs(value: Long): Long {
		return if (value < 0f) -value else value
	}

	/**
	 * Returns true if the value is zero
	 * @param tolerance represent an upper bound below which the value is considered zero.
	 */
	fun isZero(value: Float, tolerance: Float = FLOAT_ROUNDING_ERROR): Boolean {
		return kotlin.math.abs(value.toDouble()) <= tolerance
	}

	/**
	 * Returns true if the value is zero
	 * @param tolerance represent an upper bound below which the value is considered zero.
	 */
	fun isZero(value: Double, tolerance: Float = FLOAT_ROUNDING_ERROR): Boolean {
		return kotlin.math.abs(value) <= tolerance
	}

	/**
	 * Returns true if a is nearly equal to b. The function uses the default floating error tolerance.
	 * @param a the first value.
	 * @param b the second value.
	 */
	fun isEqual(a: Float, b: Float): Boolean {
		return kotlin.math.abs(a - b) <= FLOAT_ROUNDING_ERROR
	}

	/**
	 * Returns true if a is nearly equal to b.
	 * @param a the first value.
	 * @param b the second value.
	 * @param tolerance represent an upper bound below which the two values are considered equal.
	 */
	fun isEqual(a: Float, b: Float, tolerance: Float): Boolean {
		return kotlin.math.abs(a - b) <= tolerance
	}

	/**
	 * @return the logarithm of x with base a
	 */
	fun log(x: Float, base: Float): Float {
		return (Math.log(x.toDouble()) / Math.log(base.toDouble())).toFloat()
	}

	/**
	 * @return the logarithm of x with base 2
	 */
	fun log2(x: Float): Float {
		return log(x, 2f)
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

	inline fun ceil(v: Float): Int {
		return Math.ceil(v.toDouble()).toInt()
	}

	inline fun floor(v: Float): Int {
		return Math.floor(v.toDouble()).toInt()
	}

	inline fun round(v: Float): Int {
		return Math.round(v.toDouble()).toInt()
	}

	inline fun sqrt(v: Float): Float {
		return Math.sqrt(v.toDouble()).toFloat()
	}

	inline fun pow(a: Float, b: Float): Float {
		return Math.pow(a.toDouble(), b.toDouble()).toFloat()
	}

	inline fun acos(v: Float): Float {
		return Math.acos(v.toDouble()).toFloat()
	}

	inline fun asin(v: Float): Float {
		return Math.asin(v.toDouble()).toFloat()
	}

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

				out.add(-2f * qSqrt * kotlin.math.cos(theta / 3f) - b / 3f)
				out.add(-2f * qSqrt * kotlin.math.cos((theta + 2f * PI) / 3f) - b / 3f)
				out.add(-2f * qSqrt * kotlin.math.cos((theta + 4f * PI) / 3f) - b / 3f)
			}
		} else {
			// one real root
			val tmp: Float = pow(sqrt(-diff) + kotlin.math.abs(r), 1f / 3f)
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
		v = MathUtils.round(v).toFloat()
		v *= snap
		return v + offset
	}

	fun floorToNearest(value: Float, snap: Float, offset: Float = 0f): Float {
		if (snap <= 0) return value
		var v = value - offset
		v /= snap
		v = MathUtils.floor(v).toFloat()
		v *= snap
		return v + offset
	}

	fun ceilToNearest(value: Float, snap: Float, offset: Float = 0f): Float {
		if (snap <= 0) return value
		var v = value - offset
		v /= snap
		v = MathUtils.ceil(v).toFloat()
		v *= snap
		return v + offset
	}
}

inline fun Float.ceil(): Int {
	return MathUtils.ceil(this)
}

/**
 * Returns the fraction of this float.
 */
inline fun Float.fpart(): Float {
	return this - MathUtils.floor(this).toFloat()
}

inline fun <T : Comparable<T>> maxOf4(a: T, b: T, c: T, d: T): T {
	return maxOf(maxOf(a, b), maxOf(c, d))
}

inline fun <T : Comparable<T>> minOf4(a: T, b: T, c: T, d: T): T {
	return minOf(minOf(a, b), minOf(c, d))
}