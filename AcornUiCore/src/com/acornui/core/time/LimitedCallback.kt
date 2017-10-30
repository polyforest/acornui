package com.acornui.core.time

import com.acornui.core.Disposable
import com.acornui.core.UpdatableChild
import com.acornui.core.UpdatableChildBase
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject

interface CallbackWrapper : Disposable {
	operator fun invoke()
}

internal class LimitedCallback(
		val timeDriver: TimeDriver,
		val duration: Float,
		val callback: () -> Unit
) : UpdatableChildBase(), UpdatableChild, CallbackWrapper {

	private var currentTime: Float = 0f
	private var callAgain: Boolean = false

	override fun update(stepTime: Float) {
		currentTime += stepTime
		if (currentTime > duration) {
			currentTime = 0f
			remove()
			if (callAgain) {
				callAgain = false
				callback()
			}
		}
	}

	override operator fun invoke() {
		if (parent == null) {
			timeDriver.addChild(this)
			callback()
		} else {
			callAgain = true
		}
	}

	override fun dispose() {
		remove()
	}
}

/**
 *
 */
internal fun Scoped.limitedCallback(duration: Float, callback: () -> Unit): CallbackWrapper {
	return LimitedCallback(inject(TimeDriver), duration, callback)
}

internal class DelayedCallback(
		val timeDriver: TimeDriver,
		val duration: Float,
		val callback: () -> Unit
) : UpdatableChildBase(), UpdatableChild, CallbackWrapper {

	private var currentTime: Float = 0f

	override fun update(stepTime: Float) {
		currentTime += stepTime
		if (currentTime > duration) {
			currentTime = 0f
			remove()
			callback()
		}
	}

	override operator fun invoke() {
		if (parent == null) {
			timeDriver.addChild(this)
		} else {
			currentTime = 0f
		}
	}

	override fun dispose() {
		remove()
	}
}

/**
 * Returns a wrapper that when invoked, will call [callback] after [duration] seconds. Every consecutive
 * invocation of the wrapper will reset this timer.
 * Useful for things like auto-save after the user is done typing for a set amount of time.
 *
 * @param duration The number of seconds before the [callback] is invoked.
 * @param callback The function to call after [duration] seconds.
 */
fun Scoped.delayedCallback(duration: Float, callback: () -> Unit): CallbackWrapper {
	return DelayedCallback(inject(TimeDriver), duration, callback)
}