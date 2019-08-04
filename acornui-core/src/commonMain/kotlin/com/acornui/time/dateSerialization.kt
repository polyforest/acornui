package com.acornui.time

import com.acornui.serialization.Reader
import com.acornui.serialization.Writer
import com.acornui.text.parseDate
import com.acornui.time.Date
import com.acornui.time.DateRo
import com.acornui.time.time


fun Reader.dateIso(): Date? = parseDate(string())
fun Reader.dateIso(name: String): Date? = get(name)?.dateIso()
fun Writer.dateIso(date: DateRo?) {
	if (date == null) writeNull()
	else string(date.toIsoString())
}
fun Writer.dateIso(name: String, date: DateRo?) = property(name).dateIso(date)

/**
 * Reads the date as a Long - the number of milliseconds from the Unix Epoch.
 */
fun Reader.dateTime(): Date? {
	val t = long() ?: return null
	return date(t)
}
fun Reader.dateTime(name: String): Date? = get(name)?.dateTime()

/**
 * Writes the date as a Long - the number of milliseconds from the Unix Epoch.
 */
fun Writer.dateTime(date: DateRo?) {
	if (date == null) writeNull()
	else long(date.time)
}
fun Writer.dateTime(name: String, date: DateRo?) = property(name).dateTime(date)