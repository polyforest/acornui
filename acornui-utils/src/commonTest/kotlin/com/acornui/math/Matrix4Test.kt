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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Matrix4Test {

	private lateinit var m1: Matrix4
	private lateinit var m2: Matrix4
	private lateinit var m3: Matrix4
	private lateinit var m4: Matrix4

	@BeforeTest
	fun setUp() {
		m1 = Matrix4(floatArrayOf(
				3f, 5f, 6f, 13f,
				17f, 23f, 27f, 35f,
				19f, 101f, 73f, 19f,
				11f, 25f, 41f, 43f))

		m2 = Matrix4(floatArrayOf(
				9f, 4f, 77f, 65f,
				44f, 36f, 16f, 98f,
				26f, 3f, 9f, 27f,
				43f, 87f, 38f, 75f))

		m3 = Matrix4(floatArrayOf(
				5f, 0f, 0f, 15f,
				0f, 6f, 0f, 78f,
				0f, 0f, -9f, 37f,
				0f, 0f, 0f, 75f))

		m4 = Matrix4(floatArrayOf(
				1f, 0f, 0f, -17f,
				0f, 1f, 0f, 45f,
				0f, 0f, 1f, 23f,
				0f, 0f, 0f, 1f))
	}

	@Test
	fun mul() {
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
		m1.trn(3f, 2f, -2f)
		assertListEquals(arrayListOf(3f, 5f, 6f, 13f,
				17f, 23f, 27f, 35f,
				19f, 101f, 73f, 19f,
				11f + 3f, 25f + 2f, 41f - 2f, 43f), m1.values)
	}

	@Test
	fun mulLeft() {
		m2.mulLeft(m1)
		assertListEquals(arrayListOf(2273f, 9539f, 8448f, 4515f, 2126f, 5114f, 6422f, 6350f, 597f, 1783f, 2001f, 1775f, 3155f, 7929f, 8456f, 7551f), m2.values)
	}

	@Test
	fun idt() {
		m1.idt()
		assertEquals(Matrix4.IDENTITY, m1)
	}

	@Test
	fun inv() {
		m1.inv()
		assertListEquals(arrayListOf(-0.220252f, 0.13327442f, -0.010191573f, -0.037388105f, 0.16295114f, -0.02223616f, 0.016406294f, -0.038414393f, -0.21033126f, 0.0021095844f, -0.006043674f, 0.06454188f, 0.16215292f, -0.02317692f, -0.0011688238f, -0.0063857688f), m1.values)
	}

	@Test
	fun det() {
		m1.det()
		assertListEquals(arrayListOf(3.0f, 5.0f, 6.0f, 13.0f, 17.0f, 23.0f, 27.0f, 35.0f, 19.0f, 101.0f, 73.0f, 19.0f, 11.0f, 25.0f, 41.0f, 43.0f), m1.values)
	}

	@Test
	fun setTranslation() {
		m1.setTranslation(3f, 62f, 23f)
		assertListEquals(arrayListOf(3.0f, 5.0f, 6.0f, 13.0f, 17.0f, 23.0f, 27.0f, 35.0f, 19.0f, 101.0f, 73.0f, 19.0f, 3.0f, 62.0f, 23.0f, 43.0f), m1.values)
	}

	@Test
	fun setFromEulerAnglesRad() {
		m1.setFromEulerAnglesRad(3f, 62f, 23f)
		assertListEquals(arrayListOf(0.61562574f, -0.5700127f, -0.5441419f, 0.0f, -0.782292f, -0.35881627f, -0.50918555f, 0.0f, 0.094995186f, 0.73914564f, -0.6668129f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f), m1.values, comparator = {a, b -> a.closeTo(b, 0.01f)})
	}

	@Test
	fun setToLookAt() {
		m1.setToLookAt(Vector3(3f, 62f, 23f), Vector3(2f, 32f, -23f))
		assertListEquals(arrayListOf(-0.99850476f, 0.030565504f, -0.045319494f, 0.0f, 0.053111956f, 0.3463439f, -0.9366029f, 0.0f, -0.012931608f, -0.93760955f, -0.34744945f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f), m1.values)
	}

	@Test
	fun setToGlobal() {
		m1.setToGlobal(Vector3(3f, 62f, 23f), Vector3(2f, 32f, -23f), Vector3(7f, 12f, 322f))
		assertListEquals(arrayListOf(0.99694073f, 0.059497714f, -0.050685726f, 0.0f, -0.07585418f, 0.5801475f, -0.8109716f, 0.0f, -0.01884576f, 0.8123355f, 0.58288586f, 0.0f, 3.0f, 62.0f, 23.0f, 1.0f), m1.values)

	}

	@Test
	fun scl() {
		m1.scl(0.2f, 3f, 0.22f)
		assertListEquals(arrayListOf(0.6f, 5.0f, 6.0f, 13.0f, 17.0f, 69.0f, 27.0f, 35.0f, 19.0f, 101.0f, 16.06f, 19.0f, 11.0f, 25.0f, 41.0f, 43.0f), m1.values)
	}

	@Test
	fun getTranslationX() {
		assertEquals(11f, m1.translationX)
	}

	@Test
	fun getTranslationY() {
		assertEquals(25f, m1.translationY)
	}

	@Test
	fun getTranslationZ() {
		assertEquals(41f, m1.translationZ)
	}

	@Test
	fun getTranslation() {
		val out = Vector3()
		assertEquals(Vector3(11f, 25f, 41f), m1.getTranslation(out))
	}

	@Test
	fun getRotation() {
		val out = Quaternion()
		assertTrue(Quaternion(-3.7f, 0.65f, -0.6f, 5f).closeTo(m1.getRotation(out)))
	}

	@Test
	fun getRotation1() {
		val out = Quaternion()
		assertTrue(Quaternion(0.1115717f, 0.21993284f, 1.438346f, 0.7539517f).closeTo(m1.getRotation(out, true)))
	}

	@Test
	fun getScaleXSquared() {
		assertEquals(659f, m1.getScaleXSquared())
	}

	@Test
	fun getScaleYSquared() {
		assertEquals(10755f, m1.getScaleYSquared())
	}

	@Test
	fun getScaleZSquared() {
		assertEquals(6094f, m1.getScaleZSquared())
	}

	@Test
	fun getScaleX() {
		assertEquals(25.670996f, m1.getScaleX())
	}

	@Test
	fun getScaleY() {
		assertEquals(103.706314f, m1.getScaleY())
	}

	@Test
	fun getScaleZ() {
		assertEquals(78.06408f, m1.getScaleZ())
	}

	@Test
	fun getScale() {
		assertEquals(Vector3(25.670996f, 103.706314f, 78.06408f), m1.getScale(Vector3()))
	}

	@Test
	fun translate() {
		m1.translate(3f, 23f, 44f)
		assertListEquals(arrayListOf(3.0f, 5.0f, 6.0f, 13.0f, 17.0f, 23.0f, 27.0f, 35.0f, 19.0f, 101.0f, 73.0f, 19.0f, 1247.0f, 5013.0f, 3892.0f, 1723.0f), m1.values)
	}

	@Test
	fun translate1() {
		m1.translate(Vector3(3f, 23f, 44f))
		assertListEquals(arrayListOf(3.0f, 5.0f, 6.0f, 13.0f, 17.0f, 23.0f, 27.0f, 35.0f, 19.0f, 101.0f, 73.0f, 19.0f, 1247.0f, 5013.0f, 3892.0f, 1723.0f), m1.values)
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
		assertListEquals(listOf(-8.74778f, -18.490776f, -18.35281f, -23.043419f, 13.780127f, 11.134449f, 17.438988f, 28.59626f, -19.81375f, -101.43531f, -73.84483f, -20.155771f, 11.0f, 25.0f, 41.0f, 43.0f), m1.values, { a, b -> a.closeTo(b) })
	}

	@Test
	fun scale() {
		m1.scale(Vector3(3f, 23f, 44f))
		assertListEquals(arrayListOf(9.0f, 15.0f, 18.0f, 39.0f, 391.0f, 529.0f, 621.0f, 805.0f, 836.0f, 4444.0f, 3212.0f, 836.0f, 11.0f, 25.0f, 41.0f, 43.0f), m1.values)
	}

	@Test
	fun scale1() {
		m1.scale(3f, 23f, 44f)
		assertListEquals(arrayListOf(9.0f, 15.0f, 18.0f, 39.0f, 391.0f, 529.0f, 621.0f, 805.0f, 836.0f, 4444.0f, 3212.0f, 836.0f, 11.0f, 25.0f, 41.0f, 43.0f), m1.values)
	}

	@Test
	fun prj() {
		assertEquals(Vector3(0.90207374f, 4.524194f, 3.3231566f), m1.prj(Vector3(3f, 6f, 76f)))
		assertEquals(Vector3(x=0.0044117644f, y=0.010588235f, z=-0.20117646f), m3.prj(Vector3(3f, 6f, 76f)))
		assertEquals(Vector3(x=0.0015243902f, y=0.0030487804f, z=0.038617887f), m4.prj(Vector3(3f, 6f, 76f)))
		assertEquals(Vector3(3f, 6f, 76f), Matrix4.IDENTITY.prj(Vector3(3f, 6f, 76f)))
	}

	@Test
	fun rot() {
		assertEquals(Vector3(3f, 6f, 76f), Matrix4.IDENTITY.rot(Vector3(3f, 6f, 76f)))
		assertEquals(Vector3(1555f, 7829f, 5728f), m1.rot(Vector3(3f, 6f, 76f)))
		assertEquals(Vector3(15f, 36f, -684f), m3.rot(Vector3(3f, 6f, 76f)))
		assertEquals(Vector3(3f, 6f, 76f), m4.rot(Vector3(3f, 6f, 76f)))
	}

	@Test
	fun rot1() {
		assertEquals(Vector2(111f, 153f), m1.rot(Vector2(3f, 6f)))
	}

	@Test
	fun shearZ() {
		m1.shearZ(0.2f, 0.5f)
		assertListEquals(arrayListOf(11.5f, 16.5f, 6.0f, 13.0f, 17.6f, 24.0f, 27.0f, 35.0f, 19.0f, 101.0f, 73.0f, 19.0f, 11.0f, 25.0f, 41.0f, 43.0f), m1.values)
	}

	@Test
	fun copy() {
		val copy = m1.copy()
		assertFalse(copy.values === m1.values)
		assertListEquals(copy.values, m1.values)
	}

	@Test
	fun tra() {
		val rotationMat = m1.copy()
		rotationMat.set(Quaternion().setFromAxis(Vector3.Y, 0.25f))

		val tra = rotationMat.copy()
		tra.tra()

		val inv = rotationMat.copy()
		inv.inv()

		assertEquals(tra, inv)
	}


}

private fun <E> List<E>.printF() {
	println(joinToString("f, ") + "f")
}