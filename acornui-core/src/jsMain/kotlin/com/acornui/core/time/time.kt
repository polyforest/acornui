package com.acornui.core.time

/**
 * @author nbilyk
 */
private class TimeProviderImpl : TimeProvider {

	private val startTime: Long

	init {
		startTime = nowMs()
	}

	override fun now(): Date {
		return DateImpl()
	}

	override fun nowMs(): Long {
		return (js("Date.now()") as Double).toLong()
	}

	override fun nanoElapsed(): Long {
		return ((js("performance.now()") as Number).toLong() - startTime) * 1_000_000L
	}
}

/**
 * A global abstracted time provider.
 */
actual val time: TimeProvider = TimeProviderImpl()