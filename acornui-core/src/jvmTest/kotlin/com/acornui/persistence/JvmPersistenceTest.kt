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

package com.acornui.persistence

import org.junit.Test
import com.acornui.Version
import org.junit.After
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmPersistenceTest {

	private val persistenceDir: File = File("build/processedResources/jvm/test-out")

	@After
	fun finish() {
		persistenceDir.deleteRecursively()
	}

	@Test
	fun getItem() {
		run {
			val persistence = JvmPersistence(Version(1, 2, 3, 4), "acornPersistenceTest", persistenceDir)
			assertNull(persistence.version)
			persistence.setItem("key1", "test")
			assertEquals("test", persistence.getItem("key1"))
		}
		run {
			val persistence = JvmPersistence(Version(1, 0, 0, 0), "acornPersistenceTest", persistenceDir)
			assertEquals(Version(1, 2, 3, 4), persistence.version)
			assertEquals("test", persistence.getItem("key1"))
		}
	}

}