package com.acornui

import com.acornui.core.filterWithWords
import com.acornui.test.assertListEquals
import kotlin.test.Test

class StringKtTest {

	@Test fun filterWithWords() {
		val haystack = listOf("one_two-Three", "FourFiveSix", "FourFive_Seven", "ThreeTwoOne", "Two-one.Three", "Five.six.four")
		assertListEquals(listOf("one_two-Three", "ThreeTwoOne", "Two-one.Three"), haystack.filterWithWords(listOf("one", "two", "three")))
		assertListEquals(listOf("FourFiveSix", "Five.six.four"), haystack.filterWithWords(listOf("four", "six", "five")))

	}
}