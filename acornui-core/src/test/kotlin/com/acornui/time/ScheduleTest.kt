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

package com.acornui.time

import com.acornui.test.assertClose
import com.acornui.test.runAsyncTest
import kotlin.test.Test
import kotlin.time.TimeSource
import kotlin.time.seconds

class ScheduleTest {

	@Test
	fun scheduleTest() = runAsyncTest(timeout = 4.seconds) {
		resolve, _ ->
		val mark = TimeSource.Monotonic.markNow()
		schedule(1.seconds) {
			assertClose(1.0, mark.elapsedNow().inSeconds, 0.2) // CI For mac has an unusually high variance
			resolve()
		}
	}
}