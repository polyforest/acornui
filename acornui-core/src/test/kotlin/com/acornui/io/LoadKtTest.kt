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

package com.acornui.io

import com.acornui.logging.Log
import com.acornui.logging.Logger
import com.acornui.test.runTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlin.test.*
import kotlin.time.seconds

@Ignore // Problems with npm test-only dependencies: https://discuss.kotlinlang.org/t/js-test-npm-dependencies/16450
class LoadKtTest {

	/**
	 * Yuck... https://github.com/polyforest/acornui/issues/254
	 * https://youtrack.jetbrains.com/issue/KT-36824
	 */
	private val root = "https://raw.githubusercontent.com/polyforest/acornui/f9d37ef39a218ea2bac7ca766ae02bbb16a1dfab/acornui-utils/src/jvmTest/resources"

	@BeforeTest
	fun setup() {
		Log.level = Logger.VERBOSE
	}
	
	@Test
	fun loadText() = runTest {
		val request = "$root/textToLoad.txt".toUrlRequestData()
		assertEquals("text to load contents", TextLoader().load(request))
	}

	@Ignore
	@Test
	fun loadBinary() = runTest {
//		val request = "$root/binaryToLoad.bin".toUrlRequestData()
//		assertEquals(TestData("binary to load contents"), binaryParse(TestData.serializer(), BinaryLoader().load(request).toByteArray()))
	}

	@Test
	fun shouldNotRemoveFirstSlash() {
		val request = "/src/test".toUrlRequestData()
		assertEquals("/src/test", request.url)
	}

	@Test
	fun connectTimeout() = runTest {
		val request = "http://10.255.255.1".toUrlRequestData()
		assertFailsWith(ResponseConnectTimeoutException::class) {
			val textLoader = TextLoader()
			textLoader.load(request, textLoader.requestSettings.copy(connectTimeout = 2.seconds))
		}
	}
	
	@Ignore
	@Test
	fun loadTimeout() = runTest {
		val request = "http://10.255.255.2".toUrlRequestData()
		assertFailsWith(CancellationException::class) {
			withTimeout(0.5.seconds) {
				TextLoader().load(request)
			}
		}
	}

	@Serializable
	private data class TestData(val text: String)
}