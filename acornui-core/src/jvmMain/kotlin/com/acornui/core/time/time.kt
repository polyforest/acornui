package com.acornui.core.time

/**
 * @author nbilyk
 */
private class TimeProviderImpl: TimeProvider {

	private var _startTime = (DateImpl().time - msElapsed())

	override fun now(): Date {
		return DateImpl()
	}

	override fun nowMs(): Long {
		return _startTime + msElapsed()
	}

	override fun nanoElapsed(): Long {
		return System.nanoTime()
	}
}

/**
 * A global abstracted time provider.
 */
actual val time: TimeProvider = TimeProviderImpl()