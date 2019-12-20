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
import com.acornui.component.Stage
import com.acornui.component.StageImpl
import com.acornui.di.dKey
import com.acornui.di.inject
import com.acornui.gl.core.CachedGl20
import com.acornui.gl.core.Gl20
import com.acornui.graphic.exit
import com.acornui.input.KeyInput
import com.acornui.input.MouseInput
import com.acornui.io.BinaryLoader
import com.acornui.io.TextLoader
import com.acornui.io.file.Files
import com.acornui.io.file.FilesImpl
import com.acornui.test.ExpectedException
import com.acornui.test.runTest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.seconds

class JvmHeadlessApplicationTest {

	private val appConfig = AppConfig(assetsManifestPath = null)

	@Test fun start() = runBlocking {
		JvmHeadlessApplication().start(appConfig) {
			assertTrue(inject(Files) is FilesImpl)
			assertTrue(inject(Stage) is StageImpl)
			assertEquals(inject(CachedGl20), MockGl20)
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
	@Test fun failedApplicationShouldntStall() = runTest {
		assertFailsWith(ExpectedException::class) {
			object : JvmHeadlessApplication() {
				@Suppress("unused")
				val failedTask by task(dKey()) {
					throw ExpectedException("Should fail")
				}
			}.start(appConfig) {
				fail("Application should not start")
			}
		}
	}

	/**
	 * Test that boot tasks that take longer than their set timeouts fail, and if the task is optional the application 
	 * should not start.
	 */
	@Test fun taskTimeout() = runTest {
		assertFailsWith(TimeoutCancellationException::class) {
			object : JvmHeadlessApplication() {
				@Suppress("unused")
				val failedTask by task(dKey(), timeout = 1f) {
					delay(20.seconds)
				}
			}.start(appConfig) {
				fail("Application should not start")
			}
		}
	}
	
	/**
	 * Test that boot tasks that take longer than their set timeouts fail, and if the task is optional the application 
	 * should not start.
	 */
	@Test fun optionalTaskTimeout() = runTest {
		object : JvmHeadlessApplication() {
			@Suppress("unused")
			val failedTask by task(dKey(), timeout = 1f, isOptional = true) {
				delay(20.seconds)
			}
		}.start(appConfig) {
			exit()
		}
	}
}