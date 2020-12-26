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
import com.acornui.frame
import com.acornui.math.Easing
import com.acornui.math.Interpolation
import com.acornui.signal.Signal
import com.acornui.signal.unmanagedSignal
import com.acornui.tween.Tween.Companion.SMALL_DURATION
import kotlin.time.Duration
import kotlin.time.nanoseconds
import kotlin.time.seconds

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
	var currentTime: Duration

	/**
	 * Sets the current time.
	 * @param newTime The new time (in seconds) to update to.
	 * @param jump If true, the play head will jump instead of scrub. Callbacks will not be invoked.
	 */
	fun setCurrentTime(newTime: Duration, jump: Boolean)

	/**
	 * Sets the current time.
	 * The play head will jump instead of scrub. Callbacks will not be invoked.
	 * @see setCurrentTime
	 */
	fun jumpTo(newTime: Duration) = setCurrentTime(newTime, true)

	/**
	 * When the tween is rewound using [rewind], the [currentTime] will be set to this value.
	 * This can be set to a negative value to indicate a delay.
	 */
	var startTime: Duration

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
	val duration: Duration

	/**
	 * The current 0-1 progress of the tween.
	 *
	 * If the implementation has a delayed start, this value will start as negative.
	 * @see update
	 */
	var alpha: Double
		get() = currentTime / duration
		set(value) {
			currentTime = duration * value
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
	override fun update(dT: Duration) {
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
	 * Marks this tween as complete. This will leave this tween's progress as it is.
	 *
	 * Use [finish] to first set this tween's progress to 100% and stop.
	 *
	 * This will stop the Tween, removing itself from frame callbacks.
	 */
	fun complete()

	/**
	 * Given the current time, returns the looped or clamped time.
	 */
	fun apparentTime(value: Duration): Duration {
		return if (loopAfter && value >= duration || loopBefore && value <= Duration.ZERO) {
			com.acornui.math.mod(value.inSeconds, duration.inSeconds).seconds
		} else {
			com.acornui.math.clamp(value, Duration.ZERO, duration)
		}
	}

	/**
	 * Starts this tween by adding its [update] method to the frame callback.
	 */
	fun start(): Tween

	fun stop()

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

		val SMALL_DURATION = 1.nanoseconds
	}

}

/**
 * A standard base class for tweens. It handles the signals, easing, and looping. Just override [updateToTime].
 */
abstract class TweenBase : Tween, Disposable {

	override val completed = unmanagedSignal<Tween>()

	override var loopBefore: Boolean = false
	override var loopAfter: Boolean = false
	override var allowCompletion = false
	override var paused = false

	override var startTime: Duration = Duration.ZERO

	protected var ease: Interpolation = Easing.linear

	private var _currentTime: Duration = Duration.ZERO

	override var currentTime: Duration
		get() = _currentTime
		set(newTime) {
			setCurrentTime(newTime, jump = false)
		}

	override fun setCurrentTime(newTime: Duration, jump: Boolean) {
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

	protected open fun completionCheck(lastTime: Duration, time: Duration, lastApparentTime: Duration, apparentTime: Duration): Boolean {
		// Completion check
		if ((!loopBefore || allowCompletion) && apparentTime >= duration && lastApparentTime < duration)
			return true
		else if ((!loopAfter || allowCompletion) && apparentTime <= Duration.ZERO && lastApparentTime > Duration.ZERO)
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
	abstract fun updateToTime(lastTime: Duration, newTime: Duration, apparentLastTime: Duration, apparentNewTime: Duration, jump: Boolean)

	override fun complete() {
		completed.dispatch(this)
		stop()
	}

	private var frameHandle: Disposable? = null

	override fun start(): TweenBase {
		if (frameHandle == null)
			frameHandle = frame.listen(::update)
		return this
	}

	override fun stop() {
		frameHandle?.dispose()
		frameHandle = null
	}

	override fun dispose() {
		completed.dispose()
		stop()
	}
}

/**
 *
 */
class TweenImpl(
		duration: Duration,
		ease: Interpolation,
		delay: Duration,
		loop: Boolean,
		private val tween: (previousAlpha: Double, currentAlpha: Double) -> Unit
) : TweenBase() {

	private var previousAlpha = 0.0

	override val duration = if (duration <= Duration.ZERO) SMALL_DURATION else duration

	init {
		this.ease = ease
		this.loopAfter = loop
		startTime = -delay - SMALL_DURATION // Subtract a small amount so time handlers at 0.0 time get invoked.
		jumpTo(startTime)
	}

	override fun updateToTime(lastTime: Duration, newTime: Duration, apparentLastTime: Duration, apparentNewTime: Duration, jump: Boolean) {
		val currentAlpha = ease(apparentNewTime / duration)
		tween(previousAlpha, currentAlpha)
		previousAlpha = currentAlpha
	}
}

/**
 * Creates a new Tween.
 * This tween is not automatically started.
 * @see TweenImpl.start
 */
fun tween(duration: Duration, ease: Interpolation, delay: Duration = Duration.ZERO, loop: Boolean = false, tween: (previousAlpha: Double, currentAlpha: Double) -> Unit): Tween {
	return TweenImpl(duration, ease, delay, loop, tween)
}

fun toFromTween(start: Double, end: Double, duration: Duration, ease: Interpolation, delay: Duration = Duration.ZERO, loop: Boolean = false, setter: (Double) -> Unit): Tween {
	val d = (end - start)
	return TweenImpl(duration, ease, delay, loop) {
		_: Double, currentAlpha: Double ->
		setter(d * currentAlpha + start)
	}
}

fun relativeTween(delta: Double, duration: Duration, ease: Interpolation, delay: Duration = Duration.ZERO, loop: Boolean = false, getter: () -> Double, setter: (Double) -> Unit): Tween {
	return TweenImpl(duration, ease, delay, loop) {
		previousAlpha: Double, currentAlpha: Double ->
		setter(getter() + (currentAlpha - previousAlpha) * delta)
	}
}

fun tweenCall(delay: Duration = Duration.ZERO, setter: () -> Unit): Tween {
	return TweenImpl(Duration.ZERO, Easing.linear, delay, false) {
		_: Double, _: Double ->
		setter()
	}
}
