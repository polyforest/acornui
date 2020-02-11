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

@file:Suppress("UNUSED_VARIABLE")

package com.acornui.di

import com.acornui.async.delay
import com.acornui.test.assertUnorderedListEquals
import com.acornui.test.runTest
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.seconds

class BootstrapTest {

	@Test fun get() = runTest {
		val key1 = dKey<String>()
		val key2 = dKey<String>()
		val bootstrap = Bootstrap(defaultTaskTimeout = 1f)
		val task1 by bootstrap.task(key1) {
			delay(0.1.seconds)
			"dependency 1"
		}
		val task2 by bootstrap.task(key2) {
			delay(0.1.seconds)
			"dependency 2: ${bootstrap.get(key1)}"
		}
		bootstrap.awaitAll()
		assertEquals("dependency 2: dependency 1", bootstrap.get(key2))
	}

	@Test fun getOrderDoesntMatter() = runTest {
		val key1 = dKey<String>()
		val key2 = dKey<String>()
		val bootstrap = Bootstrap(defaultTaskTimeout = 1f)
		val task2 by bootstrap.task(key2) {
			"dependency 2: ${bootstrap.get(key1)}"
		}
		val task1 by bootstrap.task(key1) {
			delay(0.1.seconds)
			"dependency 1"
		}
		bootstrap.awaitAll()
		assertEquals("dependency 2: dependency 1", bootstrap.get(key2))
	}

	@Test fun dependenciesList() = runTest {
		val key1 = dKey<String>()
		val key2 = dKey<String>()
		val bootstrap = Bootstrap(defaultTaskTimeout = 1f)
		val task2 by bootstrap.task(key2) {
			"dependency 2: ${bootstrap.get(key1)}"
		}
		val task1 by bootstrap.task(key1) {
			delay(0.1.seconds)
			"dependency 1"
		}
		val dependencyList = bootstrap.dependencies()
		assertUnorderedListEquals(listOf(key1 to "dependency 1", key2 to "dependency 2: dependency 1"), dependencyList)
	}

	@Test fun extendedKeys() = runTest {
		val key1 = dKey<String>()
		val key2 = object : DKey<String> {
			override val extends: DKey<*>? = key1
		}
		val bootstrap = Bootstrap(defaultTaskTimeout = 1f)
		val task1 by bootstrap.task(key2) {
			delay(0.1.seconds)
			"Extended key"
		}
		bootstrap.awaitAll()
		assertEquals("Extended key", bootstrap.get(key1))
		assertEquals("Extended key", bootstrap.get(key2))
		val dependencyList = bootstrap.dependencies()
		assertUnorderedListEquals(listOf(key1 to "Extended key", key2 to "Extended key"), dependencyList)
	}

	@Test fun timeout() = runTest {
		val key1 = dKey<String>()
		val key2 = object : DKey<String> {
			override val extends: DKey<*>? = key1
		}
		val bootstrap = Bootstrap(defaultTaskTimeout = 0.5f)
		val task1 by bootstrap.task(key2) {
			delay(1.seconds) // Will cause a timeout
			"Extended key"
		}
		assertFailsWith(BootstrapTaskTimeoutException::class) {
			bootstrap.awaitAll()
		}
	}

	@Test fun optionalTaskTimeout() = runTest {
		val key1 = dKey<String>()
		val key2 = object : DKey<String> {
			override val extends: DKey<*>? = key1
		}
		val bootstrap = Bootstrap(defaultTaskTimeout = 0.5f)
		val task1 by bootstrap.task(key2, isOptional = true) {
			delay(1.seconds) // Will cause a timeout
			"Extended key"
		}
		assertEquals(DependencyMap(), bootstrap.dependencies())
	}
}