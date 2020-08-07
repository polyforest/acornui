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

import com.acornui.AppConfig
import com.acornui.app
import com.acornui.async.withTimeout
import com.acornui.collection.toList
import com.acornui.component.Stage
import com.acornui.math.Rectangle
import com.acornui.math.Vector2
import com.acornui.math.Vector3
import com.acornui.math.clamp
import com.acornui.number.closeTo
import initMockDom
import kotlinx.coroutines.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.js.Promise
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.seconds

fun assertListEquals(expected: IntArray, actual: IntArray) {
	assertListEquals(expected.iterator(), actual.iterator())
}

fun assertListEquals(expected: BooleanArray, actual: BooleanArray) {
	assertListEquals(expected.iterator(), actual.iterator())
}

fun assertListEquals(expected: DoubleArray, actual: DoubleArray, margin: Double = 0.0001) {
	assertListEquals(expected.iterator(), actual.iterator()) { a, b ->
		a.closeTo(
			b,
			margin
		)
	}
}

fun assertListEquals(expected: CharArray, actual: CharArray) {
	assertListEquals(expected.iterator(), actual.iterator())
}

fun <T> assertListEquals(
	expected: Array<T>,
	actual: Array<T>,
	comparator: (a: T, b: T) -> Boolean = { a, b -> a == b }
) {
	assertListEquals(expected.iterator(), actual.iterator(), comparator)
}

fun <T> assertListEquals(
	expected: Array<T>,
	actual: Iterable<T>,
	comparator: (a: T, b: T) -> Boolean = { a, b -> a == b }
) {
	assertListEquals(expected.iterator(), actual.iterator(), comparator)
}

fun <T> assertListEquals(
	expected: Iterable<T>,
	actual: Array<T>,
	comparator: (a: T, b: T) -> Boolean = { a, b -> a == b }
) {
	assertListEquals(expected.iterator(), actual.iterator(), comparator)
}

fun <T> assertListEquals(
	expected: Iterable<T>,
	actual: Iterable<T>,
	comparator: (a: T, b: T) -> Boolean = { a, b -> a == b }
) {
	assertListEquals(expected.iterator(), actual.iterator(), comparator)
}

fun assertListEquals(expected: Iterable<Double>, actual: Iterable<Double>, tolerance: Double = 0.0001) {
	assertListEquals(expected.iterator(), actual.iterator()) { a, b ->
		a.closeTo(
			b,
			tolerance
		)
	}
}

