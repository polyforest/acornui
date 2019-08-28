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

import com.acornui.asset.Loaders
import com.acornui.async.delay
import com.acornui.component.Stage
import com.acornui.component.StageImpl
import com.acornui.di.dKey
import com.acornui.di.inject
import com.acornui.gl.core.Gl20
import com.acornui.graphic.exit
import com.acornui.input.KeyInput
import com.acornui.input.MouseInput
import com.acornui.io.BinaryLoader
import com.acornui.io.TextLoader
import com.acornui.io.file.Files
import com.acornui.io.file.FilesImpl
import com.acornui.mock.MockGl20
import com.acornui.mock.MockKeyInput
import com.acornui.mock.MockMouseInput
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.test.*

class JvmHeadlessApplicationTest {

	@Test fun start() {
		JvmHeadlessApplication("src/jvmTest/resources").start {
			assertTrue(inject(Files) is FilesImpl)
			assertTrue(inject(Stage) is StageImpl)
			assertEquals(inject(Gl20), MockGl20)
			assertEquals(inject(MouseInput), MockMouseInput)
			assertEquals(inject(KeyInput), MockKeyInput)
			assertTrue(inject(Loaders.textLoader) is TextLoader)
			assertTrue(inject(Loaders.binaryLoader) is BinaryLoader)
			exit()
		}
	}

	@Test fun failedApplicationShouldntStall() {
		assertFailsWith(ExpectedException::class) {
			object : JvmHeadlessApplication("src/jvmTest/resources") {
				val failedTask by task(dKey()) {
					throw ExpectedException("Should fail")
				}
			}.start {
				fail("Application should not start")
			}
		}
	}

	@Test fun taskTimeout() {
		assertFailsWith(TimeoutCancellationException::class) {
			object : JvmHeadlessApplication("src/jvmTest/resources") {
				val failedTask by task(dKey(), timeout = 1f) {
					delay(20f)
				}
			}.start {
				fail("Application should not start")
			}
		}
	}
}

private class ExpectedException(message: String) : Exception(message)