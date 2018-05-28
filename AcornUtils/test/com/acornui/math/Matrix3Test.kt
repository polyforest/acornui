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
import com.acornui.test.assertClose
import com.acornui.test.assertListEquals
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * @author nbilyk
 */
class Matrix3Test {

	lateinit var m1: Matrix3
	lateinit var m2: Matrix3

	@Before
	fun setUp() {
		m1 = Matrix3(0.3f, 0.7f, 1.4f, 13f, 5.9f, 0.1f, 1.1f, 1.17f, 23f)
		m2 = Matrix3(-0.5f, -1.6f, 2.5f, 9f, 1.6f, 2.34f, 9.32f, -2.2f, -1.15f)
	}

	@Test
	fun scl() {
		assertListEquals(Matrix3(1f * 2f, 2f, 3f, 4f, 5f * 2f, 6f, 7f, 8f, 9f).values, Matrix3(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f).scl(2f).values)
	}

	@Test
	fun idt() {
		m1.idt()
		assertListEquals(Matrix3.IDENTITY.values, m1.values)
	}

	@Test
	fun mul() {
		m1.mul(m2)
		assertListEquals(listOf(-18.2f, -6.8650007f, 56.64f, 26.074001f, 18.4778f, 66.58f, -27.069f, -7.801501f, -13.622f), m1.values)
	}

	@Test
	fun times() {
		assertListEquals(listOf(-18.2f, -6.8650007f, 56.64f, 26.074001f, 18.4778f, 66.58f, -27.069f, -7.801501f, -13.622f), (m1 * m2).values)
	}

	@Test
	fun mulLeft() {
		m2.mulLeft(m1)
		assertListEquals(listOf(-18.2f, -6.8650007f, 56.64f, 26.074001f, 18.4778f, 66.58f, -27.069f, -7.801501f, -13.622f), m2.values)
	}

	@Test
	fun prj() {
		assertClose(Vector2(34.5f, 18.02f), m1.prj(Vector2(3f, 2.5f)))
	}

	@Test
	fun det() {
		assertClose(-156.34009f, m1.det())
	}

	@Test
	fun inv() {
		m1.inv()
		assertListEquals(listOf(-0.86723113f, 0.09250347f, 0.0523858f, 1.9117938f, -0.034284234f, -0.11622098f, -0.05577584f, -0.0026800546f, 0.046884965f), m1.values)
		m2.inv()
		assertListEquals(listOf(-0.023647474f, 0.05247052f, 0.055358544f, -0.2298895f, 0.1624513f, -0.16920671f, 0.24814126f, 0.114462934f, -0.097220585f), m2.values)
	}

	@Test
	fun set() {
		m1.set(m2)
		assertEquals(m2, m1)
	}

	@Test
	fun set1() {
		m1.set(m2.values)
		assertEquals(m2, m1)
	}

	@Test
	fun set2() {
		val m3 = Matrix4(arrayListOf(
				3f, 5f, 6f, 13f,
				17f, 23f, 27f, 35f,
				19f, 101f, 73f, 19f,
				11f, 25f, 41f, 43f))

		m1.set(m3)
		assertListEquals(listOf(3.0f, 5.0f, 6.0f, 17.0f, 23.0f, 27.0f, 19.0f, 101.0f, 73.0f), m1.values)
	}

	@Test
	fun setTranslation() {
		m1.setTranslation(2f, 3f)
		assertListEquals(listOf(0.3f, 0.7f, 1.4f, 13f, 5.9f, 0.1f, 2f, 3f, 23f), m1.values)
	}

	@Test
	fun trn() {
		m1.trn(9f, -23f)
		assertListEquals(listOf(0.3f, 0.7f, 1.4f, 13f, 5.9f, 0.1f, 1.1f + 9f, 1.17f - 23f, 23f), m1.values)
	}

	@Test
	fun trn1() {
		m1.trn(Vector2(9f, -23f))
		assertListEquals(listOf(0.3f, 0.7f, 1.4f, 13f, 5.9f, 0.1f, 1.1f + 9f, 1.17f - 23f, 23f), m1.values)
	}

	@Test
	fun trn2() {
		m1.trn(Vector3(9f, -23f, 100f))
		assertListEquals(listOf(0.3f, 0.7f, 1.4f, 13f, 5.9f, 0.1f, 1.1f + 9f, 1.17f - 23f, 23f), m1.values)
	}

	@Test
	fun rotate() {
		m1.rotate(PI / 2f)
		assertListEquals(listOf(13.0f, 5.9f, 0.1f, -0.3f, -0.7f, -1.4f, 1.1f, 1.17f, 23.0f), m1.values, { a, b -> a.closeTo(b) })
		m1.rotate(-PI / 2f)
		assertListEquals(listOf(0.3f, 0.7f, 1.4f, 13f, 5.9f, 0.1f, 1.1f, 1.17f, 23f), m1.values, { a, b -> a.closeTo(b) })
		m1.rotate(2.131f)
		assertListEquals(listOf(10.853501f, 4.6262155f, -0.65918756f, -7.1618085f, -3.7280197f, -1.2391415f, 1.1f, 1.17f, 23.0f), m1.values, { a, b -> a.closeTo(b) })
	}

	@Test
	fun getTranslation() {
		assertClose(Vector2(1.1f, 1.17f), m1.getTranslation(Vector2()))
	}

	@Test
	fun getScale() {
		assertClose(Vector2(13.003461f, 5.9413805f), m1.getScale(Vector2()))
	}

	@Test
	fun getRotation() {
		assertClose(1.1659045f, m1.getRotation())
		m1.rotate(PI)
		assertClose(-1.9756892f, m1.getRotation())
	}

	@Test
	fun scl1() {
		m1.scl(3f)
		assertListEquals(listOf(0.90000004f, 0.7f, 1.4f, 13.0f, 17.7f, 0.1f, 1.1f, 1.17f, 23.0f), m1.values)
	}

	@Test
	fun scl2() {
		m1.scl(3f, 4f)
		assertListEquals(listOf(0.90000004f, 0.7f, 1.4f, 13.0f, 23.6f, 0.1f, 1.1f, 1.17f, 23.0f), m1.values)
	}

	@Test
	fun scl3() {
		m1.scl(Vector2(3f, 4f))
		assertListEquals(listOf(0.90000004f, 0.7f, 1.4f, 13.0f, 23.6f, 0.1f, 1.1f, 1.17f, 23.0f), m1.values)
	}

	@Test
	fun transpose() {
		m1.transpose()
		assertListEquals(listOf(0.3f, 13.0f, 1.1f, 0.7f, 5.9f, 1.17f, 1.4f, 0.1f, 23.0f), m1.values)
	}

	@Test
	fun copy() {
		val copy = m1.copy()
		assertListEquals(m1.values, copy.values)
		assertFalse(m1.values === copy.values)
	}


}


private fun <E> List<E>.printF() {
	println(joinToString("f, ") + "f")
}
