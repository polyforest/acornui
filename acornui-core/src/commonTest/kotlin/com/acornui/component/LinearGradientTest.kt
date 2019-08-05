package com.acornui.component

import com.acornui.graphic.Color
import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlin.test.Test
import kotlin.test.assertEquals

class LinearGradientTest {

	@Test fun serialize() {
		val g = LinearGradient(GradientDirection.ANGLE, Color.RED, Color.BLACK, Color.BLUE)
		val json = jsonStringify(LinearGradient.serializer(), g)
		assertEquals(g, jsonParse(LinearGradient.serializer(), json))
	}
}