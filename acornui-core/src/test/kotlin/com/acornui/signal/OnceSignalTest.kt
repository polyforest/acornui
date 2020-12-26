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
import kotlin.test.*

class OnceSignalTest {
	

	@Test fun dispatchCanOnlyHappenOnce() {
		var actual = -1
		val s = OnceSignal<Int>()
		s.listen { actual = it }
		s.dispatch(3)
		assertEquals(3, actual)
		assertFails {
			s.dispatch(4)
		}
	}

	@Test fun listenDefaultsToOnce() {

		val arr = Array(5) { -1 }
		var i = 0

		val handler1 = { it: Int -> arr[i++] = 1 }
		val handler2 = { it: Int -> arr[i++] = 2 }
		val handler3 = { it: Int -> arr[i++] = 3 }
		val handler4 = { it: Int -> arr[i++] = 4 }
		val handler5 = { it: Int -> arr[i++] = 5 }

		val s = OnceSignal<Int>()
		s.listen(true, handler1)
		val sub = s.listen(handler2)
		assertTrue(sub.isOnce)
		s.listen(true, handler3)
		s.listen(handler4)
		s.listen(true, handler5)

		s.dispatch(0)

		assertTrue(s.isEmpty())

        assertListEquals(arrayOf(1, 2, 3, 4, 5), arr)
	}

	@Test fun addAfterDispatchIsIgnored() {
		val s = OnceSignal<Int>()
		s.listen {}
		s.dispatch(0)
		s.listen {}
		assertTrue(s.isEmpty())
	}

}
