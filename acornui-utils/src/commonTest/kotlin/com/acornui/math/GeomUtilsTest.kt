package com.acornui.math

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeomUtilsTest {

	@Test fun testIntersectTriangleTriangleBounds() {

	}

	@Test fun testIntersectTriangleTriangle() {

	}

	@Test fun testIntersectsPointTriangle() {
		assertTrue(GeomUtils.intersectPointTriangle(Vector2(0f, 0f), Vector2(0f, 0f), Vector2(10f, 0f), Vector2(10f, 10f)))
		assertTrue(GeomUtils.intersectPointTriangle(Vector2(5f, 5f), Vector2(0f, 0f), Vector2(10f, 0f), Vector2(10f, 10f)))
		assertFalse(GeomUtils.intersectPointTriangle(Vector2(0f, 1f), Vector2(0f, 0f), Vector2(10f, 0f), Vector2(10f, 10f)))
	}
}