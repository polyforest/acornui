/*
 * Copyright 2020 Poly Forest, LLC
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

package com.acornui.observe

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ModTagWatchImplTest {

	@Test
	fun checkSingleModTag() {
		val modTag = modTag()
		val watch = Watch()
		assertTrue(watch.check(modTag))
		assertFalse(watch.check(modTag))
		modTag.updateCheckValue()
		assertTrue(watch.check(modTag))
		assertFalse(watch.check(modTag))
		assertFalse(watch.check(modTag))
		modTag.updateCheckValue()
		assertTrue(watch.check(modTag))
		assertFalse(watch.check(modTag))
		watch.check(modTag) {
			fail()
		}
		var called = false
		modTag.updateCheckValue()
		watch.check(modTag) {
			called = true
		}
		assertTrue(called)
	}

	@Test
	fun checkMultipleMutable() {
		val modTagA = modTag()
		val modTagB = modTag()
		val watch = Watch()
		assertTrue(watch.check(modTagA, modTagB))
		assertFalse(watch.check(modTagA, modTagB))
		watch.check(modTagA, modTagB) {
			fail()
		}
		run {
			var called = false
			modTagA.updateCheckValue()
			watch.check(modTagA, modTagB) {
				called = true
			}
			assertTrue(called)
		}
		assertFalse(watch.check(modTagA, modTagB))
		modTagB.updateCheckValue()
		assertTrue(watch.check(modTagA, modTagB))
		assertTrue(watch.check(modTagA))
	}

	@Test
	fun checkMultipleDataObjects() {
		val dataA = TestData(1, 2)
		val dataB = TestData(3, 4)
		val dataC = TestData(5, 6)
		val watch = Watch()
		assertTrue(watch.check(dataA, dataB, dataC))
		assertFalse(watch.check(dataA, dataB, dataC))
		assertFalse(watch.check(dataA, TestData(3, 4), dataC))
		assertTrue(watch.check(dataA, TestData(4, 4), dataC))
	}

	@Test
	fun checkMixed() {
		val dataA = TestData(1, 2)
		val dataB = modTag()
		val dataC = TestData(5, 6)
		val watch = Watch()
		assertTrue(watch.check(dataA, dataB, dataC))
		assertFalse(watch.check(dataA, dataB, dataC))
		assertFalse(watch.check(dataA, dataB, TestData(5, 6)))
		assertTrue(watch.check(dataA, dataB, TestData(6, 6)))
		assertFalse(watch.check(dataA, dataB, TestData(6, 6)))
		dataB.updateCheckValue()
		assertTrue(watch.check(dataA, dataB, TestData(6, 6)))

	}

	private data class TestData(val a: Int, var b: Int)
}