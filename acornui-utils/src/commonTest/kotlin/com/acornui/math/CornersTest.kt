package com.acornui.math

import com.acornui.serialization.jsonParse
import com.acornui.serialization.jsonStringify
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class CornersTest {

	@Test
	fun serialization() {
		run {
			val c = CornersWrapper(Corners(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f))
			val json = jsonStringify(CornersWrapper.serializer(), c)
			assertEquals("""{"c":[1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0]}""", json)
			assertEquals(c, jsonParse(CornersWrapper.serializer(), json))
		}

		run {
			val c = Corners(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
			val json = jsonStringify(Corners.serializer(), c)
			assertEquals("""[1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0]""", json)
			assertEquals(c, jsonParse(Corners.serializer(), json))
		}

		run {
			val c = CornersWrapper(Corners(1f, 2f, 3f, 4f))
			val json = jsonStringify(CornersWrapper.serializer(), c)
			assertEquals("""{"c":[1.0,2.0,3.0,4.0]}""", json)
			assertEquals(c, jsonParse(CornersWrapper.serializer(), json))
		}

		run {
			val c = Corners(1f)
			val json = jsonStringify(Corners.serializer(), c)
			assertEquals("""[1.0]""", json)
			assertEquals(c, jsonParse(Corners.serializer(), json))
		}
	}
}

@Serializable
private data class CornersWrapper(val c: CornersRo)