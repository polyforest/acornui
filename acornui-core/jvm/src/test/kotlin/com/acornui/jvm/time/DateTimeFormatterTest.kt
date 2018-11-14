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

package com.acornui.jvm.time

import com.acornui.core.UserInfo
import com.acornui.core.i18n.Locale
import com.acornui.core.text.dateTimeFormatterProvider
import com.acornui.core.text.getMonths
import com.acornui.core.text.numberFormatterProvider
import com.acornui.core.time.time
import com.acornui.core.userInfo
import com.acornui.jvm.text.DateTimeFormatterImpl
import com.acornui.jvm.text.NumberFormatterImpl
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DateTimeFormatterTest {
	@BeforeTest
	fun setUp() {
		val u = UserInfo(
				isTouchDevice = false,
				userAgent = "headless",
				platformStr = System.getProperty("os.name") ?: UserInfo.UNKNOWN_PLATFORM,
				systemLocale = listOf(Locale("en-US"))
		)
		userInfo = u
		time = TimeProviderImpl()
		numberFormatterProvider = { NumberFormatterImpl() }
		dateTimeFormatterProvider = { DateTimeFormatterImpl() }
	}

	@Test
	fun getMonthsTest() {
		val monthsShort = getMonths(false, listOf(Locale("en-US")))
		assertEquals(listOf("Jan","Mar","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"), monthsShort)

		val monthsLong = getMonths(true, listOf(Locale("en-US")))
		assertEquals(listOf("January","March","March","April","May","June","July","August","September","October","November","December"), monthsLong)

		val monthsLongDe = getMonths(true, listOf(Locale("de-DE")))
		assertEquals(listOf("Januar","März","März","April","Mai","Juni","Juli","August","September","Oktober","November","Dezember"), monthsLongDe)

		val monthsShortDe = getMonths(false, listOf(Locale("de-DE")))
		assertEquals(listOf("Jan","Mär","Mär","Apr","Mai","Jun","Jul","Aug","Sep","Okt","Nov","Dez"), monthsShortDe)

		val monthsShortDe2 = getMonths(false, listOf(Locale("de-DE")))
		assertSame(monthsShortDe, monthsShortDe2) // Test caching
	}
}