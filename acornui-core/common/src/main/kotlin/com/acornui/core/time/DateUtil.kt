/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.core.time

/**
 * @author nbilyk
 */
class DateUtil {

	companion object {

		/**
		 * The normalized year of the gregorian cutover in Gregorian, with
		 * 0 representing 1 BCE, -1 representing 2 BCE, etc.
		 */
		private val GREGORIAN_CUTOVER_YEAR = 1582

		/**
		 * Determines if the given year is a leap year. Returns <code>true</code> if
		 * the given year is a leap year. To specify BC year numbers,
		 * <code>1 - year number</code> must be given. For example, year BC 4 is
		 * specified as -3.
		 *
		 * @param year the given year.
		 * @return <code>true</code> if the given year is a leap year; <code>false</code> otherwise.
		 */
		fun isLeapYear(year: Int): Boolean {
			if ((year and 3) != 0) {
				return false
			}
			return if (year >= GREGORIAN_CUTOVER_YEAR) {
				(year % 100 != 0) || (year % 400 == 0) // Gregorian
			} else true // Julian calendar had no correction.
		}

	}

}