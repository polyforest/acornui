package com.acornui.graphic

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HsvTest {
	
	@Test fun toRgb() {
		assertEquals(Color.BLACK, Hsv(0f, 0f, 0f).toRgb(Color()))
		assertEquals(Color.WHITE, Hsv(0f, 0f, 1f).toRgb(Color()))
		assertEquals(Color.RED, Hsv(0f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.GREEN, Hsv(120f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.BLUE, Hsv(240f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.YELLOW, Hsv(60f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.CYAN, Hsv(180f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.MAGENTA, Hsv(300f, 1f, 1f).toRgb(Color()))
		assertEquals(Color.LIGHT_GRAY, Hsv(0f, 0f, 0.75f).toRgb(Color()))
		assertEquals(Color.GRAY, Hsv(0f, 0f, 0.5f).toRgb(Color()))
		assertEquals(Color.MAROON, Hsv(0f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.OLIVE, Hsv(60f, 1f, 0.5f).toRgb(Color()))
	}

	@Test fun testEquals() {
		assertEquals(Hsv(180f, 1f, 1f), Hsv(180f, 1f, 1f))
		assertEquals(Hsv(180f, 2f, 1f), Hsv(180f, 2f, 1f))
		assertNotEquals(Hsv(181f, 1f, 1f), Hsv(180f, 1f, 1f))
		assertNotEquals(Hsv(180f, 2f, 1f), Hsv(180f, 1f, 1f))
		assertNotEquals(Hsv(180f, 1f, 2f), Hsv(180f, 1f, 1f))
	}

	@Test fun serialize() {
		val hsv = Hsv(1f, 2f, 3f, 4f)
		val json = jsonStringify(Hsv.serializer(), hsv)
		assertEquals(hsv, jsonParse(Hsv.serializer(), json))
	}
}