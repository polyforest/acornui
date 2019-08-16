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
import com.acornui.UpdatableChildBase
import com.acornui.math.Easing
import com.acornui.math.Interpolation
import com.acornui.math.MathUtils
import com.acornui.recycle.ObjectPool
import com.acornui.signal.Signal
import com.acornui.signal.Signal1
import com.acornui.time.FrameDriver

/**
 * A Tween is an object representing an interpolation over time.
 */
interface Tween {

	/**
	 * Dispatched when the play head has scrubbed past a boundary, either 0f or [duration]. If looping is enabled, this
	 * will not be invoked unless [allowCompletion] is called.
	 */
	val completed: Signal<(Tween) -> Unit>

	/**
	 * The current time of the tween, in seconds.
	 *
	 * If the implementation has a delayed start, this value will start as negative.
	 * Settings this value is the same as calling [setCurrentTime] with jump = false.
	 * @see update
	 */
	var currentTime: Float

	/**
	 * Sets the current time.
	 * @param newTime The new time (in seconds) to update to.
	 * @param jump If true, the play head will jump instead of scrub. Callbacks will not be invoked.
	 */
	fun setCurrentTime(newTime: Float, jump: Boolean)

	/**
	 * Sets the current time.
	 * The play head will jump instead of scrub. Callbacks will not be invoked.
	 * @see setCurrentTime
	 */
	fun jumpTo(newTime: Float) = setCurrentTime(newTime, true)

	/**
	 * When the tween is rewound using [rewind], the [currentTime] will be set to this value.
	 * This can be set to a negative value to indicate a delay.
	 */
	var startTime: Float

	/**
	 * If true, when this tween's [currentTime] <= 0f, it will loop back to [duration].
	 */
	var loopBefore: Boolean

	/**
	 * If true, when this tween's [currentTime] >= [duration], it will loop back to zero.
	 */
	var loopAfter: Boolean

	/**
	 * The total duration of this tween, in seconds.
	 */
	val duration: Float

	/**
	 * 1f / duration.
	 */
	val durationInv: Float

	/**
	 * The current 0-1 progress of the tween.
	 *
	 * If the implementation has a delayed start, this value will start as negative.
	 * @see update
	 */
	var alpha: Float
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
	 * Steps forward [tickTime] seconds.
	 * @param tickTime The number of seconds to progress. This may be negative.
	 */
	fun update(tickTime: Float) {
		if (!paused)
			currentTime += tickTime
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
	 */
	fun complete()

	/**
	 * Given the current time, returns the looped or clamped time.
	 */
	fun apparentTime(value: Float): Float {
		return if (loopAfter && value >= duration || loopBefore && value <= 0f) {
			MathUtils.mod(value, duration)
		} else {
			MathUtils.clamp(value, 0f, duration)
		}
	}

	companion object {

		/**
		 * Initializes the otherwise lazy-loaded classes so that they are ready the instant they are needed.
		 * This prevents a stutter on the first used animation at the cost of startup time.
		 */
		fun prepare() {
			TweenRegistry; Easing; TweenDriver; CallbackTween
		}
	}

}

/**
 * A standard base class for tweens. It handles the signals, easing, and looping. Just override [updateToTime].
 */
abstract class TweenBase : Tween {

	protected val _completed = Signal1<Tween>()
	override val completed = _completed.asRo()

	override var loopBefore: Boolean = false
	override var loopAfter: Boolean = false
	override var allowCompletion = false
	override var paused = false

	override var startTime: Float = 0f

	protected var ease: Interpolation = Easing.linear

	protected var _currentTime: Float = 0f

	override var currentTime: Float
		get() = _currentTime
		set(newTime) {
			setCurrentTime(newTime, jump = false)
		}

