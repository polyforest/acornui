package com.acornui.particle

import com.acornui.test.assertClose
import com.acornui.test.assertListEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class PropertyTimelineTest {

	@Test
	fun badStride() {
		assertFails { PropertyTimeline(property = "test", timeline = floatArrayOf(1f), numComponents = 2).getValueAtTime(0f) }
	}
	
	@Test
	fun getValueAtTime() {
		assertEquals(0f, PropertyTimeline(property = "test", timeline = floatArrayOf()).getValueAtTime(0f))
		assertClose(0.5f, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 1f, 1f)).getValueAtTime(0.5f))
		assertClose(1.5f, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 1f, 1f, 2f, 2f)).getValueAtTime(1.5f))
		assertClose(0.5f, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 1f, 1f, 2f, 0f)).getValueAtTime(1.5f))
		assertClose(1f, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 1f, 1f, 2f, 0f)).getValueAtTime(1f))
		assertClose(0f, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 1f, 1f, 2f, 0f)).getValueAtTime(3f))
		assertClose(3f, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 1f, 1f, 2f, 3f)).getValueAtTime(3f))
	}

	@Test
	fun getValuesAtTime() {
		assertListEquals(floatArrayOf(0f, 0f), PropertyTimeline(property = "test", timeline = floatArrayOf(), numComponents = 2).getValuesAtTime(0f))
		assertListEquals(floatArrayOf(0.5f, 0.25f), PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 0.5f, 1f, 1f, 0.0f), numComponents = 2).getValuesAtTime(0.5f))
		assertListEquals(floatArrayOf(0.5f, 0.5f), PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 0.5f, 1f, 1f, 0.0f, 2f, 0f, 1f), numComponents = 2).getValuesAtTime(1.5f))
		assertListEquals(floatArrayOf(0f, 1f), PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 0.5f, 1f, 1f, 0.0f, 2f, 0f, 1f), numComponents = 2).getValuesAtTime(2f))
		assertListEquals(floatArrayOf(0f, 1f), PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 0.5f, 1f, 1f, 0.0f, 2f, 0f, 1f), numComponents = 2).getValuesAtTime(3f))
		assertListEquals(floatArrayOf(0f, 0.5f), PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 0.5f, 1f, 1f, 0.0f, 2f, 0f, 1f), numComponents = 2).getValuesAtTime(0f))
		assertListEquals(floatArrayOf(0f, 0.5f), PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 0f, 0.5f, 1f, 1f, 0.0f, 2f, 0f, 1f), numComponents = 2).getValuesAtTime(-1f))
	}
	
	@Test
	fun getIndexClosestToTime() {
		assertEquals(1, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 1f, 2f, 3f), numComponents = 0).getIndexClosestToTime(1f))
		assertEquals(1, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 1f, 2f, 3f), numComponents = 0).getIndexClosestToTime(1.1f))
		assertEquals(1, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 1f, 2f, 3f), numComponents = 0).getIndexClosestToTime(0.9f))
		assertEquals(5, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 1f, 2f, 3f, 6f, 8f, 10f), numComponents = 0).getIndexClosestToTime(8.9f))
		assertEquals(2, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, 1f, -1f, 2f, -1f, 3f, -1f), numComponents = 1).getIndexClosestToTime(1f))
		assertEquals(2, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, 1f, -1f, 2f, -1f, 3f, -1f), numComponents = 1).getIndexClosestToTime(1.1f))
		assertEquals(2, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, 1f, -1f, 2f, -1f, 3f, -1f), numComponents = 1).getIndexClosestToTime(0.9f))
		assertEquals(15, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, -1f, 1f, -1f, -1f, 2f, -1f, -1f, 3f, -1f, -1f, 6f, -1f, -1f, 8f, -1f, -1f, 10f, -1f, -1f), numComponents = 2).getIndexClosestToTime(8.9f))
	}
	
	@Test
	fun getIndexCloseToTime() {
		val affordance = 0.25f
		assertEquals(1, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 1f, 2f, 3f), numComponents = 0).getIndexCloseToTime(1f, affordance))
		assertEquals(1, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 1f, 2f, 3f), numComponents = 0).getIndexCloseToTime(1.1f, affordance))
		assertEquals(-1, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 1f, 2f, 3f), numComponents = 0).getIndexCloseToTime(1.3f, affordance))
		assertEquals(1, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 1f, 2f, 3f), numComponents = 0).getIndexCloseToTime(0.9f, affordance))
		assertEquals(5, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, 1f, 2f, 3f, 6f, 8f, 10f), numComponents = 0).getIndexCloseToTime(7.9f, affordance))
		assertEquals(2, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, 1f, -1f, 2f, -1f, 3f, -1f), numComponents = 1).getIndexCloseToTime(1f, affordance))
		assertEquals(2, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, 1f, -1f, 2f, -1f, 3f, -1f), numComponents = 1).getIndexCloseToTime(1.1f, affordance))
		assertEquals(-1, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, 1f, -1f, 2f, -1f, 3f, -1f), numComponents = 1).getIndexCloseToTime(1.3f, affordance))
		assertEquals(2, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, 1f, -1f, 2f, -1f, 3f, -1f), numComponents = 1).getIndexCloseToTime(0.9f, affordance))
		assertEquals(15, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, -1f, 1f, -1f, -1f, 2f, -1f, -1f, 3f, -1f, -1f, 6f, -1f, -1f, 8f, -1f, -1f, 10f, -1f, -1f), numComponents = 2).getIndexCloseToTime(8.24f, affordance))
		assertEquals(15, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, -1f, 1f, -1f, -1f, 2f, -1f, -1f, 3f, -1f, -1f, 6f, -1f, -1f, 8f, -1f, -1f, 10f, -1f, -1f), numComponents = 2).getIndexCloseToTime(8.25f, affordance))
		assertEquals(-1, PropertyTimeline(property = "test", timeline = floatArrayOf(0f, -1f, -1f, 1f, -1f, -1f, 2f, -1f, -1f, 3f, -1f, -1f, 6f, -1f, -1f, 8f, -1f, -1f, 10f, -1f, -1f), numComponents = 2).getIndexCloseToTime(8.9f, affordance))
		
	}
}