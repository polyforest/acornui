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

@file:Suppress("unused")

package com.acornui.test

import com.acornui.async.toPromiseOrBlocking
import com.acornui.closeTo
import com.acornui.collection.toList
import com.acornui.math.RectangleRo
import com.acornui.math.Vector2Ro
import com.acornui.math.Vector3Ro
import com.acornui.time.nanoElapsed
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.seconds

fun assertListEquals(expected: IntArray, actual: IntArray) {
	assertListEquals(expected.iterator(), actual.iterator())
}

fun assertListEquals(expected: BooleanArray, actual: BooleanArray) {
	assertListEquals(expected.iterator(), actual.iterator())
}

fun assertListEquals(expected: DoubleArray, actual: DoubleArray, margin: Double = 0.0001) {
	assertListEquals(expected.iterator(), actual.iterator()) { a, b -> a.closeTo(b, margin) }
}

fun assertListEquals(expected: FloatArray, actual: FloatArray, margin: Float = 0.0001f) {
	assertListEquals(expected.iterator(), actual.iterator()) { a, b -> a.closeTo(b, margin) }
}

fun assertListEquals(expected: CharArray, actual: CharArray) {
	assertListEquals(expected.iterator(), actual.iterator())
}

fun <T> assertListEquals(expected: Array<T>, actual: Array<T>, comparator: (a: T, b: T) -> Boolean = { a, b -> a == b }) {
	assertListEquals(expected.iterator(), actual.iterator(), comparator)
}

fun <T> assertListEquals(expected: Array<T>, actual: Iterable<T>, comparator: (a: T, b: T) -> Boolean = { a, b -> a == b }) {
	assertListEquals(expected.iterator(), actual.iterator(), comparator)
}

fun <T> assertListEquals(expected: Iterable<T>, actual: Array<T>, comparator: (a: T, b: T) -> Boolean = { a, b -> a == b }) {
	assertListEquals(expected.iterator(), actual.iterator(), comparator)
}

fun <T> assertListEquals(expected: Iterable<T>, actual: Iterable<T>, comparator: (a: T, b: T) -> Boolean = { a, b -> a == b }) {
	assertListEquals(expected.iterator(), actual.iterator(), comparator)
}

fun assertListEquals(expected: Iterable<Float>, actual: Iterable<Float>, tolerance: Float = 0.0001f) {
	assertListEquals(expected.iterator(), actual.iterator()) { a, b -> a.closeTo(b, tolerance)}
}

fun assertListEquals(expected: Iterable<Double>, actual: Iterable<Double>, tolerance: Double = 0.0001) {
	assertListEquals(expected.iterator(), actual.iterator()) { a, b -> a.closeTo(b, tolerance)}
}

fun <T> assertListEquals(expected: Iterator<T>, actual: Iterator<T>, comparator: (a: T, b: T) -> Boolean = { a, b -> a == b }) {
	var expectedSize = 0
	var actualSize = 0
	while (expected.hasNext() || actual.hasNext()) {
		if (expected.hasNext() && actual.hasNext()) {
			val expectedI = expected.next()
			val actualI = actual.next()
			if (!comparator(expectedI, actualI)) {
				fail("At index $expectedSize expected: $expectedI actual: $actualI")
			}
			expectedSize++
			actualSize++
		} else if (expected.hasNext()) {
			expected.next()
			expectedSize++
		} else if (actual.hasNext()) {
			actual.next()
			actualSize++
		}
	}
	if (expectedSize != actualSize) {
		fail("actual size: $actualSize expected size: $expectedSize")
	}
}

fun assertUnorderedListEquals(expected: IntArray, actual: IntArray) {
	assertUnorderedListEquals(expected.iterator(), actual.iterator())
}

fun assertUnorderedListEquals(expected: BooleanArray, actual: BooleanArray) {
	assertUnorderedListEquals(expected.iterator(), actual.iterator())
}

fun assertUnorderedListEquals(expected: DoubleArray, actual: DoubleArray) {
	assertUnorderedListEquals(expected.iterator(), actual.iterator())
}

fun assertUnorderedListEquals(expected: FloatArray, actual: FloatArray) {
	assertUnorderedListEquals(expected.iterator(), actual.iterator())
}

fun assertUnorderedListEquals(expected: CharArray, actual: CharArray) {
	assertUnorderedListEquals(expected.iterator(), actual.iterator())
}

fun <T : Any> assertUnorderedListEquals(expected: Array<T>, actual: Array<T>) {
	assertUnorderedListEquals(expected.iterator(), actual.iterator())
}

fun <T : Any> assertUnorderedListEquals(expected: Array<T>, actual: Iterable<T>) {
	assertUnorderedListEquals(expected.iterator(), actual.iterator())
}

fun <T : Any> assertUnorderedListEquals(expected: Iterable<T>, actual: Array<T>) {
	assertUnorderedListEquals(expected.iterator(), actual.iterator())
}

fun <T : Any> assertUnorderedListEquals(expected: Iterable<T>, actual: Iterable<T>) {
	assertUnorderedListEquals(expected.iterator(), actual.iterator())
}

fun <T : Any> assertUnorderedListEquals(expected: Iterator<T>, actual: Iterator<T>) {
	val expectedList = expected.toList()
	val actualList = actual.toList()
	if (expectedList.size != actualList.size) {
		fail("expected size: ${expectedList.size} actual size: ${actualList.size}")
	}

	val finds = Array(expectedList.size) { false }
	for (i in 0..expectedList.lastIndex) {
		val expectedI = expectedList[i]
		var found: T? = null
		for (j in 0..actualList.lastIndex) {
			if (!finds[j] && actualList[j] == expectedI) {
				finds[j] = true
				found = actualList[j]
				break
			}
		}
		if (found == null) fail("The expected element $expectedI at index $i was not found.")
	}


}

