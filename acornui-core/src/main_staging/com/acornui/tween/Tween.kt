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

import com.acornui.Disposable
import com.acornui.Updatable
import com.acornui.di.Context
import com.acornui.math.Easing
import com.acornui.math.Interpolation
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.stop
import com.acornui.time.FrameDriver

/**
 * A Tween is an object representing an interpolation over time.
 */
interface Tween : Updatable, Disposable {

	/**
	 * Dispatched when the play head has scrubbed past a boundary, either 0.0 or [duration]. If looping is enabled, this
	 * will not be invoked unless [allowCompletion] is called.
	 */
	val completed: Signal<Tween>

	/**
	 * The current time of the tween, in seconds.
	 *
	 * If the implementation has a delayed start, this value will start as negative.
	 * Settings this value is the same as calling [setCurrentTime] with jump = false.
	 * @see update
	 */
	var currentTime: Double

	/**
	 * Sets the current time.
	 * @param newTime The new time (in seconds) to update to.
	 * @param jump If true, the play head will jump instead of scrub. Callbacks will not be invoked.
	 */
	fun setCurrentTime(newTime: Double, jump: Boolean)

	/**
	 * Sets the current time.
	 * The play head will jump instead of scrub. Callbacks will not be invoked.
	 * @see setCurrentTime
	 */
	fun jumpTo(newTime: Double) = setCurrentTime(newTime, true)

	/**
	 * When the tween is rewound using [rewind], the [currentTime] will be set to this value.
	 * This can be set to a negative value to indicate a delay.
	 */
	var startTime: Double

	/**
	 * If true, when this tween's [currentTime] <= 0.0, it will loop back to [duration].
	 */
	var loopBefore: Boolean

	/**
	 * If true, when this tween's [currentTime] >= [duration], it will loop back to zero.
	 */
	var loopAfter: Boolean

	/**
	 * The total duration of this tween, in seconds.
	 */
	val duration: Double

	/**
	 * 1.0 / duration.
	 */
	val durationInv: Double

	/**
	 * The current 0-1 progress of the tween.
	 *
	 * If the implementation has a delayed start, this value will start as negative.
	 * @see update
	 */
	var alpha: Double
		get() = currentTime * durationInv
		set(value) {
			currentTime = value * duration
		}

	/**
	 * If this is true, [completed] will be dispatched when the play head scrubs past an endpoint regardless if looping
	 * is enabled.
	 */
	var allowCompletion: Boolean

	/**
	 * If true, [update] will not advance the current time.
	 * This does not affect directly changing the current time.
	 */
	var paused: Boolean

	/**
	 * Steps forward [dT] seconds.
	 * @param dT The number of seconds to progress. This may be negative.
	 */
	override fun update(dT: Double) {
		if (!paused)
			currentTime += dT
	}

	/**
	 * Sets the tween to the next 100% loop and completes the tween.
	 * The play head update will be a jump.
	 */
	fun finish() {
		setCurrentTime(currentTime + duration - apparentTime(currentTime), jump = true)
		complete()
	}

	/**
	 * Rewinds this tween to [startTime]
	 */
	fun rewind() {
		currentTime = startTime
	}

	/**
	 * Marks this tween as completed. This will leave this tween's progress as it is.
	 *
	 * Use [finish] to first set this tween's progress to 100% and stop.
	 *
	 * If this tween is being driven by the [FrameDriver], it will be stopped.
	 * @see [Updatable.stop]
	 */
	fun complete()

	/**
	 * Given the current time, returns the looped or clamped time.
	 */
	fun apparentTime(value: Double): Double {
		return if (loopAfter && value >= duration || loopBefore && value <= 0.0) {
			com.acornui.math.mod(value, duration)
		} else {
			com.acornui.math.clamp(value, 0.0, duration)
		}
	}

	companion object {

		/**
		 * If set to false, all tweens will [finish] on their first update.
		 */
		var animationsEnabled = true

		/**
		 * Initializes the otherwise lazy-loaded classes so that they are ready the instant they are needed.
		 * This prevents a stutter on the first used animation at the cost of startup time.
		 */
		fun prepare() {
			TweenRegistry; Easing; CallbackTween
		}
	}

}

/**
 * A standard base class for tweens. It handles the signals, easing, and looping. Just override [updateToTime].
 */
abstract class TweenBase() : Tween {

	private val _completed = Signal1<Tween>()
	override val completed = _completed.asRo()

	override var loopBefore: Boolean = false
	override var loopAfter: Boolean = false
	override var allowCompletion = false
	override var paused = false

