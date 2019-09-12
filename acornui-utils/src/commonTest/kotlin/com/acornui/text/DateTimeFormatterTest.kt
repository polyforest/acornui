/*
 * Copyright 2018 Nicholas Bilyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.text

import com.acornui.i18n.Locale
import com.acornui.system.userInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DateTimeFormatterTest {

	@Test
	fun getMonthsTest() {
		val monthsShort = getMonths(false, listOf(Locale("en-US")))
		assertEquals(listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"), monthsShort)

		val monthsLong = getMonths(true, listOf(Locale("en-US")))
		assertEquals(listOf("January","February","March","April","May","June","July","August","September","October","November","December"), monthsLong)
	}

	@Test
	fun getMonthsTestI18n() {
		if (!userInfo.isBrowser) return
		val monthsLongDe = getMonths(true, listOf(Locale("de-DE")))
		assertEquals(listOf("Januar","Februar","März","April","Mai","Juni","Juli","August","September","Oktober","November","Dezember"), monthsLongDe)

		val monthsShortDe = getMonths(false, listOf(Locale("de-DE")))
		assertEquals(listOf("Jan","Feb","Mär","Apr","Mai","Jun","Jul","Aug","Sep","Okt","Nov","Dez"), monthsShortDe)

		val monthsShortDe2 = getMonths(false, listOf(Locale("de-DE")))
		assertSame(monthsShortDe, monthsShortDe2) // Test caching
	}
}