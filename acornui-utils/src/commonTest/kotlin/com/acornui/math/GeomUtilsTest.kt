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
		assertTrue(GeomUtils.intersectPointTriangle(vec2(0f, 0f), vec2(0f, 0f), vec2(10f, 0f), vec2(10f, 10f)))
		assertTrue(GeomUtils.intersectPointTriangle(vec2(5f, 5f), vec2(0f, 0f), vec2(10f, 0f), vec2(10f, 10f)))
		assertFalse(GeomUtils.intersectPointTriangle(vec2(0f, 1f), vec2(0f, 0f), vec2(10f, 0f), vec2(10f, 10f)))
	}
}