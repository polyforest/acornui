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

import com.acornui.Disposable
import com.acornui.function.as1
import kotlin.time.Duration

interface CallbackWrapper : Disposable {
	operator fun invoke()
}

private class LimitedCallback(
		val duration: Duration,
		val callback: () -> Unit
) : CallbackWrapper {

	private var nextAllowed = markNow()
	private var isInvoking = false

	private var frameHandle: Disposable? = null

	private fun update() {
		frameHandle = null
		isInvoking = true
		nextAllowed += duration
		callback()
		isInvoking = false
	}

	override operator fun invoke() {
		if (isInvoking) return
		val elapsed = nextAllowed.elapsedNow()
		if (!elapsed.isNegative()) {
			callback()
			nextAllowed += duration
		} else {
			if (frameHandle == null) {
				frameHandle = timer(-elapsed, callback = ::update.as1)
			}
		}
	}

	override fun dispose() {
		stop()
	}

	private fun stop() {
		frameHandle?.dispose()
		frameHandle = null
	}
}

/**
 * Creates a callback wrapper that will prevent a method from being invoked too rapidly.
 * @param duration When the wrapper is invoked, a timed lock will be created for this number of seconds. If the wrapper
 * is invoked while this timed lock is in place, the callback will be invoked at the end of the lock.
 * Note that calls to the wrapper from within the callback will be ignored.
 */
fun limitedCallback(duration: Duration, callback: () -> Unit): CallbackWrapper {
	return LimitedCallback(duration, callback)
}

private class DelayedCallback(
		val duration: Duration,
		val callback: () -> Unit
) : CallbackWrapper {

	private var frameHandle: Disposable? = null
	private var isInvoking = false

	private fun update() {
		frameHandle = null
		isInvoking = true
		callback()
		isInvoking = false
	}

	override operator fun invoke() {
		if (isInvoking) return
		frameHandle?.dispose()
		frameHandle = timer(duration, callback = ::update.as1)
	}

	override fun dispose() {
		stop()
	}

	private fun stop() {
		frameHandle?.dispose()
		frameHandle = null
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
fun delayedCallback(duration: Duration, callback: () -> Unit): CallbackWrapper =
	DelayedCallback(duration, callback)