	override fun setCurrentTime(newTime: Float, jump: Boolean) {
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

	protected open fun completionCheck(lastTime: Float, time: Float, lastApparentTime: Float, apparentTime: Float): Boolean {
		// Completion check
		if ((!loopBefore || allowCompletion) && apparentTime >= duration && lastApparentTime < duration)
			return true
		else if ((!loopAfter || allowCompletion) && apparentTime <= 0f && lastApparentTime > 0f)
			return true
		return false
	}

	protected open fun ease(x: Float): Float {
		var y = ease.apply(MathUtils.clamp(x, 0f, 1f))
		if (y < 0.00001f && y > -0.00001f) y = 0f
		if (y < 1.00001f && y > 0.99999f) y = 1f
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
	abstract fun updateToTime(lastTime: Float, newTime: Float, apparentLastTime: Float, apparentNewTime: Float, jump: Boolean)

	override fun complete() {
		_completed.dispatch(this)
	}

}

/**
 * A tween instance with pooling.
 * Use [tween] to obtain a new Tween instance.
 * Pooled tweens are recycled on completion. (See [completed] signal)
 */
class TweenImpl(duration: Float, ease: Interpolation, delay: Float, loop: Boolean, tween: (previousAlpha: Float, currentAlpha: Float) -> Unit) : TweenBase() {

	private var tween: ((previousAlpha: Float, currentAlpha: Float) -> Unit)? = null
	private var previousAlpha = 0f

	override val duration = if (duration <= 0f) 0.0000001f else duration
	override val durationInv = 1f / duration

	init {
		this.ease = ease
		this.tween = tween
		this.loopAfter = loop
		startTime = -delay - 0.0000001f // Subtract a small amount so time handlers at 0f time get invoked.
		jumpTo(startTime)
	}

	override fun updateToTime(lastTime: Float, newTime: Float, apparentLastTime: Float, apparentNewTime: Float, jump: Boolean) {
		val currentAlpha = ease(apparentNewTime * durationInv)
		tween!!(previousAlpha, currentAlpha)
		previousAlpha = currentAlpha
	}

	/**
	 * Removes this Tween from the time driver, and recycles this instance. This will leave this tween's progress
	 * as it is.
	 *
	 * After a tween has been stopped, all references to this instance should be forgotten; it will be recycled.
	 *
	 * Use [finish] to first set this tween's progress to 100% and stop.
	 */
	override fun complete() {
		_completed.dispatch(this)
	}
}

/**
 * A tween driver will update a single tween, then remove and recycle itself when the tween is completed.
 */
class TweenDriver private constructor() : UpdatableChildBase(), Disposable {

	private var _tween: Tween? = null

	private val tweenCompletedHandler = {
		t: Tween ->
		remove()
	}

	var tween: Tween?
		get() = _tween
		set(value) {
			_tween?.completed?.remove(tweenCompletedHandler)
			_tween = value
			_tween?.completed?.add(tweenCompletedHandler)
		}

	override fun update(dT: Float) {
		tween?.update(dT)
	}

	override fun dispose() {
		if (tween == null) return
		tween = null
		pool.free(this)
	}

	companion object {
		private val pool = ObjectPool { TweenDriver() }
		fun obtain(tween: Tween): TweenDriver {
			val driver = pool.obtain()
			driver.tween = tween
			return driver
		}
	}
}

/**
 * Creates a tween driver and updates the tween forward until completion.
 */
@Deprecated("use tween.start", ReplaceWith("tween.start()"))
fun <T : Tween> driveTween(tween: T): T {
	val driver = TweenDriver.obtain(tween)
	FrameDriver.addChild(driver)
	return tween
}

/**
 * Creates a tween driver and updates the tween forward until completion.
 */
@Deprecated("Use tween.start", ReplaceWith("this.start()"))
fun <T : Tween> T.drive(any: Any): T {
	val driver = TweenDriver.obtain(this)
	FrameDriver.addChild(driver)
	return this
}

/**
 * Obtains a tween driver and updates the tween forward until completion.
 */
fun <T : Tween> T.start(): T {
	val driver = TweenDriver.obtain(this)
	FrameDriver.addChild(driver)
	return this
}

fun tween(duration: Float, ease: Interpolation, delay: Float = 0f, loop: Boolean = false, tween: (previousAlpha: Float, currentAlpha: Float) -> Unit): Tween {
	return TweenImpl(duration, ease, delay, loop, tween)
}

fun toFromTween(start: Float, end: Float, duration: Float, ease: Interpolation, delay: Float = 0f, loop: Boolean = false, setter: (Float) -> Unit): Tween {
	val d = (end - start)
	val tweenInstance = TweenImpl(duration, ease, delay, loop) {
		previousAlpha: Float, currentAlpha: Float ->
		setter(d * currentAlpha + start)
	}
	return tweenInstance
}

fun relativeTween(delta: Float, duration: Float, ease: Interpolation, delay: Float = 0f, loop: Boolean = false, getter: () -> Float, setter: (Float) -> Unit): Tween {
	val tweenInstance = TweenImpl(duration, ease, delay, loop) {
		previousAlpha: Float, currentAlpha: Float ->
		setter(getter() + (currentAlpha - previousAlpha) * delta)
	}
	return tweenInstance
}

fun tweenCall(delay: Float = 0f, setter: () -> Unit): Tween {
	val tweenInstance = TweenImpl(0f, Easing.linear, delay, false) {
		previousAlpha: Float, currentAlpha: Float ->
		setter()
	}
	return tweenInstance
}
