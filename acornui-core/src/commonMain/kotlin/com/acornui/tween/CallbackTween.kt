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


/**
 * A Tween that does nothing but invoke callbacks when the play head scrubs through the requested time.
 */
class CallbackTween(duration: Float, ease: Interpolation, delay: Float, loop: Boolean) : TweenBase() {

	private val watchedTimes = ArrayList<Float>()
	private val watchedTimeSignals = ArrayList<Signal1<Tween>>()
	override val duration: Float = if (duration <= 0f) 0.0000001f else duration
	override val durationInv = 1f / duration

	init {
		this.ease = ease
		this.loopAfter = loop
		jumpTo(-delay - 0.0000001f) // Subtract a small amount so time handlers at 0f time get invoked.
	}

	/**
	 * Returns a signal that is invoked when the tween passes over the provided alpha.
	 */
	fun watchAlpha(alpha: Float): Signal<(Tween) -> Unit> = watchTime(alpha * duration)

	/**
	 * Returns a signal that will be invoked when the tween passes over the provided time (in seconds).
	 */
	fun watchTime(time: Float): Signal<(Tween) -> Unit> {
		val insertionIndex = watchedTimes.sortedInsertionIndex(time)
		if (insertionIndex < watchedTimes.size && watchedTimes[insertionIndex] == time) {
			return watchedTimeSignals[insertionIndex]
		} else {
			val signal = Signal1<Tween>()
			watchedTimes.add(insertionIndex, time)
			watchedTimeSignals.add(insertionIndex, signal)
			return signal
		}
	}

	override fun updateToTime(lastTime: Float, newTime: Float, apparentLastTime: Float, apparentNewTime: Float, jump: Boolean) {
		if (jump || watchedTimes.isEmpty()) return
		if (lastTime == newTime) return
		if (loopAfter || loopBefore) {
			if (newTime >= lastTime) {
				if (apparentNewTime > apparentLastTime) {
					invokeWatchedTimeHandlers(apparentLastTime, apparentNewTime)
				} else {
					invokeWatchedTimeHandlers(apparentLastTime, duration)
					invokeWatchedTimeHandlers(0f, apparentNewTime)
				}
			} else {
				if (apparentNewTime < apparentLastTime) {
					invokeWatchedTimeHandlers(apparentLastTime, apparentNewTime)
				} else {
					invokeWatchedTimeHandlers(apparentLastTime, 0f)
					invokeWatchedTimeHandlers(duration, apparentNewTime)
				}
			}
		} else {
			invokeWatchedTimeHandlers(lastTime, newTime)
		}
	}

	private fun invokeWatchedTimeHandlers(lastTime: Float, newTime: Float) {
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
