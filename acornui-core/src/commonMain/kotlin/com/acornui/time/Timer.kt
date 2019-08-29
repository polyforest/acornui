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

/**
 * @author nbilyk
 */
internal class Timer private constructor() : Updatable, Clearable, Disposable {

	var isActive = false
	var duration: Float = 0f
	var repetitions: Int = 1
	var currentTime: Float = 0f
	var currentRepetition: Int = 0
	var callback: () -> Unit = NOOP

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
 * @author nbilyk
 */
internal class Tick private constructor() : Updatable, Clearable, Disposable {

	/**
	 * How many frames before the callback begins to be invoked.
	 * This should be greater than or equal to 1.
	 */
	var startFrame = 1

	var isActive = false
		private set

	/**
	 * The number of times to invoke the callback.
	 */
	var repetitions: Int = 1

	/**
	 * The current frame.
	 */
	private var currentFrame: Int = 0

	/**
	 * The callback to invoke, starting at [startFrame] and continues for [repetitions].
	 */
	private var callback: Disposable.() -> Unit = NOOP

	override fun update(dT: Float) {
		++currentFrame
		if (currentFrame >= startFrame)
			this.callback()
		if (repetitions >= 0 && currentFrame - startFrame + 1 >= repetitions) {
			dispose()
		}
	}

	override fun clear() {
		startFrame = 1
		repetitions = 1
		currentFrame = 0
		callback = NOOP
	}

	override fun dispose() {
		if (!isActive) return
		isActive = false
		stop()
		pool.free(this)
	}

	companion object {

		private val NOOP: Disposable.() -> Unit = {}

		private val pool = ClearableObjectPool { Tick() }

		internal fun obtain(repetitions: Int = -1, startFrame: Int = 1, callback: Disposable.() -> Unit): Disposable {
			val e = pool.obtain()
			e.callback = callback
			e.repetitions = repetitions
			e.startFrame = startFrame
			e.isActive = true
			e.start()
			return e
		}
	}
}

fun callLater(startFrame: Int = 1, callback: Disposable.() -> Unit): Disposable {
	return tick(1, startFrame, callback)
}

//-----------------------------------------------

/**
 * Invokes [callback] on every time driver tick until disposed.
 */
fun tick(repetitions: Int = -1, callback: Disposable.() -> Unit): Disposable = tick(repetitions, 1, callback)

/**
 * Invokes [callback] on every time driver tick until disposed.
 */
fun tick(repetitions: Int = -1, startFrame: Int = 1, callback: Disposable.() -> Unit): Disposable {
	if (repetitions == 0) throw IllegalArgumentException("repetitions argument may not be zero.")
	return Tick.obtain(repetitions, startFrame, callback)
}
