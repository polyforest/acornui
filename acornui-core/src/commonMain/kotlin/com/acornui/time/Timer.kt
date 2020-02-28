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

@file:Suppress("ConvertTwoComparisonsToRangeCheck")

package com.acornui.time

import com.acornui.Disposable
import com.acornui.Updatable
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.start
import com.acornui.stop
import kotlinx.coroutines.DisposableHandle
import kotlin.time.Duration

/**
 * A Timer invokes a [callback] after a set amount of time.
 *
 * Constructing a Timer object from the constructor will not automatically [start] the timer, however constructing
 * from the dsl methods will.
 *
 * @author nbilyk
 */
class Timer(

		/**
		 * The frame loop.
		 */
		override val frameDriver: FrameDriverRo,

		/**
		 * The interval between callbacks.
		 */
		val duration: Float = 0f,

		/**
		 * The number of repetitions before stopping.
		 * If this is negative, the timer will continue until it's stopped manually.
		 * @see stop
		 */
		val repetitions: Int = 1,

		/**
		 * The first callback will be invoked at `delay + duration`.
		 */
		val delay: Float = 0f,

		/**
		 * The callback to invoke on each repetition.
		 */
		val callback: () -> Unit

) : Updatable, Disposable, DisposableHandle {
	// For convenience, Timer implements both com.acornui.Disposable and kotlinx.coroutines.DisposableHandle
	// This is so that timers used in scheduled dispatchers don't need to wrap the Disposable reference.

	private var currentTime: Float = -delay
	private var currentRepetition: Int = 0

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

	/**
	 * Sets the [currentTime] to 0f and the current repetition to 0
	 */
	fun rewind() {
		currentTime = 0f
		currentRepetition = 0
	}

	override fun dispose() {
		stop()
	}
}

/**
 * Creates a [Timer] object and immediately starts it.
 *
 * @param duration The duration between repetitions.
 * @param repetitions The number of repetitions the timer will be invoked. If this is -1, the callback will be invoked
 * until disposal.
 * @param callback The function to call after every repetition.
 */
fun Context.timer(duration: Duration, repetitions: Int = 1, delay: Duration = Duration.ZERO, callback: () -> Unit): Disposable {
	require(repetitions != 0) { "repetitions argument may not be zero." }
	return Timer(inject(FrameDriverRo), duration.inSeconds.toFloat(), repetitions, delay.inSeconds.toFloat(), callback).start()
}