class TestUtils {

	@Test
	fun testAssertListEquals() {
		assertListEquals(arrayOf(1, 2, 3, 4), arrayOf(1, 2, 3, 4))
		assertListEquals(arrayListOf(1, 2, 3, 4), arrayOf(1, 2, 3, 4))
		assertListEquals(arrayOf(1, 2, 3, 4), arrayListOf(1, 2, 3, 4))

		assertFails {
			assertListEquals(arrayOf(1, 2, 3, 4), arrayOf(1, 2, 3))
		}
		assertFails {
			assertListEquals(arrayOf(1, 2, 3, 4), arrayOf(1, 2, 3))
		}
		assertFails {
			assertListEquals(arrayOf(1, 3, 2), arrayOf(1, 2, 3))
		}
		assertFails {
			assertListEquals(arrayOf(1, 1, 2, 3), arrayOf(1, 2, 3, 2))
		}
	}

	@Test
	fun testAssertUnorderedListEquals() {
		assertUnorderedListEquals(arrayOf(1, 2, 3, 4), arrayListOf(1, 2, 3, 4))
		assertUnorderedListEquals(arrayOf(1, 2, 3, 4), arrayListOf(2, 1, 3, 4))
		assertUnorderedListEquals(arrayOf(1, 1, 3, 4), arrayListOf(3, 1, 1, 4))
		assertUnorderedListEquals(arrayOf(1, 4, 3, 1), arrayListOf(3, 1, 1, 4))

		assertFails {
			assertUnorderedListEquals(arrayOf(1, 2, 3, 4), arrayListOf(1, 3, 4))
		}
		assertFails {
			assertUnorderedListEquals(arrayOf(1, 2, 3), arrayListOf(1, 3, 4, 2))
		}
		assertFails {
			assertUnorderedListEquals(arrayOf(1, 1, 2, 3), arrayListOf(1, 3, 4, 2))
		}
		assertFails {
			assertUnorderedListEquals(arrayOf(1, 1, 2, 3), arrayListOf(1, 2, 3, 2))
		}
		assertFails {
			assertUnorderedListEquals(arrayOf(1, 2, 3, 4), arrayListOf(1, 2, 2, 3))
		}
	}
}

fun assertClose(expected: Float, actual: Float, maxDifference: Float = 0.0001f, propertyName: String = "") {
	val difference = abs(expected - actual)
	if (difference > maxDifference) {
		val name = if (propertyName.isEmpty()) "" else "'$propertyName' "
		fail("expected ${name}close:<$expected> but was:<$actual> difference:<$difference> max:<$maxDifference> ")
	}
}

fun assertClose(expected: Double, actual: Double, maxDifference: Double = 0.0001, propertyName: String = "") {
	val difference = abs(expected - actual)
	if (difference > maxDifference) {
		val name = if (propertyName.isEmpty()) "" else "'$propertyName' "
		fail("expected ${name}close:<$expected> but was:<$actual> difference:<$difference> max:<$maxDifference> ")
	}
}

fun assertClose(expected: Vector3Ro, actual: Vector3Ro, maxDifference: Float = 0.0001f) {
	assertClose(expected.x, actual.x, maxDifference, "x")
	assertClose(expected.y, actual.y, maxDifference, "y")
	assertClose(expected.z, actual.z, maxDifference, "z")
}

fun assertClose(expected: Vector2Ro, actual: Vector2Ro, maxDifference: Float = 0.0001f) {
	assertClose(expected.x, actual.x, maxDifference, "x")
	assertClose(expected.y, actual.y, maxDifference, "y")
}

fun assertClose(expected: RectangleRo, actual: RectangleRo, maxDifference: Float = 0.0001f) {
	assertClose(expected.x, actual.x, maxDifference, "x")
	assertClose(expected.y, actual.y, maxDifference, "y")
	assertClose(expected.width, actual.width, maxDifference, "width")
	assertClose(expected.height, actual.height, maxDifference, "height")
}

fun <T: Comparable<T>> assertRange(expectedMin: T, expectedMax: T, actual: T) {
	assertGreaterThan(expectedMin, actual)
	assertLessThan(expectedMax, actual)
}

fun <T: Comparable<T>> assertGreaterThan(expectedMin: T, actual: T) {
	if (actual <= expectedMin)
		fail("expected greater than:<$expectedMin> but was:<$actual>")
}

fun <T: Comparable<T>> assertLessThan(expectedMax: T, actual: T) {
	if (actual >= expectedMax)
		fail("expected less than:<$expectedMax> but was:<$actual>")
}

/**
 * Returns the median amount of time each call took, in milliseconds.
 */
fun benchmark(iterations: Int = 1000, testCount: Int = 10, warmCount: Int = 2, call: () -> Unit): Float {
	val results = ArrayList<Float>(testCount)
	for (i in 0 until testCount + warmCount) {
		val startTime = nanoElapsed()
		for (j in 0 until iterations) {
			call()
		}
		if (i < warmCount) continue
		val endTime = nanoElapsed()
		val elapsed = (endTime - startTime) / 1e6.toFloat()
		results.add(elapsed / iterations.toFloat())
	}
	results.sort()
	return results[results.size / 2]
}

/**
 * Runs a coroutine, converting its deferred result to be used for platform-specific testing.
 * @see toPromiseOrBlocking
 */
fun <R> runTest(timeout: Duration = 10.seconds, block: suspend CoroutineScope.() -> R) = GlobalScope.async {
	withTimeout(timeout.toLongMilliseconds()) {
		block()
	}
}.toPromiseOrBlocking()