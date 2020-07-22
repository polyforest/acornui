package com.acornui.component

import com.acornui.graphic.Color
import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorStopTest {

	@Test fun serialize() {
		val g = ColorStop(Color.RED)
		val json = jsonStringify(ColorStop.serializer(), g)
		assertEquals("""{"color":"#ff0000ff"}""", json)
		assertEquals(g, jsonParse(ColorStop.serializer(), json))
	}

	@Test fun toCssString() {
		assertEquals("rgba(255, 0, 0, 1) 0px", ColorStop(Color.RED, dp = 0.0).toCssString())
		assertEquals("rgba(255, 0, 0, 1) 50%", ColorStop(Color.RED, percent = 0.5).toCssString())
	}
}