package com.acornui.graphic

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HslTest {

	@Test fun toRgb() {
		assertEquals(Color.BLACK, Hsl(0.0, 0.0, 0.0).toRgb())
		assertEquals(Color.WHITE, Hsl(0.0, 0.0, 1.0).toRgb())
		assertEquals(Color.RED, Hsl(0.0, 1.0, 0.5).toRgb())
		assertEquals(Color.GREEN, Hsl(120.0, 1.0, 0.5).toRgb())
		assertEquals(Color.BLUE, Hsl(240.0, 1.0, 0.5).toRgb())
		assertEquals(Color.YELLOW, Hsl(60.0, 1.0, 0.5).toRgb())
		assertEquals(Color.CYAN, Hsl(180.0, 1.0, 0.5).toRgb())
		assertEquals(Color.MAGENTA, Hsl(300.0, 1.0, 0.5).toRgb())
		assertEquals(Color.LIGHT_GRAY, Hsl(0.0, 0.0, 0.75).toRgb())
		assertEquals(Color.GRAY, Hsl(0.0, 0.0, 0.5).toRgb())
		assertEquals(Color.MAROON, Hsl(0.0, 1.0, 0.25).toRgb())
		assertEquals(Color.OLIVE, Hsl(60.0, 1.0, 0.25).toRgb())
	}

	@Test fun testEquals() {
		assertEquals(Hsl(180.0, 1.0, 1.0), Hsl(180.0, 1.0, 1.0))
		assertEquals(Hsl(180.0, 2.0, 1.0), Hsl(180.0, 2.0, 1.0))
		assertNotEquals(Hsl(181.0, 1.0, 1.0), Hsl(180.0, 1.0, 1.0))
		assertNotEquals(Hsl(180.0, 2.0, 1.0), Hsl(180.0, 1.0, 1.0))
		assertNotEquals(Hsl(180.0, 1.0, 2.0), Hsl(180.0, 1.0, 1.0))
	}
	
	@Test fun serialize() {
		val hsl = Hsl(1.0, 2.0, 3.0, 4.0)
		val json = jsonStringify(Hsl.serializer(), hsl)
		assertEquals(hsl, jsonParse(Hsl.serializer(), json))
	}
}