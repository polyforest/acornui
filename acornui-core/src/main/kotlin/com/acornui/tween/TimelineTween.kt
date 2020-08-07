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

import com.acornui.component.ComponentInit
import com.acornui.di.Context
import com.acornui.math.Easing
import com.acornui.math.Interpolation
import com.acornui.tween.Tween.Companion.SMALL_DURATION
import kotlin.time.Duration
import kotlin.time.seconds


/**
 * A Tween timeline that allows for a sequence of tweens.
 */
class TimelineTween(ease: Interpolation, delay: Duration, loop: Boolean) : TweenBase() {

	private val _children = ArrayList<Tween>()
	private val _offsets = ArrayList<Duration>()

	val children: List<Tween>
		get() = _children

	val offsets: List<Duration>
		get() = _offsets

	/**
	 * The duration of the timeline tween can be scaled.
	 */
	var timeScale = 1.0

	override val duration: Duration
		get() {
			var d = SMALL_DURATION
			for (i in 0.._children.lastIndex) {
				val child = _children[i]
				val offset = _offsets[i]
				d = maxOf(d, child.duration + offset - child.startTime)
			}
			return d / timeScale
		}

	init {
		this.ease = ease
		this.loopAfter = loop
		startTime = -delay - SMALL_DURATION // Subtract a small amount so time handlers at 0.0 time get invoked.
		jumpTo(startTime)
	}

	operator fun Tween.unaryPlus() {
		add(this)
	}

	operator fun Tween.unaryMinus() {
		remove(this)
	}

	fun add(tween: Tween, offset: Duration = Duration.ZERO) = add(_children.size, tween, offset)

	/**
	 * Adds a tween to this timeline.
	 */
	fun add(index: Int, tween: Tween, offset: Duration = Duration.ZERO) {
		_children.add(index, tween)
		_offsets.add(index, offset)
	}

	fun remove(tween: Tween): Boolean {
		val index = _children.indexOf(tween)
		if (index != -1) {
			remove(index)
			return true
		}
		return false
	}

	fun remove(index: Int) {
		_children.removeAt(index)
		_offsets.removeAt(index)
	}

	/**
	 * Adds a tween relative to the start of the previous tween.
	 */
	fun stagger(tween: Tween, offset: Duration = 0.25.seconds) {
		val lastTweenOffset = _offsets.lastOrNull() ?: Duration.ZERO
		add(tween, lastTweenOffset + offset)
	}

	/**
	 * Adds a tween relative to the current ending.
	 */
	fun then(tween: Tween, offset: Duration = Duration.ZERO) {
		add(tween, duration + offset)
	}

	override fun updateToTime(lastTime: Duration, newTime: Duration, apparentLastTime: Duration, apparentNewTime: Duration, jump: Boolean) {
		if (_children.isEmpty()) return
		val t = duration * ease(apparentNewTime / duration)

		if (newTime >= lastTime) {
			for (i in 0.._children.lastIndex) {
				val c = _children[i]
				c.setCurrentTime(t - _offsets[i] + c.startTime, jump)
			}
		} else {
			for (i in _children.lastIndex downTo 0) {
				val c = _children[i]
				c.setCurrentTime(t - _offsets[i] + c.startTime, jump)
			}
		}
	}
}

fun timelineTween(ease: Interpolation = Easing.linear, delay: Duration = Duration.ZERO, loop: Boolean = false, inner: ComponentInit<TimelineTween> = {}): TimelineTween {
	val t = TimelineTween(ease, delay, loop)
	t.inner()
	return t
}
