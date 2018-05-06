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

import com.acornui.core.closeTo
import com.acornui.test.assertListEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class Matrix4Test {

	private lateinit var m1: Matrix4
	private lateinit var m2: Matrix4

	@Before
	fun setUp() {
		m1 = Matrix4(arrayListOf(
				3f, 5f, 6f, 13f,
				17f, 23f, 27f, 35f,
				19f, 101f, 73f, 19f,
				11f, 25f, 41f, 43f))

		m2 = Matrix4(arrayListOf(
				9f, 4f, 77f, 65f,
				44f, 36f, 16f, 98f,
				26f, 3f, 9f, 27f,
				43f, 87f, 38f, 75f))
	}

	@Test fun mul() {
		m1.mul(m2)
		assertListEquals(arrayListOf(2273f, 9539f, 8448f, 4515f, 2126f, 5114f, 6422f, 6350f, 597f, 1783f, 2001f, 1775f, 3155f, 7929f, 8456f, 7551f), m1.values)
	}

	@Test
	fun set() {
		m1.set(m2)
		assertEquals(m1, m2)
	}

	@Test
	fun set1() {
		m1.set(m2.values)
		assertEquals(m1, m2)
	}

	@Test
	fun trn() {
	}

	@Test
	fun trn1() {
	}

	@Test
	fun mulLeft() {
	}

	@Test
	fun tra() {
	}

	@Test
	fun idt() {
	}

	@Test
	fun inv() {
	}

	@Test
	fun det() {
	}

	@Test
	fun det3x3() {
	}

	@Test
	fun setTranslation() {
	}

	@Test
	fun setTranslation1() {
	}

	@Test
	fun setFromEulerAnglesRad() {
	}

	@Test
	fun setToLookAt() {
	}

	@Test
	fun setToLookAt1() {
	}

	@Test
	fun setToGlobal() {
	}

	@Test
	fun lerp() {
	}

	@Test
	fun set9() {
	}

	@Test
	fun scl() {
	}

	@Test
	fun scl1() {
	}

	@Test
	fun scl2() {
	}

	@Test
	fun getTranslationX() {
	}

	@Test
	fun getTranslationY() {
	}

	@Test
	fun getTranslationZ() {
	}

	@Test
	fun getTranslation() {
	}

	@Test
	fun getRotation() {
	}

	@Test
	fun getRotation1() {
	}

	@Test
	fun getScaleXSquared() {
	}

	@Test
	fun getScaleYSquared() {
	}

	@Test
	fun getScaleZSquared() {
	}

	@Test
	fun getScaleX() {
	}

	@Test
	fun getScaleY() {
	}

	@Test
	fun getScaleZ() {
	}

	@Test
	fun getScale() {
	}

	@Test
	fun toNormalMatrix() {
	}

	@Test
	fun translate() {
	}

	@Test
	fun translate1() {
	}

	@Test
	fun rotate() {
		val quat = Quaternion(0.2f, 0.6f, 0.1f, 0.7f)
		m1.rotate(quat)
		println(m1.values)
		assertListEquals(listOf(-7.96f, -70.76f, -46.58f, 1.48f, 23.2f, 61.6f, 54.1f, 40.4f, 3.72f, 20.92f, 15.56f, 9.64f, 11f, 25f, 41f, 43f), m1.values, { a, b -> a.closeTo(b) })
	}

	@Test
	fun rotate1() {
		m1.rotate(Vector3(0.2f, 0.65f, 0.33f), Vector3(-0.4f, 2.5f, 33f))
		assertListEquals(listOf(3f, 5f, 6f, 13f, 17f, 23f, 27f, 35f, 19f, 101f, 73f, 19f, 11f, 25f, 41f, 43f), m1.values, { a, b -> a.closeTo(b) })
	}

	@Test
	fun rotate2() {
		m1.rotate(3f, -23f, 1f, 3.23f)
		assertListEquals(listOf(-8.751207f, -18.508787f, -18.365849f, -23.04679f, 13.779734f, 11.132234f, 17.437412f, 28.595972f, -19.812506f, -101.43224f, -73.84195f, -20.152323f, 11f, 25f, 41f, 43f), m1.values, { a, b -> a.closeTo(b) })
	}

	@Test
	fun scale() {
	}

	@Test
	fun scale1() {
	}

	@Test
	fun extract4x3Matrix() {
	}

	@Test
	fun prj() {
	}

	@Test
	fun prj1() {
	}

	@Test
	fun rot() {
	}

	@Test
	fun rot1() {
	}

	@Test
	fun shearZ() {
	}

	@Test
	fun getValues() {
	}

	@Test
	fun component1() {
	}

	@Test
	fun copy() {
	}


}