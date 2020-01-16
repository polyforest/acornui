package com.acornui

import com.acornui.test.assertListEquals
import kotlin.test.Test

class StringKtTest {

	private val words = listOf(
			"THIS-IS-A-TEST",
			"THIS_IS_A_TEST",
			"this_is_a_test",
			"thisIsATest",
			"IOTest",
			"TestIO",
			"TestOne",
			"testOne",
			"test1",
			"testB",
			"test_one",
			"test-one",
			"test.one",
			"testOne",
			"test2",
			"test-3",
			"testTwoThree",
			"123test",
			"test.123"
	)

	@Test
	fun toHyphenCase() {
		val expected = listOf(
				"this-is-a-test",
				"this-is-a-test",
				"this-is-a-test",
				"this-is-a-test",
				"io-test",
				"test-io",
				"test-one",
				"test-one",
				"test-1",
				"test-b",
				"test-one",
				"test-one",
				"test-one",
				"test-one",
				"test-2",
				"test-3",
				"test-two-three",
				"123-test",
				"test-123"
		)

		assertListEquals(expected, words.map { it.toHyphenCase() })
	}

	@Test
	fun toUnderscoreCase() {
		val expected = listOf(
				"this_is_a_test",
				"this_is_a_test",
				"this_is_a_test",
				"this_is_a_test",
				"io_test",
				"test_io",
				"test_one",
				"test_one",
				"test_1",
				"test_b",
				"test_one",
				"test_one",
				"test_one",
				"test_one",
				"test_2",
				"test_3",
				"test_two_three",
				"123_test",
				"test_123"
		)

		assertListEquals(expected, words.map { it.toUnderscoreCase() })
	}

	@Test
	fun toCamelCase() {
		val expected = listOf(
				"thisIsATest",
				"thisIsATest",
				"thisIsATest",
				"thisIsATest",
				"ioTest",
				"testIo",
				"testOne",
				"testOne",
				"test1",
				"testB",
				"testOne",
				"testOne",
				"testOne",
				"testOne",
				"test2",
				"test3",
				"testTwoThree",
				"123Test",
				"test123"
		)

		assertListEquals(expected, words.map { it.toCamelCase() })
	}
}