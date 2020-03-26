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
import com.acornui.test.assertClose
import com.acornui.test.assertListEquals
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

/**
 * @author nbilyk
 */
class Matrix2Test {

	@BeforeTest
	fun setUp() {
	}

	@Test
	fun scl() {
		assertListEquals(Matrix2(1f * 2f, 2f, 3f, 4f * 2f).values, Matrix2(1f, 2f, 3f, 4f).scl(2f).values)
	}

	@Test
	fun idt() {
		val m1 = Matrix2(0.3f, 0.7f, 13f, 5.9f).idt()
		assertListEquals(Matrix2.IDENTITY.values, m1.values)
	}

	@Test
	fun mul() {
		val m1 = Matrix2(0.3f, 5.9f, 13f, 0.7f)
		val m2 = Matrix2().scl(2f, 3f)
		m1.mul(m2)
		assertListEquals(Matrix2(0.3f * 2f, 5.9f * 2f, 13f * 3f, 0.7f * 3f).values, m1.values)
	}


	@Test
	fun prj() {
		val m1 = Matrix2().scl(3f, 4f)
		assertClose(vec2(3f * 3f, 2.5f * 4f), m1.prj(vec2(3f, 2.5f)))
	}

	@Test
	fun inv() {
		run {
			val m1 = Matrix2(0.3f, 0.7f, 13f, 5.9f)
			val m2 = Matrix2(-0.5f, -1.6f, 9f, 1.6f)
			assertListEquals(Matrix2.IDENTITY.values, m1.set(m2).inv().mul(m2).values)
		}
		run {
			val m1 = Matrix2(-0.5f, -1.6f, 9f, 1.6f)
			val m2 = Matrix2(0.3f, 0.7f, 13f, 5.9f)
			assertListEquals(Matrix2.IDENTITY.values, m1.set(m2).inv().mul(m2).values)
		}
	}

	@Test
	fun set() {
		val m1 = Matrix2(0.3f, 0.7f, 13f, 5.9f)
		val m2 = Matrix2(-0.5f, -1.6f, 9f, 1.6f)
		m1.set(m2)
		assertEquals(m2, m1)
	}

	@Test
	fun set1() {
		val m1 = Matrix2(0.3f, 0.7f, 13f, 5.9f)
		val m2 = Matrix2(-0.5f, -1.6f, 9f, 1.6f)
		m1.set(m2.values)
		assertEquals(m2, m1)
	}

	@Test
	fun rotate() {
		val m1 = Matrix2(1f, 0f, 2f, 0f)
		m1.rotate(PI / 2f)
		assertListEquals(floatArrayOf(2f, 0f, -1f, 0f), m1.values)
		m1.rotate(-PI / 2f)
		assertListEquals(floatArrayOf(1f, 0f, 2f, 0f), m1.values)

		val m3 = Matrix2()
		m3.rotate(PI / 2f)
		assertClose(vec2(0f, 3f), m3.prj(vec2(3f, 0f)))
		m3.rotate(PI / 2f)
		assertClose(vec2(-3f, 0f), m3.prj(vec2(3f, 0f)))
	}

	@Test
	fun getScale() {
		val m1 = Matrix2(0.3f, 0.7f, 13f, 5.9f)
		println(Matrix2().apply {
			scl(2f, 3f)
		})
		assertClose(vec2(13.003461f, 5.9413805f), m1.getScale(vec2()))
	}

	@Test
	fun getRotation() {
		val m1 = Matrix2(0.3f, 0.7f, 13f, 5.9f)
		assertClose(1.1659045f, m1.getRotation())
		m1.rotate(PI)
		assertClose(-1.9756892f, m1.getRotation())
	}

	@Test
	fun scl1() {
		val m1 = Matrix2(1f, 2f, 3f, 4f)
		m1.scl(3f)
		assertListEquals(floatArrayOf(3f, 2f, 3f, 12f), m1.values)
	}

	@Test
	fun tra() {
		val m1 = Matrix2(1f, 2f, 3f, 4f)
		m1.tra()
		assertListEquals(floatArrayOf(1f, 3f, 2f, 4f), m1.values)
	}

	@Test
	fun copy() {
		val m1 = Matrix2(0.3f, 0.7f, 13f, 5.9f)
		val copy = m1.copy()
		assertListEquals(m1.values, copy.values)
		assertNotSame(m1.values, copy.values)
	}

	@Test
	fun serialize() {
		val m1 = Matrix2(0.3f, 0.7f, 13f, 5.9f)
		val m2 = Matrix2(-0.5f, -1.6f, 9f, 1.6f)

		val json1 = jsonStringify(Matrix2.serializer(), m1)
		assertEquals(m1, jsonParse(Matrix2.serializer(), json1))

		val json2 = jsonStringify(Matrix2.serializer(), m2)
		assertEquals(m2, jsonParse(Matrix2.serializer(), json2))
	}
}
