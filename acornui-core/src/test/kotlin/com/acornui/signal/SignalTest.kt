/*
 * Copyright 2014 Nicholas Bilyk
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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.signal

import com.acornui.function.as1
import com.acornui.test.assertListEquals
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SignalTest {
	

	@Test fun dispatch() {
		var actual = -1
		val s = unmanagedSignal<Int>()
		s.listen { actual = it }
		s.dispatch(3)
		assertEquals(3, actual)
		s.dispatch(4)
		assertEquals(4, actual)
	}

	@Test fun remove() {
		var actual = -1
		val s = unmanagedSignal<Int>()
		val handler = { it: Int -> actual = it }
		val sub = s.listen(handler)
		s.dispatch(3)
		assertEquals(3, actual)
		sub.dispose()
		s.dispatch(4)
		assertEquals(3, actual)
	}

	@Test fun addMany() {

		val arr = Array(5) { -1 }
		var i = 0

		val handler1 = { it: Int -> arr[i++] = 1 }
		val handler2 = { it: Int -> arr[i++] = 2 }
		val handler3 = { it: Int -> arr[i++] = 3 }
		val handler4 = { it: Int -> arr[i++] = 4 }
		val handler5 = { it: Int -> arr[i++] = 5 }

		val s = unmanagedSignal<Int>()
		s.listen(handler1)
		s.listen(handler2)
		s.listen(handler3)
		s.listen(handler4)
		s.listen(handler5)

		s.dispatch(0)

        assertListEquals(arrayOf(1, 2, 3, 4, 5), arr)
	}

	@Test fun addOnceMany() {

		val arr = Array(5) { -1 }
		var i = 0

		val handler1 = { it: Int -> arr[i++] = 1 }
		val handler2 = { it: Int -> arr[i++] = 2 }
		val handler3 = { it: Int -> arr[i++] = 3 }
		val handler4 = { it: Int -> arr[i++] = 4 }
		val handler5 = { it: Int -> arr[i++] = 5 }

		val s = unmanagedSignal<Int>()
		s.listen(true, handler1)
		s.listen(true, handler2)
		s.listen(true, handler3)
		s.listen(true, handler4)
		s.listen(true, handler5)

		s.dispatch(0)

        assertListEquals(arrayOf(1, 2, 3, 4, 5), arr)
	}

	@Test fun concurrentAdd() {
		val arr = arrayListOf<Int>()

		val s = unmanagedSignal<Unit>()
		s.once {
			arr.add(1)
			s.once {
				arr.add(4)
				s.once {
					arr.add(7)
					s.once {
						arr.add(8)
					}
				}
			}
		}
		s.once {
			arr.add(2)
			s.once { arr.add(5) }
			s.once { arr.add(6) }
		}
		s.once {
			arr.add(3)
		}

		// Adding a handler within a handler should not invoke the inner handler until next dispatch.
		s.dispatch(Unit)
        assertListEquals(arrayOf(1, 2, 3), arr)
		s.dispatch(Unit)
        assertListEquals(arrayOf(1, 2, 3, 4, 5, 6), arr)
		s.dispatch(Unit)
        assertListEquals(arrayOf(1, 2, 3, 4, 5, 6, 7), arr)
		s.dispatch(Unit)
        assertListEquals(arrayOf(1, 2, 3, 4, 5, 6, 7, 8), arr)
	}

	@Test fun concurrentRemove() {
		TestConcurrentRemove()
	}
	
	@Test fun disposeHandle() {
		val s = unmanagedSignal<Unit>()
		var c = 0
		val handle = s.listen {
			c++
		}
		s.dispatch(Unit)
		assertEquals(1, c)
		handle.dispose()
		s.dispatch(Unit)
		assertEquals(1, c)
	}

}

private class TestConcurrentRemove {

	private var sub2: SignalSubscription
	private var sub4: SignalSubscription
	private val s = unmanagedSignal<Unit>()
	private val arr = arrayListOf<Int>()

	init {
		s.listen(::testHandler1.as1)
		sub2 = s.listen(::testHandler2.as1)
		s.listen(::testHandler3.as1)
		sub4 = s.listen(::testHandler4.as1)

		s.dispatch(Unit)
		s.dispatch(Unit)

        assertListEquals(arrayOf(1, 2, 3, 1, 3), arr)
	}

	private fun testHandler1() {
		arr.add(1)
	}

	private fun testHandler2() {
		arr.add(2)
		sub2.dispose()
	}

	private fun testHandler3() {
		arr.add(3)
		sub4.dispose()
	}

	private fun testHandler4() {
		fail("testHandler4 should not have been invoked.")
	}
}
