package com.acornui.math

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlin.test.Test
import kotlin.test.assertEquals

// Note: JS will stringify 1.0f as "1" and JVM is "1.0"

class CornersTest {

	@Test
	fun serialization() {
		run {
			val c = Corners(1.1f, 2.1f, 3.1f, 4.1f, 5.1f, 6.1f, 7.1f, 8.1f)
			val json = jsonStringify(Corners.serializer(), c)
			assertEquals("""[1.1,2.1,3.1,4.1,5.1,6.1,7.1,8.1]""", json)
			assertEquals(c, jsonParse(Corners.serializer(), json))
		}

		run {
			val c = Corners(1.1f, 2.1f, 3.1f, 4.1f, 5.1f, 6.1f, 7.1f, 8.1f)
			val json = jsonStringify(Corners.serializer(), c)
			assertEquals("""[1.1,2.1,3.1,4.1,5.1,6.1,7.1,8.1]""", json)
			assertEquals(c, jsonParse(Corners.serializer(), json))
		}

		run {
			val c = Corners(1.1f, 2.1f, 3.1f, 4.1f)
			val json = jsonStringify(Corners.serializer(), c)
			assertEquals("""[1.1,2.1,3.1,4.1]""", json)
			assertEquals(c, jsonParse(Corners.serializer(), json))
		}

		run {
			val c = Corners(1.1f)
			val json = jsonStringify(Corners.serializer(), c)
			assertEquals("""[1.1]""", json)
			assertEquals(c, jsonParse(Corners.serializer(), json))
		}
	}
}