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
import com.acornui.signal.SignalImpl
import com.acornui.tween.Tween.Companion.SMALL_DURATION
import kotlin.time.Duration

/**
 * A Tween that does nothing but invoke callbacks when the play head scrubs through the requested time.
 */
class CallbackTween(duration: Duration, ease: Interpolation, delay: Duration, loop: Boolean) : TweenBase() {

	private val watchedTimes = ArrayList<Duration>()
	private val watchedTimeSignals = ArrayList<SignalImpl<Tween>>()
	override val duration: Duration = if (duration <= Duration.ZERO) SMALL_DURATION else duration

	init {
		this.ease = ease
		this.loopAfter = loop
		jumpTo(-delay + -SMALL_DURATION) // Subtract a small amount so time handlers at 0.0 time get invoked.
	}

	/**
	 * Returns a signal that is invoked when the tween passes over the provided alpha.
	 */
	fun watchAlpha(alpha: Double): Signal<Tween> = watchTime(duration * alpha)

	/**
	 * Returns a signal that will be invoked when the tween passes over the provided time (in seconds).
	 */
	fun watchTime(time: Duration): Signal<Tween> {
		val insertionIndex = watchedTimes.sortedInsertionIndex(time)
		return if (insertionIndex < watchedTimes.size && watchedTimes[insertionIndex] == time) {
			watchedTimeSignals[insertionIndex]
		} else {
			val signal = SignalImpl<Tween>()
			watchedTimes.add(insertionIndex, time)
			watchedTimeSignals.add(insertionIndex, signal)
			signal
		}
	}

	override fun updateToTime(lastTime: Duration, newTime: Duration, apparentLastTime: Duration, apparentNewTime: Duration, jump: Boolean) {
		if (jump || watchedTimes.isEmpty()) return
		if (lastTime == newTime) return
		if (loopAfter || loopBefore) {
			if (newTime >= lastTime) {
				if (apparentNewTime > apparentLastTime) {
					invokeWatchedTimeHandlers(apparentLastTime, apparentNewTime)
				} else {
					invokeWatchedTimeHandlers(apparentLastTime, duration)
					invokeWatchedTimeHandlers(Duration.ZERO, apparentNewTime)
				}
			} else {
				if (apparentNewTime < apparentLastTime) {
					invokeWatchedTimeHandlers(apparentLastTime, apparentNewTime)
				} else {
					invokeWatchedTimeHandlers(apparentLastTime, Duration.ZERO)
					invokeWatchedTimeHandlers(duration, apparentNewTime)
				}
			}
		} else {
			invokeWatchedTimeHandlers(lastTime, newTime)
		}
	}

	private fun invokeWatchedTimeHandlers(lastTime: Duration, newTime: Duration) {
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
