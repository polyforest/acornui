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
import com.acornui.Updatable
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import kotlin.time.Duration

/**
 * @author nbilyk
 */
private class Timer private constructor() : Updatable, Clearable, Disposable {

	private var isActive = false
	private var duration: Float = 0f
	private var repetitions: Int = 1
	private var currentTime: Float = 0f
	private var currentRepetition: Int = 0
	private var callback: () -> Unit = NOOP

	override fun update(dT: Float) {
		currentTime += dT
		while (currentTime > duration) {
			currentTime -= duration
			currentRepetition++
			callback()
			if (repetitions >= 0 && currentRepetition >= repetitions) {
				dispose()
			}
		}
	}

	override fun clear() {
		repetitions = 1
		duration = 0f
		currentTime = 0f
		currentRepetition = 0
		callback = NOOP
	}

	/**
	 * Sets the duration to 0f and the current repetition to 0
	 */
	fun rewind() {
		duration = 0f
		currentRepetition = 0
	}

	override fun dispose() {
		if (!isActive) return
		isActive = false
		stop()
		pool.free(this)
	}

	companion object {

		private val NOOP = {}

		private val pool = ClearableObjectPool { Timer() }

		internal fun obtain(duration: Float, repetitions: Int = 1, delay: Float = 0f, callback: () -> Unit): Disposable {
			val timer = pool.obtain()
			timer.isActive = true
			timer.currentTime = -delay
			timer.duration = duration
			timer.callback = callback
			timer.repetitions = repetitions
			timer.start()
			return timer
		}
	}
}

/**
 * @param duration The number of seconds between repetitions.
 * @param repetitions The number of repetitions the timer will be invoked. If this is -1, the callback will be invoked
 * until disposal.
 * @param callback The function to call after every repetition.
 */
fun timer(duration: Float, repetitions: Int = 1, delay: Float = 0f, callback: () -> Unit): Disposable {
	require(repetitions != 0) { "repetitions argument may not be zero." }
	return Timer.obtain(duration, repetitions, delay, callback)
}

/**
 * @param duration The duration between repetitions.
 * @param repetitions The number of repetitions the timer will be invoked. If this is -1, the callback will be invoked
 * until disposal.
 * @param callback The function to call after every repetition.
 */
fun timer(duration: Duration, repetitions: Int = 1, delay: Duration = Duration.ZERO, callback: () -> Unit): Disposable {
	require(repetitions != 0) { "repetitions argument may not be zero." }
	return Timer.obtain(duration.inSeconds.toFloat(), repetitions, delay.inSeconds.toFloat(), callback)
}