/*
 * Copyright 2018 Poly Forest
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

package com.acornui.collection

fun <A, B, C> tuple(first: A, second: B, third: C) = Triple(first, second, third)
fun <A, B, C, D> tuple(first: A, second: B, third: C, fourth: D) = Tuple4(first, second, third, fourth)

infix fun <A, B> A.tuple(second: B) = Pair(this, second)
infix fun <A, B, C> Pair<A, B>.tuple(third: C) = Triple(first, second, third)
infix fun <A, B, C, D> Triple<A, B, C>.tuple(fourth: D) = Tuple4(first, second, third, fourth)

/**
 * Represents four values
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Tuple4 exhibits value semantics, i.e. two tuple4s are equal if all four components are equal.
 * An example of decomposing it into values:
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @param D type of the fourth value.
 * @property first First value.
 * @property second Second value.
 * @property third Third value.
 * @property fourth Fourth value.
 */
data class Tuple4<out A, out B, out C, out D>(
		val first: A,
		val second: B,
		val third: C,
		val fourth: D
) {

	/**
	 * Returns string representation of the [Triple] including its [first], [second], [third], and [fourth] values.
	 */
	override fun toString(): String = "($first, $second, $third, $fourth)"
}