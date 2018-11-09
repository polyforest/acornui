package com.acornui.geom

import com.acornui.math.Vector2
import com.acornui.test.assertListEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Polygon2Test {

	@Test fun testCopy() {
		val p1 = Polygon2(listOf(1f, 2f, 3f, 4f))
		val p2 = p1.copy()
		assertTrue(p1 !== p2)
		assertTrue(p1.vertices !== p2.vertices)
		assertListEquals(p1.vertices, p2.vertices)
	}

	@Test fun bounds() {
		val p1 = Polygon2(listOf(-1f, 2f, 3f, 4f, 5f, 5f, 2f, 1f, 5f, 6f))

		assertEquals(-1f, p1.bounds.xMin)
		assertEquals(1f, p1.bounds.yMin)
		assertEquals(5f, p1.bounds.xMax)
		assertEquals(6f, p1.bounds.yMax)
	}

	@Test fun intersects() {
		val p1 = Polygon2(listOf(0f, 0f, 10f, 10f, 10f, 0f))
		val p2 = Polygon2(listOf(8f, 5f, 20f, 15f, 20f, 5f))
		assertTrue(p1.intersects(p2))
	}

	@Test fun intersects2() {
		val p1 = Polygon2(listOf(0f, 0f, 10f, 0f, 10f, 10f, 0f, 10f))
		val p2 = Polygon2(listOf(8f, 2f, 12f, 2f, 12f, 14f, 8f, 4f))
		val resolver = Vector2()
		assertTrue(p1.intersects(p2, resolver))
		assertEquals(Vector2(2f, 0f), resolver)
	}

	@Test fun intersects3() {
		val p1 = Polygon2(listOf(0f, 0f, 10f, 0f, 10f, 10f, 0f, 10f))
		val p2 = Polygon2(listOf(2f, 8f, 4f, 8f, 4f, 12f, 2f, 12f))
		val resolver = Vector2()
		assertTrue(p1.intersects(p2, resolver))
		assertEquals(Vector2(-0f, 2f), resolver)
	}

	@Test fun intersects4() {
		val p1 = Polygon2(listOf(0f, 0f, 10f, 0f, 10f, 10f, 0f, 10f))
		val p2 = Polygon2(listOf(2f, -2f, 4f, -2f, 4f, 2f, 2f, 2f))
		val resolver = Vector2()
		assertTrue(p1.intersects(p2, resolver))
		assertEquals(Vector2(-0f, -2f), resolver)
	}

}