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

package com.acornui.io.file

import kotlin.test.Test
import kotlin.test.assertEquals

class PathTest {

	@Test
	fun stripComponents() {
		assertEquals("one/two/three", Path("one/two/three").stripComponents(0).value)
		assertEquals("two/three", Path("one/two/three").stripComponents(1).value)
		assertEquals("three", Path("one/two/three").stripComponents(2).value)
	}

	@Test
	fun parent() {
		assertEquals("one/two", Path("one/two/three").parent.value)
		assertEquals("one", Path("one/two/three").parent.parent.value)
	}

	@Test
	fun depth() {
		assertEquals(3, Path("one/two/three").depth)
		assertEquals(2, Path("one/two").depth)
		assertEquals(1, Path("one").depth)
		assertEquals(0, Path("").depth)
	}
}