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

package com.acornui.io

import com.acornui.serialization.binaryParse
import com.acornui.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadKtTest {

	@Test
	fun loadTextLocal() = runTest {
		val progressReporter = ProgressReporterImpl()
		val request = "src/jvmTest/resources/textToLoad.txt".toUrlRequestData()
		assertEquals("text to load contents", TextLoader().load(request, progressReporter))
	}

	@Test
	fun loadBinaryLocal() = runTest {
		val progressReporter = ProgressReporterImpl()
		val request = "src/jvmTest/resources/binaryToLoad.bin".toUrlRequestData()
		assertEquals(TestData("binary to load contents"), binaryParse(TestData.serializer(), BinaryLoader().load(request, progressReporter).toByteArray()))
	}

	@Test
	fun shouldNotRemoveFirstSlash() {
		val request = "/src/test".toUrlRequestData()
		assertEquals("/src/test", request.url)
	}

	@Serializable
	private data class TestData(val text: String)
}