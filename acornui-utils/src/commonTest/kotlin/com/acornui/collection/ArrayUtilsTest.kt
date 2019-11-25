package com.acornui.collection

import com.acornui.test.assertListEquals
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayUtilsTest {

	@Test fun floatArrayAdd() {
		assertListEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 4f).add(1, floatArrayOf(2f, 3f)) )
		assertListEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f).add(2, floatArrayOf(3f, 4f)) )
		assertListEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(3f, 4f).add(0, floatArrayOf(1f, 2f)) )
		assertListEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f).add(0, floatArrayOf()) )
		assertListEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf().add(0, floatArrayOf(1f, 2f, 3f, 4f)) )
	}
	
	@Test fun floatArrayRemove() {
		assertListEquals(floatArrayOf(1f, 4f), floatArrayOf(1f, 2f, 3f, 4f).remove(1, 2))
		assertListEquals(floatArrayOf(2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f).remove(0, 1))
		assertListEquals(floatArrayOf(1f, 2f), floatArrayOf(1f, 2f, 3f, 4f).remove(2, 2))
		assertListEquals(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(1f, 2f, 3f, 4f).remove(2, 0))
		assertListEquals(floatArrayOf(), floatArrayOf(1f, 2f, 3f, 4f).remove(0, 4))
	}
	
	@Test fun floatArrayGetInsertionIndex() {
		assertEquals(1, floatArrayOf(1f, 2f, 3f, 4f).getInsertionIndex(1f))
		assertEquals(4, floatArrayOf(1f, 2f, 3f, 4f).getInsertionIndex(4f))
		assertEquals(2, floatArrayOf(1f, 2f, 3f, 4f).getInsertionIndex(2f))

		assertEquals(2, floatArrayOf(1f, 9f, 2f, 10f, 3f, 11f, 4f, 13f).getInsertionIndex(1f, stride = 2))
		assertEquals(8, floatArrayOf(1f, 9f, 2f, 10f, 3f, 11f, 4f, 13f).getInsertionIndex(4f, stride = 2))
		assertEquals(4, floatArrayOf(1f, 9f, 2f, 10f, 3f, 11f, 4f, 13f).getInsertionIndex(2f, stride = 2))

		assertEquals(3, floatArrayOf(-1f, 1f, 9f, -2f, 2f, 10f, -3f, 3f, 11f, -4f, 4f, 13f).getInsertionIndex(1f, stride = 3, offset = 1))
		assertEquals(12, floatArrayOf(-1f, 1f, 9f, -2f, 2f, 10f, -3f, 3f, 11f, -4f, 4f, 13f).getInsertionIndex(4f, stride = 3, offset = 1))
		assertEquals(6, floatArrayOf(-1f, 1f, 9f, -2f, 2f, 10f, -3f, 3f, 11f, -4f, 4f, 13f).getInsertionIndex(2f, stride = 3, offset = 1))
	}
}