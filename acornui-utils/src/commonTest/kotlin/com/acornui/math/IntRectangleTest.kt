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

package com.acornui.math

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class IntRectangleTest {

	@Test fun serialize() {
		val v = IntFoo987(IntRectangle(1, 2, 3, 4))
		val str = jsonStringify(IntFoo987.serializer(), v)
		assertEquals("""{"rect":[1,2,3,4]}""", str)
		assertEquals(v, jsonParse(IntFoo987.serializer(), """{"rect":[1,2,3,4]}"""))
	}

	@Test fun area() {
		assertEquals(12, IntRectangle(1, 2, 3, 4).area)
	}
}

@Serializable
private data class IntFoo987(val rect: IntRectangleRo)