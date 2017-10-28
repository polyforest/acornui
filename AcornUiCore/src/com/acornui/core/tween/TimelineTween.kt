package com.acornui.core.tween

import com.acornui.math.Easing
import com.acornui.math.Interpolation


/**
 * A Tween timeline that allows for a sequence of tweens.
 */
class TimelineTween(ease: Interpolation, delay: Float, loop: Boolean) : TweenBase() {

	private val children = ArrayList<Tween>()
	private val offsets = ArrayList<Float>()

	/**
	 * The duration of the timeline tween can be scaled.
	 */
	var timeScale = 1f

	private var _duration: Float = 0f

	override val duration: Float
		get() = _duration / timeScale

	private var _durationInv: Float = 0f
	override val durationInv: Float
		get() = _durationInv * timeScale

	init {
		this.ease = ease
		this.loopAfter = loop
		startTime = -delay - 0.0000001f // Subtract a small amount so time handlers at 0f time get invoked.
		jumpTo(startTime)
	}

	/**
	 * Adds a tween relative to 0f.
	 */
	fun add(tween: Tween, offset: Float = 0f) {
		_duration = maxOf(_duration, tween.duration + offset - tween.startTime)
		_durationInv = 1f / _duration
		children.add(tween)
		offsets.add(offset)
	}

	fun remove(tween: Tween): Boolean {
		val index = children.indexOf(tween)
		if (index != -1) {
			children.removeAt(index)
			offsets.removeAt(index)
			return true
		}
		return false
	}

	/**
	 * Adds a tween relative to the start of the previous tween.
	 */
	fun stagger(tween: Tween, offset: Float = 0.25f) {
		val lastTweenOffset = offsets.lastOrNull() ?: 0f
		add(tween, lastTweenOffset + offset)
	}

	/**
	 * Adds a tween relative to the current ending.
	 */
	fun then(tween: Tween, offset: Float = 0f) {
		add(tween, _duration + offset)
	}

	override fun updateToTime(lastTime: Float, newTime: Float, apparentLastTime: Float, apparentNewTime: Float, jump: Boolean) {
		if (children.isEmpty()) return
		val t = ease(apparentNewTime * durationInv) * _duration

		if (newTime >= lastTime) {
			for (i in 0..children.lastIndex) {
				val c = children[i]
				c.setCurrentTime(t - offsets[i] + c.startTime, jump)
			}
		} else {
			for (i in children.lastIndex downTo 0) {
				val c = children[i]
				c.setCurrentTime(t - offsets[i] + c.startTime, jump)
			}
		}
	}
}

fun timelineTween(ease: Interpolation = Easing.linear, delay: Float = 0f, loop: Boolean = false): TimelineTween {
	return TimelineTween(ease, delay, loop)
}