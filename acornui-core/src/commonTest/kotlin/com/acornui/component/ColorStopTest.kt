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
}