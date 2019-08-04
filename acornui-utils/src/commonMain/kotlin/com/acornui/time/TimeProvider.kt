/*
 * Copyright 2019 Poly Forest, LLC
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

package com.acornui.time
import com.acornui.time.date as aDate
import com.acornui.time.time as aTime
import com.acornui.time.utcDate as aUtcDate
import com.acornui.time.utcTime  as aUtcTime

/**
 * @author nbilyk
 */
interface TimeProvider {

	/**
	 * Returns a new date object with the time set.
	 * @param time The time as UTC milliseconds from the epoch.
	 */
	@Deprecated("Use com.acornui.time.date", ReplaceWith("com.acornui.time.date(time)"))
	fun date(time: Long): Date = aDate(time)

	/**
	 * Returns a new date object with the time set.
	 * @param fullYear The full year according to local time.  (e.g. 1999, not 99)
	 * @param month The 1 indexed month according to local time. 1 - January, 12 - December
	 * @param dayOfMonth The 1 indexed day of the month according to local time. 1st - 1, 31st - 31
	 * @param hour Hour of the day using 24-hour clock according to local time.
	 * @param minute The minute within the hour according to local time.
	 * @param second The second within the minute according to local time.
	 * @param milli The millisecond within the second according to local time.
	 */
	@Deprecated("Use com.acornui.time.date", ReplaceWith("com.acornui.time.date(fullYear, month, dayOfMonth, hour, minute, second, milli)"))
	fun date(fullYear: Int, month: Int, dayOfMonth: Int = 1, hour: Int = 0, minute: Int = 0, second: Int = 0, milli: Int = 0): Date {
		return aDate(fullYear, month, dayOfMonth, hour, minute, second, milli)
	}

	/**
	 * Returns a new date object with the time set according to universal time.
	 * @param fullYear The full year according to universal time.  (e.g. 1999, not 99)
	 * @param month The 1 indexed month according to universal time. 1 - January, 12 - December
	 * @param dayOfMonth The 1 indexed day of the month according to universal time. 1st - 1, 31st - 31
	 * @param hour Hour of the day using 24-hour clock according to universal time.
	 * @param minute The minute within the hour according to universal time.
	 * @param second The second within the minute according to universal time.
	 * @param milli The millisecond within the second according to universal time.
	 */
	@Deprecated("Use com.acornui.time.date", ReplaceWith("com.acornui.time.utcDate(fullYear, month, dayOfMonth, hour, minute, second, milli)"))
	fun utcDate(fullYear: Int, month: Int, dayOfMonth: Int = 1, hour: Int = 0, minute: Int = 0, second: Int = 0, milli: Int = 0): Date {
		return aUtcDate(fullYear, month, dayOfMonth, hour, minute, second, milli)
	}

	/**
	 * Returns a date object where the time is set relative to the unix epoch, in local time.
	 */
	@Deprecated("Use com.acornui.time.time", ReplaceWith("com.acornui.time.time(hour, minute, second, milli)"))
	fun time(hour: Int, minute: Int, second: Int = 0, milli: Int = 0): Date {
		return aTime(hour, minute, second, milli)
	}

	/**
	 * Returns a date object where the time is set relative to the unix epoch, in universal time.
	 */
	@Deprecated("Use com.acornui.time.utcTime", ReplaceWith("com.acornui.time.utcTime(hour, minute, second, milli)"))
	fun utcTime(hour: Int, minute: Int, second: Int = 0, milli: Int = 0): Date {
		return aUtcTime(hour, minute, second, milli)
	}

	/**
	 * Returns a Date representing the current system time.
	 */
	@Deprecated("Use Date()", ReplaceWith("Date()"))
	fun now(): Date = Date()

	/**
	 * Returns the number of milliseconds elapsed since 1 January 1970 00:00:00 UTC.
	 */
	fun nowMs(): Long

	/**
	 * Returns the number of seconds elapsed since 1 January 1970 00:00:00 UTC.
	 */
	fun nowS(): Double = (nowMs().toDouble() / 1000.0)

	/**
	 * Returns the current value of the running high-resolution time source, in nanoseconds from the time the
	 * application started.
	 *
	 * On the JS backend, this is accurate to the nearest 5 microseconds. see [performance.now]
	 * For the JVM side, see [System.nanoTime]
	 */
	fun nanoElapsed(): Long

	fun msElapsed(): Long = nanoElapsed() / 1_000_000L

}

/**
 * A global abstracted time provider.
 */
expect val time: TimeProvider
