/*
 * Copyright 2015 Nicholas Bilyk
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

import com.acornui.serialization.jsonStringify
import com.acornui.test.assertClose
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * @author nbilyk
 */
class Vector3Test {

	@Test fun class_len() {
		assertEquals(1.0, Vector3.len(1.0, 0.0, 0.0))
		assertEquals(1.0, Vector3.len(0.0, 0.0, 1.0))
		assertEquals(1.0, Vector3.len(0.0, 1.0, 0.0))
        assertClose(sqrt(3.0), Vector3.len(-1.0, 1.0, -1.0))
	}

	@Test fun class_len2() {
		assertEquals(1.0, Vector3.len2(1.0, 0.0, 0.0))
		assertEquals(1.0, Vector3.len2(0.0, 0.0, 1.0))
		assertEquals(1.0, Vector3.len2(0.0, 1.0, 0.0))
        assertClose(3.0, Vector3.len2(-1.0, 1.0, -1.0))
	}

	@Test fun copy() {
		assertEquals(vec3(1.0, 2.0, 3.0), vec3(1.0, 2.0, 3.0).copy())
	}

	@Test fun plus() {
		assertEquals(vec3(4.0, 5.0, 6.0), vec3(1.0, -1.0, 9.0) + vec3(3.0, 6.0, -3.0))
		assertEquals(vec3(4.0, 5.0, 6.0), vec3(1.0, -1.0, 9.0).plus(3.0, 6.0, -3.0))
	}

	@Test fun sub() {
		assertEquals(vec3(-2.0, -7.0, 12.0), vec3(1.0, -1.0, 9.0).minus(vec3(3.0, 6.0, -3.0)))
		assertEquals(vec3(-2.0, -7.0, 12.0), vec3(1.0, -1.0, 9.0).minus(3.0, 6.0, -3.0))
	}

	@Test fun crs() {
		assertEquals(vec3(1.0, 0.0, -1.0), vec3(0.0, 1.0, 0.0).crs(vec3(1.0, 0.0, 1.0)))
		assertEquals(vec3(1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0).crs(vec3(0.0, 0.0, 1.0)))
	}

	@Test fun equalsTest() {
		assertEquals(vec3(1.0, 2.0, 3.0), vec3(1.0, 2.0, 3.0))
		assertNotEquals(vec3(1.0, 2.0, 3.0), vec3(1.0, 2.0, 4.0))
	}

	@Test fun serialize() {
		jsonStringify(Vector3.serializer(), vec3(1.0, 2.0, 3.0))
	}
}