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

import com.acornui.collection.ClearableObjectPool
import com.acornui.collection.Clearable
import com.acornui.core.Disposable
import com.acornui.core.DrivableChildBase
import com.acornui.core.di.Scoped
import com.acornui.core.di.inject

/**
 * @author nbilyk
 */
internal class Timer private constructor() : DrivableChildBase(), Clearable {

	var duration: Float = 0f
	var repetitions: Int = 1
	var currentTime: Float = 0f
	var currentRepetition: Int = 0
	var callback: () -> Unit = NOOP

	override fun update(stepTime: Float) {
		currentTime += stepTime
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
		remove()
		pool.free(this)
	}

	companion object {

		private val NOOP = {}

		private val pool = ClearableObjectPool { Timer() }

		fun obtain(): Timer {
			return pool.obtain()
		}
	}
}

fun Scoped.timer(duration: Float, repetitions: Int = 1, delay: Float = 0f, callback: () -> Unit): Disposable {
	return timer(inject(TimeDriver), duration, repetitions, delay, callback)
}

/**
 * @param timeDriver The time driver to add the Timer instance to.
 * @param duration The number of seconds between repetitions.
 * @param repetitions The number of repetitions the timer will be invoked
 */
fun timer(timeDriver: TimeDriver, duration: Float, repetitions: Int = 1, delay: Float = 0f, callback: () -> Unit): Disposable {
	if (repetitions == 0) throw IllegalArgumentException("repetitions argument may not be zero.")
	val timer = Timer.obtain()
	timer.currentTime = -delay
	timer.duration = duration
	timer.callback = callback
	timer.repetitions = repetitions
	timeDriver.addChild(timer)
	return timer
}

/**
 * @author nbilyk
 */
internal class EnterFrame private constructor() : DrivableChildBase(), Clearable {

	var repetitions: Int = 1
	var currentRepetition: Int = 0
	var callback: () -> Unit = NOOP

	override fun update(stepTime: Float) {
		++currentRepetition
		callback()
		if (repetitions >= 0 && currentRepetition >= repetitions) {
			dispose()
		}
	}

	override fun clear() {
		repetitions = 1
		currentRepetition = 0
		callback = NOOP
	}

	override fun dispose() {
		remove()
		pool.free(this)
	}

	companion object {

		private val NOOP = {}

		private val pool = ClearableObjectPool { EnterFrame() }

		fun obtain(): EnterFrame {
			return pool.obtain()
		}
	}
}

fun Scoped.callLater(callback: () -> Unit): Disposable {
	return enterFrame(1, callback)
}

fun TimeDriver.callLater(callback: () -> Unit): Disposable {
	return enterFrame(1, callback)
}

fun Scoped.enterFrame(repetitions: Int = -1, callback: () -> Unit): Disposable {
	return inject(TimeDriver).enterFrame(repetitions, callback)
}

fun TimeDriver.enterFrame(repetitions: Int = -1, callback: () -> Unit): Disposable {
	if (repetitions == 0) throw IllegalArgumentException("repetitions argument may not be zero.")
	val enterFrame = EnterFrame.obtain()
	enterFrame.callback = callback
	enterFrame.repetitions = repetitions
	addChild(enterFrame)
	return enterFrame
}