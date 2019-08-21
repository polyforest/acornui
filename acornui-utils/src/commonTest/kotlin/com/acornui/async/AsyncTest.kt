/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.async

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsyncTest {


	@Test fun testAsync() {

		globalLaunch {
			val a = globalAsync { 3 }
			val b = globalAsync { 4 }

			assertEquals(7, a.await() + b.await())
		}
	}

	private var t = 0

	@Test fun testAsyncMultiple() {
		var launched = false
		globalLaunch {
			val a = globalAsync { t++; 3 }
			assertEquals(9, a.await() + a.await() + a.await())
			assertEquals(1, t)
			launched = true
		}
		assertTrue(launched)
	}

	private var s = 0

//	@Test fun testAsyncNested() {
//
//		launch {
//			val a = async {
//				s++
//				val a1 = async {
//					delay(1)
//					3
//				}
//				val a2 = async { 4 }
//				a1.await() + a2.await()
//			}
//
//			assertEquals(21, a.await() + a.await() + a.await())
//			assertEquals(1, s)
//		}
//	}
}