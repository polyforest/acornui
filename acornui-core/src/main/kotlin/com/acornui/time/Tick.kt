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

import com.acornui.*
import com.acornui.di.Context
import com.acornui.signal.once

import kotlin.time.Duration

/**
 * Invokes [callback] on every frame until disposed.
 */
class Tick(

		/**
		 * The number of times to invoke the callback.
		 * If this is negative the callback will be invoked indefinitely until disposal.
		 */
		val repetitions: Int = -1,

		/**
		 * How many frames before the callback begins to be invoked.
		 * This should be greater than or equal to 1.
		 */
		val startFrame: Int = 1,

		/**
		 * The callback to invoke, starting at [startFrame] and continues for [repetitions].
		 */
		private val callback: Disposable.(dT: Duration) -> Unit

) : Updatable, Disposable {

	init {
		require(repetitions != 0) { "repetitions argument may not be zero." }
		require(startFrame > 0) { "startFrame must be greater than zero. " }
	}

	private var currentFrame: Int = 0

	override fun update(dT: Duration) {
		++currentFrame
		if (currentFrame >= startFrame)
			this.callback(dT)
		if (repetitions >= 0 && currentFrame - startFrame + 1 >= repetitions) {
			dispose()
		}
	}

	private var handle: Disposable? = null

	fun start(): Tick {
		handle = frame.listen(::update)
		return this
	}

	override fun dispose() {
		handle?.dispose()
		handle = null
	}
}

/**
 * Invokes [callback] on every frame until disposed.
 * Constructs a new [Tick] instance and immediately invokes [Tick.start].
 * When this context is disposed, the tick will be stopped.
 */
fun Context.tick(repetitions: Int = -1, startFrame: Int = 1, callback: Disposable.(dT: Duration) -> Unit): Tick {
	return own(Tick(repetitions, startFrame, callback).start())
}
