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
import kotlinx.browser.window
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
	 * The interval between callbacks.
	 */
	val duration: Duration = Duration.ZERO,

	/**
	 * The number of repetitions before stopping.
	 * If this is negative, the timer will continue until it's disposed manually.
	 * @see dispose
	 */
	val repetitions: Int = 1,

	/**
	 * The first callback will be invoked at `delay + duration`.
	 */
	val delay: Duration = Duration.ZERO,

	/**
	 * The callback to invoke on each repetition.
	 */
	val callback: (timer: Timer) -> Unit

) : Disposable {

	var currentRepetition: Int = 0
		private set

	private val handler = {
		currentRepetition++
		callback(this)
		if (repetitions < 0 || currentRepetition < repetitions)
			next()
	}

	private fun next() {
		handle = window.setTimeout(handler, duration.inMilliseconds.toInt())
	}

	/**
	 * Sets the [currentTime] to `-delay` and the current repetition to `0`
	 */
	fun rewind() {
		currentRepetition = 0
	}

	private var handle: Int = -1

	fun start(): Timer {
		handle = window.setTimeout(handler, (delay + duration).inMilliseconds.toInt())
		return this
	}

	override fun dispose() {
		window.clearTimeout(handle)
	}
}

/**
 * Creates a [Timer] object and immediately starts it.
 *
 * @param duration The duration between repetitions.
 * @param repetitions The number of repetitions the timer will be invoked. If this is -1, the callback will be invoked
 * until disposal.
 * @param delay The first invocation of [callback] will be `delay + duration`. May be negative.
 * @param callback The function to call after every repetition.
 */
fun timer(
	duration: Duration,
	repetitions: Int = 1,
	delay: Duration = Duration.ZERO,
	callback: (timer: Timer) -> Unit
): Disposable {
	require(repetitions != 0) { "repetitions argument may not be zero." }
	return Timer(duration, repetitions, delay, callback).start()
}