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

package com.acornui.tween

import com.acornui.collection.sortedInsertionIndex
import com.acornui.math.Interpolation
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.time.FrameDriver

/**
 * A Tween that does nothing but invoke callbacks when the play head scrubs through the requested time.
 */
class CallbackTween(frameDriver: FrameDriver, duration: Double, ease: Interpolation, delay: Double, loop: Boolean) : TweenBase(frameDriver) {

	private val watchedTimes = ArrayList<Double>()
	private val watchedTimeSignals = ArrayList<Signal1<Tween>>()
	override val duration: Double = if (duration <= 0.0) 0.0000001 else duration
	override val durationInv = 1.0 / duration

	init {
		this.ease = ease
		this.loopAfter = loop
		jumpTo(-delay - 0.0000001) // Subtract a small amount so time handlers at 0.0 time get invoked.
	}

	/**
	 * Returns a signal that is invoked when the tween passes over the provided alpha.
	 */
	fun watchAlpha(alpha: Double): Signal<Tween> = watchTime(alpha * duration)

	/**
	 * Returns a signal that will be invoked when the tween passes over the provided time (in seconds).
	 */
	fun watchTime(time: Double): Signal<Tween> {
		val insertionIndex = watchedTimes.sortedInsertionIndex(time)
		return if (insertionIndex < watchedTimes.size && watchedTimes[insertionIndex] == time) {
			watchedTimeSignals[insertionIndex]
		} else {
			val signal = Signal1<Tween>()
			watchedTimes.add(insertionIndex, time)
			watchedTimeSignals.add(insertionIndex, signal)
			signal
		}
	}

	override fun updateToTime(lastTime: Double, newTime: Double, apparentLastTime: Double, apparentNewTime: Double, jump: Boolean) {
		if (jump || watchedTimes.isEmpty()) return
		if (lastTime == newTime) return
		if (loopAfter || loopBefore) {
			if (newTime >= lastTime) {
				if (apparentNewTime > apparentLastTime) {
					invokeWatchedTimeHandlers(apparentLastTime, apparentNewTime)
				} else {
					invokeWatchedTimeHandlers(apparentLastTime, duration)
					invokeWatchedTimeHandlers(0.0, apparentNewTime)
				}
			} else {
				if (apparentNewTime < apparentLastTime) {
					invokeWatchedTimeHandlers(apparentLastTime, apparentNewTime)
				} else {
					invokeWatchedTimeHandlers(apparentLastTime, 0.0)
					invokeWatchedTimeHandlers(duration, apparentNewTime)
				}
			}
		} else {
			invokeWatchedTimeHandlers(lastTime, newTime)
		}
	}

	private fun invokeWatchedTimeHandlers(lastTime: Double, newTime: Double) {
		if (newTime >= lastTime) {
			var index = watchedTimes.sortedInsertionIndex(lastTime, matchForwards = true)
			while (index < watchedTimes.size && newTime >= watchedTimes[index]) {
				watchedTimeSignals[index].dispatch(this)
				index++
			}
		} else {
			var index = watchedTimes.sortedInsertionIndex(lastTime, matchForwards = false)
			while (index > 0 && newTime <= watchedTimes[index - 1]) {
				watchedTimeSignals[index].dispatch(this)
				index--
			}
		}
	}

	companion object
}
