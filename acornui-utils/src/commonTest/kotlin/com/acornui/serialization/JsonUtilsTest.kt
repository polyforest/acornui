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
		assertEquals(Foo001(7, 6), jsonParseOrElse(Foo001.serializer(), """{"a": 7, "b": 6}""") { Foo001(3, 4) })
		assertEquals(Foo001(3, 4), jsonParseOrElse(Foo001.serializer(), """{"junk": 1, "b": 2}""") { Foo001(3, 4) })
		assertEquals(Foo001(3, 4), jsonParseOrElse(Foo001.serializer(), "") { Foo001(3, 4) })
		assertEquals(Foo001(3, 4), jsonParseOrElse(Foo001.serializer(), null) { Foo001(3, 4) })
	}
}

@Serializable
private data class Foo001(val a: Int, val b: Int)