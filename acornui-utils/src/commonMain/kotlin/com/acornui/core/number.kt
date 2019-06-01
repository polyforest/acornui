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

package com.acornui.core

import com.acornui.math.TO_DEG
import com.acornui.math.TO_RAD
import kotlin.math.abs
import kotlin.math.round

/**
 * A constant holding the maximum value a `long` can
 * have, 2^53-1.
 * Note: This is 53 bits instead of 63 for the sake of JavaScript Number.
 */
val LONG_MAX_VALUE: Long = 0x1fffffffFFFFFFL

@Deprecated("Use Int.MAX_VALUE", ReplaceWith("Int.MAX_VALUE"))
val INT_MAX_VALUE: Int = 0x7fffffff

@Deprecated("Use Int.MIN_VALUE", ReplaceWith("Int.MIN_VALUE"))
val INT_MIN_VALUE: Int = -2147483648

/**
 * A constant holding the minimum value a `long` can
 * have, -2^53.
 * Note: This is 53 bits instead of 63 for the sake of JavaScript Number.
 */
val LONG_MIN_VALUE: Long = -0x20000000000000L

/**
 * Returns the number of zero bits preceding the highest-order
 * ("leftmost") one-bit in the two's complement binary representation
 * of the specified `int` value.  Returns 32 if the
 * specified value has no one-bits in its two's complement representation,
 * in other words if it is equal to zero.
 *
 * Note that this method is closely related to the logarithm base 2.
 * 
 * For all positive `int` values x:
 * ```
 * floor(log2(x)) = 31 - numberOfLeadingZeros(x)
 * ceil(log2(x)) = 32 - numberOfLeadingZeros(x - 1)
 *```
 *
 * @return the number of zero bits preceding the highest-order
 *     ("leftmost") one-bit in the two's complement binary representation
 *     of the specified `int` value, or 32 if the value
 *     is equal to zero.
 */
fun Int.numberOfLeadingZeros(): Int {
	var i = this
	if (i == 0)
		return 32
	var n = 1
	if (i.ushr(16) == 0) {
		n += 16
		i = i shl 16
	}
	if (i.ushr(24) == 0) {
		n += 8
		i = i shl 8
	}
	if (i.ushr(28) == 0) {
		n += 4
		i = i shl 4
	}
	if (i.ushr(30) == 0) {
		n += 2
		i = i shl 2
	}
	n -= i.ushr(31)
	return n
}

/**
 * Returns the number of zero bits following the lowest-order ("rightmost")
 * one-bit in the two's complement binary representation of the specified
 * `int` value.  Returns 32 if the specified value has no
 * one-bits in its two's complement representation, in other words if it is
 * equal to zero.
 *
 * @return the number of zero bits following the lowest-order ("rightmost")
 *     one-bit in the two's complement binary representation of the
 *     specified `int` value, or 32 if the value is equal
 *     to zero.
 */
fun Int.numberOfTrailingZeros(): Int {
	var i = this
	var y: Int
	if (i == 0) return 32
	var n = 31
	y = i shl 16
	if (y != 0) {
		n -= 16
		i = y
	}
	y = i shl 8
	if (y != 0) {
		n -= 8
		i = y
	}
	y = i shl 4
	if (y != 0) {
		n -= 4
		i = y
	}
	y = i shl 2
	if (y != 0) {
		n -= 2
		i = y
	}
	return n - (i shl 1).ushr(31)
}

@Deprecated("use floor(float)", ReplaceWith("floor(this)", "kotlin.math.floor"))
fun Float.floor(): Float {
	return toInt().toFloat()
}

@Deprecated("Use native math", ReplaceWith("kotlin.math.round(this)"), DeprecationLevel.ERROR)
fun Float.round(): Float {
	return round(this)
}

fun Float.notCloseTo(other: Float, tolerance: Float = 0.0001f): Boolean {
	return abs(this - other) > tolerance
}

fun Float.closeTo(other: Float, tolerance: Float = 0.0001f): Boolean {
	return abs(this - other) <= tolerance
}

fun Double.closeTo(other: Double, tolerance: Double = 0.0001): Boolean {
	return abs(this - other) <= tolerance
}

fun Boolean.toInt(): Int {
	return if (this) 1 else 0
}

fun Number.zeroPadding(intDigits: Int, decimalDigits: Int = 0): String {
	return toString().zeroPadding(intDigits, decimalDigits)
}

fun String.zeroPadding(intDigits: Int, decimalDigits: Int = 0): String {
	var str = this
	if (intDigits == 0 && decimalDigits == 0) return str
	val decimalMarkIndex = str.indexOf(".")
	val currIntDigits: Int
	val currDecDigits: Int
	if (decimalMarkIndex != -1) {
		currIntDigits = decimalMarkIndex
		currDecDigits = str.length - decimalMarkIndex - 1
	} else {
		currIntDigits = str.length
		currDecDigits = 0
	}
	if (intDigits > currIntDigits) {
		str = "0".repeat2(intDigits - currIntDigits) + str
	}
	if (decimalDigits > currDecDigits) {
		if (decimalMarkIndex == -1) str += "."
		str += "0".repeat2(decimalDigits - currDecDigits)
	}
	return str
}

fun Float.radToDeg(): Float {
	return this * TO_DEG
}

fun Float.degToRad(): Float {
	return this * TO_RAD
}

/**
 * If this Float is zero, returns [Float.MIN_VALUE]. Otherwise, returns this Float.
 */
fun Float.nonZero(): Float {
	return if (this == 0f) Float.MIN_VALUE else this
}

/**
 * If this Double is zero, returns [Double.MIN_VALUE]. Otherwise, returns this Double.
 */
fun Double.nonZero(): Double {
	return if (this == 0.0) Double.MIN_VALUE else this
}