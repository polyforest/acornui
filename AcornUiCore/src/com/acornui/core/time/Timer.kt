/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.core.time

import com.acornui.collection.Clearable
import com.acornui.collection.ClearableObjectPool
import com.acornui.core.Disposable
import com.acornui.core.UpdatableChildBase
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @author nbilyk
 */
internal class Timer private constructor() : UpdatableChildBase(), Clearable, Disposable {

	var isActive = false
	var duration: Float = 0f
	var repetitions: Int = 1
	var currentTime: Float = 0f
	var currentRepetition: Int = 0
	var callback: () -> Unit = NOOP

	override fun update(tickTime: Float) {
		currentTime += tickTime
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
		remove()
		pool.free(this)
	}

	companion object {

		private val NOOP = {}

		private val pool = ClearableObjectPool { Timer() }

		internal fun obtain(timeDriver: TimeDriver, duration: Float, repetitions: Int = 1, delay: Float = 0f, callback: () -> Unit): Disposable {
			val timer = pool.obtain()
			timer.isActive = true
			timer.currentTime = -delay
			timer.duration = duration
			timer.callback = callback
			timer.repetitions = repetitions
			timeDriver.addChild(timer)
			return timer
		}
	}
}

fun Scoped.timer(duration: Float, repetitions: Int = 1, delay: Float = 0f, callback: () -> Unit): Disposable {
	return timer(inject(TimeDriver), duration, repetitions, delay, callback)
}

/**
 * Suspends the coroutine for [duration] seconds.
 */
suspend fun Scoped.delay(
		duration: Float
) = suspendCoroutine<Unit> { cont ->
	timer(inject(TimeDriver), duration, 1, 0f) {
		cont.resume(Unit)
	}
}


/**
 * @param timeDriver The time driver to add the Timer instance to.
 * @param duration The number of seconds between repetitions.
 * @param repetitions The number of repetitions the timer will be invoked.
 * @param callback The function to call after every repetition.
 */
fun timer(timeDriver: TimeDriver, duration: Float, repetitions: Int = 1, delay: Float = 0f, callback: () -> Unit): Disposable {
	if (repetitions == 0) throw IllegalArgumentException("repetitions argument may not be zero.")
	return Timer.obtain(timeDriver, duration, repetitions, delay, callback)
}

/**
 * @author nbilyk
 */
internal class Tick private constructor() : UpdatableChildBase(), Clearable, Disposable {

	/**
	 * How many frames before the callback begins to be invoked.
	 * This should be greater than or equal to 1.
	 */
	var startFrame = 1

	var isActive = false

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
	var callback: () -> Unit = NOOP

	override fun update(tickTime: Float) {
		++currentFrame
		if (currentFrame >= startFrame)
			callback()
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
		remove()
		pool.free(this)
	}

	companion object {

		private val NOOP = {}

		private val pool = ClearableObjectPool { Tick() }

		internal fun obtain(timeDriver: TimeDriver, repetitions: Int = -1, startFrame: Int = 1, callback: () -> Unit): Disposable {
			val e = pool.obtain()
			e.callback = callback
			e.repetitions = repetitions
			e.startFrame = startFrame
			e.isActive = true
			timeDriver.addChild(e)
			return e
		}
	}
}

fun Scoped.callLater(callback: () -> Unit): Disposable {
	return tick(repetitions = 1, startFrame = 1, callback = callback)
}

fun callLater(timeDriver: TimeDriver, startFrame: Int = 1, callback: () -> Unit): Disposable {
	return tick(timeDriver, 1, startFrame, callback)
}

@Deprecated("Use tick", ReplaceWith("this.tick(repetitions, callback)"))
fun Scoped.enterFrame(repetitions: Int = -1, callback: () -> Unit): Disposable = tick(repetitions, 1, callback)

@Deprecated("Use tick", ReplaceWith("this.tick(repetitions, startFrame, callback)"))
fun Scoped.enterFrame(repetitions: Int = -1, startFrame: Int = 1, callback: () -> Unit): Disposable =
		tick(repetitions, startFrame, callback)

@Deprecated("Use tick", ReplaceWith("tick(timeDriver, repetitions, callback)"))
fun enterFrame(timeDriver: TimeDriver, repetitions: Int = -1, callback: () -> Unit): Disposable = tick(timeDriver, repetitions, callback)

@Deprecated("Use tick", ReplaceWith("tick(timeDriver, repetitions, startFrame, callback)"))
fun enterFrame(timeDriver: TimeDriver, repetitions: Int = -1, startFrame: Int = 1, callback: () -> Unit): Disposable =
		tick(timeDriver, repetitions, startFrame, callback)



//-----------------------------------------------

/**
 * Invokes [callback] on every time driver tick until disposed.
 */
fun Scoped.tick(repetitions: Int = -1, callback: () -> Unit): Disposable =
		tick(inject(TimeDriver), repetitions, 1, callback)

/**
 * Invokes [callback] on every time driver tick until disposed.
 */
fun Scoped.tick(repetitions: Int = -1, startFrame: Int = 1, callback: () -> Unit): Disposable =
		tick(inject(TimeDriver), repetitions, startFrame, callback)

/**
 * Invokes [callback] on every time driver tick until disposed.
 */
fun tick(timeDriver: TimeDriver, repetitions: Int = -1, callback: () -> Unit): Disposable = tick(timeDriver, repetitions, 1, callback)

/**
 * Invokes [callback] on every time driver tick until disposed.
 */
fun tick(timeDriver: TimeDriver, repetitions: Int = -1, startFrame: Int = 1, callback: () -> Unit): Disposable {
	if (repetitions == 0) throw IllegalArgumentException("repetitions argument may not be zero.")
	return Tick.obtain(timeDriver, repetitions, startFrame, callback)
}