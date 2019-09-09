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

package com.acornui.serialization

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonUtilsTest {

	@Test fun testJsonParseOrElse() {
		assertEquals(Foo(1, 2), jsonParseOrElse(Foo.serializer(), """{"a": 1, "b": 2}""") { Foo(3, 4) })
		assertEquals(Foo(3, 4), jsonParseOrElse(Foo.serializer(), """{"junk": 1, "b": 2}""") { Foo(3, 4) })
		assertEquals(Foo(3, 4), jsonParseOrElse(Foo.serializer(), "") { Foo(3, 4) })
		assertEquals(Foo(3, 4), jsonParseOrElse(Foo.serializer(), null) { Foo(3, 4) })
	}
}

@Serializable
private data class Foo(val a: Int, val b: Int)