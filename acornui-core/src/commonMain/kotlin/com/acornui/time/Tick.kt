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
import com.acornui.di.Context
import com.acornui.di.own
import com.acornui.start
import com.acornui.stop

/**
 * Invokes [callback] on every time driver tick until disposed.
 */
class Tick(

		override val frameDriver: FrameDriverRo,

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
		private val callback: Disposable.(dT: Float) -> Unit

) : Updatable, Disposable {

	init {
		require(repetitions != 0) { "repetitions argument may not be zero." }
		require(startFrame > 0) { "startFrame must be greater than zero. " }
	}

	private var currentFrame: Int = 0

	override fun update(dT: Float) {
		++currentFrame
		if (currentFrame >= startFrame)
			this.callback(dT)
		if (repetitions >= 0 && currentFrame - startFrame + 1 >= repetitions) {
			dispose()
		}
	}

	override fun dispose() {
		stop()
	}
}

fun Context.callLater(startFrame: Int = 1, callback: Disposable.(dT: Float) -> Unit): Disposable {
	return tick(1, startFrame, callback)
}

/**
 * Constructs a new [Tick] instance and immediately invokes [Updatable.start].
 * When this context is disposed, the tick will be stopped.
 */
fun Context.tick(repetitions: Int = -1, startFrame: Int = 1, callback: Disposable.(dT: Float) -> Unit): Tick {
	return own(Tick(inject(FrameDriverRo), repetitions, startFrame, callback).start())
}
