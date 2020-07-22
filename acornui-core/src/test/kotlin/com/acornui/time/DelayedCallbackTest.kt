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

package com.acornui.time

import com.acornui.async.delay
import com.acornui.exitMain
import com.acornui.test.runMainTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.seconds

class DelayedCallbackTest {


	@Test fun limit() = runMainTest {
		var c = 0
		val call = delayedCallback(0.1.seconds) {
			c++
		}

		assertEquals(0, c)
		call()
		assertEquals(0, c)
		call()
		call()
		call()
		assertEquals(0, c)
		delay(0.15.seconds)
		assertEquals(1, c)
		call()
		delay(0.05.seconds)
		call()
		delay(0.1.seconds)
		assertEquals(2, c)
		exitMain()

	}

	@Test fun dispose() = runMainTest {
		var c = 0
		val call = delayedCallback(0.1.seconds) {
			c++
		}

		assertEquals(0, c)
		call()
		assertEquals(0, c)
		call()
		call.dispose()
		delay(0.2.seconds)
		assertEquals(0, c)
		exitMain()
	}
}