	override var startTime: Double = 0.0

	protected var ease: Interpolation = Easing.linear

	private var _currentTime: Double = 0.0

	override var currentTime: Double
		get() = _currentTime
		set(newTime) {
			setCurrentTime(newTime, jump = false)
		}

	override fun setCurrentTime(newTime: Double, jump: Boolean) {
		if (!Tween.animationsEnabled) {
			updateToTime(startTime, duration, startTime, duration, true)
			complete()
			return
		}
		val lastTime = _currentTime
		if (lastTime == newTime) return
		_currentTime = newTime

		val apparentLastTime = apparentTime(lastTime)
		val apparentTime = apparentTime(newTime)
		if (apparentLastTime != apparentTime) {
			updateToTime(lastTime, newTime, apparentLastTime, apparentTime, jump)
		}
		if (!jump && completionCheck(lastTime, newTime, apparentLastTime, apparentTime)) {
			complete()
		}

	}

	protected open fun completionCheck(lastTime: Double, time: Double, lastApparentTime: Double, apparentTime: Double): Boolean {
		// Completion check
		if ((!loopBefore || allowCompletion) && apparentTime >= duration && lastApparentTime < duration)
			return true
		else if ((!loopAfter || allowCompletion) && apparentTime <= 0.0 && lastApparentTime > 0.0)
			return true
		return false
	}

	protected open fun ease(x: Double): Double {
		var y = ease.apply(com.acornui.math.clamp(x, 0.0, 1.0))
		if (y < 0.00001 && y > -0.00001) y = 0.0
		if (y < 1.00001 && y > 0.99999) y = 1.0
		return y
	}

	/**
	 * Updates the tween to the given time.
	 * @param lastTime The time of the last update. (Without clamping or looping)
	 * @param newTime The time of the current update. (Without clamping or looping)
	 * @param newTime The time of the current update. (Without clamping or looping)
	 * @param apparentLastTime The time of the last update, clamped and looped.
	 * @param apparentNewTime The time of the current update, clamped and looped.
	 */
	abstract fun updateToTime(lastTime: Double, newTime: Double, apparentLastTime: Double, apparentNewTime: Double, jump: Boolean)

	override fun complete() {
		_completed.dispatch(this)
		stop()
	}

	override fun dispose() {
		_completed.dispose()
	}
}

/**
 *
 */
class TweenImpl(
		duration: Double,
		ease: Interpolation,
		delay: Double,
		loop: Boolean,
		private val tween: (previousAlpha: Double, currentAlpha: Double) -> Unit
) : TweenBase() {

	private var previousAlpha = 0.0

	override val duration = if (duration <= 0.0) 0.0000001 else duration
	override val durationInv = 1.0 / duration

	init {
		this.ease = ease
		this.loopAfter = loop
		startTime = -delay - 0.0000001 // Subtract a small amount so time handlers at 0.0 time get invoked.
		jumpTo(startTime)
	}

	override fun updateToTime(lastTime: Double, newTime: Double, apparentLastTime: Double, apparentNewTime: Double, jump: Boolean) {
		val currentAlpha = ease(apparentNewTime * durationInv)
		tween(previousAlpha, currentAlpha)
		previousAlpha = currentAlpha
	}
}

/**
 * Creates a new Tween.
 * This tween is not automatically started.
 * @see Updatable.start
 */
fun tween(duration: Double, ease: Interpolation, delay: Double = 0.0, loop: Boolean = false, tween: (previousAlpha: Double, currentAlpha: Double) -> Unit): Tween {
	return TweenImpl(duration, ease, delay, loop, tween)
}

fun toFromTween(start: Double, end: Double, duration: Double, ease: Interpolation, delay: Double = 0.0, loop: Boolean = false, setter: (Double) -> Unit): Tween {
	val d = (end - start)
	return TweenImpl(duration, ease, delay, loop) {
		_: Double, currentAlpha: Double ->
		setter(d * currentAlpha + start)
	}
}

fun relativeTween(delta: Double, duration: Double, ease: Interpolation, delay: Double = 0.0, loop: Boolean = false, getter: () -> Double, setter: (Double) -> Unit): Tween {
	return TweenImpl(duration, ease, delay, loop) {
		previousAlpha: Double, currentAlpha: Double ->
		setter(getter() + (currentAlpha - previousAlpha) * delta)
	}
}

fun tweenCall(delay: Double = 0.0, setter: () -> Unit): Tween {
	return TweenImpl(0.0, Easing.linear, delay, false) {
		_: Double, _: Double ->
		setter()
	}
}
