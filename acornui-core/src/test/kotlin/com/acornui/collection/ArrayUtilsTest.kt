package com.acornui.collection

import com.acornui.test.assertListEquals
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayUtilsTest {

	@Test fun doubleArrayAdd() {
        assertListEquals(
            doubleArrayOf(1.0, 2.0, 3.0, 4.0),
            doubleArrayOf(1.0, 4.0).add(1, doubleArrayOf(2.0, 3.0))
        )
        assertListEquals(
            doubleArrayOf(1.0, 2.0, 3.0, 4.0),
            doubleArrayOf(1.0, 2.0).add(2, doubleArrayOf(3.0, 4.0))
        )
        assertListEquals(
            doubleArrayOf(1.0, 2.0, 3.0, 4.0),
            doubleArrayOf(3.0, 4.0).add(0, doubleArrayOf(1.0, 2.0))
        )
        assertListEquals(
            doubleArrayOf(1.0, 2.0, 3.0, 4.0),
            doubleArrayOf(1.0, 2.0, 3.0, 4.0).add(0, doubleArrayOf())
        )
        assertListEquals(
            doubleArrayOf(1.0, 2.0, 3.0, 4.0),
            doubleArrayOf().add(0, doubleArrayOf(1.0, 2.0, 3.0, 4.0))
        )
	}
	
	@Test fun doubleArrayRemove() {
        assertListEquals(
            doubleArrayOf(1.0, 4.0),
            doubleArrayOf(1.0, 2.0, 3.0, 4.0).remove(1, 2)
        )
        assertListEquals(
            doubleArrayOf(2.0, 3.0, 4.0),
            doubleArrayOf(1.0, 2.0, 3.0, 4.0).remove(0, 1)
        )
        assertListEquals(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(1.0, 2.0, 3.0, 4.0).remove(2, 2)
        )
        assertListEquals(
            doubleArrayOf(1.0, 2.0, 3.0, 4.0),
            doubleArrayOf(1.0, 2.0, 3.0, 4.0).remove(2, 0)
        )
        assertListEquals(doubleArrayOf(), doubleArrayOf(1.0, 2.0, 3.0, 4.0).remove(0, 4))
	}
	
	@Test fun doubleArrayGetInsertionIndex() {
		assertEquals(1, doubleArrayOf(1.0, 2.0, 3.0, 4.0).getInsertionIndex(1.0))
		assertEquals(4, doubleArrayOf(1.0, 2.0, 3.0, 4.0).getInsertionIndex(4.0))
		assertEquals(2, doubleArrayOf(1.0, 2.0, 3.0, 4.0).getInsertionIndex(2.0))

		assertEquals(2, doubleArrayOf(1.0, 9.0, 2.0, 10.0, 3.0, 11.0, 4.0, 13.0).getInsertionIndex(1.0, stride = 2))
		assertEquals(8, doubleArrayOf(1.0, 9.0, 2.0, 10.0, 3.0, 11.0, 4.0, 13.0).getInsertionIndex(4.0, stride = 2))
		assertEquals(4, doubleArrayOf(1.0, 9.0, 2.0, 10.0, 3.0, 11.0, 4.0, 13.0).getInsertionIndex(2.0, stride = 2))

		assertEquals(3, doubleArrayOf(-1.0, 1.0, 9.0, -2.0, 2.0, 10.0, -3.0, 3.0, 11.0, -4.0, 4.0, 13.0).getInsertionIndex(1.0, stride = 3, offset = 1))
		assertEquals(12, doubleArrayOf(-1.0, 1.0, 9.0, -2.0, 2.0, 10.0, -3.0, 3.0, 11.0, -4.0, 4.0, 13.0).getInsertionIndex(4.0, stride = 3, offset = 1))
		assertEquals(6, doubleArrayOf(-1.0, 1.0, 9.0, -2.0, 2.0, 10.0, -3.0, 3.0, 11.0, -4.0, 4.0, 13.0).getInsertionIndex(2.0, stride = 3, offset = 1))
	}
}