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

package com.acornui.headless

import com.acornui.app
import com.acornui.async.TimeoutException
import com.acornui.component.ComponentInit
import com.acornui.component.Div
import com.acornui.component.UiComponent
import com.acornui.component.stage
import com.acornui.di.Context
import com.acornui.own
import com.acornui.test.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.seconds

class ApplicationTest {

	@Test
	fun expectTimeout1() = runTest(timeout = 1.seconds) {
		delay(2.seconds)
	}.assertFailsWith<TimeoutCancellationException>()

	@Test
	fun expectTimeout2() = runTest(timeout = 1.seconds) {
		launch {
			delay(2.seconds)
		}
	}.assertFailsWith<TimeoutCancellationException>()

	@Test
	fun expectFails1() = runTest {
		launch {
			delay(1.seconds)
			throw ExpectedException()
		}
	}.assertFailsWith<ExpectedException>()

	@Test
	fun expectFails2() = runTest {
		launch {
			fail("Expected failure")
		}
	}.assertFails()

	@Test
	fun expectFails3() = runApplicationTest(timeout = 1.seconds) { _, _ ->
		own(launch {
			throw ExpectedException()
		})
	}.assertFailsWith<ExpectedException>()

	@Test
	fun expectFails4(): Promise<Unit> = runAsyncTest(2.seconds) { resolve, reject ->
		val innerPromise = runApplicationTest(timeout = 0.3.seconds) { _, _ ->
			own(launch {
				delay(1.seconds)
				println("SHOULD NOT")
				fail("Expected failure 2")
			})
		}.assertFailsWith<TimeoutException>()
		innerPromise.then { resolve() }
		innerPromise.catch(reject)
	}

	@Test
	fun addToStage() = runTest {
		app {
			stage.addElement(testComponent())
			+testComponent()
			assertEquals(2, dom.childElementCount)
		}
	}

}

private class TestComponent(owner: Context) : Div(owner)

private inline fun Context.testComponent(init: ComponentInit<UiComponent> = {}): UiComponent = TestComponent(this).apply(init)