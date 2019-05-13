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

package com.acornui.core.tween

import com.acornui.math.Easing
import com.acornui.math.Interpolation


/**
 * A Tween timeline that allows for a sequence of tweens.
 */
class TimelineTween(ease: Interpolation, delay: Float, loop: Boolean) : TweenBase() {

	private val _children = ArrayList<Tween>()
	private val _offsets = ArrayList<Float>()

	val children: List<Tween>
		get() = _children

	val offsets: List<Float>
		get() = _offsets

	/**
	 * The duration of the timeline tween can be scaled.
	 */
	var timeScale = 1f

	override val duration: Float
		get() {
			var d = 0.0000001f
			for (i in 0.._children.lastIndex) {
				val child = _children[i]
				val offset = _offsets[i]
				d = maxOf(d, child.duration + offset - child.startTime)
			}
			return d / timeScale
		}

	override val durationInv: Float
		get() = 1f / duration

	init {
		this.ease = ease
		this.loopAfter = loop
		startTime = -delay - 0.0000001f // Subtract a small amount so time handlers at 0f time get invoked.
		jumpTo(startTime)
	}

	fun add(tween: Tween, offset: Float = 0f) = add(_children.size, tween, offset)

	/**
	 * Adds a tween to this timeline.
	 */
	fun add(index: Int, tween: Tween, offset: Float = 0f) {
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
	fun stagger(tween: Tween, offset: Float = 0.25f) {
		val lastTweenOffset = _offsets.lastOrNull() ?: 0f
		add(tween, lastTweenOffset + offset)
	}

	/**
	 * Adds a tween relative to the current ending.
	 */
	fun then(tween: Tween, offset: Float = 0f) {
		add(tween, duration + offset)
	}

	override fun updateToTime(lastTime: Float, newTime: Float, apparentLastTime: Float, apparentNewTime: Float, jump: Boolean) {
		if (_children.isEmpty()) return
		val t = ease(apparentNewTime * durationInv) * duration

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

fun timelineTween(ease: Interpolation = Easing.linear, delay: Float = 0f, loop: Boolean = false): TimelineTween {
	return TimelineTween(ease, delay, loop)
}
