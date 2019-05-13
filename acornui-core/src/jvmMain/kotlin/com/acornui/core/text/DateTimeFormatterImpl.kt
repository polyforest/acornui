package com.acornui.core.text


import com.acornui.collection.copy
import com.acornui.core.i18n.Locale
import com.acornui.core.time.DateImpl
import com.acornui.core.time.DateRo
import com.acornui.core.userInfo
import com.acornui.logging.Log
import com.acornui.reflect.observable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.ReadWriteProperty
import java.util.Locale as JvmLocale

actual class DateTimeFormatter : StringFormatter<DateRo> {

	actual var type by watched(DateTimeFormatType.DATE_TIME)
	actual var timeStyle by watched(DateTimeFormatStyle.DEFAULT)
	actual var dateStyle by watched(DateTimeFormatStyle.DEFAULT)
	actual var timeZone: String? by watched(null)
	actual var locales: List<Locale>? by watched(null)

	private var currentUserLocales: List<Locale> = listOf()
	private var _formatter: DateFormat? = null
	private val formatter: DateFormat
		get() {
			if (locales == null && currentUserLocales != userInfo.currentLocale.value) {
				_formatter = null
				currentUserLocales = userInfo.currentLocale.value.copy()
			}
			if (_formatter == null) {
				val locales = locales ?: currentUserLocales
				for (locale in locales) {
					val jvmLocale = java.util.Locale.Builder().setLanguageTag(locale.value).build()
					_formatter = getFormatterForLocale(jvmLocale)
					if (_formatter != null) break
				}
				if (_formatter == null) {
					Log.warn("Could not create a date formatter for the current language chain.")
					_formatter = getFormatterForLocale(java.util.Locale.getDefault())
				}
				_formatter!!.timeZone = if (timeZone == null) TimeZone.getDefault() else TimeZone.getTimeZone(timeZone)
			}
			return _formatter!!
		}

	override fun format(value: DateRo): String {
		value as DateImpl
		return formatter.format(value.time)
	}

	private fun getFormatterForLocale(jvmLocale: java.util.Locale): DateFormat {
		return when (type) {
			DateTimeFormatType.DATE -> DateFormat.getDateInstance(dateStyle.toInt(), jvmLocale)
			DateTimeFormatType.TIME -> DateFormat.getTimeInstance(timeStyle.toInt(), jvmLocale)
			DateTimeFormatType.DATE_TIME -> DateFormat.getDateTimeInstance(dateStyle.toInt(), timeStyle.toInt(), jvmLocale)
			DateTimeFormatType.MONTH -> SimpleDateFormat(when (dateStyle) {
				DateTimeFormatStyle.FULL -> "MMMMM"
				DateTimeFormatStyle.LONG -> "MMM"
				DateTimeFormatStyle.MEDIUM -> "MM"
				DateTimeFormatStyle.SHORT -> "M"
				DateTimeFormatStyle.DEFAULT -> "MM"
			}, jvmLocale)
			DateTimeFormatType.YEAR -> SimpleDateFormat(when (dateStyle) {
				DateTimeFormatStyle.FULL, DateTimeFormatStyle.LONG -> "YYYY"
				else -> "YY"
			}, jvmLocale)
			DateTimeFormatType.WEEKDAY -> SimpleDateFormat(when (dateStyle) {
				DateTimeFormatStyle.FULL, DateTimeFormatStyle.LONG -> "EEEEE"
				DateTimeFormatStyle.MEDIUM -> "EEE"
				DateTimeFormatStyle.SHORT -> "EE"
				DateTimeFormatStyle.DEFAULT -> "EE"
			}, jvmLocale)
		}
	}

	private fun DateTimeFormatStyle.toInt(): Int {
		return when (this) {
			DateTimeFormatStyle.FULL -> DateFormat.FULL
			DateTimeFormatStyle.LONG -> DateFormat.LONG
			DateTimeFormatStyle.MEDIUM -> DateFormat.MEDIUM
			DateTimeFormatStyle.SHORT -> DateFormat.SHORT
			DateTimeFormatStyle.DEFAULT -> DateFormat.DEFAULT
		}
	}

	private fun <T> watched(initial: T): ReadWriteProperty<Any?, T> {
		return observable(initial) {
			_formatter = null
		}
	}
}