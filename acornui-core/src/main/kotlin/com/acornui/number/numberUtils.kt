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

package com.acornui.number

import com.acornui.math.ROUNDING_ERROR
import com.acornui.math.TO_DEG
import com.acornui.math.TO_RAD
import org.w3c.dom.css.CSSStyleDeclaration
import kotlin.math.abs
import kotlin.math.ceil

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

fun Double.notCloseTo(other: Double, tolerance: Double = ROUNDING_ERROR): Boolean {
	return abs(this - other) > tolerance
}

fun Double.closeTo(other: Double, tolerance: Double = ROUNDING_ERROR): Boolean {
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
		str = "0".repeat(intDigits - currIntDigits) + str
	}
	if (decimalDigits > currDecDigits) {
		if (decimalMarkIndex == -1) str += "."
		str += "0".repeat(decimalDigits - currDecDigits)
	}
	return str
}

fun radToDeg(value: Double): Double {
	return value * TO_DEG
}

fun degToRad(value: Double): Double {
	return value * TO_RAD
}

fun ceilInt(x: Double): Int = ceil(x).toInt()

/**
 * If this Double is zero, returns [Double.MIN_VALUE]. Otherwise, returns this Double.
 */
fun Double.nonZero(): Double {
	return if (this == 0.0) Double.MIN_VALUE else this
}

// Nullable arithmetic

operator fun Double?.plus(other: Double?): Double? {
	if (this == null || other == null) return null
	return this + other
}

operator fun Double?.minus(other: Double?): Double? {
	if (this == null || other == null) return null
	return this - other
}

operator fun Double?.times(other: Double?): Double? {
	if (this == null || other == null) return null
	return this * other
}

operator fun Double?.div(other: Double?): Double? {
	if (this == null || other == null) return null
	return this / other
}

fun String.cssToDouble(): Double {
	if (isEmpty()) return 0.0
	return substringBefore("px").toDoubleOrNull() ?: 0.0
}