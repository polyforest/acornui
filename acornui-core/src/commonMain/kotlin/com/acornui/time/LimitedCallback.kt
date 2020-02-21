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

import com.acornui.*
import com.acornui.di.Context

interface CallbackWrapper : Disposable {
	operator fun invoke()
}

private class LimitedCallback(
		override val frameDriver: FrameDriverRo,
		val duration: Float,
		val callback: () -> Unit
) : Updatable, CallbackWrapper {

	private var currentTime = 0f
	private var pendingInvoke = false
	private var isInvoking = false

	override fun update(dT: Float) {
		currentTime += dT
		if (currentTime > duration) {
			currentTime = 0f
			if (pendingInvoke) {
				pendingInvoke = false
				isInvoking = true
				callback()
				isInvoking = false
			} else {
				stop()
			}
		}
	}

	override operator fun invoke() {
		if (isInvoking) return
		if (isDriven) {
			pendingInvoke = true
		} else {
			start()
			callback()
		}
	}

	override fun dispose() {
		stop()
	}
}

/**
 * Creates a callback wrapper that will prevent a method from being invoked too rapidly.
 * @param duration When the wrapper is invoked, a timed lock will be created for this number of seconds. If the wrapper
 * is invoked while this timed lock is in place, the callback will be invoked at the end of the lock.
 * Note that calls to the wrapper from within the callback will be ignored.
 */
fun Context.limitedCallback(duration: Float, callback: () -> Unit): CallbackWrapper {
	return LimitedCallback(inject(FrameDriverRo), duration, callback)
}

fun limitedCallback(frameDriver: FrameDriverRo, duration: Float, callback: () -> Unit): CallbackWrapper {
	return LimitedCallback(frameDriver, duration, callback)
}

private class DelayedCallback(
		override val frameDriver: FrameDriverRo,
		val duration: Float,
		val callback: () -> Unit
) : Updatable, CallbackWrapper {

	private var currentTime: Float = 0f

	private var isInvoking = false

	override fun update(dT: Float) {
		currentTime += dT
		if (currentTime > duration) {
			currentTime = 0f
			stop()
			isInvoking = true
			callback()
			isInvoking = false
		}
	}

	override operator fun invoke() {
		if (isInvoking) return
		if (!isDriven) {
			start()
		} else {
			currentTime = 0f
		}
	}

	override fun dispose() {
		stop()
	}
}

/**
 * Returns a wrapper that when invoked, will call [callback] after [duration] seconds. Every consecutive
 * invocation of the wrapper will reset this timer.
 * Useful for things like auto-save after the user is done typing for a set amount of time.
 *
 * Note that calls to the wrapper from within the callback will be ignored.
 *
 * @param duration The number of seconds before the [callback] is invoked.
 * @param callback The function to call after [duration] seconds.
 */
fun Context.delayedCallback(duration: Float, callback: () -> Unit): CallbackWrapper {
	return DelayedCallback(inject(FrameDriverRo), duration, callback)
}

fun delayedCallback(frameDriver: FrameDriverRo, duration: Float, callback: () -> Unit): CallbackWrapper {
	return DelayedCallback(frameDriver, duration, callback)
}