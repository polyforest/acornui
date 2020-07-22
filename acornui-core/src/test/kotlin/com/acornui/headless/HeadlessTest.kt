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

import com.acornui.component.*
import com.acornui.di.Context
import com.acornui.di.exit
import com.acornui.exitMain
import com.acornui.test.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.fail
import kotlin.time.seconds

class HeadlessTest {

	@Test
	fun expectTimeout1() = runMainTest(timeout = 1.seconds) {
		delay(2.seconds)
		exitMain()
	}.assertFailsWith<TimeoutCancellationException>()

	@Test
	fun expectTimeout2() = runMainTest(timeout = 1.seconds) {
		launch {
			delay(2.seconds)
			exitMain()
		}
	}.assertFailsWith<TimeoutCancellationException>()

	@Test
	fun expectFails1() = runMainTest() {
		launch {
			delay(1.seconds)
			throw ExpectedException()
		}
	}.assertFailsWith<ExpectedException>()

	@Test
	fun expectFails2() = runMainTest {
		launch {
			fail("Expected failure")
		}
	}.assertFails()

	@Test
	fun expectFails3() = runHeadlessTest {
		launch {
			fail("Expected failure")
		}
	}.assertFails()

	@Test
	fun expectFails4() = runHeadlessTest(timeout = 1.seconds) {
		launch {
			delay(2.seconds)
			fail("Expected failure")
		}
	}.assertFailsWith<TimeoutCancellationException>()

	@Test
	fun runHeadlessTest() = runHeadlessTest(timeout = 1.seconds) {
		exitMain()
	}

	@Test
	fun addToStage() = runMainTest {
		headlessApplication {
			stage.addElement(testComponent())
			+testComponent()
			exitMain()
		}
	}

}

private class TestComponent(owner: Context) : DivComponent(owner)

private inline fun Context.testComponent(init: ComponentInit<UiComponent> = {}): UiComponent = TestComponent(this).apply(init)