fun <T> assertListEquals(
	expected: Iterator<T>,
	actual: Iterator<T>,
	comparator: (a: T, b: T) -> Boolean = { a, b -> a == b }
) {
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
		fail("Expected size: ${expectedList.size} actual size: ${actualList.size}")
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

fun assertClose(expected: Double, actual: Double, maxDifference: Double = 0.0001, propertyName: String = "") {
	val difference = abs(expected - actual)
	if (difference > maxDifference) {
		val name = if (propertyName.isEmpty()) "" else "'$propertyName' "
		fail("expected ${name}close:<$expected> but was:<$actual> difference:<$difference> max:<$maxDifference> ")
	}
}

fun assertClose(expected: Vector3, actual: Vector3, maxDifference: Double = 0.0001) {
	assertClose(expected.x, actual.x, maxDifference, "x")
	assertClose(expected.y, actual.y, maxDifference, "y")
	assertClose(expected.z, actual.z, maxDifference, "z")
}

fun assertClose(expected: Vector2, actual: Vector2, maxDifference: Double = 0.0001) {
	assertClose(expected.x, actual.x, maxDifference, "x")
	assertClose(expected.y, actual.y, maxDifference, "y")
}

fun assertClose(expected: Rectangle, actual: Rectangle, maxDifference: Double = 0.0001) {
	assertClose(expected.x, actual.x, maxDifference, "x")
	assertClose(expected.y, actual.y, maxDifference, "y")
	assertClose(expected.width, actual.width, maxDifference, "width")
	assertClose(expected.height, actual.height, maxDifference, "height")
}

fun <T : Comparable<T>> assertRange(expectedMin: T, expectedMax: T, actual: T) {
	assertGreaterThan(expectedMin, actual)
	assertLessThan(expectedMax, actual)
}

fun <T : Comparable<T>> assertGreaterThan(expectedMin: T, actual: T) {
	if (actual <= expectedMin)
		fail("expected greater than:<$expectedMin> but was:<$actual>")
}

fun <T : Comparable<T>> assertLessThan(expectedMax: T, actual: T) {
	if (actual >= expectedMax)
		fail("expected less than:<$expectedMax> but was:<$actual>")
}

/**
 * Returns the median amount of time each call took.
 * If [inner] takes longer than 0.5s to complete, it will only be invoked once and that result is returned.
 * If [inner] takes less than 0.5s, the median call time will be returned. The number of times [inner] is called
 * is dependent on the amount of time its first invocation took.
 * This benchmark can be expected to run for 5s, or the duration of one call.
 */
fun benchmark(inner: () -> Unit): Duration {
	val warmStart = TimeSource.Monotonic.markNow()
	inner()
	val estimatedTime = warmStart.elapsedNow()
	if (estimatedTime > 0.5.seconds) return estimatedTime
	val sets = 5
	val iterations = clamp((5.seconds / estimatedTime / sets).toInt(), 1, 1000)

	val results = ArrayList<Duration>(sets)
	for (i in 0 until sets) {
		val start = TimeSource.Monotonic.markNow()
		for (j in 0 until iterations) {
			inner()
		}
		results.add(start.elapsedNow() / iterations)
	}
	results.sort()
	return results[results.size / 2]
}

class ExpectedException(message: String? = "An expected exception for unit tests.") : Exception(message)

/**
 * Asserts that a promise fails with an expected exception type.
 */
inline fun <reified T : Throwable> Promise<Any?>.assertFailsWith(message: String? = null): Promise<Unit> {
	@Suppress("RemoveExplicitTypeArguments")
	return this.then<Unit>(onFulfilled = {
		fail((if (message == null) "" else "$message. ") + "Expected an exception to be thrown, but was completed successfully.")
	}, onRejected = {
		assertFailsWith<T> { throw it }
	})
}

fun Promise<Any?>.assertFails(message: String? = null): Promise<Unit> = assertFailsWith<Throwable>(message)

/**
 * Runs a test in an async block with a timeout.
 * Additionally, this will initialize polyfills and nodejs mock dom.
 *
 * @return Returns a Promise, suitable for asynchronous unit test frameworks.
 */
fun runTest(timeout: Duration = 10.seconds, block: suspend CoroutineScope.() -> Unit): Promise<Unit> {
	contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
	initMockDom()
	return GlobalScope.async {
		withTimeout(timeout, block)
	}.asPromise()
}

/**
 * Runs a test with a timeout.
 * Additionally, this will initialize polyfills and nodejs mock dom.
 *
 * @param block The block to execute. This will be given two callbacks, resolve, and reject.
 *
 * @return Returns a Promise, suitable for asynchronous unit test frameworks.
 */
fun runAsyncTest(
	timeout: Duration = 10.seconds,
	block: (resolve: () -> Unit, reject: (e: Throwable) -> Unit) -> Unit
): Promise<Unit> {
	contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
	initMockDom()
	return Promise<Unit> {
		resolve, reject ->
		block({ resolve(Unit) }, reject)
	}.withTimeout(timeout)
}

/**
 * Runs a test within an acorn application.
 * The test will be completed when the stage has been disposed.
 */
fun runApplicationTest(
	appConfig: AppConfig = AppConfig(),
	timeout: Duration = 10.seconds,
	block: Stage.(resolve: () -> Unit, reject: (e: Throwable) -> Unit) -> Unit
): Promise<Unit> =
	runAsyncTest(timeout) { resolve, reject ->
		app(appConfig = appConfig) {
			coroutineContext[Job]!!.invokeOnCompletion {
				reject(it!!)
			}
			block(resolve, reject)
		}
	}
