package com.acornui.graphic

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HsvTest {
	
	@Test fun toRgb() {
		assertEquals(Color.BLACK, Hsv(0.0, 0.0, 0.0).toRgb())
		assertEquals(Color.WHITE, Hsv(0.0, 0.0, 1.0).toRgb())
		assertEquals(Color.RED, Hsv(0.0, 1.0, 1.0).toRgb())
		assertEquals(Color.GREEN, Hsv(120.0, 1.0, 1.0).toRgb())
		assertEquals(Color.BLUE, Hsv(240.0, 1.0, 1.0).toRgb())
		assertEquals(Color.YELLOW, Hsv(60.0, 1.0, 1.0).toRgb())
		assertEquals(Color.CYAN, Hsv(180.0, 1.0, 1.0).toRgb())
		assertEquals(Color.MAGENTA, Hsv(300.0, 1.0, 1.0).toRgb())
		assertEquals(Color.LIGHT_GRAY, Hsv(0.0, 0.0, 0.75).toRgb())
		assertEquals(Color.GRAY, Hsv(0.0, 0.0, 0.5).toRgb())
		assertEquals(Color.MAROON, Hsv(0.0, 1.0, 0.5).toRgb())
		assertEquals(Color.OLIVE, Hsv(60.0, 1.0, 0.5).toRgb())
	}

	@Test fun testEquals() {
		assertEquals(Hsv(180.0, 1.0, 1.0), Hsv(180.0, 1.0, 1.0))
		assertEquals(Hsv(180.0, 2.0, 1.0), Hsv(180.0, 2.0, 1.0))
		assertNotEquals(Hsv(181.0, 1.0, 1.0), Hsv(180.0, 1.0, 1.0))
		assertNotEquals(Hsv(180.0, 2.0, 1.0), Hsv(180.0, 1.0, 1.0))
		assertNotEquals(Hsv(180.0, 1.0, 2.0), Hsv(180.0, 1.0, 1.0))
	}

	@Test fun serialize() {
		val hsv = Hsv(1.0, 2.0, 3.0, 4.0)
		val json = jsonStringify(Hsv.serializer(), hsv)
		assertEquals(hsv, jsonParse(Hsv.serializer(), json))
	}
}