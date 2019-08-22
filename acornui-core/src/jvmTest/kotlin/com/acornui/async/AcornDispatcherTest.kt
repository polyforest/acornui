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

package com.acornui.async

import com.acornui.test.runTest
import com.acornui.time.FrameDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AcornDispatcherTest {

	@Test
	fun uiDispatcher() = runTest {
		uiThread = Thread.currentThread()
		var ranUiLaunch = false
		lateinit var asyncThread: Thread
		lateinit var launchUiThread: Thread
		launch(Dispatchers.IO) {
			asyncThread = Thread.currentThread()
			launch(Dispatchers.UI) {
				launchUiThread = Thread.currentThread()
				ranUiLaunch = true
			}
		}
		while (!ranUiLaunch) {
			FrameDriver.dispatch(0f)
			delay(10)
		}
		assertNotEquals(uiThread, asyncThread)
		assertEquals(uiThread, launchUiThread)
	}
}