package com.acornui.graphic

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HslTest {

	@Test fun toRgb() {
		assertEquals(Color.BLACK, Hsl(0f, 0f, 0f).toRgb(Color()))
		assertEquals(Color.WHITE, Hsl(0f, 0f, 1f).toRgb(Color()))
		assertEquals(Color.RED, Hsl(0f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.GREEN, Hsl(120f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.BLUE, Hsl(240f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.YELLOW, Hsl(60f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.CYAN, Hsl(180f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.MAGENTA, Hsl(300f, 1f, 0.5f).toRgb(Color()))
		assertEquals(Color.LIGHT_GRAY, Hsl(0f, 0f, 0.75f).toRgb(Color()))
		assertEquals(Color.GRAY, Hsl(0f, 0f, 0.5f).toRgb(Color()))
		assertEquals(Color.MAROON, Hsl(0f, 1f, 0.25f).toRgb(Color()))
		assertEquals(Color.OLIVE, Hsl(60f, 1f, 0.25f).toRgb(Color()))
	}

	@Test fun testEquals() {
		assertEquals(Hsl(180f, 1f, 1f), Hsl(180f, 1f, 1f))
		assertEquals(Hsl(180f, 2f, 1f), Hsl(180f, 2f, 1f))
		assertNotEquals(Hsl(181f, 1f, 1f), Hsl(180f, 1f, 1f))
		assertNotEquals(Hsl(180f, 2f, 1f), Hsl(180f, 1f, 1f))
		assertNotEquals(Hsl(180f, 1f, 2f), Hsl(180f, 1f, 1f))
	}
	
	@Test fun serialize() {
		val hsl = Hsl(1f, 2f, 3f, 4f)
		val json = jsonStringify(Hsl.serializer(), hsl)
		assertEquals(hsl, jsonParse(Hsl.serializer(), json))
	}
}