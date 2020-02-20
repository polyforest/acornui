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

import com.acornui.AppConfig
import com.acornui.asset.Loaders
import com.acornui.async.delay
import com.acornui.di.BootstrapTaskTimeoutException
import com.acornui.di.contextKey
import com.acornui.gl.core.CachedGl20
import com.acornui.graphic.exit
import com.acornui.input.KeyInput
import com.acornui.input.MouseInput
import com.acornui.io.BinaryLoader
import com.acornui.io.TextLoader
import com.acornui.runMainTest
import com.acornui.test.ExpectedException
import kotlin.test.*
import kotlin.time.seconds

class JvmHeadlessApplicationTest {

	private val appConfig = AppConfig()

	@Test
	fun start() = runMainTest {
		headlessApplication(appConfig) {
			assertEquals(inject(CachedGl20), MockCachedGl20)
			assertEquals(inject(MouseInput), MockMouseInput)
			assertEquals(inject(KeyInput), MockKeyInput)
			assertTrue(inject(Loaders.textLoader) is TextLoader)
			assertTrue(inject(Loaders.binaryLoader) is BinaryLoader)
			exit()
		}
	}

	/**
	 * An application with a failing task should terminate.
	 */
	@Test
	fun failedApplicationShouldntStall() {
		assertFailsWith(ExpectedException::class) {
			runMainTest {
				object : JvmHeadlessApplication(this@runMainTest) {
					@Suppress("unused")
					val failedTask by task(contextKey()) {
						throw ExpectedException("Should fail")
					}
				}.startAsync(appConfig) {
					fail("Application should not start")
				}
			}
		}
	}

	/**
	 * Test that boot tasks that take longer than their set timeouts fail, and if the task is optional the application
	 * should not start.
	 */
	@Test
	fun taskTimeout() {
		assertFailsWith(BootstrapTaskTimeoutException::class) {
			runMainTest {
				object : JvmHeadlessApplication(this@runMainTest) {
					@Suppress("unused")
					val failedTask by task(contextKey(), timeout = 1.seconds) {
						delay(20.seconds)
					}
				}.startAsync(appConfig) {
					fail("Application should not start")
				}
			}
		}
	}

	/**
	 * Test that boot tasks that take longer than their set timeouts fail, and if the task is optional the application
	 * should not start.
	 */
	@Test
	fun optionalTaskTimeout() {
		runMainTest {
			object : JvmHeadlessApplication(this@runMainTest) {
				@Suppress("unused")
				val failedTask by task(contextKey(), timeout = 1.seconds, isOptional = true) {
					delay(20.seconds)
				}
			}.startAsync(appConfig) {
				exit()
			}
		}
	}
}