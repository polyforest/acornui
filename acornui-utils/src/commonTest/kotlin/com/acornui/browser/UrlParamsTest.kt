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

package com.acornui.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UrlParamsTest {

	@Test
	fun toQueryString() {
		assertEquals("one=1&two=2&three=3", UrlParams("one" to "1", "two" to "2", "three" to "3").toQueryString())
		assertEquals("one=1", UrlParams("one" to "1").toQueryString())
		assertEquals("one=1&two=%26%3B%23%40", UrlParams("one" to "1", "two" to "&;#@").toQueryString())
	}

	@Test
	fun contains() {
		assertTrue(UrlParams("one" to "1").contains("one"))
		assertFalse(UrlParams("one" to "1").contains("two"))
		assertTrue(UrlParams("one" to "1", "two" to "2", "three" to "3").contains("three"))
	}

	@Test
	fun getAll() {
		assertEquals(listOf("1"), UrlParams("one" to "1").getAll("one"))
		assertEquals(listOf("1", "3"), UrlParams("one" to "1", "two" to "2", "one" to "3").getAll("one"))
	}
}