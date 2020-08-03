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
import com.acornui.test.assertClose
import com.acornui.test.runTest
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.seconds

class TimerTest {

	@Test fun dispose() = runTest {
		val handle = timer(0.5.seconds) {
			fail("Disposing handle expected to prevent timer")
		}
		launch {
			delay(0.1.seconds)
			handle.dispose()
		}
		delay(1.seconds)
	}

	@Test fun disposeOnCallback() = runTest {
		var c = 0
		timer(0.1.seconds) {
			c++
			it.dispose()
		}
		delay(0.5.seconds)
		assertEquals(1, c)
	}

	@Test fun repetitions() = runTest {
		var c = 0
		timer(0.1.seconds, repetitions = 3) {
			c++
		}
		delay(0.5.seconds)
		assertEquals(3, c)
	}

	@Test fun delay() = runTest {
		val start = markNow()
		var callTime = -1.0
		timer(0.1.seconds, delay = 0.3.seconds) {
			callTime = start.elapsedNow().inMilliseconds
		}
		delay(0.5.seconds)
		assertClose(400.0, callTime, 50.0)
	}
}

