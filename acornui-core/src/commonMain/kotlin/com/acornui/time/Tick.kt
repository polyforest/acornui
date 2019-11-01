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

package com.acornui.time

import com.acornui.Disposable
import com.acornui.Updatable
import com.acornui.recycle.Clearable
import com.acornui.recycle.ClearableObjectPool
import kotlin.time.Duration
import kotlin.time.seconds

/**
 * @author nbilyk
 */
private class Tick private constructor() : Updatable, Clearable, Disposable {

	/**
	 * How many frames before the callback begins to be invoked.
	 * This should be greater than or equal to 1.
	 */
	private var startFrame = 1

	private var isActive = false

	/**
	 * The number of times to invoke the callback.
	 */
	private var repetitions: Int = 1

	/**
	 * The current frame.
	 */
	private var currentFrame: Int = 0

	/**
	 * The desired amount of time between each tick.
	 */
	private var tickTime: Duration = 1.seconds / 60.0

	/**
	 * The callback to invoke, starting at [startFrame] and continues for [repetitions].
	 */
	private var callback: Disposable.() -> Unit = NOOP

	private var accum = 0.0

	override fun update(dT: Float) {
		accum += dT
		while (accum >= tickTime.inSeconds && isActive) {
			accum -= tickTime.inSeconds
			++currentFrame
			if (currentFrame >= startFrame)
				this.callback()
			if (repetitions >= 0 && currentFrame - startFrame + 1 >= repetitions) {
				dispose()
			}
		}
	}

	override fun clear() {
		accum = 0.0
		tickTime = 1.seconds / 60.0
		startFrame = 1
		repetitions = 1
		currentFrame = 0
		callback = NOOP
	}

	override fun dispose() {
		if (!isActive) return
		isActive = false
		stop()
		pool.free(this)
	}

	companion object {

		private val NOOP: Disposable.() -> Unit = {}

		private val pool = ClearableObjectPool { Tick() }

		internal fun obtain(repetitions: Int = -1, startFrame: Int = 1, tickTime: Duration, callback: Disposable.() -> Unit): Disposable {
			val e = pool.obtain()
			e.repetitions = repetitions
			e.startFrame = startFrame
			e.tickTime = tickTime
			e.callback = callback
			e.isActive = true
			e.start()
			return e
		}
	}
}

fun callLater(startFrame: Int = 1, callback: Disposable.() -> Unit): Disposable {
	return tick(1, startFrame, 1.seconds / 60.0, callback)
}

/**
 * Invokes [callback] on every time driver tick until disposed.
 */
fun tick(repetitions: Int = -1, tickTime: Duration = 1.seconds / 60.0, callback: Disposable.() -> Unit): Disposable = tick(repetitions, 1, tickTime, callback)

/**
 * Invokes [callback] on every time driver tick until disposed.
 * @param repetitions The number of repetitions [callback] should be invoked. If this is a negative value, the
 * callback will be invoked until disposal.
 * @param startFrame How many frames to wait before invoking [callback].
 * @param callback Invoked every frame between [startFrame] and `startFrame + repetitions`. The receiver is the
 * disposable handle, from which this callback can be removed.
 * @return Returns a handle where upon disposal, the callback will be removed.
 */
fun tick(repetitions: Int = -1, startFrame: Int = 1, tickTime: Duration = 1.seconds / 60.0, callback: Disposable.() -> Unit): Disposable {
	require(repetitions != 0) { "repetitions argument may not be zero." }
	require(startFrame > 0) { "startFrame must be greater than zero. "}
	return Tick.obtain(repetitions, startFrame, tickTime, callback)
}
