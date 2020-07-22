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
		assertTrue(GeomUtils.intersectPointTriangle(vec2(0.0, 0.0), vec2(0.0, 0.0), vec2(10.0, 0.0), vec2(10.0, 10.0)))
		assertTrue(GeomUtils.intersectPointTriangle(vec2(5.0, 5.0), vec2(0.0, 0.0), vec2(10.0, 0.0), vec2(10.0, 10.0)))
		assertFalse(GeomUtils.intersectPointTriangle(vec2(0.0, 1.0), vec2(0.0, 0.0), vec2(10.0, 0.0), vec2(10.0, 10.0)))
	}
}